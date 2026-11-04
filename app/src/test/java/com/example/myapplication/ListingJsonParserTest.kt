package com.example.myapplication

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.remote.ListingJsonParser
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingJsonParserTest {

    @Test
    fun parsesListings_andUsesDefaultsForUnknownValues() {
        val json = """
            {"listings": [
              {"id": "1", "title": "Kettle", "category": "KITCHEN", "condition": "LIKE_NEW"},
              {"id": "2", "title": "Lamp", "category": "SPACESHIP", "condition": "???"},
              {"id": "3", "title": ""}
            ]}
        """.trimIndent()

        val items = ListingJsonParser.parse(json)

        assertEquals(2, items.size) // item 3 has no title -> skipped
        assertEquals("server-1", items[0].id)
        assertEquals(Category.KITCHEN, items[0].category)
        assertEquals(Condition.LIKE_NEW, items[0].condition)
        assertEquals(Category.OTHER, items[1].category)
        assertEquals(Condition.GOOD, items[1].condition)
    }

    @Test(expected = JSONException::class)
    fun brokenJson_throwsException() {
        ListingJsonParser.parse("<html>404 not found</html>")
    }
}
