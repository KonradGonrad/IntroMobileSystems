package com.example.myapplication

import com.example.myapplication.data.auth.AuthValidator
import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {
    @Test fun acceptsOnlyTheAgreedUniversityDomain() {
        assertNull(AuthValidator.validate(" student@edu.p.lodz.pl ", "Password1", "Demo Student", "Password1"))
        listOf("edu@gmail.com", "a@lodz.pl", "a@sub.edu.p.lodz.pl", "a@edu.p.lodz.pl.evil.com", "a@@edu.p.lodz.pl", ".a@edu.p.lodz.pl", "a..b@edu.p.lodz.pl")
            .forEach { assertNotNull(it, AuthValidator.validate(it,"Password1")) }
    }
    @Test fun normalizesCaseAndWhitespaceWithoutChangingPasswords() {
        assertEquals("student@edu.p.lodz.pl", AuthValidator.normalizeEmail(" STUDENT@EDU.P.LODZ.PL "))
        assertNull(AuthValidator.validate("STUDENT@EDU.P.LODZ.PL", "12345678"))
    }
    @Test fun validatesPasswordBoundaryAndConfirmation() {
        assertNotNull(AuthValidator.validate("a@edu.p.lodz.pl", "1234567"))
        assertNull(AuthValidator.validate("a@edu.p.lodz.pl", "12345678"))
        assertNotNull(AuthValidator.validate("a@edu.p.lodz.pl", "Password1", "Student", "password1"))
        assertNotNull(AuthValidator.validate("a@edu.p.lodz.pl", "Password1", " ", "Password1"))
    }
}
