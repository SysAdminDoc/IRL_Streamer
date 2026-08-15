package com.irlstreamer.reconstruction.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun AuditedDialogHost(
    request: DialogRequest,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>) -> Unit,
) {
    var input by remember(request.id, request.initialValue) { mutableStateOf(request.initialValue) }
    val selected = remember(request.id, request.selectedOptions) {
        mutableStateListOf<String>().also { it.addAll(request.selectedOptions) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            Surface(
                modifier = Modifier
                    .width(486.76.dp.coerceAtMost(maxWidth - 32.dp))
                    .testTag("dialog_${request.id}"),
                color = AuditColors.Dialog,
                shape = RoundedCornerShape(2.dp),
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
            ) {
                Column {
                    Text(
                        text = request.title,
                        color = AuditColors.PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp),
                    )

                    when (request.type) {
                        DialogType.CHOICE_SINGLE, DialogType.CHOICE_MULTIPLE -> LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 292.dp),
                        ) {
                            items(request.options, key = { it }) { option ->
                                val checked = option in selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable {
                                            if (request.type == DialogType.CHOICE_SINGLE) {
                                                selected.clear()
                                                selected.add(option)
                                                if (request.dismissOnChoice) onConfirm(option, setOf(option))
                                            } else if (checked) {
                                                selected.remove(option)
                                            } else {
                                                selected.add(option)
                                            }
                                        }
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (checked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = AuditColors.Accent,
                                            modifier = Modifier.width(32.dp),
                                        )
                                    } else {
                                        Spacer(Modifier.width(32.dp))
                                    }
                                    Text(option, color = AuditColors.PrimaryText, fontSize = 16.sp)
                                }
                            }
                        }

                        DialogType.TEXT, DialogType.NUMBER -> {
                            val fields = request.options.ifEmpty { listOf("") }
                            fields.forEachIndexed { index, label ->
                                var extraValue by remember(request.id, index) { mutableStateOf("") }
                                val fieldValue = if (index == 0) input else extraValue
                                TextField(
                                    value = fieldValue,
                                    onValueChange = { if (index == 0) input = it else extraValue = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp)
                                        .testTag("dialog_field_${request.id}_$index"),
                                    label = label.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (request.type == DialogType.NUMBER) KeyboardType.Number else KeyboardType.Uri,
                                    ),
                                    visualTransformation = if (label.contains("key", true) || label.contains("password", true)) {
                                        PasswordVisualTransformation()
                                    } else {
                                        VisualTransformation.None
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = AuditColors.Accent,
                                        cursorColor = AuditColors.Accent,
                                        focusedTextColor = AuditColors.PrimaryText,
                                        unfocusedTextColor = AuditColors.PrimaryText,
                                        focusedLabelColor = AuditColors.Accent,
                                        unfocusedLabelColor = AuditColors.SecondaryText,
                                    ),
                                )
                            }
                        }

                        DialogType.ALERT, DialogType.ABOUT -> Text(
                            text = request.message,
                            color = AuditColors.PrimaryText,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (request.type != DialogType.ABOUT) {
                            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AuditColors.Accent)) {
                                Text(request.negativeLabel.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (request.type != DialogType.CHOICE_SINGLE || !request.dismissOnChoice) {
                            TextButton(
                                onClick = { onConfirm(input, selected.toSet()) },
                                colors = ButtonDefaults.textButtonColors(contentColor = AuditColors.Accent),
                            ) {
                                Text(request.positiveLabel.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        } else if (request.type == DialogType.ALERT || request.type == DialogType.ABOUT || request.type == DialogType.TEXT || request.type == DialogType.NUMBER) {
                            TextButton(
                                onClick = { onConfirm(input, selected.toSet()) },
                                colors = ButtonDefaults.textButtonColors(contentColor = AuditColors.Accent),
                            ) {
                                Text(request.positiveLabel.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
