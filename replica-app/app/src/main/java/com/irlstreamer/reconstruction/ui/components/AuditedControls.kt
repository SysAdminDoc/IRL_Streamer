package com.irlstreamer.reconstruction.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.irlstreamer.reconstruction.ui.theme.AuditColors

/** Audit evidence: screen 083 switch_widget is 46.93 x 27.02 dp. */
@Composable
fun AuditedSwitch(
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val width = 46.93.dp
    val height = 27.02.dp
    val thumb = 24.dp
    val inset = 1.51.dp
    val trackColor = when {
        !enabled -> Color(0xFF555555)
        checked -> AuditColors.Accent.copy(alpha = 0.52f)
        else -> Color(0xFF676767)
    }
    val thumbColor = when {
        !enabled -> AuditColors.DisabledText
        checked -> AuditColors.Accent
        else -> Color(0xFFBDBDBD)
    }

    Box(
        modifier = modifier
            .size(width, height)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                stateDescription = if (checked) "On" else "Off"
                if (!enabled) disabled()
            },
    ) {
        // The audited widget's semantic/touch bounds are 27.02 dp high, while
        // the AppCompat visual track is a much thinner capsule behind the thumb.
        Box(
            modifier = Modifier
                .size(width = width, height = 14.dp)
                .align(Alignment.Center)
                .background(trackColor, RoundedCornerShape(percent = 50)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = if (checked) width - thumb - inset else inset)
                .size(thumb)
                .background(thumbColor, RoundedCornerShape(percent = 50)),
        )
    }
}

/** Thin AppCompat-like seek bar with a full semantic/touch surface. */
@Composable
fun AuditedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    touchHeight: Dp = 28.dp,
    trackHeight: Dp = 1.4.dp,
    thumbDiameter: Dp = 12.dp,
) {
    val clamped = value.coerceIn(valueRange)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((clamped - valueRange.start) / span).coerceIn(0f, 1f)
    val interaction = if (valueRange.endInclusive > valueRange.start) {
        Modifier.pointerInput(valueRange, onValueChange) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun update(horizontal: Float) {
                    val normalized = (horizontal / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + normalized * span)
                }
                update(down.position.x)
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    update(change.position.x)
                    if (!change.pressed) break
                    change.consume()
                }
            }
        }
    } else Modifier

    Canvas(
        modifier = modifier
            .height(touchHeight)
            .then(interaction)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, valueRange, steps)
                setProgress { target ->
                    onValueChange(target.coerceIn(valueRange))
                    true
                }
            },
    ) {
        val radius = thumbDiameter.toPx() / 2f
        val centerY = size.height / 2f
        val startX = radius
        val endX = (size.width - radius).coerceAtLeast(startX)
        val thumbX = startX + (endX - startX) * fraction
        drawLine(Color(0xFF777777), Offset(startX, centerY), Offset(endX, centerY), trackHeight.toPx(), StrokeCap.Round)
        drawLine(AuditColors.Accent, Offset(startX, centerY), Offset(thumbX, centerY), trackHeight.toPx(), StrokeCap.Round)
        drawCircle(AuditColors.Accent, radius = radius, center = Offset(thumbX, centerY))
    }
}
