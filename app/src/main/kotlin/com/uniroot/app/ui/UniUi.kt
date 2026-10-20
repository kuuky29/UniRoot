@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.uniroot.app.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.RunLogFile
import com.uniroot.app.newmethod.KsudClassicProfiles
import com.uniroot.app.newmethod.KsudNextProfile
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
    val autoRootOnBoot: Boolean = false,
    val rmgStatus: String = "",
    val ksuNextMode: Boolean = false,
    val runLogs: List<RunLogFile> = emptyList(),
    val logViewerFile: RunLogFile? = null,
    val logViewerContent: String = "",
    val instantRootEnabled: Boolean = false,
    val instantRootInstalled: Boolean = false,
    val instantRootOfferVisible: Boolean = false,
    val keepInstantRootVisible: Boolean = false,
    val instantRootInstallVisible: Boolean = false,
    val instantRootLogVisible: Boolean = false,
    val instantRootLogContent: String = "",
    val superuserNeededVisible: Boolean = false,
    val injectConfirmVisible: Boolean = false,
    // NEW METHOD (fast, DFRoot engine): fully separated from the old flow.
    val newMethod: Boolean = false,
    val newMethodBootEnabled: Boolean = false,
    val newMethodAutoSoftReboot: Boolean = true,
    val newMethodProgress: Int = 0,
    val newMethodStage: String = "",
    // New méthode + KernelSU Next : profil ksud embarqué choisi (3.3.0, 3.4.0…).
    val ksudNextProfiles: List<KsudNextProfile> = emptyList(),
    val ksudNextProfileId: String = "",
    // New méthode + KernelSU classic : profil classic (bundled / téléchargé).
    val ksudClassicProfiles: List<KsudClassicProfiles.ClassicProfile> = emptyList(),
    val ksudClassicProfileId: String = "",
    // DEV — updater ksud Next (télécharge + patche + crée un profil).
    val ksudUpdateRunning: Boolean = false,
    val ksudUpdateLog: String = "",
    val ksudUpdateSheetVisible: Boolean = false,
    val ksudUpdateProgress: Int = 0,
    val ksudUpdateStage: String = "",

    val deviceInfoRows: List<Pair<String, String>> = emptyList(),
    val rootStateRow: String = "",
    val rootDetailRow: String = "",
    val selinuxRow: String = "",
)

interface UniActions {
    fun onRun()
    fun onDisableRoot()
    fun onInstantRootEnable()
    fun onInstantRootNotNow()
    fun onKeepInstantRoot(keep: Boolean)
    fun onInstantRootEnabledChanged(enabled: Boolean)
    fun onUninstallHelper()
    fun onInstantRootInstallNow()
    fun onInstantRootInstallLater()
    fun onOpenInstantRootLog()
    fun onCloseInstantRootLog()
    fun onOpenKsuManager()
    fun onDismissSuperuserNeeded()
    fun onInjectContinue()
    fun onInjectCancel()
    fun onOpenHelper()
    fun onCloseExecutionSheet()
    fun onTabSelected(tab: UniTab)
    fun onToggleAdvanced()
    fun onCopyLogs()
    fun onProfileSelected(index: Int)
    fun onShizukuChanged(enabled: Boolean)
    fun onKsuFlavorChanged(next: Boolean)
    fun onResetProfiles()
    fun onProfileSave(profile: DeviceProfile, originalName: String?)
    fun onProfileDelete(name: String)
    fun onUseLatestKsuChanged(enabled: Boolean)
    fun onDownloadLatestKsu()
    fun onAutoRootChanged(enabled: Boolean)
    fun onCheckRmg()
    fun onRunLogOpen(file: RunLogFile)
    fun onRunLogShare(file: RunLogFile)
    fun onRunLogDelete(file: RunLogFile)
    fun onRunLogViewerClose()
    fun onRootMethodChanged(newMethod: Boolean)
    fun onNewMethodBootChanged(enabled: Boolean)
    fun onNewMethodSoftRebootChanged(enabled: Boolean)
    fun onKsudNextProfileChanged(id: String)
    fun onUpdateKsud()
    fun onCloseKsudUpdateSheet()
    fun onDeleteKsudProfile(id: String, classic: Boolean)
    fun onKsudClassicProfileChanged(id: String)
}

// ---------------------------------------------------------------------
// Material 3 Expressive Theme & Segmented Shape Helpers
// ---------------------------------------------------------------------

private enum class SegmentPosition { SINGLE, TOP, MIDDLE, BOTTOM }

@Composable
private fun segmentedCardShape(position: SegmentPosition): Shape = when (position) {
    SegmentPosition.SINGLE -> RoundedCornerShape(24.dp)
    SegmentPosition.TOP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    SegmentPosition.MIDDLE -> RoundedCornerShape(6.dp)
    SegmentPosition.BOTTOM -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
}

@Composable
private fun UniExpressiveTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = remember(context, darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }.getOrElse {
                if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
            }
        } else {
            if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.surface.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
        }
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(),
        typography = Typography(),
        content = content,
    )
}

@Composable
internal fun UniApp(
    state: UniUiState,
    actions: UniActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    var profileSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }

    UniExpressiveTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                MediumFlexibleTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (state.rooted) Icons.Rounded.Verified else Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMediumEmphasized,
                            )
                        }
                    },
                    subtitle = {
                        val subtitleText = when (state.tab) {
                            UniTab.UNIROOT -> {
                                val method = if (state.newMethod) stringResource(R.string.method_new) else stringResource(R.string.method_old)
                                val flavor = if (state.ksuNextMode) stringResource(R.string.flavor_ksunext) else stringResource(R.string.flavor_kernelsu)
                                "$method · $flavor"
                            }
                            UniTab.LOGS -> {
                                if (state.runLogs.isNotEmpty()) {
                                    "${stringResource(R.string.log_title)} · ${state.runLogs.size}"
                                } else {
                                    stringResource(R.string.log_title)
                                }
                            }
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    actions = {
                        SettingsMenu(
                            advancedVisible = state.advancedVisible,
                            onToggleAdvanced = actions::onToggleAdvanced,
                            onShowAbout = { aboutVisible = true },
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            },
            bottomBar = {
                UniBottomBar(
                    current = state.tab,
                    logCount = state.runLogs.size,
                    hasSessionLogs = state.logLines.isNotEmpty(),
                    onSelect = actions::onTabSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = state.tab,
                    transitionSpec = {
                        fadeIn(animationSpec = fadeInSpec) togetherWith
                            fadeOut(animationSpec = fadeOutSpec)
                    },
                    label = "tabTransition",
                ) { currentTab ->
                    when (currentTab) {
                        UniTab.UNIROOT -> {
                            val content: @Composable (Modifier) -> Unit = { modifier ->
                                UniRootContent(
                                    state = state,
                                    actions = actions,
                                    scrollBehavior = scrollBehavior,
                                    onOpenProfileSheet = { profileSheetOpen = true },
                                    onAddProfile = { editingKey = "" },
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
            }

            UniExecutionSheet(state = state, actions = actions)
            UniKsudUpdateSheet(state = state, actions = actions)
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
            if (state.instantRootOfferVisible) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.instant_root_offer_title),
                    body = stringResource(R.string.instant_root_offer_body),
                    icon = Icons.Rounded.Bolt,
                    polygon = MaterialShapes.Sunny,
                    confirmText = stringResource(R.string.instant_root_enable),
                    dismissText = stringResource(R.string.instant_root_not_now),
                    onConfirm = actions::onInstantRootEnable,
                    onDismiss = actions::onInstantRootNotNow,
                )
            }
            if (state.keepInstantRootVisible) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.instant_root_keep_title),
                    body = stringResource(R.string.instant_root_keep_body),
                    icon = Icons.Rounded.PowerSettingsNew,
                    polygon = MaterialShapes.Cookie9Sided,
                    confirmText = stringResource(R.string.instant_root_keep_yes),
                    dismissText = stringResource(R.string.instant_root_keep_no),
                    onConfirm = { actions.onKeepInstantRoot(true) },
                    onDismiss = { actions.onKeepInstantRoot(false) },
                    onDismissRequest = { actions.onKeepInstantRoot(true) },
                )
            }
            if (state.instantRootInstallVisible) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.instant_root_install_title),
                    body = stringResource(R.string.instant_root_install_body),
                    icon = Icons.Rounded.Download,
                    polygon = MaterialShapes.Clover8Leaf,
                    confirmText = stringResource(R.string.instant_root_install_now),
                    dismissText = stringResource(R.string.instant_root_install_later),
                    onConfirm = actions::onInstantRootInstallNow,
                    onDismiss = actions::onInstantRootInstallLater,
                )
            }
            if (state.instantRootLogVisible) {
                AlertDialog(
                    onDismissRequest = actions::onCloseInstantRootLog,
                    shape = MaterialTheme.shapes.extraLarge,
                    icon = {
                        ExpressivePolygonBadge(
                            polygon = MaterialShapes.Cookie6Sided,
                            icon = Icons.Rounded.Terminal,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            size = 48.dp,
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.instant_root_log),
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                            textAlign = TextAlign.Center,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B1220))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = state.instantRootLogContent,
                                    color = Color(0xFFD1D5DB),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        FilledTonalButton(
                            onClick = actions::onCloseInstantRootLog,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.action_close),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }
                    },
                )
            }
            if (state.superuserNeededVisible) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.superuser_needed_title),
                    body = stringResource(R.string.superuser_needed_body),
                    icon = Icons.Rounded.Security,
                    polygon = MaterialShapes.SoftBurst,
                    confirmText = stringResource(R.string.superuser_needed_open),
                    dismissText = stringResource(R.string.cancel),
                    onConfirm = actions::onOpenKsuManager,
                    onDismiss = actions::onDismissSuperuserNeeded,
                )
            }
            if (state.injectConfirmVisible) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.instant_root_inject_title),
                    body = stringResource(R.string.instant_root_inject_body),
                    icon = Icons.Rounded.RocketLaunch,
                    polygon = MaterialShapes.Sunny,
                    confirmText = stringResource(R.string.instant_root_inject_continue),
                    dismissText = stringResource(R.string.cancel),
                    onConfirm = actions::onInjectContinue,
                    onDismiss = actions::onInjectCancel,
                )
            }
            state.logViewerFile?.let { entry ->
                AlertDialog(
                    onDismissRequest = actions::onRunLogViewerClose,
                    shape = MaterialTheme.shapes.extraLarge,
                    icon = {
                        ExpressivePolygonBadge(
                            polygon = MaterialShapes.Cookie7Sided,
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 48.dp,
                        )
                    },
                    title = {
                        Text(
                            text = entry.file.name,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            textAlign = TextAlign.Center,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B1220))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
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
                    },
                    confirmButton = {
                        Button(
                            onClick = { actions.onRunLogShare(entry) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.log_share),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }
                    },
                    dismissButton = {
                        FilledTonalButton(
                            onClick = actions::onRunLogViewerClose,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(
                                text = stringResource(R.string.action_close),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpressivePolygonBadge(
    polygon: RoundedPolygon,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(polygon.toShape())
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun ExpressiveConfirmDialog(
    title: String,
    body: String,
    icon: ImageVector,
    polygon: RoundedPolygon,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissRequest: () -> Unit = onDismiss,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            ExpressivePolygonBadge(
                polygon = polygon,
                icon = icon,
                containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                size = 52.dp,
                iconSize = 26.dp,
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    text = dismissText,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        },
    )
}

// ---------------------------------------------------------------------
// Top App Bar Menu & About Dialog
// ---------------------------------------------------------------------

@Composable
private fun SettingsMenu(
    advancedVisible: Boolean,
    onToggleAdvanced: () -> Unit,
    onShowAbout: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(end = 8.dp)) {
        FilledTonalIconButton(
            onClick = {
                focusManager.clearFocus()
                expanded = true
            },
            shapes = IconButtonDefaults.shapes(),
            colors = if (advancedVisible) {
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.action_advanced),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.advanced_settings),
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = if (advancedVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (advancedVisible) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onToggleAdvanced()
                },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    expanded = false
                    onShowAbout()
                },
            )
        }
    }
}

@Composable
private fun UniAboutDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (!show) return
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(colorResource(R.color.launcher_icon_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.requiredSize(76.dp),
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.made_by),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = { uriHandler.openUri("https://github.com/YuKongA/ghostlock-app") },
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.view_source),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = AnnotatedString(
                                text = "GhostLock",
                                spanStyle = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Launch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.opensource_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.disclaimer),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    text = stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        },
    )
}

// ---------------------------------------------------------------------
// Bottom Sheets (ksud Updater & Execution Sheet)
// ---------------------------------------------------------------------

@Composable
private fun UniKsudUpdateSheet(
    state: UniUiState,
    actions: UniActions,
) {
    if (!state.ksudUpdateSheetVisible) return
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !state.ksudUpdateRunning },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!state.ksudUpdateRunning) actions.onCloseKsudUpdateSheet() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExpressivePolygonBadge(
                    polygon = MaterialShapes.Clover8Leaf,
                    icon = Icons.Rounded.CloudDownload,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 42.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.ksuNextMode) "Update ksud Next" else "Update ksud classic",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(
                    enabled = !state.ksudUpdateRunning,
                    onClick = actions::onCloseKsudUpdateSheet,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.ksudUpdateRunning) {
                ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                ExpressivePolygonBadge(
                    polygon = MaterialShapes.Sunny,
                    icon = Icons.Rounded.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    size = 56.dp,
                    iconSize = 30.dp,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = state.ksudUpdateStage.ifBlank { "Preparing…" },
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            val barAnim by animateFloatAsState(
                targetValue = state.ksudUpdateProgress.coerceIn(0, 100) / 100f,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "ksudUpdateBar",
            )
            LinearWavyProgressIndicator(
                progress = { barAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = "${state.ksudUpdateProgress}%",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            if (state.ksudUpdateLog.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFF0B1220),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = state.ksudUpdateLog,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFD1D5DB),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UniExecutionSheet(
    state: UniUiState,
    actions: UniActions,
) {
    if (!state.executionSheetVisible) return
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { state.executionSheetDismissible },
    )
    ModalBottomSheet(
        onDismissRequest = { if (state.executionSheetDismissible) actions.onCloseExecutionSheet() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(
                    onClick = actions::onCopyLogs,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                    )
                }
                Text(
                    text = stringResource(R.string.log_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(
                    enabled = state.executionSheetDismissible,
                    onClick = actions::onCloseExecutionSheet,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.newMethod) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (state.running) {
                        ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                    } else {
                        val success = state.newMethodProgress >= 100
                        ExpressivePolygonBadge(
                            polygon = if (success) MaterialShapes.Sunny else MaterialShapes.SoftBurst,
                            icon = if (success) Icons.Rounded.Verified else Icons.Rounded.ErrorOutline,
                            containerColor = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            size = 64.dp,
                            iconSize = 34.dp,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = state.newMethodStage.ifBlank { "Preparing…" },
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val barAnim by animateFloatAsState(
                        targetValue = state.newMethodProgress.coerceIn(0, 100) / 100f,
                        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                        label = "newMethodBar",
                    )
                    LinearWavyProgressIndicator(
                        progress = { barAnim },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${state.newMethodProgress}%",
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                if (state.running) {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }
                LogPanel(
                    lines = state.logLines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 500.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// UniRoot Tab Content
// ---------------------------------------------------------------------

@Composable
private fun UniRootContent(
    state: UniUiState,
    actions: UniActions,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "controls") {
            ControlPanel(
                state = state,
                actions = actions,
                onOpenProfileSheet = onOpenProfileSheet,
                onAddProfile = onAddProfile,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "made") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape,
                ) {
                    Text(
                        text = stringResource(R.string.made_by),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMediumEmphasized,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ActivationStatusCard(
            rooted = state.rooted,
            supported = state.kernelSupported,
            modifier = Modifier.fillMaxWidth(),
            liveRootState = state.rootStateRow,
            liveRootDetail = state.rootDetailRow,
            selinux = state.selinuxRow,
        )

        DeviceInfoCard(
            deviceName = state.deviceName,
            socName = state.socName,
            kernelRelease = state.kernelRelease,
            modifier = Modifier.fillMaxWidth(),
            infoRows = state.deviceInfoRows,
        )

        // Connected M3 Expressive toggle groups for Method & Flavor
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MethodSwitch(
                    newMethod = state.newMethod,
                    onSelect = actions::onRootMethodChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlavorSwitch(
                    next = state.ksuNextMode,
                    onSelect = actions::onKsuFlavorChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!state.newMethod) {
            if (state.profiles.isNotEmpty()) {
                ExpressiveSpinnerPreference(
                    title = stringResource(R.string.profile_label),
                    items = state.profiles.map { it.name },
                    selectedIndex = state.profiles.indexOfFirst { it.name == state.selectedProfileName },
                    icon = Icons.Rounded.Tune,
                    polygon = MaterialShapes.Cookie7Sided,
                    onSelectedIndexChange = actions::onProfileSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FilledTonalButton(
                onClick = onAddProfile,
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
                Text(
                    text = stringResource(R.string.action_add_profile),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
            }

            RunButton(
                rooted = state.rooted,
                running = state.running,
                supported = state.kernelSupported,
                onRun = actions::onRun,
                modifier = Modifier.fillMaxWidth(),
            )

            ExpressiveSwitchCard(
                checked = state.shizukuEnabled,
                onCheckedChange = actions::onShizukuChanged,
                title = stringResource(R.string.shizuku_label),
                summary = stringResource(R.string.shizuku_summary),
                icon = Icons.Rounded.Terminal,
                polygon = MaterialShapes.Cookie6Sided,
                position = SegmentPosition.SINGLE,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // NEW METHOD — Expressive ksud updater button
            FilledTonalButton(
                onClick = { actions.onUpdateKsud() },
                enabled = !state.ksudUpdateRunning,
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                if (state.ksudUpdateRunning) {
                    LoadingIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                    )
                }
                Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
                Text(
                    text = when {
                        state.ksudUpdateRunning -> "Updating ksud…"
                        state.ksuNextMode -> "Update ksud Next"
                        else -> "Update ksud classic"
                    },
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
            }

            RunButton(
                rooted = state.rooted,
                running = state.running,
                supported = true,
                onRun = actions::onRun,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.ksuNextMode && state.ksudNextProfiles.isNotEmpty()) {
                ExpressiveSpinnerPreference(
                    title = stringResource(R.string.ksud_next_profile_label),
                    items = state.ksudNextProfiles.map { it.label },
                    selectedIndex = state.ksudNextProfiles
                        .indexOfFirst { it.id == state.ksudNextProfileId }
                        .coerceAtLeast(0),
                    icon = Icons.Rounded.AutoAwesome,
                    polygon = MaterialShapes.Clover8Leaf,
                    onSelectedIndexChange = { index ->
                        actions.onKsudNextProfileChanged(state.ksudNextProfiles[index].id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!state.ksuNextMode && state.ksudClassicProfiles.isNotEmpty()) {
                ExpressiveSpinnerPreference(
                    title = stringResource(R.string.ksud_next_profile_label),
                    items = state.ksudClassicProfiles.map { it.label },
                    selectedIndex = state.ksudClassicProfiles
                        .indexOfFirst { it.id == state.ksudClassicProfileId }
                        .coerceAtLeast(0),
                    icon = Icons.Rounded.Shield,
                    polygon = MaterialShapes.Cookie9Sided,
                    onSelectedIndexChange = { index ->
                        actions.onKsudClassicProfileChanged(state.ksudClassicProfiles[index].id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Segmented M3 Expressive group for Start on boot & Auto soft reboot
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ExpressiveSwitchCard(
                    checked = state.newMethodBootEnabled,
                    onCheckedChange = actions::onNewMethodBootChanged,
                    title = stringResource(R.string.new_method_boot_title),
                    summary = stringResource(R.string.new_method_boot_summary),
                    icon = Icons.Rounded.PowerSettingsNew,
                    polygon = MaterialShapes.Sunny,
                    position = SegmentPosition.TOP,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExpressiveSwitchCard(
                    checked = state.newMethodAutoSoftReboot,
                    onCheckedChange = actions::onNewMethodSoftRebootChanged,
                    title = stringResource(R.string.new_method_soft_reboot_title),
                    summary = stringResource(R.string.new_method_soft_reboot_summary),
                    icon = Icons.Rounded.RestartAlt,
                    polygon = MaterialShapes.Cookie6Sided,
                    position = SegmentPosition.BOTTOM,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnimatedVisibility(
            visible = state.advancedVisible,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
        ) {
            if (state.newMethod) {
                NewMethodOptions(state = state, actions = actions)
            } else {
                AdvancedOptions(
                    state = state,
                    actions = actions,
                    onOpenProfileSheet = onOpenProfileSheet,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Expressive Reusable Preference Components
// ---------------------------------------------------------------------

@Composable
private fun ExpressiveSpinnerPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    icon: ImageVector,
    polygon: RoundedPolygon,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val currentLabel = items.getOrNull(selectedIndex) ?: stringResource(R.string.no_profile_selected)

    Surface(
        onClick = { sheetOpen = true },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressivePolygonBadge(
                polygon = polygon,
                icon = icon,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExpressivePolygonBadge(
                        polygon = polygon,
                        icon = icon,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 40.dp,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    itemsIndexed(items) { index, itemLabel ->
                        val isSelected = index == selectedIndex
                        val pos = when {
                            items.size == 1 -> SegmentPosition.SINGLE
                            index == 0 -> SegmentPosition.TOP
                            index == items.lastIndex -> SegmentPosition.BOTTOM
                            else -> SegmentPosition.MIDDLE
                        }
                        Surface(
                            onClick = {
                                sheetOpen = false
                                onSelectedIndexChange(index)
                            },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            shape = segmentedCardShape(pos),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemLabel,
                                    style = if (isSelected) {
                                        MaterialTheme.typography.bodyLargeEmphasized
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveSwitchCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    summary: String,
    icon: ImageVector,
    polygon: RoundedPolygon,
    position: SegmentPosition = SegmentPosition.SINGLE,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = segmentedCardShape(position),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressivePolygonBadge(
                polygon = polygon,
                icon = icon,
                containerColor = if (checked) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (checked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

// ---------------------------------------------------------------------
// Connected M3 Expressive Button Groups: MethodSwitch & FlavorSwitch
// ---------------------------------------------------------------------

@Composable
private fun MethodSwitch(
    newMethod: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        false to stringResource(R.string.method_old),
        true to stringResource(R.string.method_new),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        entries.forEachIndexed { index, (isNew, label) ->
            val selected = isNew == newMethod
            val shapes = if (index == 0) {
                ButtonGroupDefaults.connectedLeadingButtonShapes()
            } else {
                ButtonGroupDefaults.connectedTrailingButtonShapes()
            }
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(isNew) },
                shapes = shapes,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = when {
                        selected -> Icons.Rounded.Check
                        isNew -> Icons.Rounded.Bolt
                        else -> Icons.Rounded.History
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FlavorSwitch(
    next: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        false to stringResource(R.string.flavor_kernelsu),
        true to stringResource(R.string.flavor_ksunext),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        entries.forEachIndexed { index, (isNext, label) ->
            val selected = isNext == next
            val shapes = if (index == 0) {
                ButtonGroupDefaults.connectedLeadingButtonShapes()
            } else {
                ButtonGroupDefaults.connectedTrailingButtonShapes()
            }
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(isNext) },
                shapes = shapes,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                    checkedContentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = when {
                        selected -> Icons.Rounded.Check
                        isNext -> Icons.Rounded.AutoAwesome
                        else -> Icons.Rounded.Shield
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Hero Root Status Card & Segmented Device Info
// ---------------------------------------------------------------------

@Composable
private fun ActivationStatusCard(
    rooted: Boolean,
    supported: Boolean,
    modifier: Modifier = Modifier,
    liveRootState: String = "",
    liveRootDetail: String = "",
    selinux: String = "",
) {
    val dark = isSystemInDarkTheme()
    val rootedLive = liveRootState.startsWith("Rooted") || (liveRootState == "Armed" && rooted)
    val failedLive = liveRootState.startsWith("Chain failed")
    val positive = rooted || rootedLive

    val containerColor = when {
        positive -> if (dark) Color(0xFF123824) else Color(0xFFD6F6E0)
        failedLive -> MaterialTheme.colorScheme.errorContainer
        supported -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when {
        positive -> if (dark) Color(0xFFB7F5CD) else Color(0xFF0A381E)
        failedLive -> MaterialTheme.colorScheme.onErrorContainer
        supported -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val badgeBgColor = when {
        positive -> if (dark) Color(0xFF215E3D) else Color(0xFF9EE8B7)
        failedLive -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        supported -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
    }
    val badgeIconColor = when {
        positive -> if (dark) Color(0xFF67E891) else Color(0xFF0C592E)
        failedLive -> MaterialTheme.colorScheme.error
        supported -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    val statusIcon = when {
        positive -> Icons.Rounded.Verified
        failedLive -> Icons.Rounded.ErrorOutline
        supported -> Icons.Rounded.CheckCircle
        else -> Icons.Rounded.WarningAmber
    }
    val statusPolygon = when {
        positive -> MaterialShapes.Sunny
        failedLive -> MaterialShapes.SoftBurst
        supported -> MaterialShapes.Cookie9Sided
        else -> MaterialShapes.Cookie4Sided
    }
    val title = when {
        rootedLive -> stringResource(R.string.status_rooted)
        rooted -> stringResource(R.string.status_rooted)
        failedLive -> stringResource(R.string.root_chain_failed)
        supported -> stringResource(R.string.kernel_supported)
        else -> stringResource(R.string.kernel_unsupported)
    }

    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp),
        ) {
            // Expressive watermark polygon in the background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 34.dp, y = 34.dp)
                    .size(152.dp)
                    .alpha(0.18f)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(badgeIconColor),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ExpressivePolygonBadge(
                    polygon = statusPolygon,
                    icon = statusIcon,
                    containerColor = badgeBgColor,
                    contentColor = badgeIconColor,
                    size = 64.dp,
                    iconSize = 32.dp,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = contentColor,
                    )
                    if (liveRootDetail.isNotBlank()) {
                        Text(
                            text = liveRootDetail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.82f),
                        )
                    }
                    if (selinux.isNotBlank()) {
                        Surface(
                            color = badgeBgColor,
                            contentColor = contentColor,
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = "SELinux · $selinux",
                                style = MaterialTheme.typography.labelSmallEmphasized,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceName: String,
    socName: String,
    kernelRelease: String,
    modifier: Modifier = Modifier,
    infoRows: List<Pair<String, String>> = emptyList(),
) {
    val rows = remember(infoRows, deviceName, socName, kernelRelease) {
        infoRows.ifEmpty {
            listOf(
                "Device" to deviceName,
                "SoC" to socName,
                "Kernel" to kernelRelease,
            )
        }
    }
    val deviceLabel = stringResource(R.string.device_label)
    val socLabel = stringResource(R.string.soc_label)
    val kernelLabel = stringResource(R.string.kernel_label)

    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            rows.forEachIndexed { index, (rawKey, value) ->
                val displayTitle = when (rawKey) {
                    "Device" -> deviceLabel
                    "SoC" -> socLabel
                    "Kernel" -> kernelLabel
                    else -> rawKey
                }
                val (icon, polygon) = when (rawKey) {
                    "Device" -> Icons.Rounded.Smartphone to MaterialShapes.Cookie6Sided
                    "Android" -> Icons.Rounded.Android to MaterialShapes.Clover4Leaf
                    "Kernel" -> Icons.Rounded.Memory to MaterialShapes.Gem
                    "SoC" -> Icons.Rounded.DeveloperBoard to MaterialShapes.Pentagon
                    else -> Icons.Rounded.Info to MaterialShapes.Cookie9Sided
                }
                val pos = when {
                    rows.size == 1 -> SegmentPosition.SINGLE
                    index == 0 -> SegmentPosition.TOP
                    index == rows.lastIndex -> SegmentPosition.BOTTOM
                    else -> SegmentPosition.MIDDLE
                }
                DeviceInfoItem(
                    title = displayTitle,
                    value = value,
                    icon = icon,
                    polygon = polygon,
                    position = pos,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoItem(
    title: String,
    value: String,
    icon: ImageVector,
    polygon: RoundedPolygon,
    position: SegmentPosition,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = segmentedCardShape(position),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressivePolygonBadge(
                polygon = polygon,
                icon = icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 42.dp,
                iconSize = 20.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Bottom Navigation Bar: ShortNavigationBar (M3 Expressive)
// ---------------------------------------------------------------------

@Composable
private fun UniBottomBar(
    current: UniTab,
    logCount: Int,
    hasSessionLogs: Boolean,
    onSelect: (UniTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortNavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        arrangement = ShortNavigationBarArrangement.EqualWeight,
    ) {
        val entries = listOf(
            UniTab.UNIROOT to stringResource(UniTab.UNIROOT.labelRes),
            UniTab.LOGS to stringResource(UniTab.LOGS.labelRes),
        )
        for ((tab, label) in entries) {
            val selected = tab == current
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                iconPosition = NavigationItemIconPosition.Start,
                icon = {
                    when (tab) {
                        UniTab.UNIROOT -> Icon(
                            imageVector = if (selected) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                            contentDescription = label,
                        )
                        UniTab.LOGS -> {
                            if (logCount > 0 || hasSessionLogs) {
                                BadgedBox(
                                    badge = {
                                        if (logCount > 0) {
                                            Badge { Text(text = logCount.toString()) }
                                        } else {
                                            Badge()
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Terminal,
                                        contentDescription = label,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = label,
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = if (selected) {
                            MaterialTheme.typography.labelLargeEmphasized
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                    )
                },
            )
        }
    }
}

// ---------------------------------------------------------------------
// Logs Page: Current Session Log + Run History Boxes
// ---------------------------------------------------------------------

@Composable
private fun LogsPage(
    state: UniUiState,
    actions: UniActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "current-log") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExpressivePolygonBadge(
                            polygon = MaterialShapes.Cookie6Sided,
                            icon = Icons.Rounded.Terminal,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 42.dp,
                        )
                        Text(
                            text = stringResource(R.string.log_title),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.logLines.isNotEmpty()) {
                            FilledTonalIconButton(
                                onClick = actions::onCopyLogs,
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.action_copy),
                                )
                            }
                        }
                    }
                    if (state.logLines.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.logs_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LogPanel(
                            lines = state.logLines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .heightIn(max = 420.dp),
                        )
                    }
                }
            }
        }
        if (state.runLogs.isNotEmpty()) {
            item(key = "history-title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.run_history),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = state.runLogs.size.toString(),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
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

@Composable
private fun RunLogBox(
    entry: RunLogFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = remember(entry.timeMillis) {
        java.text.SimpleDateFormat("MMM d, yyyy · HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(entry.timeMillis))
    }
    val (statusColor, statusLabel) = when (entry.status.lowercase()) {
        "success", "completed" -> Color(0xFF36D167) to "Completed"
        "failed" -> Color(0xFFFF6B6B) to "Failed"
        "crash" -> Color(0xFFF5A623) to "Crash"
        "reboot-required" -> Color(0xFF60A5FA) to "Reboot required"
        "failed-interrupted" -> Color(0xFFFF9F6B) to "Failed (interrupted)"
        else -> MaterialTheme.colorScheme.primary to entry.status
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExpressivePolygonBadge(
                    polygon = MaterialShapes.Cookie7Sided,
                    icon = Icons.Rounded.Description,
                    containerColor = statusColor.copy(alpha = 0.16f),
                    contentColor = statusColor,
                    size = 42.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        contentColor = statusColor,
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onShare,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.log_share),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }

            Button(
                onClick = onOpen,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.log_view),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Primary Action Button & Advanced Sections
// ---------------------------------------------------------------------

@Composable
private fun RunButton(
    rooted: Boolean,
    running: Boolean,
    supported: Boolean,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when {
            running -> R.string.action_running
            rooted -> R.string.action_already_root
            else -> R.string.action_run
        }
    )
    Button(
        onClick = onRun,
        enabled = !running && !rooted && supported,
        shapes = ButtonDefaults.shapesFor(ButtonDefaults.LargeContainerHeight),
        contentPadding = ButtonDefaults.LargeContentPadding,
        modifier = modifier.heightIn(min = ButtonDefaults.LargeContainerHeight),
    ) {
        when {
            running -> {
                LoadingIndicator(
                    modifier = Modifier.size(26.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rooted -> {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.LargeIconSize),
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.LargeIconSize),
                )
            }
        }
        Spacer(modifier = Modifier.width(ButtonDefaults.LargeIconSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
    }
}

@Composable
private fun NewMethodOptions(
    state: UniUiState,
    actions: UniActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ExpressivePolygonBadge(
                    polygon = MaterialShapes.SoftBurst,
                    icon = Icons.Rounded.Info,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    size = 42.dp,
                )
                Text(
                    text = stringResource(R.string.new_method_advanced_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.runLogs.firstOrNull()?.let { latest ->
            FilledTonalButton(
                onClick = { actions.onRunLogOpen(latest) },
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
                Text(
                    text = stringResource(R.string.see_last_log),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
            }
        }

        val dynamicNext = state.ksudNextProfiles.filter { it.dynamic }
        if (dynamicNext.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "Downloaded ksud Next profiles",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    for (p in dynamicNext) {
                        ProfileDeleteRow(label = p.label, note = p.note) {
                            actions.onDeleteKsudProfile(p.id, classic = false)
                        }
                    }
                }
            }
        }

        val dynamicClassic = state.ksudClassicProfiles.filter { it.dynamic }
        if (dynamicClassic.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "Downloaded ksud classic profiles",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    for (p in dynamicClassic) {
                        ProfileDeleteRow(label = p.label, note = p.note) {
                            actions.onDeleteKsudProfile(p.id, classic = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDeleteRow(label: String, note: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalIconButton(
            onClick = onDelete,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete $label",
            )
        }
    }
}

@Composable
private fun AdvancedOptions(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
) {
    var uninstallConfirm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Root My Galaxy card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ExpressivePolygonBadge(
                        polygon = MaterialShapes.Clover8Leaf,
                        icon = Icons.Rounded.CloudSync,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 44.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Root My Galaxy",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = state.rmgStatus.ifEmpty { stringResource(R.string.rmg_idle) },
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = actions::onCheckRmg,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.rmg_check),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = onOpenProfileSheet,
            shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
            Text(
                text = stringResource(R.string.edit_profiles),
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
        }

        // Latest KernelSU builds card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExpressiveSwitchCard(
                    checked = state.useLatestKsu,
                    onCheckedChange = actions::onUseLatestKsuChanged,
                    title = stringResource(R.string.use_latest_ksu),
                    summary = stringResource(R.string.use_latest_ksu_warning),
                    icon = Icons.Rounded.CloudDownload,
                    polygon = MaterialShapes.Cookie9Sided,
                    position = SegmentPosition.SINGLE,
                )
                AnimatedVisibility(visible = state.useLatestKsu) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (state.latestKsuTag.isEmpty()) {
                                    stringResource(R.string.use_latest_not_downloaded)
                                } else {
                                    stringResource(R.string.use_latest_downloaded, state.latestKsuTag)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMediumEmphasized,
                            )
                        }
                        FilledTonalButton(
                            onClick = actions::onDownloadLatestKsu,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.download_latest_ksu),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }
                    }
                }
            }
        }

        // Auto-root on boot
        ExpressiveSwitchCard(
            checked = state.autoRootOnBoot,
            onCheckedChange = actions::onAutoRootChanged,
            title = stringResource(R.string.auto_root_on_boot),
            summary = stringResource(R.string.auto_root_on_boot_summary),
            icon = Icons.Rounded.PowerSettingsNew,
            polygon = MaterialShapes.Sunny,
            position = SegmentPosition.SINGLE,
            modifier = Modifier.fillMaxWidth(),
        )

        // Instant Root card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                ExpressiveSwitchCard(
                    checked = state.instantRootEnabled,
                    onCheckedChange = actions::onInstantRootEnabledChanged,
                    title = stringResource(R.string.instant_root_title),
                    summary = when {
                        state.instantRootEnabled -> stringResource(R.string.instant_root_enabled_summary)
                        state.instantRootInstalled -> stringResource(R.string.instant_root_off_summary)
                        else -> stringResource(R.string.instant_root_not_installed)
                    },
                    icon = Icons.Rounded.Bolt,
                    polygon = MaterialShapes.SoftBurst,
                    position = SegmentPosition.SINGLE,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = if (state.instantRootInstalled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (state.instantRootInstalled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = CircleShape,
                    ) {
                        Text(
                            text = if (state.instantRootInstalled) {
                                stringResource(R.string.instant_root_status_installed)
                            } else {
                                stringResource(R.string.instant_root_status_missing)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                        )
                    }

                    FilledTonalButton(
                        onClick = actions::onOpenInstantRootLog,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.instant_root_log),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                        )
                    }

                    if (state.instantRootInstalled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = actions::onOpenHelper,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.instant_root_open_helper),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                )
                            }
                            FilledTonalButton(
                                onClick = actions::onInstantRootInstallNow,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.instant_root_update_helper),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { uninstallConfirm = true },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.instant_root_uninstall_helper),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = actions::onResetProfiles,
            shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
            Icon(
                imageVector = Icons.Rounded.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
            Text(
                text = stringResource(R.string.reset_profiles),
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
        }

        state.runLogs.firstOrNull()?.let { latest ->
            FilledTonalButton(
                onClick = { actions.onRunLogOpen(latest) },
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
                Text(
                    text = stringResource(R.string.see_last_log),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
            }
        }

        if (uninstallConfirm) {
            ExpressiveConfirmDialog(
                title = stringResource(R.string.instant_root_uninstall_helper),
                body = stringResource(R.string.instant_root_uninstall_confirm),
                icon = Icons.Rounded.DeleteOutline,
                polygon = MaterialShapes.SoftBurst,
                confirmText = stringResource(R.string.instant_root_uninstall),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    uninstallConfirm = false
                    actions.onUninstallHelper()
                },
                onDismiss = { uninstallConfirm = false },
                isDestructive = true,
            )
        }
    }
}

// ---------------------------------------------------------------------
// Terminal Log Panel & Tone Coloring
// ---------------------------------------------------------------------

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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B1220))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
// Profile Manager & Editor (Advanced -> Edit profiles)
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

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExpressivePolygonBadge(
                        polygon = MaterialShapes.Cookie7Sided,
                        icon = Icons.Rounded.Tune,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 42.dp,
                    )
                    Text(
                        text = stringResource(R.string.edit_profiles),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(profiles, key = { it.name }) { profile ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = fileSummary(profile),
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilledTonalButton(
                                        onClick = { onEdit(profile) },
                                        shapes = ButtonDefaults.shapes(),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_edit),
                                            style = MaterialTheme.typography.labelLargeEmphasized,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { pendingDelete = profile },
                                        shapes = ButtonDefaults.shapes(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_delete),
                                            style = MaterialTheme.typography.labelLargeEmphasized,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "new") {
                        Button(
                            onClick = onNew,
                            shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.MediumIconSpacing))
                            Text(
                                text = stringResource(R.string.new_profile),
                                style = MaterialTheme.typography.titleSmallEmphasized,
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { victim ->
        ExpressiveConfirmDialog(
            title = stringResource(R.string.action_delete),
            body = stringResource(R.string.delete_profile_confirm, victim.name),
            icon = Icons.Rounded.DeleteOutline,
            polygon = MaterialShapes.SoftBurst,
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                onDelete(victim)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
            isDestructive = true,
        )
    }
}

private fun fileSummary(profile: DeviceProfile): String {
    fun n(path: String) = File(path).name.ifEmpty { "?" }
    val parts = mutableListOf(
        "SO: ${n(profile.pathSo)}",
        "KO: ${n(profile.pathKo)}",
        "KSUD: ${n(profile.pathKsud)}",
    )
    if (!profile.pathCveNormal.isNullOrEmpty()) parts.add("CVE: ${n(profile.pathCveNormal)}")
    if (!profile.pathCveRoot.isNullOrEmpty()) parts.add("ROOT: ${n(profile.pathCveRoot)}")
    if (profile.useShizuku) parts.add("Shizuku")
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
    var useShizuku by remember(stateKey) { mutableStateOf(profile?.useShizuku ?: false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            ExpressivePolygonBadge(
                polygon = MaterialShapes.Cookie9Sided,
                icon = if (profile == null) Icons.Rounded.Add else Icons.Rounded.Edit,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 48.dp,
            )
        },
        title = {
            Text(
                text = stringResource(if (profile == null) R.string.new_profile else R.string.edit_profile),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kaslr,
                    onValueChange = { kaslr = it },
                    label = { Text(stringResource(R.string.field_kaslr)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Connected ButtonGroup for Samsung vs Other
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(
                        checked = deviceType == "samsung",
                        onCheckedChange = { deviceType = "samsung" },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (deviceType == "samsung") {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(
                            text = stringResource(R.string.field_device_samsung),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                        )
                    }
                    ToggleButton(
                        checked = deviceType == "oppo",
                        onCheckedChange = { deviceType = "oppo" },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (deviceType == "oppo") {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(
                            text = stringResource(R.string.field_device_other),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                        )
                    }
                }

                ExpressiveSwitchCard(
                    checked = useShizuku,
                    onCheckedChange = { useShizuku = it },
                    title = stringResource(R.string.profile_use_shizuku),
                    summary = stringResource(R.string.profile_use_shizuku_summary),
                    icon = Icons.Rounded.Terminal,
                    polygon = MaterialShapes.Cookie6Sided,
                    position = SegmentPosition.SINGLE,
                    modifier = Modifier.fillMaxWidth(),
                )

                FileField(stringResource(R.string.field_exploit_so), soPath) {
                    pendingRole = FileRole.SO
                    picker.launch("*/*")
                }
                FileField(stringResource(R.string.field_ko), koPath) {
                    pendingRole = FileRole.KO
                    picker.launch("*/*")
                }
                FileField(stringResource(R.string.field_ksud), ksudPath) {
                    pendingRole = FileRole.KSUD
                    picker.launch("*/*")
                }
                FileField(stringResource(R.string.field_cve_normal), cveNormalPath) {
                    pendingRole = FileRole.CVE_NORMAL
                    picker.launch("*/*")
                }
                FileField(stringResource(R.string.field_cve_root), cveRootPath) {
                    pendingRole = FileRole.CVE_ROOT
                    picker.launch("*/*")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || soPath.isBlank() || koPath.isBlank() || ksudPath.isBlank()) {
                        Toast.makeText(context, R.string.profile_missing_fields, Toast.LENGTH_SHORT).show()
                        return@Button
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
                        flavor = profile?.flavor ?: "kernelsu",
                        useShizuku = useShizuku,
                    )
                    onSave(updated, profile?.name)
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = path.substringAfterLast('/').ifEmpty { stringResource(R.string.no_file) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = onPick,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.choose_file),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        }
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
