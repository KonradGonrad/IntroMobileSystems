package com.example.myapplication

import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.SyncHelper
import org.junit.Assert.*
import org.junit.Test

class CloudSyncTest {
    private val item=Item("one","Demo kettle","Synthetic test description",Category.KITCHEN,Condition.GOOD,"Dorm 5","Demo")
    @Test fun rejectedReservationUsesServerTruth() {
        val local=item.copy(status=ListingStatus.RESERVED,reservedByMe=true,pendingSync=true)
        val remote=item.copy(status=ListingStatus.RESERVED,reservedByMe=false)
        assertEquals(listOf(remote),SyncHelper.mergeCloudItems(listOf(remote),listOf(local),emptySet(),emptySet(),emptySet()))
    }
    @Test fun offlineEditsAndNewDraftsSurviveDownload() {
        val local=item.copy(title="Edited locally",pendingSync=true)
        val draft=item.copy(id="two",isMine=true,pendingSync=true)
        assertEquals(listOf(local,draft),SyncHelper.mergeCloudItems(listOf(item),listOf(local,draft),setOf("one","two"),emptySet(),emptySet()))
    }
    @Test fun confirmedDeletionRemovesStaleCacheButKeepsRejectedDraft() {
        val draft=item.copy(id="two",pendingSync=true)
        assertEquals(listOf(draft),SyncHelper.mergeCloudItems(emptyList(),listOf(item,draft),emptySet(),setOf("two"),emptySet()))
    }
    @Test fun offlineDeletionDoesNotReappearDuringRefresh() {
        assertTrue(SyncHelper.mergeCloudItems(listOf(item),emptyList(),setOf("one"),emptySet(),setOf("one")).isEmpty())
    }
    @Test fun likesOnOtherDeviceAreNotOverwrittenByOldCache() {
        val remote=item.copy(isLiked=true)
        assertEquals(listOf(remote),SyncHelper.mergeCloudItems(listOf(remote),listOf(item),emptySet(),emptySet(),emptySet()))
    }
}
