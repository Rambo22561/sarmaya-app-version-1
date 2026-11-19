package com.example

import com.example.data.model.RegistrationValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationLogicTest {

    @Test
    fun `valid registration input without referral code passes`() {
        assertNull(RegistrationValidator.validate("Ayesha Khan", "03001234567", ""))
        assertNull(RegistrationValidator.validate("  Ali Raza ", "+92 300 1234567", "  "))
    }

    @Test
    fun `valid registration input with referral code passes`() {
        assertNull(RegistrationValidator.validate("Bilal Ahmed", "03219876543", "SS12AB34"))
    }

    @Test
    fun `short or blank full name is rejected`() {
        assertNotNull(RegistrationValidator.validate("", "03001234567", ""))
        assertNotNull(RegistrationValidator.validate("A", "03001234567", ""))
    }

    @Test
    fun `invalid phone number is rejected`() {
        assertNotNull(RegistrationValidator.validate("Ayesha Khan", "123", ""))
        assertNotNull(RegistrationValidator.validate("Ayesha Khan", "abc-defg", ""))
    }

    @Test
    fun `malformed referral code is rejected`() {
        assertNotNull(RegistrationValidator.validate("Ayesha Khan", "03001234567", "AB"))
        assertNotNull(RegistrationValidator.validate("Ayesha Khan", "03001234567", "BAD CODE!"))
    }
}
