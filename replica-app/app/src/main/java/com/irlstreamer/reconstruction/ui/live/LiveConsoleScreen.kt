package com.irlstreamer.reconstruction.ui.live

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.R
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.ConsoleTelemetry
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun LiveConsoleScreen(state: AppUiState, viewModel: MainViewModel) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("live_console"),
    ) {
        // The debug-state harness captures screenshots from a warm process and
        // compares them pixel for pixel, so audited states keep the deterministic
        // fixture (D003). Everything else shows the real camera.
        val useFixture = state.runtime.debugScreenId != null
        if (!state.runtime.launchInitializing) {
            if (useFixture) {
                Image(
                    painter = painterResource(R.drawable.preview_fixture),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CameraPreview(
                    cameraId = state.runtime.currentCameraId,
                    onError = viewModel::showToast,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (state.effectiveGrid) GridGuide()
        if (state.effectiveSafeMargins) {
            SafeMarginGuide(state.effectiveSafeMarginIndent, state.settings.safeMarginRatios)
        }
        if (state.effectiveTimestamp) {
            Text(
                text = "Aug 14, 21:07:39",
                color = AuditColors.Timestamp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (state.effectiveAudioMeter) {
            AudioMeter(
                muted = state.runtime.microphoneMuted,
                modifier = Modifier
                    .offset(x = 2.5.dp, y = 219.dp)
                    .size(width = 22.dp, height = 150.dp),
            )
        }

        ConsoleIconButton(
            icon = Icons.Default.Settings,
            description = "Settings",
            modifier = Modifier.offset(x = 0.dp, y = 33.78.dp),
            visualDiameter = 40.dp,
            onClick = { viewModel.navigateTo(SettingsPage.ROOT) },
        )

        ConsoleIconButton(
            icon = Icons.Default.Refresh,
            description = "Reload chat and overlays",
            modifier = Modifier.offset(x = maxWidth - 108.dp, y = (-5.6).dp),
            visualDiameter = 24.dp,
            onClick = viewModel::reload,
        )

        ConsoleIconButton(
            icon = Icons.Default.MoreVert,
            description = "Quick settings",
            modifier = Modifier.offset(x = maxWidth - 108.18.dp, y = 37.96.dp),
            visualDiameter = 40.dp,
            selected = state.runtime.quickPanelOpen,
            onClick = viewModel::toggleQuickPanel,
        )

        TelemetryBlock(
            telemetry = state.effectiveTelemetry,
            modifier = Modifier.offset(x = maxWidth - 214.14.dp, y = 8.dp),
        )

        ConsoleIconButton(
            icon = Icons.Default.CameraAlt,
            description = "Take snapshot (local simulation)",
            modifier = Modifier.offset(x = maxWidth - 108.18.dp, y = 185.51.dp),
            visualDiameter = 40.dp,
            onClick = { viewModel.showToast("Snapshot simulation complete; no media was created") },
        )

        ConsoleIconButton(
            icon = Icons.Default.Cameraswitch,
            description = "Flip camera",
            modifier = Modifier.offset(x = maxWidth - 108.09.dp, y = 239.64.dp),
            visualDiameter = 48.dp,
            // Both front lenses (1 and 3) count as flipped, not only id 1.
            selected = state.runtime.currentCameraId == 1 || state.runtime.currentCameraId == 3,
            onClick = viewModel::flipCamera,
        )

        ConsoleIconButton(
            icon = if (state.runtime.microphoneMuted) Icons.Default.MicOff else Icons.Default.Mic,
            description = if (state.runtime.microphoneMuted) "Unmute microphone" else "Mute microphone",
            modifier = Modifier.offset(x = maxWidth - 120.27.dp, y = 311.73.dp),
            visualDiameter = 72.dp,
            selected = state.runtime.microphoneMuted,
            onClick = viewModel::toggleMicrophone,
        )

        FpsPill(
            modifier = Modifier.offset(x = 248.89.dp, y = maxHeight - 42.04.dp),
        )

        StartButton(
            modifier = Modifier.offset(x = 334.4.dp, y = maxHeight - 48.dp),
            onClick = { viewModel.startBroadcast() },
        )

        LensSelector(
            selectedId = state.runtime.currentCameraId,
            onSelect = viewModel::setCurrentCamera,
            modifier = Modifier.offset(x = maxWidth - 289.07.dp, y = maxHeight - 77.16.dp),
        )

        if (state.runtime.quickPanelOpen) {
            QuickSettingsPanel(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .offset(x = maxWidth - 428.1.dp, y = 42.dp)
                    .size(width = 320.dp, height = 292.dp)
                    .zIndex(4f),
            )
        }
    }
}

@Composable
private fun ConsoleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier,
    visualDiameter: androidx.compose.ui.unit.Dp,
    /** Null for a plain button; true/false only for controls that really have a state. */
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                // Settings, Reload and Snapshot are plain buttons. Announcing
                // "Not selected" on them told a screen-reader user about a state
                // they do not have.
                if (selected != null) {
                    stateDescription = if (selected) "Selected" else "Not selected"
                }
            }
            .testTag(description.lowercase().replace(' ', '_')),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(visualDiameter)
                .clip(CircleShape)
                .background(if (selected == true) Color(0xAA555555) else Color(0x773C3C3C))
                .border(0.5.dp, Color(0x886F6F6F), CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (selected == true) Color.White else Color(0xFF888888),
                modifier = Modifier.size((visualDiameter * 0.55f).coerceAtMost(32.dp)),
            )
        }
    }
}

@Composable
private fun TelemetryBlock(telemetry: ConsoleTelemetry, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(100.dp), horizontalAlignment = Alignment.End) {
        Icon(Icons.Default.BatteryChargingFull, "Battery 73 percent", tint = Color(0xFF15942D), modifier = Modifier.size(width = 42.dp, height = 20.dp))
        // Deterministic sanitised fixtures, not live device readings (D009). Each
        // audited live capture recorded its own values, so they come from the
        // state rather than being pinned to screen 001's reading.
        Text("${telemetry.currentMilliamps} mA", color = Color(0xFF767676), fontSize = 9.sp, lineHeight = 13.5.sp)
        Text("${telemetry.powerMilliwatts} mW", color = Color(0xFF767676), fontSize = 9.sp, lineHeight = 13.5.sp)
        Text("${telemetry.temperatureCelsius} °C", color = Color(0xFF767676), fontSize = 9.sp, lineHeight = 13.5.sp)
    }
}

@Composable
private fun AudioMeter(muted: Boolean, modifier: Modifier = Modifier) {
    val color = if (muted) AuditColors.AudioMuted else AuditColors.AudioActive
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        val widths = listOf(8, 8, 15, 15, 15, 15, 15, 15, 15, 15, 15, 8, 8, 8, 8)
        widths.forEach { width ->
            Spacer(
                modifier = Modifier
                    .width(width.dp)
                    .height(5.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun FpsPill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 56.18.dp, height = 25.96.dp)
            .border(0.7.dp, Color(0xFF777777), RoundedCornerShape(50))
            .background(Color(0x33222222), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text("30 fps", color = Color(0xFF777777), fontSize = 13.sp)
    }
}

@Composable
private fun StartButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.size(width = 80.dp, height = 48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 24.dp)
                .border(2.dp, Color.White, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
                .testTag("start_broadcast"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, "Start broadcast", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LensSelector(
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.width(43.dp),
        ) {
            Box(Modifier.size(width = 43.dp, height = 28.09.dp), contentAlignment = Alignment.CenterEnd) {
                Text("FRONT", color = Color(0xFF555555), fontSize = 13.sp, lineHeight = 18.sp)
            }
            Box(Modifier.size(width = 43.dp, height = 28.09.dp), contentAlignment = Alignment.CenterEnd) {
                Text("BACK", color = Color(0xFF555555), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.width(5.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                LensPill("61°", 3, selectedId, onSelect)
                LensPill("67°", 1, selectedId, onSelect)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                LensPill("73°", 0, selectedId, onSelect, detail = "OIS")
                LensPill("103°", 2, selectedId, onSelect)
            }
        }
    }
}

@Composable
private fun LensPill(
    label: String,
    id: Int,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    detail: String? = null,
) {
    val selected = id == selectedId
    Box(
        modifier = Modifier
            .size(width = 45.87.dp, height = 28.09.dp)
            .border(0.7.dp, if (selected) Color(0xFFAAAAAA) else Color(0xFF484848), RoundedCornerShape(50))
            .background(Color(0x25222222), RoundedCornerShape(50))
            .testTag("lens_$id")
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .clickable(onClick = { onSelect(id) }),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (detail != null) Text(detail, color = Color(0xFF777777), fontSize = 5.sp, lineHeight = 5.sp)
            Text(label, color = if (selected) Color(0xFF999999) else Color(0xFF494949), fontSize = 13.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun GridGuide() {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val stroke = 1.dp.toPx()
                val color = Color.White.copy(alpha = 0.55f)
                drawLine(color, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
                drawLine(color, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
                drawLine(color, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
                drawLine(color, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), stroke)
            },
    )
}

/**
 * Draws one guide per selected safe-margin ratio.
 *
 * "Safe margins ratios" is a persisted multi-select with nine options, but the
 * overlay used to draw a single hardcoded 16:9 rectangle, so choosing 21:9 or
 * several ratios changed nothing on the console. The default selection is a
 * single 16:9 entry, which is what the audited captures show.
 */
@Composable
private fun SafeMarginGuide(indentPercent: Int, ratios: Set<String>) {
    val aspects = remember(ratios) { ratios.mapNotNull(::parseAspectRatio).ifEmpty { listOf(16f / 9f) } }
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val inset = size.minDimension * (indentPercent / 100f)
                val availableWidth = size.width - inset * 2f
                val availableHeight = size.height - inset * 2f
                aspects.forEach { ratio: Float ->
                    val rectWidth: Float
                    val rectHeight: Float
                    if (availableWidth / availableHeight > ratio) {
                        rectHeight = availableHeight
                        rectWidth = rectHeight * ratio
                    } else {
                        rectWidth = availableWidth
                        rectHeight = rectWidth / ratio
                    }
                    drawRect(
                        color = AuditColors.SafeMargin,
                        topLeft = Offset((size.width - rectWidth) / 2f, (size.height - rectHeight) / 2f),
                        size = Size(rectWidth, rectHeight),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            },
    )
}

/** "21:9 (2.33)" -> 2.33. The parenthesised decimal is the audited label's own value. */
internal fun parseAspectRatio(label: String): Float? {
    val decimal = label.substringAfter('(', "").substringBefore(')').toFloatOrNull()
    if (decimal != null && decimal > 0f) return decimal
    val parts = label.substringBefore('(').trim().split(':')
    if (parts.size != 2) return null
    val width = parts[0].trim().toFloatOrNull() ?: return null
    val height = parts[1].trim().toFloatOrNull() ?: return null
    return if (height > 0f) width / height else null
}
