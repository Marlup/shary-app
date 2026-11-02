package com.shary.app.security

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Guards the zero-knowledge backup boundary: Android Auto Backup and
 * device-to-device transfer must never copy Shary vault data or metadata.
 */
class BackupPolicyTest {

    @Test
    fun manifestDisablesAndroidBackup() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()
        assertTrue(
            "android:allowBackup must be false",
            Regex("""android:allowBackup\s*=\s*"false"""").containsMatchIn(manifest)
        )
    }

    @Test
    fun backupRulesExcludeEveryAppDomain() {
        val domains = excludeDomains(projectFile("app/src/main/res/xml/backup_rules.xml"))
        assertEquals(
            "backup_rules.xml must exclude every app domain",
            setOf("root", "file", "database", "sharedpref", "external"),
            domains
        )
    }

    @Test
    fun dataExtractionRulesExcludeCloudAndDeviceTransfer() {
        val document = parse(projectFile("app/src/main/res/xml/data_extraction_rules.xml"))
        val expected = setOf("root", "file", "database", "sharedpref", "external")

        for (section in listOf("cloud-backup", "device-transfer")) {
            val element = document.getElementsByTagName(section).item(0) as? Element
                ?: error("Missing $section section")
            val excludes = element.getElementsByTagName("exclude")
            val domains = (0 until excludes.length).map { index ->
                excludes.item(index).attributes.getNamedItem("domain").nodeValue
            }.toSet()
            assertEquals("$section exclusions", expected, domains)
        }
    }

    private fun excludeDomains(file: File): Set<String> {
        val document = parse(file)
        val excludes = document.getElementsByTagName("exclude")
        return (0 until excludes.length).map { index ->
            excludes.item(index).attributes.getNamedItem("domain").nodeValue
        }.toSet()
    }

    private fun parse(file: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path"), File("app/$path"))
            .firstOrNull { it.isFile }
            ?: error("Missing $path from ${File(".").absolutePath}")
}
