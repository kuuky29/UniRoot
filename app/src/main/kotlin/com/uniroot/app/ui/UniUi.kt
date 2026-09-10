package com.uniroot.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.RunLogFile
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

enum class UniTab(val labelRes: Int) {
    UNIROOT(R.string.tab_uniroot),
    LOGS(R.string.tab_logs),
}

data class UniUiState(
    val deviceName: String = "",
    val kernelRelease: String = "",
    val socName: String = "",
    val kernelSupported: Boolean = false,
    val rooted: Boolean = false,
    val running: Boolean = false,
    val advancedVisible: Boolean = false,
    val shizukuEnabled: Boolean = false,
    val profiles: List<DeviceProfile> = emptyList(),
    val selectedProfileName: String? = null,
    val executionSheetVisible: Boolean = false,
    val executionSheetDismissible: Boolean = false,
    val logLines: List<String> = emptyList(),
    val tab: UniTab = UniTab.UNIROOT,
    val useLatestKsu: Boolean = false,
    val latestKsuTag: String = "",
    val usePatchedKsud: Boolean = false,
    val patchedKsudName: String = "",
    val autoRootOnBoot: Boolean = false,
    val rmgStatus: String = "",
    val runLogs: List<RunLogFile> = emptyList(),
    val logViewerFile: RunLogFile? = null,
    val logViewerContent: String = "",
)

interface UniActions {
    fun onRun()
    fun onCloseExecutionSheet()
    fun onTabSelected(tab: UniTab)
    fun onToggleAdvanced()
    fun onCopyLogs()
    fun onProfileSelected(index: Int)
    fun onShizukuChanged(enabled: Boolean)
    fun onResetProfiles()
    fun onProfileSave(profile: DeviceProfile, originalName: String?)
    fun onProfileDelete(name: String)
    fun onUseLatestKsuChanged(enabled: Boolean)
    fun onDownloadLatestKsu()
    fun onUsePatchedKsudChanged(enabled: Boolean)
    fun onInstallPatchedKsud(file: File)
    fun onAutoRootChanged(enabled: Boolean)
    fun onCheckRmg()
    fun onRunLogOpen(file: RunLogFile)
    fun onRunLogShare(file: RunLogFile)
    fun onRunLogDelete(file: RunLogFile)
    fun onRunLogViewerClose()
}

@Composable
internal fun UniApp(
    state: UniUiState,
    actions: UniActions,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    // Profile manager UI state: null = closed, "" = new profile, else the profile name.
    var profileSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    MiuixTheme(
        colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = "Uni-Root",
                    scrollBehavior = scrollBehavior,
                    actions = {
                        SettingsMenu(
                            advancedVisible = state.advancedVisible,
                            onToggleAdvanced = actions::onToggleAdvanced,
                            onShowAbout = { aboutVisible = true },
                        )
                    },
                )
            },
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (state.tab) {
                        UniTab.UNIROOT -> {
                            val content: @Composable (Modifier) -> Unit = { modifier ->
                                UniRootContent(
                                    state = state,
                                    actions = actions,
                                    scrollBehavior = scrollBehavior,
                                    onOpenProfileSheet = { profileSheetOpen = true },
                                    modifier = modifier,
                                )
                            }
                            if (maxWidth < 768.dp) {
                                content(Modifier.fillMaxSize())
                            } else {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    content(Modifier.fillMaxHeight().weight(1f))
                                }
                            }
                        }
                        UniTab.LOGS -> LogsPage(
                            state = state,
                            actions = actions,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                UniBottomBar(
                    current = state.tab,
                    onSelect = actions::onTabSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            UniExecutionSheet(state = state, actions = actions)
            UniAboutDialog(show = aboutVisible, onDismissRequest = { aboutVisible = false })
            ProfileManagerSheet(
                show = profileSheetOpen,
                profiles = state.profiles,
                onEdit = { profile -> profileSheetOpen = false; editingKey = profile.name },
                onNew = { profileSheetOpen = false; editingKey = "" },
                onDelete = { actions.onProfileDelete(it.name) },
                onDismiss = { profileSheetOpen = false },
            )
            if (editingKey != null) {
                val editing = state.profiles.firstOrNull { it.name == editingKey }
                ProfileEditDialog(
                    profile = editing,
                    onDismiss = { editingKey = null },
                    onSave = { profile, originalName ->
                        actions.onProfileSave(profile, originalName)
                        editingKey = null
                    },
                )
            }
            state.logViewerFile?.let { entry ->
                OverlayDialog(
                    show = true,
                    title = entry.file.name,
                    onDismissRequest = actions::onRunLogViewerClose,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B1220))
                                .padding(12.dp),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = state.logViewerContent,
                                    color = Color(0xFFD1D5DB),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(
                                text = stringResource(R.string.log_share),
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                onClick = { actions.onRunLogShare(entry) },
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                text = stringResource(R.string.action_close),
                                onClick = actions::onRunLogViewerClose,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// UniRoot tab
// ---------------------------------------------------------------------

@Composable
private fun SettingsMenu(
    advancedVisible: Boolean,
    onToggleAdvanced: () -> Unit,
    onShowAbout: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val entry = DropdownEntry(
        items = listOf(
            DropdownItem(
                text = stringResource(R.string.advanced_settings),
                selected = advancedVisible,
                onClick = onToggleAdvanced,
            ),
            DropdownItem(
                text = stringResource(R.string.about),
                onClick = onShowAbout,
            ),
        ),
    )
    OverlayIconDropdownMenu(
        entry = entry,
        modifier = Modifier.size(40.dp),
        onExpandedChange = { expanded -> if (expanded) focusManager.clearFocus() },
    ) {
        Icon(
            imageVector = MiuixIcons.Settings,
            tint = MiuixTheme.colorScheme.onBackground,
            contentDescription = stringResource(R.string.action_advanced),
        )
    }
}

@Composable
private fun UniAboutDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.about),
        onDismissRequest = onDismissRequest,
        content = {
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorResource(R.color.launcher_icon_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.requiredSize(67.dp),
                    )
                }
                Column {
                    Text(text = stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Text(
                        text = stringResource(R.string.made_by),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.view_source) + " ")
                Text(
                    text = AnnotatedString(
                        text = "GhostLock",
                        spanStyle = SpanStyle(textDecoration = TextDecoration.Underline, color = MiuixTheme.colorScheme.primary),
                    ),
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/YuKongA/ghostlock-app") },
                )
            }
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = stringResource(R.string.opensource_info),
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                text = stringResource(R.string.disclaimer),
            )
        },
    )
}

@Composable
private fun UniExecutionSheet(
    state: UniUiState,
    actions: UniActions,
) {
    OverlayBottomSheet(
        show = state.executionSheetVisible,
        title = stringResource(R.string.log_title),
        allowDismiss = state.executionSheetDismissible,
        onDismissRequest = actions::onCloseExecutionSheet,
        startAction = {
            IconButton(onClick = actions::onCopyLogs) {
                Icon(
                    imageVector = MiuixIcons.Copy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        endAction = {
            IconButton(
                enabled = state.executionSheetDismissible,
                onClick = actions::onCloseExecutionSheet,
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        },
        content = {
            LogPanel(
                lines = state.logLines,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 520.dp)
                    .navigationBarsPadding(),
            )
        },
    )
}

@Composable
private fun UniRootContent(
    state: UniUiState,
    actions: UniActions,
    scrollBehavior: ScrollBehavior,
    onOpenProfileSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .overScrollVertical()
            .fillMaxHeight()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "controls") {
            ControlPanel(
                state = state,
                actions = actions,
                onOpenProfileSheet = onOpenProfileSheet,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "run") {
            RunButton(
                running = state.running,
                supported = state.kernelSupported,
                onClick = actions::onRun,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.made_by),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Root My Galaxy style box: timestamp + final result, opens the saved .txt. */
@Composable
private fun RunLogBox(
    entry: RunLogFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = remember(entry.timeMillis) {
        java.text.SimpleDateFormat("MMM d, yyyy · HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(entry.timeMillis))
    }
    val (statusColor, statusLabel) = when (entry.status.lowercase()) {
        "success", "completed" -> Color(0xFF36D167) to "Completed"
        "failed" -> Color(0xFFFF6B6B) to "Failed"
        "crash" -> Color(0xFFF5A623) to "Crash"
        "reboot-required" -> Color(0xFF60A5FA) to "Reboot required"
        "failed-interrupted" -> Color(0xFFFF9F6B) to "Failed (interrupted)"
        else -> Color(0xFFD1D5DB) to entry.status
    }
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = time,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.log_share),
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_delete),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                text = stringResource(R.string.log_view),
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ControlPanel(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ActivationStatusCard(
            rooted = state.rooted,
            supported = state.kernelSupported,
            modifier = Modifier.fillMaxWidth(),
        )
        DeviceInfoCard(
            deviceName = state.deviceName,
            socName = state.socName,
            kernelRelease = state.kernelRelease,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (state.profiles.isNotEmpty()) {
            Card(modifier = modifier.padding(top = 12.dp)) {
                OverlaySpinnerPreference(
                    title = stringResource(R.string.profile_label),
                    items = state.profiles.map { DropdownItem(icon = null, title = it.name) },
                    selectedIndex = state.profiles.indexOfFirst { it.name == state.selectedProfileName },
                    showValue = true,
                    onSelectedIndexChange = actions::onProfileSelected,
                )
            }
        }
        Card(modifier = modifier.padding(top = 12.dp)) {
            SwitchPreference(
                checked = state.shizukuEnabled,
                onCheckedChange = actions::onShizukuChanged,
                title = stringResource(R.string.shizuku_label),
                summary = stringResource(R.string.shizuku_summary),
            )
        }
        AnimatedVisibility(
            visible = state.advancedVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            AdvancedOptions(
                state = state,
                actions = actions,
                onOpenProfileSheet = onOpenProfileSheet,
            )
        }
    }
}

@Composable
private fun ActivationStatusCard(
    rooted: Boolean,
    supported: Boolean,
    modifier: Modifier = Modifier,
) {
    val positive = rooted || supported
    val cardColor = if (isSystemInDarkTheme()) {
        if (positive) Color(0xFF173923) else Color(0xFF3B1715)
    } else {
        if (positive) Color(0xFFDFFAE4) else Color(0xFFF8E2E2)
    }
    val statusIcon = if (positive) Icons.Rounded.CheckCircleOutline else Icons.Rounded.RemoveCircleOutline
    val statusIconColor = if (positive) {
        if (isSystemInDarkTheme()) Color(0xFF62D783) else Color(0xFF36D167)
    } else {
        if (isSystemInDarkTheme()) Color(0xFFFFC56C) else Color(0xFFF5A623)
    }
    val title = stringResource(
        when {
            rooted -> R.string.status_rooted
            supported -> R.string.kernel_supported
            else -> R.string.kernel_unsupported
        }
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = cardColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 14.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusIconColor,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 27.dp, y = 31.dp).size(110.dp),
            )
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceName: String,
    socName: String,
    kernelRelease: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, insideMargin = PaddingValues(16.dp)) {
        SelectionContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DeviceInfoItem(title = stringResource(R.string.device_label), value = deviceName)
                DeviceInfoItem(title = stringResource(R.string.soc_label), value = socName)
                DeviceInfoItem(title = stringResource(R.string.kernel_label), value = kernelRelease)
            }
        }
    }
}

@Composable
private fun DeviceInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
    }
}

// ---------------------------------------------------------------------
// Bottom switcher: UniRoot | Logs
// ---------------------------------------------------------------------

@Composable
private fun UniBottomBar(
    current: UniTab,
    onSelect: (UniTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val entries = listOf(
            UniTab.UNIROOT to stringResource(UniTab.UNIROOT.labelRes),
            UniTab.LOGS to stringResource(UniTab.LOGS.labelRes),
        )
        for ((tab, label) in entries) {
            val selected = tab == current
            val bg = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)
            val fg = if (selected) {
                if (isSystemInDarkTheme()) Color(0xFF0B1220) else Color.White
            } else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------------------------------------------------------------------
// Logs page: current session log + run history boxes
// ---------------------------------------------------------------------

@Composable
private fun LogsPage(
    state: UniUiState,
    actions: UniActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .overScrollVertical()
            .fillMaxHeight()
            .imePadding(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "current-log") {
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.log_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    if (state.logLines.isEmpty()) {
                        Text(
                            text = stringResource(R.string.logs_empty),
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    } else {
                        LogPanel(
                            lines = state.logLines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .heightIn(max = 420.dp),
                        )
                    }
                }
            }
        }
        if (state.runLogs.isNotEmpty()) {
            item(key = "history-title") {
                Text(
                    text = stringResource(R.string.run_history),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
            items(state.runLogs, key = { it.file.absolutePath }) { entry ->
                RunLogBox(
                    entry = entry,
                    onOpen = { actions.onRunLogOpen(entry) },
                    onShare = { actions.onRunLogShare(entry) },
                    onDelete = { actions.onRunLogDelete(entry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Advanced options (UniRoot tab)
// ---------------------------------------------------------------------

@Composable
private fun RunButton(
    running: Boolean,
    supported: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        text = stringResource(if (running) R.string.action_running else R.string.action_run),
        enabled = supported && !running,
        colors = ButtonDefaults.textButtonColorsPrimary(),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AdvancedOptions(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
) {
    Column {
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Root My Galaxy",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.rmgStatus.ifEmpty { stringResource(R.string.rmg_idle) },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
                TextButton(
                    text = stringResource(R.string.rmg_check),
                    onClick = actions::onCheckRmg,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
        TextButton(
            text = stringResource(R.string.edit_profiles),
            onClick = onOpenProfileSheet,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column {
                SwitchPreference(
                    checked = state.useLatestKsu,
                    onCheckedChange = actions::onUseLatestKsuChanged,
                    title = stringResource(R.string.use_latest_ksu),
                    summary = stringResource(R.string.use_latest_ksu_warning),
                )
                if (state.useLatestKsu) {
                    Text(
                        text = if (state.latestKsuTag.isEmpty()) {
                            stringResource(R.string.use_latest_not_downloaded)
                        } else {
                            stringResource(R.string.use_latest_downloaded, state.latestKsuTag)
                        },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    TextButton(
                        text = stringResource(R.string.download_latest_ksu),
                        onClick = actions::onDownloadLatestKsu,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column {
                SwitchPreference(
                    checked = state.usePatchedKsud,
                    onCheckedChange = actions::onUsePatchedKsudChanged,
                    title = stringResource(R.string.use_patched_ksud),
                    summary = stringResource(R.string.use_patched_ksud_summary),
                )
                if (state.usePatchedKsud) {
                    val context = LocalContext.current
                    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        if (uri != null) {
                            val f = copyPickedFile(context, uri, prefix = "ksud-test")
                            if (f == null) Toast.makeText(context, R.string.pick_failed, Toast.LENGTH_SHORT).show()
                            else actions.onInstallPatchedKsud(f)
                        }
                    }
                    Text(
                        text = if (state.patchedKsudName.isEmpty()) stringResource(R.string.patched_ksud_none)
                        else stringResource(R.string.patched_ksud_set, state.patchedKsudName),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    TextButton(
                        text = stringResource(R.string.patched_ksud_choose),
                        onClick = { picker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            SwitchPreference(
                checked = state.autoRootOnBoot,
                onCheckedChange = actions::onAutoRootChanged,
                title = stringResource(R.string.auto_root_on_boot),
                summary = stringResource(R.string.auto_root_on_boot_summary),
            )
        }
        TextButton(
            text = stringResource(R.string.reset_profiles),
            onClick = actions::onResetProfiles,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun LogPanel(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B1220))
            .padding(12.dp),
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                items(lines) { line ->
                    Text(
                        text = line.trimEnd('\r', '\n'),
                        color = toneColor(formatLogTone(line)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

/** GhostLock log semantics (FormatLogUseCase): per-line tone from markers and keywords. */
enum class LogTone { Default, Error, Success, Warning, Progress }

private val ansiRegex = Regex("\\u001B\\[[;\\d]*m")
private val leadingTags = Regex("^(\\[[^]]+\\]\\s*)+")

fun formatLogTone(line: String): LogTone {
    val text = stripAnsi(line)
    val marker = text.getOrNull(1).takeIf { text.startsWith('[') && text.getOrNull(2) == ']' }
    val message = text.replace(leadingTags, "").removePrefix("=== ")
    return when {
        isWriteRound(message) && (marker == '-' || marker == '!') -> LogTone.Error
        isWriteRound(message) -> LogTone.Progress
        marker == '+' -> LogTone.Success
        marker == '-' || marker == '!' -> LogTone.Error
        marker == '*' -> LogTone.Warning
        text.startsWith("error", ignoreCase = true) -> LogTone.Error
        text.startsWith("warning", ignoreCase = true) -> LogTone.Warning
        else -> LogTone.Default
    }
}

private fun isWriteRound(message: String): Boolean =
    listOf("W1", "W2", "W3", "Write 1").any(message::startsWith)

private fun stripAnsi(value: String): String = value.replace(ansiRegex, "")

private fun toneColor(tone: LogTone): Color = when (tone) {
    LogTone.Error -> Color(0xFFFF6B6B)
    LogTone.Success -> Color(0xFF5FD68A)
    LogTone.Warning -> Color(0xFFFFC94D)
    LogTone.Progress -> Color(0xFF60A5FA)
    LogTone.Default -> Color(0xFFD1D5DB)
}

// ---------------------------------------------------------------------
// Profile manager (Advanced → Edit profiles)
// ---------------------------------------------------------------------

@Composable
private fun ProfileManagerSheet(
    show: Boolean,
    profiles: List<DeviceProfile>,
    onEdit: (DeviceProfile) -> Unit,
    onNew: () -> Unit,
    onDelete: (DeviceProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<DeviceProfile?>(null) }
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.edit_profiles),
        allowDismiss = true,
        onDismissRequest = onDismiss,
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(profiles, key = { it.name }) { profile ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = profile.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = fileSummary(profile),
                                modifier = Modifier.padding(top = 4.dp),
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    text = stringResource(R.string.action_edit),
                                    onClick = { onEdit(profile) },
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = stringResource(R.string.action_delete),
                                    onClick = { pendingDelete = profile },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item(key = "new") {
                    TextButton(
                        text = stringResource(R.string.new_profile),
                        onClick = onNew,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
    pendingDelete?.let { victim ->
        OverlayDialog(
            show = true,
            title = stringResource(R.string.action_delete),
            onDismissRequest = { pendingDelete = null },
            content = {
                Text(text = stringResource(R.string.delete_profile_confirm, victim.name))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { pendingDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { onDelete(victim); pendingDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

private fun fileSummary(profile: DeviceProfile): String {
    fun n(path: String) = File(path).name.ifEmpty { "?" }
    val parts = mutableListOf("SO: ${n(profile.pathSo)}", "KO: ${n(profile.pathKo)}", "KSUD: ${n(profile.pathKsud)}")
    if (!profile.pathCveNormal.isNullOrEmpty()) parts.add("CVE: ${n(profile.pathCveNormal)}")
    if (!profile.pathCveRoot.isNullOrEmpty()) parts.add("ROOT: ${n(profile.pathCveRoot!!)}")
    return parts.joinToString("  ·  ")
}

private enum class FileRole { SO, KO, KSUD, CVE_NORMAL, CVE_ROOT }

@Composable
private fun ProfileEditDialog(
    profile: DeviceProfile?,
    onDismiss: () -> Unit,
    onSave: (DeviceProfile, String?) -> Unit,
) {
    val context = LocalContext.current
    val stateKey = profile?.name ?: "__new__"
    var name by remember(stateKey) { mutableStateOf(profile?.name ?: "") }
    var kaslr by remember(stateKey) { mutableStateOf(profile?.kaslrOffset ?: "") }
    var deviceType by remember(stateKey) { mutableStateOf(profile?.deviceType ?: "samsung") }
    var soPath by remember(stateKey) { mutableStateOf(profile?.pathSo ?: "") }
    var koPath by remember(stateKey) { mutableStateOf(profile?.pathKo ?: "") }
    var ksudPath by remember(stateKey) { mutableStateOf(profile?.pathKsud ?: "") }
    var cveNormalPath by remember(stateKey) { mutableStateOf(profile?.pathCveNormal ?: "") }
    var cveRootPath by remember(stateKey) { mutableStateOf(profile?.pathCveRoot ?: "") }
    var pendingRole by remember { mutableStateOf<FileRole?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val role = pendingRole
        pendingRole = null
        if (uri != null && role != null) {
            val dest = copyPickedFile(context, uri)
            if (dest == null) {
                Toast.makeText(context, R.string.pick_failed, Toast.LENGTH_SHORT).show()
            } else {
                when (role) {
                    FileRole.SO -> soPath = dest.absolutePath
                    FileRole.KO -> koPath = dest.absolutePath
                    FileRole.KSUD -> ksudPath = dest.absolutePath
                    FileRole.CVE_NORMAL -> cveNormalPath = dest.absolutePath
                    FileRole.CVE_ROOT -> cveRootPath = dest.absolutePath
                }
            }
        }
    }

    OverlayDialog(
        show = true,
        title = stringResource(if (profile == null) R.string.new_profile else R.string.edit_profile),
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.field_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = kaslr,
                    onValueChange = { kaslr = it },
                    label = stringResource(R.string.field_kaslr),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = stringResource(R.string.field_device_samsung),
                        colors = if (deviceType == "samsung") ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
                        onClick = { deviceType = "samsung" },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.field_device_other),
                        colors = if (deviceType == "oppo") ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
                        onClick = { deviceType = "oppo" },
                        modifier = Modifier.weight(1f),
                    )
                }
                FileField(stringResource(R.string.field_exploit_so), soPath) { pendingRole = FileRole.SO; picker.launch("*/*") }
                FileField(stringResource(R.string.field_ko), koPath) { pendingRole = FileRole.KO; picker.launch("*/*") }
                FileField(stringResource(R.string.field_ksud), ksudPath) { pendingRole = FileRole.KSUD; picker.launch("*/*") }
                FileField(stringResource(R.string.field_cve_normal), cveNormalPath) { pendingRole = FileRole.CVE_NORMAL; picker.launch("*/*") }
                FileField(stringResource(R.string.field_cve_root), cveRootPath) { pendingRole = FileRole.CVE_ROOT; picker.launch("*/*") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.save),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            if (name.isBlank() || soPath.isBlank() || koPath.isBlank() || ksudPath.isBlank()) {
                                Toast.makeText(context, R.string.profile_missing_fields, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val updated = DeviceProfile(
                                name = name.trim(),
                                kaslrOffset = kaslr.trim(),
                                pathSo = soPath,
                                pathKo = koPath,
                                pathKsud = ksudPath,
                                deviceType = deviceType,
                                pathCveNormal = cveNormalPath.ifBlank { null },
                                pathCveRoot = cveRootPath.ifBlank { null },
                            )
                            onSave(updated, profile?.name)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun FileField(
    label: String,
    path: String,
    onPick: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.choose_file),
                onClick = onPick,
                modifier = Modifier.heightIn(min = 32.dp),
            )
        }
        Text(
            text = path.substringAfterLast('/').ifEmpty { stringResource(R.string.no_file) },
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
    }
}

private fun copyPickedFile(context: Context, uri: Uri, prefix: String = ""): File? = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "file.bin"
    val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val dir = File(context.getExternalFilesDir(null), "custom").apply { mkdirs() }
    val dest = File(dir, "${System.currentTimeMillis()}${if (prefix.isNotEmpty()) "-$prefix" else ""}-$safe")
    context.contentResolver.openInputStream(uri)!!.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    dest.setReadable(true, false)
    dest.setExecutable(true, false)
    dest
}.getOrNull()
