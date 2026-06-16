package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun verifyLogoExists() {
        val destFile = File("src/main/res/drawable/logo.jpg").absoluteFile
        assertTrue("logo.jpg must exist in drawable folder", destFile.exists())
        assertTrue("logo.jpg must be a valid non-empty image file", destFile.length() > 0)
    }
}
