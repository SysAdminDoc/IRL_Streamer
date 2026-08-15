package com.irlstreamer.reconstruction.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.irlstreamer.reconstruction.model.PopupBounds
import com.irlstreamer.reconstruction.ui.theme.AuditColors
import com.irlstreamer.reconstruction.ui.theme.AuditMetrics

@Composable
fun AuditedDialogHost(
    request: DialogRequest,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember(request.id, request.initialValue) { mutableStateOf(request.initialValue) }
    val selected = remember(request.id, request.selectedOptions) {
        mutableStateListOf<String>().also { it.addAll(request.selectedOptions) }
    }

    if (request.type == DialogType.POPUP_MENU && request.popupBounds != null) {
        AuditedSpinnerPopup(request, request.popupBounds, onDismiss, onConfirm)
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // The dialog window must span the whole display so the modal can be placed
            // against the audited insets instead of whatever inset the host device
            // reports. On the validation AVD the platform-fitted dialog window started
            // at x=131 px, which pushed every modal 28 px right of the audited centre.
            decorFitsSystemWindows = false,
        ),
    ) {
        // Audit evidence: modals centre inside [75 px, 2181 px] horizontally (centre
        // x = 1127.5 px) and inside [83 px, 1080 px] vertically on settings surfaces.
        // The live console hides the status bar, so its modal centres on the full
        // height (screen 142, centre y = 539.5 px).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = AuditMetrics.LeftSafeInset,
                    end = AuditMetrics.NavigationInset,
                    top = if (request.overLiveConsole) 0.dp else AuditMetrics.Dialog.SettingsTopRegionInset,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(AuditMetrics.Dialog.Width)
                    .heightIn(max = AuditMetrics.Dialog.MaxHeight)
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
                        // The audited 76 px title line box is taller than Compose's default
                        // 20 sp leading. The audit device runs fontScale 1.0, where 1 sp
                        // resolves to 1 dp, so the measured dp height is set directly.
                        lineHeight = 27.02.sp,
                        // Audit evidence: alertTitle inset 68 px from both visible modal
                        // edges and 51 px below the visible modal top.
                        modifier = Modifier.padding(
                            start = AuditMetrics.Dialog.TitleHorizontalPadding,
                            end = AuditMetrics.Dialog.TitleHorizontalPadding,
                            top = AuditMetrics.Dialog.TitleTopPadding,
                        ),
                    )

                    when (request.type) {
                        // Audit evidence: choice lists start 150 px below the visible modal
                        // top, i.e. 23 px below the 51+76 px title block, and span the full
                        // modal width with no side inset.
                        DialogType.CHOICE_SINGLE, DialogType.CHOICE_MULTIPLE -> LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.18.dp)
                                .weight(1f, fill = false),
                            // The audited *_menu_middle / *_menu_lower states are scrolled
                            // choice lists; the anchor names the first option the audit
                            // hierarchy shows at the top of the list viewport.
                            state = rememberLazyListState(
                                initialFirstVisibleItemIndex = request.listAnchorIndex,
                            ),
                        ) {
                            items(request.options, key = { it }) { option ->
                                val checked = option in selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(AuditMetrics.Dialog.ListRowHeight)
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
                                        .padding(start = AuditMetrics.Dialog.ListStartInset, end = 24.dp),
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
                                val focusRequester = remember(request.id, index) { FocusRequester() }
                                if (request.focusFieldOnShow && index == 0) {
                                    LaunchedEffect(request.id) {
                                        focusRequester.requestFocus()
                                        keyboard?.show()
                                    }
                                }
                                TextField(
                                    value = fieldValue,
                                    onValueChange = { if (index == 0) input = it else extraValue = it },
                                    // Audit evidence: android:id/edit inset 101 px from the
                                    // modal left edge with a 135 px / 48 dp field height,
                                    // beginning flush with the title block bottom.
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = AuditMetrics.Dialog.FieldStartInset,
                                            end = AuditMetrics.Dialog.FieldStartInset,
                                        )
                                        .heightIn(min = AuditMetrics.Dialog.FieldHeight)
                                        .focusRequester(focusRequester)
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

                        // POPUP_MENU never reaches here; it returns above.
                        DialogType.ALERT, DialogType.ABOUT, DialogType.POPUP_MENU -> Text(
                            text = request.message,
                            color = AuditColors.PrimaryText,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }

                    // Audit evidence: the button row is 135 px / 48 dp tall, sits 56 px above
                    // the modal bottom edge and ends 79 px from the modal right edge.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AuditMetrics.Dialog.ButtonHeight)
                            .padding(end = AuditMetrics.Dialog.ButtonEndGap),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
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

                    Spacer(Modifier.height(AuditMetrics.Dialog.ButtonBottomGap))
                }
            }
        }
    }
}

/**
 * Anchored spinner popup used by audit screens 027 and 112.
 *
 * Unlike the AlertDialogs, these surfaces carry no title and no button bar: the
 * audit hierarchy shows a bare `ListView` of `CheckedTextView` rows placed at an
 * absolute position next to the spinner that opened them. Each row is exactly
 * 135 px / 48 dp tall and the rows begin flush with the popup's top edge, so the
 * popup height is simply row count x 48 dp.
 */
@Composable
private fun AuditedSpinnerPopup(
    request: DialogRequest,
    bounds: PopupBounds,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .offset(x = bounds.leftDp.dp, y = bounds.topDp.dp)
                    .width(bounds.widthDp.dp)
                    .testTag("popup_${request.id}"),
                // Audit evidence: the popup panel samples #303030, matching the
                // settings surface rather than the #424242 AlertDialog surface.
                color = AuditColors.SettingsBackground,
                shape = RoundedCornerShape(0.dp),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                Column {
                    request.options.forEach { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AuditMetrics.Dialog.ListRowHeight)
                                .clickable { onConfirm(option, setOf(option)) }
                                // Audit evidence: screen 027's row label spans x=125..635
                                // inside a popup at x=98..657, i.e. a 27 px start inset and
                                // a 22 px end inset. The insets are asymmetric, and using
                                // 27 px on both sides makes "Default (no authorization)"
                                // wrap where the reference keeps it on one line.
                                .padding(start = 9.6.dp, end = 7.82.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(option, color = AuditColors.PrimaryText, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
