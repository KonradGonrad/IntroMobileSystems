package com.example.myapplication

import com.example.myapplication.data.council.*
import com.example.myapplication.data.model.*
import com.example.myapplication.ui.feed.FeedFilter
import com.example.myapplication.ui.feed.FeedTab
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FairCsvTest {
    private val header = "ref;title;description;category;condition"
    private val sample = "fair-001;Desk lamp;Works perfectly;kitchen;Good condition"
    private fun parse(text: String) = FairCsv.read(text.byteInputStream())

    @Test fun shippedTemplateAcceptsAnExcelRow() {
        val asset = listOf("src/main/assets", "app/src/main/assets")
            .map { File(it, "student-council-template.csv") }.first { it.isFile }.readText()
        assertTrue(asset.startsWith("\uFEFF"))
        assertThrows(IllegalArgumentException::class.java) { parse(asset) }
        val row = parse(asset + sample).single()
        assertEquals("FAIR-001", row.ref)
        assertEquals(Category.KITCHEN, row.category)
        assertEquals(Condition.GOOD, row.condition)
    }

    @Test fun handlesCommaQuotesMultilineUnicodeAndBlankRecords() {
        val row = parse(header.replace(';', ',') + "\r\n\r\nA-1,\"Lamp, blue\",\"Łódź: \"\"blue\"\"\r\nlamp\",OTHER,USED\r\n").single()
        assertEquals("Lamp, blue", row.title)
        assertEquals("Łódź: \"blue\"\r\nlamp", row.description)
    }

    @Test fun rejectsDuplicateReferenceAndInvalidListingWithRowNumber() {
        val duplicate = assertThrows(IllegalArgumentException::class.java) {
            parse("$header\n$sample\n${sample.uppercase()}")
        }
        assertTrue(duplicate.message.orEmpty().contains("Row 3"))
        val invalid = assertThrows(IllegalArgumentException::class.java) { parse("$header\n$sample\nB;A;tiny;WRONG;GOOD") }
        assertTrue(invalid.message.orEmpty().contains("Row 3"))
        assertTrue(invalid.message.orEmpty().contains("category"))
    }

    @Test fun rejectsDamagedFilesAndExcessRows() {
        assertThrows(IllegalArgumentException::class.java) { parse("$header\nA;\"unfinished") }
        assertThrows(IllegalArgumentException::class.java) { parse("title;ref\n$sample") }
        assertThrows(IllegalArgumentException::class.java) { FairCsv.read(byteArrayOf(-1).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { FairCsv.read(ByteArray(2 * 1024 * 1024 + 1).inputStream()) }
        val rows = (1..501).joinToString("\n") { sample.replace("fair-001", "item-$it") }
        assertThrows(IllegalArgumentException::class.java) { parse("$header\n$rows") }
    }

    @Test fun previewCountsOnlyActionableRowsAndShowsPreviousValues() {
        val rows = (1..5).map { parse("$header\n$sample").single().copy(ref = "R$it") }
        val snapshot = JSONArray("""[
            {"ref":"R1","status":"new"},
            {"ref":"R2","id":"id2","status":"changed","previous":{"title":"Old lamp","description":"Old description","category":"OTHER","condition":"USED"}},
            {"ref":"R3","status":"unchanged"},{"ref":"R4","status":"reserved"},{"ref":"R5","status":"deleted"}
        ]""")
        val preview = FairPreview.decode(rows, snapshot)
        assertEquals(1, preview.added)
        assertEquals(1, preview.changed)
        assertEquals(4, preview.skipped(false))
        assertEquals(3, preview.skipped(true))
        assertEquals("Old lamp", preview.matches[1].previous?.title)
    }

    @Test fun councilTabUsesAccountRoleAndIncludesOwnFairItems() {
        val student = Item("1", "Desk lamp", "Works perfectly", Category.OTHER, Condition.GOOD, "Dorm", "Student council")
        val council = student.copy(id = "2", isCouncil = true, isMine = true)
        assertEquals(listOf(council), FeedFilter.filter(listOf(student, council), FeedTab.COUNCIL, null, "lamp"))
    }
}
