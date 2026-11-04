package com.example.myapplication

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.ui.addItem.FormField
import com.example.myapplication.ui.addItem.ListingValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingValidatorTest {

    @Test
    fun emptyForm_hasErrorForEveryField() {
        val errors = ListingValidator.validate("", "", null, null)

        assertEquals(
            setOf(FormField.TITLE, FormField.DESCRIPTION, FormField.CATEGORY, FormField.CONDITION),
            errors.keys
        )
    }

    @Test
    fun correctForm_hasNoErrors() {
        val errors = ListingValidator.validate(
            title = "Electric kettle",
            description = "Working, 1.7 L, pickup on the 2nd floor",
            category = Category.KITCHEN,
            condition = Condition.GOOD
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun spacesOnly_countAsEmpty() {
        val errors = ListingValidator.validate("   ", "          ", Category.BOOKS, Condition.USED)

        assertEquals("Title is required", errors[FormField.TITLE])
        assertEquals("Description is required", errors[FormField.DESCRIPTION])
    }

    @Test
    fun tooShortAndTooLongTitle_areRejected() {
        val tooShort = ListingValidator.validate("ab", "Long enough description", Category.OTHER, Condition.NEW)
        val tooLong = ListingValidator.validate("a".repeat(61), "Long enough description", Category.OTHER, Condition.NEW)

        assertTrue(FormField.TITLE in tooShort)
        assertTrue(FormField.TITLE in tooLong)
    }
}
