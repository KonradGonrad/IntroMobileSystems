package com.example.myapplication.ui.addItem

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition

enum class FormField { TITLE, DESCRIPTION, CATEGORY, CONDITION }

/**
 * Checks the "Add listing" form. Returns a map field -> error message.
 * Empty map means the form is OK.
 */
object ListingValidator {

    const val TITLE_MIN = 3
    const val TITLE_MAX = 60
    const val DESCRIPTION_MIN = 10

    fun validate(
        title: String,
        description: String,
        category: Category?,
        condition: Condition?
    ): Map<FormField, String> {
        val errors = mutableMapOf<FormField, String>()

        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            errors[FormField.TITLE] = "Title is required"
        } else if (cleanTitle.length < TITLE_MIN) {
            errors[FormField.TITLE] = "Title needs at least $TITLE_MIN characters"
        } else if (cleanTitle.length > TITLE_MAX) {
            errors[FormField.TITLE] = "Title can have at most $TITLE_MAX characters"
        }

        val cleanDescription = description.trim()
        if (cleanDescription.isEmpty()) {
            errors[FormField.DESCRIPTION] = "Description is required"
        } else if (cleanDescription.length < DESCRIPTION_MIN) {
            errors[FormField.DESCRIPTION] = "Write at least $DESCRIPTION_MIN characters"
        }

        if (category == null) {
            errors[FormField.CATEGORY] = "Choose a category"
        }
        if (condition == null) {
            errors[FormField.CONDITION] = "Choose the condition"
        }

        return errors
    }
}
