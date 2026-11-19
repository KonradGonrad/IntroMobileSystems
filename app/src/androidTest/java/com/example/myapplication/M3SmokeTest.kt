package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.local.PendingAction
import com.example.myapplication.data.local.UserDatabase
import com.example.myapplication.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class M3SmokeTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val automation=instrumentation.uiAutomation

    @Test fun accountCachesAndOutboxStaySeparateAfterReopen() = runBlocking {
        val a="90000000-0000-0000-0000-000000000001"
        val b="90000000-0000-0000-0000-000000000002"
        context.deleteDatabase("dormswap-$a.db"); context.deleteDatabase("dormswap-$b.db")
        var first=UserDatabase.create(context,a)
        val second=UserDatabase.create(context,b)
        try {
            first.items().upsert(Item("test","Demo book","Synthetic test book",Category.BOOKS,Condition.GOOD,"Dorm 5","Demo"))
            first.cloud().saveAction(PendingAction("op","reserve","test","{}"))
            first.close(); first=UserDatabase.create(context,a)
            assertEquals(1,first.items().count())
            assertEquals(1,first.cloud().pending().size)
            assertEquals(0,second.items().count())
            assertTrue(second.cloud().pending().isEmpty())
        } finally {
            first.close(); second.close()
            context.deleteDatabase("dormswap-$a.db"); context.deleteDatabase("dormswap-$b.db")
        }
    }

    @Test fun loginRegistrationAndInvalidDomainHaveAccessibleControls() {
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor("Create a student account")
        screenshot("01_login")
        clickText("Create a student account")
        waitFor("Repeat password")
        screenshot("02_registration")
        val fields=allNodes().filter { it.isEditable }.sortedBy {
            val rect=android.graphics.Rect();it.getBoundsInScreen(rect);rect.top
        }
        assertEquals("Registration needs four labelled fields",4,fields.size)
        listOf("Demo Student","demo@gmail.com","Password1","Password1").zip(fields).forEach { (value,node) ->
            assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)
            }))
        }
        clickText("Create account")
        waitFor("Use your @edu.p.lodz.pl email address.")
        screenshot("03_invalid_email")
        assertTrue(allNodes().any { it.text?.toString()=="Use your @edu.p.lodz.pl email address." })
    }
    @Test fun loginControlsRemainReachableAt200PercentText() {
        fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val original=shell("settings get system font_scale")
        try {
            shell("settings put system font_scale 2.0")
            context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitFor("DormSwap")
            screenshot("04_large_text_top")
            repeat(5) {
                allNodes().firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Thread.sleep(150)
            }
            assertTrue(allNodes().any { it.text?.toString()=="Create a student account" && it.isVisibleToUser })
            screenshot("05_large_text_actions")
        } finally { shell("settings put system font_scale ${original.takeIf { it!="null" } ?: "1.0"}") }
    }

    @Test fun hostedAccountScreenshots() = runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hosted")=="true")
        val app=context.applicationContext as DormSwapApp
        val suffix=java.util.UUID.randomUUID().toString().take(8)
        app.auth.signIn("m3.screen.$suffix@edu.p.lodz.pl",java.util.UUID.randomUUID().toString(),"Demo Student")
        repeat(100) { if(app.readyUser.value!=app.auth.user.value?.id) kotlinx.coroutines.delay(100) }
        check(app.readyUser.value==app.auth.user.value?.id)
        val title="Demo textbook $suffix"
        val id=app.repository.addListing(title, "A synthetic textbook for the M3 demonstration.",Category.BOOKS,Condition.GOOD,null)
        app.repository.sync()
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor("Messages")
        check(app.repository.syncStatus.value !is com.example.myapplication.data.repository.SyncStatus.Error) { app.repository.syncStatus.value.toString() }
        waitFor("My listings")
        clickText("My listings")
        waitFor(title)
        screenshot("06_authenticated_feed")
        clickText(title)
        waitFor("Edit listing")
        screenshot("07_listing_details")
        clickText("Edit listing")
        waitFor("Save changes")
        screenshot("08_edit_listing")
        val dir=File(context.getExternalFilesDir(null),"m3-screenshots")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p /sdcard/Download/dormswap-m3; cp ${dir.absolutePath}/*.png /sdcard/Download/dormswap-m3/")).use { it.readBytes() }
        app.repository.deleteListing(id)
        app.repository.sync()
        app.auth.signOut()
    }

    private fun allNodes(): List<AccessibilityNodeInfo> {
        val result=mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if(node==null) return
            result+=node
            for(i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(automation.rootInActiveWindow)
        return result
    }
    private fun waitFor(text: String) {
        repeat(100) { attempt ->
            if(attempt>0 && attempt%20==0) allNodes().firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if(allNodes().any { (it.text?.toString()?.contains(text)==true || it.contentDescription?.toString()?.contains(text)==true) }) return
            Thread.sleep(100)
        }
        fail("Missing accessible text: $text; visible: "+allNodes().mapNotNull { it.text?.toString() }.joinToString(" | "))
    }
    private fun clickText(text: String) {
        val candidates=allNodes().filter { (it.text?.toString()?.contains(text)==true || it.contentDescription?.toString()?.contains(text)==true) }
        for(candidate in candidates) {
            var node: AccessibilityNodeInfo?=candidate
            while(node!=null) {
                if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
                node=node.parent
            }
        }
        fail("No clickable control for $text")
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap=automation.takeScreenshot() ?: error("Screenshot unavailable")
        val dir=File(context.getExternalFilesDir(null),"m3-screenshots").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        if(android.os.Build.VERSION.SDK_INT>=29) {
            val values=android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/DormSwapM3")
            }
            val uri=context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
            context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
        bitmap.recycle()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p /sdcard/Download/dormswap-m3; cp ${dir.absolutePath}/$name.png /sdcard/Download/dormswap-m3/")).use { it.readBytes() }
    }
}
