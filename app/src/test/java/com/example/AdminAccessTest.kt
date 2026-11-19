package com.example

import com.example.data.firebase.MarketplaceRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAccessTest {

    @Test
    fun `admin access is strictly restricted to owner email`() {
        assertTrue(MarketplaceRepository.isAdminEmail("khan561salman@gmail.com"))
        assertTrue(MarketplaceRepository.isAdminEmail("  KHAN561SALMAN@GMAIL.COM "))

        assertFalse(MarketplaceRepository.isAdminEmail("investor@gmail.com"))
        assertFalse(MarketplaceRepository.isAdminEmail("khan561salman@yahoo.com"))
        assertFalse(MarketplaceRepository.isAdminEmail(""))
        assertFalse(MarketplaceRepository.isAdminEmail(null))
    }
}
