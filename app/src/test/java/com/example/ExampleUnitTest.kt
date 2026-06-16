package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun verifyLogoExists() {
        val destFile = File("src/main/res/drawable/logo.xml").absoluteFile
        assertTrue("logo.xml must exist in drawable folder", destFile.exists())
        assertTrue("logo.xml must be a valid non-empty XML vector file", destFile.length() > 0)
    }
}
