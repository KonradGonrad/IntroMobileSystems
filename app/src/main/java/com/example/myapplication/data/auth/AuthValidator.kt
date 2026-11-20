package com.example.myapplication.data.auth

import java.util.Locale

object AuthValidator {
    fun normalizeEmail(email: String) = email.trim().lowercase(Locale.ROOT)

    fun validate(email: String, password: String, name: String? = null, repeat: String? = null): String? {
        val address = normalizeEmail(email)
        val local = address.substringBefore('@')
        if (!Regex("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@edu\\.p\\.lodz\\.pl").matches(address) ||
            local.startsWith('.') || local.endsWith('.') || ".." in local || local.length > 64) {
            return "Use your @edu.p.lodz.pl email address."
        }
        if (password.length < 8) return "Password must have at least 8 characters."
        if (password.length > 128) return "Password can have at most 128 characters."
        if (name != null && name.trim().length !in 2..60) return "Display name must have 2–60 characters."
        if (repeat != null && password != repeat) return "Passwords do not match."
        return null
    }
}
