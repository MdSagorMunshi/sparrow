package com.ryanshelby.spw.wallet.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileNameGeneratorTest {

    @Test
    fun testGenerateRandomName_respectsConstraints() {
        for (i in 1..1000) {
            val name = ProfileNameGenerator.generateRandomName()
            assertTrue("Name should not be empty", name.isNotEmpty())
            assertTrue("Name should be max 15 characters, but got $name", name.length <= 15)
            assertTrue("Name should only contain alphabetic characters, but got $name", name.all { it.isLetter() })
            assertTrue("Name should be valid according to isValidName", ProfileNameGenerator.isValidName(name))
        }
    }

    @Test
    fun testIsValidName() {
        assertTrue(ProfileNameGenerator.isValidName("BraveTiger"))
        assertTrue(ProfileNameGenerator.isValidName("SwiftFalcon"))
        assertTrue(!ProfileNameGenerator.isValidName("BraveTiger123")) // Has numbers
        assertTrue(!ProfileNameGenerator.isValidName("Brave_Tiger")) // Has underscore
        assertTrue(!ProfileNameGenerator.isValidName("ThisNameIsWayTooLong")) // > 15 chars
        assertTrue(!ProfileNameGenerator.isValidName("")) // Empty
    }
}
