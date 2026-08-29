package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.settings.SettingAction
import com.irlstreamer.reconstruction.ui.settings.SettingItem
import com.irlstreamer.reconstruction.ui.settings.SettingsCatalog
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The displayed version drifted to "Version 0.2.0" while the build said 0.3.0,
 * because it was a literal in three places. These tests fail if it becomes one
 * again.
 */
class VersionStringTest {
    @Test
    fun settingsHeaderShowsTheBuildVersion() {
        val identity = SettingsCatalog.page(SettingsPage.ROOT).items
            .filterIsInstance<SettingItem.Row>()
            .single { it.id == "identity" }
        assertTrue(
            "settings header summary must carry the build version, was '${identity.summary}'",
            identity.summary.contains(BuildConfig.VERSION_NAME),
        )
    }

    @Test
    fun aboutDialogShowsTheBuildVersion() {
        val about = SettingsCatalog.page(SettingsPage.HELP).items
            .filterIsInstance<SettingItem.Row>()
            .single { it.id == "about" }
        val request = (about.action as SettingAction.Dialog).request
        assertTrue(
            "about dialog must carry the build version, was '${request.message}'",
            request.message.contains(BuildConfig.VERSION_NAME),
        )
    }

    @Test
    fun noHardcodedVersionLiteralRemainsInProductionSource() {
        val literal = Regex("""Version \d+\.\d+\.\d+""")
        val offenders = sourceRoot().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> literal.containsMatchIn(line) }
                    .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
            }
            .toList()
        assertTrue(
            "production source must build version strings from BuildConfig.VERSION_NAME:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /** Unit tests run from the module directory, but do not rely on it. */
    private fun sourceRoot(): File {
        var candidate: File? = File(System.getProperty("user.dir").orEmpty()).absoluteFile
        while (candidate != null) {
            val source = File(candidate, "src/main/java")
            if (source.isDirectory) return source
            candidate = candidate.parentFile
        }
        throw AssertionError("could not locate src/main/java from ${System.getProperty("user.dir")}")
    }
}
