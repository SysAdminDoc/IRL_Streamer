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
        // Any shape of our own version: "Version 0.4", "version v0.4.0", "Version 0.4.0-debug".
        val literal = Regex("""[Vv]ersion\s+v?\d+\.\d+""")
        val offenders = sourceRoots().asSequence().flatMap { it.walkTopDown() }
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

    /**
     * Both shipped source sets. The debug catalog carried the same literal and
     * is what the 145-state capture actually renders, so it is scanned too.
     */
    private fun sourceRoots(): List<File> {
        var candidate: File? = File(System.getProperty("user.dir").orEmpty()).absoluteFile
        while (candidate != null && !File(candidate, "src/main/java").isDirectory) {
            candidate = candidate.parentFile
        }
        val module = requireNotNull(candidate) {
            "could not locate src/main/java from ${System.getProperty("user.dir")}"
        }
        return listOf(File(module, "src/main/java"), File(module, "src/debug/java")).filter { it.isDirectory }
    }
}
