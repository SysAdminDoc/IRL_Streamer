package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.SETTINGS_DATASTORE_BACKUP_PATH
import com.irlstreamer.reconstruction.data.SETTINGS_DATASTORE_DIRECTORY
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings store holds the outgoing connection URL, and an RTMP URL carries
 * the stream key in its path. Auto Backup includes `getFilesDir()` by default,
 * so an exclusion that names the wrong file is the same as no exclusion at all -
 * which is exactly what shipped: the rules excluded `sharedpref/secrets.xml`,
 * a file this app has never written.
 *
 * These assertions read the shipped XML and the repository's own store name, so
 * renaming the store breaks the test rather than silently unprotecting the key.
 */
class BackupRulesTest {

    @Test
    fun cloudBackupExcludesTheStoreThatHoldsTheStreamKey() {
        val rules = readRules("backup_rules.xml")

        assertTrue(
            "backup_rules.xml must exclude $SETTINGS_DATASTORE_BACKUP_PATH, found:\n$rules",
            rules.contains(excludeFor(SETTINGS_DATASTORE_DIRECTORY)),
        )
    }

    @Test
    fun bothCloudBackupAndDeviceTransferExcludeTheStore() {
        val rules = readRules("data_extraction_rules.xml")
        val exclusion = excludeFor(SETTINGS_DATASTORE_DIRECTORY)

        // Android 12+ reads this file instead, and it splits the two ways data
        // leaves a device. Covering only one of them still leaks the key.
        val cloud = rules.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        val transfer = rules.substringAfter("<device-transfer>").substringBefore("</device-transfer>")

        assertTrue("cloud-backup must exclude the settings store, found:\n$cloud", cloud.contains(exclusion))
        assertTrue("device-transfer must exclude the settings store, found:\n$transfer", transfer.contains(exclusion))
    }

    @Test
    fun noRuleNamesAFileTheAppNeverWrites() {
        // The original rules excluded a SharedPreferences file that does not
        // exist. It read as protection and provided none.
        for (name in listOf("backup_rules.xml", "data_extraction_rules.xml")) {
            val rules = readRules(name)
            assertFalse(
                "$name still excludes secrets.xml, which this app never writes",
                rules.contains("secrets.xml"),
            )
            assertFalse(
                "$name excludes a sharedpref path, but settings live in DataStore",
                rules.contains("domain=\"sharedpref\""),
            )
        }
    }

    private fun excludeFor(path: String) = "domain=\"file\" path=\"$path\""

    private fun readRules(fileName: String): String {
        var candidate: File? = File(System.getProperty("user.dir").orEmpty()).absoluteFile
        while (candidate != null && !File(candidate, "src/main/res/xml").isDirectory) {
            candidate = candidate.parentFile
        }
        val module = requireNotNull(candidate) {
            "could not locate src/main/res/xml from ${System.getProperty("user.dir")}"
        }
        val file = File(module, "src/main/res/xml/$fileName")
        assertTrue("missing $fileName at ${file.absolutePath}", file.isFile)
        return file.readText()
    }
}
