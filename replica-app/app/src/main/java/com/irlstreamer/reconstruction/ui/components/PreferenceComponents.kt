package com.irlstreamer.reconstruction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun AuditedAppBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Audit evidence: 145 px / 51.56 dp settings app bar.
            .height(51.56.dp)
            .background(AuditColors.AppBar)
            .testTag("settings_app_bar"),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(width = 56.18.dp, height = 48.dp)
                .align(Alignment.CenterStart)
                .testTag("navigate_up"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate up",
                tint = AuditColors.PrimaryText,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = title,
            color = AuditColors.PrimaryText,
            fontSize = 16.sp,
            lineHeight = 18.84.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 72.18.dp, end = 24.dp)
                .offset(y = 1.78.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.69.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            color = AuditColors.Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 72.18.dp, end = 24.dp),
        )
    }
}

@Composable
fun PreferenceRow(
    id: String,
    title: String,
    summary: String,
    enabled: Boolean,
    accentTitle: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val height = if (summary.isBlank()) 53.69.dp else 72.53.dp
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier,
            )
            .testTag("setting_$id")
            .padding(start = 72.18.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = when {
                    !enabled -> AuditColors.DisabledText
                    accentTitle -> AuditColors.Accent
                    else -> AuditColors.PrimaryText
                },
                fontSize = 16.sp,
                lineHeight = 21.69.sp,
                fontWeight = if (accentTitle) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = if (enabled) AuditColors.SecondaryText else AuditColors.DisabledText,
                    fontSize = 14.sp,
                    lineHeight = 18.84.sp,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
fun TogglePreferenceRow(
    key: String,
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val height = if (summary.isBlank()) 53.69.dp else 72.53.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(enabled = enabled, onClick = onToggle)
            .testTag("setting_$key")
            // 48 dp is occupied by the landscape three-button navigation bar.
            .padding(start = 72.18.dp, end = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) AuditColors.PrimaryText else AuditColors.DisabledText,
                fontSize = 16.sp,
                lineHeight = 21.69.sp,
                maxLines = 2,
            )
            if (summary.isNotBlank()) {
                Text(
                    summary,
                    color = if (enabled) AuditColors.SecondaryText else AuditColors.DisabledText,
                    fontSize = 14.sp,
                    lineHeight = 18.84.sp,
                    maxLines = 3,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        AuditedSwitch(
            checked = checked,
            enabled = enabled,
            modifier = Modifier.testTag("switch_$key"),
        )
    }
}

@Composable
fun SliderPreferenceRow(
    key: String,
    title: String,
    summary: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .testTag("setting_$key")
            .padding(start = 72.18.dp, end = 24.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = AuditColors.PrimaryText, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueLabel, color = AuditColors.SecondaryText, fontSize = 14.sp)
        }
        if (summary.isNotBlank()) {
            Text(summary, color = AuditColors.SecondaryText, fontSize = 12.sp, maxLines = 1)
        }
        AuditedSlider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag("slider_$key"),
        )
    }
}

@Composable
fun InfoRow(id: String, text: String, enabled: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.53.dp)
            .testTag("info_$id"),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = if (enabled) AuditColors.SecondaryText else Color(0xFFBDBDBD),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 4,
            modifier = Modifier.padding(start = 72.18.dp, end = 24.dp),
        )
    }
}

@Composable
fun FormTextField(
    id: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    auditedHeight: androidx.compose.ui.unit.Dp = 53.69.dp,
) {
    var focused by remember { mutableStateOf(false) }
    val floating = focused || value.isNotEmpty()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = TextStyle(
            color = AuditColors.SecondaryText,
            fontSize = 14.sp,
            lineHeight = 18.84.sp,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AuditColors.Accent),
        modifier = Modifier
            .padding(start = 72.18.dp, end = 72.dp)
            .fillMaxWidth()
            .height(auditedHeight)
            .onFocusChanged { focused = it.isFocused }
            .testTag("field_$id"),
        decorationBox = { innerTextField ->
            if (!floating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                    Text(
                        label,
                        color = AuditColors.PrimaryText,
                        fontSize = 16.sp,
                        lineHeight = 21.69.sp,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    Text(
                        label,
                        color = if (focused) AuditColors.Accent else AuditColors.PrimaryText,
                        fontSize = 16.sp,
                        lineHeight = 21.69.sp,
                        modifier = Modifier.height(21.69.dp),
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(18.84.dp), contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                    }
                }
            }
        },
    )
}

@Composable
fun SettingsSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuditColors.SettingsBackground),
    ) { content() }
}
