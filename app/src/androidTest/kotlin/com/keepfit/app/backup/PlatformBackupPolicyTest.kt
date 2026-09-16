package com.keepfit.app.backup

import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class PlatformBackupPolicyTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun installedAppEnablesBackupAndPublishesBothPolicyFormats() {
        assertNotEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertNotEquals(0, policyResourceId(DATA_EXTRACTION_RULES))
        assertNotEquals(0, policyResourceId(FULL_BACKUP_RULES))
    }

    @Test
    fun dataExtractionPolicyAllowListsOnlyDatabaseAndNonSecretDataStore() {
        val rules = parseRules(DATA_EXTRACTION_RULES)
        val expected = ALLOWED_PATHS

        assertEquals(expected, rules.includesFor("cloud-backup"))
        assertEquals(expected, rules.includesFor("device-transfer"))
        assertEquals(emptySet<BackupPath>(), rules.excludes)
    }

    @Test
    fun legacyPolicyUsesTheSameNarrowAllowList() {
        val rules = parseRules(FULL_BACKUP_RULES)

        assertEquals(ALLOWED_PATHS, rules.includesFor("full-backup-content"))
        assertEquals(emptySet<BackupPath>(), rules.excludes)
    }

    private fun policyResourceId(name: String): Int =
        context.resources.getIdentifier(name, "xml", context.packageName)

    private fun parseRules(name: String): ParsedRules {
        val resourceId = policyResourceId(name)
        assertNotEquals("Missing @xml/$name", 0, resourceId)
        val parser = context.resources.getXml(resourceId)
        val includes = mutableSetOf<SectionedBackupPath>()
        val excludes = mutableSetOf<BackupPath>()
        var section: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "cloud-backup", "device-transfer", "full-backup-content" -> section = parser.name
                    "include", "exclude" -> {
                        val path = BackupPath(
                            domain = parser.getAttributeValue(null, "domain"),
                            path = parser.getAttributeValue(null, "path"),
                        )
                        if (parser.name == "include") {
                            includes += SectionedBackupPath(checkNotNull(section), path)
                        } else {
                            excludes += path
                        }
                    }
                }
            } else if (
                parser.eventType == XmlPullParser.END_TAG &&
                parser.name in setOf("cloud-backup", "device-transfer", "full-backup-content")
            ) {
                section = null
            }
            parser.next()
        }

        return ParsedRules(includes = includes, excludes = excludes)
    }

    private data class BackupPath(val domain: String, val path: String)

    private data class SectionedBackupPath(val section: String, val path: BackupPath)

    private data class ParsedRules(
        val includes: Set<SectionedBackupPath>,
        val excludes: Set<BackupPath>,
    ) {
        fun includesFor(section: String): Set<BackupPath> =
            includes.filterTo(mutableSetOf()) { it.section == section }.mapTo(mutableSetOf()) { it.path }
    }

    private companion object {
        const val DATA_EXTRACTION_RULES = "data_extraction_rules"
        const val FULL_BACKUP_RULES = "backup_rules"

        val ALLOWED_PATHS = setOf(
            BackupPath(domain = "database", path = "keepfit.db"),
            BackupPath(domain = "file", path = "datastore/keepfit_settings.preferences_pb"),
        )
    }
}
