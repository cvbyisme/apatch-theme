package me.bmax.apatch.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.component.RadioItem
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.theme.ColorMode
import me.bmax.apatch.ui.theme.DarkMode
import me.bmax.apatch.ui.theme.IconShape
import me.bmax.apatch.ui.theme.IconTint
import me.bmax.apatch.ui.theme.ThemeConfig
import me.bmax.apatch.ui.theme.ThemeStore
import me.bmax.apatch.ui.theme.themePreview
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.getKernelVersionCode
import me.bmax.apatch.util.isGkiKernel
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.outputStream
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setGlobalNamespaceEnabled
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen() {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var namespaceLoaded by remember { mutableStateOf(false) }
    // The check shells out as root; run it once off the main thread instead of
    // synchronously in composition on every recomposition. The switch stays
    // disabled until the real value lands so a fast tap can't act on the
    // placeholder and get overwritten by the late result.
    LaunchedEffect(kPatchReady && aPatchReady) {
        if (kPatchReady && aPatchReady) {
            isGlobalNamespaceEnabled = withContext(Dispatchers.IO) { isGlobalNamespaceEnabled() }
            namespaceLoaded = true
        }
    }

    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { paddingValues ->

        val loadingDialog = rememberLoadingDialog()

        val showLanguageDialog = rememberSaveable { mutableStateOf(false) }
        LanguageDialog(showLanguageDialog)

        val showResetSuPathDialog = remember { mutableStateOf(false) }
        if (showResetSuPathDialog.value) {
            ResetSUPathDialog(showResetSuPathDialog)
        }

        val showThemeChooseDialog = remember { mutableStateOf(false) }
        if (showThemeChooseDialog.value) {
            ThemeChooseDialog(showThemeChooseDialog)
        }

        val showThemeSettingsDialog = remember { mutableStateOf(false) }
        if (showThemeSettingsDialog.value) {
            ThemeSettingsDialog(showThemeSettingsDialog)
        }

        var showLogBottomSheet by remember { mutableStateOf(false) }
        val saveLog = stringResource(R.string.save_log)

        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val logSavedMessage = stringResource(R.string.log_saved)
        val exportBugreportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/gzip")
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    loadingDialog.show()
                    uri.outputStream().use { output ->
                        getBugreportFile(context).inputStream().use {
                            it.copyTo(output)
                        }
                    }
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(message = logSavedMessage)
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = APApplication.sharedPreferences

            // Global mount
            if (kPatchReady && aPatchReady) {
                SwitchItem(
                    icon = Icons.Filled.Engineering,
                    title = stringResource(id = R.string.settings_global_namespace_mode),
                    summary = stringResource(id = R.string.settings_global_namespace_mode_summary),
                    checked = isGlobalNamespaceEnabled,
                    enabled = namespaceLoaded,
                    onCheckedChange = {
                        setGlobalNamespaceEnabled(
                            if (isGlobalNamespaceEnabled) {
                                "0"
                            } else {
                                "1"
                            }
                        )
                        isGlobalNamespaceEnabled = it
                    })
            }

            // Legacy sucompat (path_probe) support
            if (kPatchReady && aPatchReady) {
                var sucompatEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("sucompat_enabled", false)
                    )
                }
                SwitchItem(
                    icon = Icons.AutoMirrored.Filled.FeaturedPlayList,
                    title = stringResource(id = R.string.settings_sucompat),
                    summary = stringResource(id = R.string.settings_sucompat_summary),
                    checked = sucompatEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch(Dispatchers.IO) {
                            val result = if (enabled) {
                                // Enable: create marker file and register hooks via supercall
                                rootShellForResult("touch ${APApplication.SUCOMPAT_FILE}")
                                Natives.controlFeature("sucompat_extra", true)
                            } else {
                                // Disable: remove marker file and unregister hooks via supercall
                                rootShellForResult("rm -f ${APApplication.SUCOMPAT_FILE}")
                                Natives.controlFeature("sucompat_extra", false)
                            }
                            Log.d("SucompatToggle", "sucompat_extra ${if (enabled) "enable" else "disable"} result: $result")
                            if (result == 0L) {
                                prefs.edit { putBoolean("sucompat_enabled", enabled) }
                                sucompatEnabled = enabled
                            }
                        }
                    })
            }

            // Hide SELinux modification (test)
            if (kPatchReady && aPatchReady) {
                val kernelVersion = remember { getKernelVersionCode() }
                val kernelSupported = (kernelVersion ?: 0) >= 419
                val isGki = remember { isGkiKernel() }
                var selinuxHideEnabled by rememberSaveable {
                    mutableStateOf(prefs.getBoolean("selinux_hide_enabled", false))
                }
                val showSelinuxHideWarning = remember { mutableStateOf(false) }

                fun applySelinuxHide(enabled: Boolean) {
                    scope.launch(Dispatchers.IO) {
                        val command = if (enabled) {
                            "touch ${APApplication.SELINUX_HIDE_FILE}"
                        } else {
                            "rm -f ${APApplication.SELINUX_HIDE_FILE}"
                        }
                        val result = rootShellForResult(command)
                        Log.d("SelinuxHideToggle", "$command result: ${result.code}")
                        if (result.isSuccess) {
                            prefs.edit { putBoolean("selinux_hide_enabled", enabled) }
                            selinuxHideEnabled = enabled
                        }
                    }
                }

                SwitchItem(
                    icon = Icons.Filled.Security,
                    title = stringResource(id = R.string.settings_selinux_hide),
                    summary = stringResource(id = R.string.settings_selinux_hide_summary),
                    checked = selinuxHideEnabled,
                    enabled = kernelSupported,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Only tested on 5.10+, and non-GKI carries a bigger risk, so warn first.
                            val below510 = (kernelVersion ?: 0) < 510
                            if (below510 || !isGki) {
                                showSelinuxHideWarning.value = true
                            } else {
                                applySelinuxHide(true)
                            }
                        } else {
                            applySelinuxHide(false)
                        }
                    }
                )

                if (showSelinuxHideWarning.value) {
                    SelinuxHideWarningDialog(
                        showDialog = showSelinuxHideWarning,
                        kernelVersion = kernelVersion,
                        isGki = isGki,
                        onConfirm = { applySelinuxHide(true) },
                    )
                }
            }

            // WebView Debug
            if (aPatchReady) {
                var enableWebDebugging by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_web_debugging", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DeveloperMode,
                    title = stringResource(id = R.string.enable_web_debugging),
                    summary = stringResource(id = R.string.enable_web_debugging_summary),
                    checked = enableWebDebugging
                ) {
                    APApplication.sharedPreferences.edit {
                        putBoolean("enable_web_debugging", it)
                    }
                    enableWebDebugging = it
                }
            }

            // Check Update
            var checkUpdate by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("check_update", true)
                )
            }

            SwitchItem(
                icon = Icons.Filled.Update,
                title = stringResource(id = R.string.settings_check_update),
                summary = stringResource(id = R.string.settings_check_update_summary),
                checked = checkUpdate
            ) {
                prefs.edit { putBoolean("check_update", it) }
                checkUpdate = it
            }

            // Night Mode Follow System
            var nightFollowSystem by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("night_mode_follow_sys", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.InvertColors,
                title = stringResource(id = R.string.settings_night_mode_follow_sys),
                summary = stringResource(id = R.string.settings_night_mode_follow_sys_summary),
                checked = nightFollowSystem
            ) {
                prefs.edit { putBoolean("night_mode_follow_sys", it) }
                nightFollowSystem = it
                refreshTheme.value = true
            }

            // Custom Night Theme Switch
            if (!nightFollowSystem) {
                var nightThemeEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("night_mode_enabled", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(id = R.string.settings_night_theme_enabled),
                    checked = nightThemeEnabled
                ) {
                    prefs.edit { putBoolean("night_mode_enabled", it) }
                    nightThemeEnabled = it
                    refreshTheme.value = true
                }
            }

            // System dynamic color theme
            val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            if (isDynamicColorSupport) {
                var useSystemDynamicColor by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("use_system_color_theme", true)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.ColorLens,
                    title = stringResource(id = R.string.settings_use_system_color_theme),
                    summary = stringResource(id = R.string.settings_use_system_color_theme_summary),
                    checked = useSystemDynamicColor
                ) {
                    prefs.edit { putBoolean("use_system_color_theme", it) }
                    useSystemDynamicColor = it
                    refreshTheme.value = true
                }

                if (!useSystemDynamicColor) {
                    ListItem(headlineContent = {
                        Text(text = stringResource(id = R.string.settings_custom_color_theme))
                    }, modifier = Modifier.clickable {
                        showThemeChooseDialog.value = true
                    }, supportingContent = {
                        val colorMode = prefs.getString("custom_color", "blue")
                        Text(
                            text = stringResource(colorNameToString(colorMode.toString())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })

                }
            } else {
                ListItem(headlineContent = {
                    Text(text = stringResource(id = R.string.settings_custom_color_theme))
                }, modifier = Modifier.clickable {
                    showThemeChooseDialog.value = true
                }, supportingContent = {
                    val colorMode = prefs.getString("custom_color", "blue")
                    Text(
                        text = stringResource(colorNameToString(colorMode.toString())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })
            }

            // Theme customization
            ListItem(
                leadingContent = { Icon(Icons.Filled.ColorLens, null) },
                headlineContent = { Text("主题自定义") },
                supportingContent = {
                    Text(
                        "壁纸 · 图标 · 字体 · 动效",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
                modifier = Modifier.clickable { showThemeSettingsDialog.value = true },
            )

            // su path
            if (kPatchReady) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Commit, stringResource(id = R.string.setting_reset_su_path)
                        )
                    },
                    supportingContent = {},
                    headlineContent = { Text(stringResource(id = R.string.setting_reset_su_path)) },
                    modifier = Modifier.clickable {
                        showResetSuPathDialog.value = true
                    })
            }

            // language
            ListItem(headlineContent = {
                Text(text = stringResource(id = R.string.settings_app_language))
            }, modifier = Modifier.clickable {
                showLanguageDialog.value = true
            }, supportingContent = {
                Text(text = AppCompatDelegate.getApplicationLocales()[0]?.displayLanguage?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                } ?: stringResource(id = R.string.system_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline)
            }, leadingContent = { Icon(Icons.Filled.Translate, null) })

            // log
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.BugReport, stringResource(id = R.string.send_log)
                    )
                },
                headlineContent = { Text(stringResource(id = R.string.send_log)) },
                modifier = Modifier.clickable {
                    showLogBottomSheet = true
                })
            if (showLogBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLogBottomSheet = false },
                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                    content = {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterHorizontally)

                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val formatter =
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                                val current = LocalDateTime.now().format(formatter)
                                                exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                                                showLogBottomSheet = false
                                            }
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Save,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.save_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center

                                    )
                                }

                            }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val bugreport = loadingDialog.withLoading {
                                                    withContext(Dispatchers.IO) {
                                                        getBugreportFile(context)
                                                    }
                                                }

                                                val uri: Uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                    bugreport
                                                )

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    setDataAndType(uri, "application/gzip")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        saveLog
                                                    )
                                                )
                                                showLogBottomSheet = false
                                            }
                                        }) {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.send_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center

                                    )
                                }

                            }
                        }
                        NavigationBarsSpacer()
                    })
            }


        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(colorsList()) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(it.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("custom_color", it.name) }
                            refreshTheme.value = true
                        })
                }

            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }

}

private data class APColor(
    val name: String, @param:StringRes val nameId: Int
)

private fun colorsList(): List<APColor> {
    return listOf(
        APColor("amber", R.string.amber_theme),
        APColor("blue_grey", R.string.blue_grey_theme),
        APColor("blue", R.string.blue_theme),
        APColor("brown", R.string.brown_theme),
        APColor("cyan", R.string.cyan_theme),
        APColor("deep_orange", R.string.deep_orange_theme),
        APColor("deep_purple", R.string.deep_purple_theme),
        APColor("green", R.string.green_theme),
        APColor("indigo", R.string.indigo_theme),
        APColor("light_blue", R.string.light_blue_theme),
        APColor("light_green", R.string.light_green_theme),
        APColor("lime", R.string.lime_theme),
        APColor("orange", R.string.orange_theme),
        APColor("pink", R.string.pink_theme),
        APColor("purple", R.string.purple_theme),
        APColor("red", R.string.red_theme),
        APColor("sakura", R.string.sakura_theme),
        APColor("teal", R.string.teal_theme),
        APColor("yellow", R.string.yellow_theme),
    )
}

@Composable
private fun colorNameToString(colorName: String): Int {
    return colorsList().find { it.name == colorName }?.nameId ?: R.string.blue_theme
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(ThemeStore.load()) }

    fun persist() {
        ThemeStore.save(config)
        themePreview.value = null
        refreshTheme.value = true
    }

    /** 拖动过程中只做实时预览，不写盘 */
    fun preview() {
        themePreview.value = config
    }

    fun applyAlpha(background: Float, surface: Float) {
        config = config.copy(backgroundAlpha = background, surfaceAlpha = surface)
        persist()
    }

    // 对话框被销毁时清掉未落盘的预览，避免主题卡在预览状态
    DisposableEffect(Unit) {
        onDispose { themePreview.value = null }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            val f = ThemeStore.importFile(context, u, "wallpaper")
            if (f != null) {
                config = config.copy(wallpaperEnabled = true, wallpaperPath = f.absolutePath)
                persist()
            } else {
                Toast.makeText(context, R.string.failure, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            val f = ThemeStore.importFile(context, u, "font")
            if (f != null) {
                config = config.copy(fontPath = f.absolutePath)
                persist()
            } else {
                Toast.makeText(context, R.string.failure, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(all = 24.dp)),
            ) {
                Text("主题自定义", style = MaterialTheme.typography.headlineSmall)

                sectionTitle("配色来源")
                RadioItem("预设配色", config.colorMode == ColorMode.PRESET) {
                    config = config.copy(colorMode = ColorMode.PRESET)
                    persist()
                }
                RadioItem("系统动态取色", config.colorMode == ColorMode.SYSTEM_DYNAMIC) {
                    config = config.copy(colorMode = ColorMode.SYSTEM_DYNAMIC)
                    persist()
                }
                RadioItem("自定义颜色", config.colorMode == ColorMode.CUSTOM_SEED) {
                    config = config.copy(colorMode = ColorMode.CUSTOM_SEED)
                    persist()
                }
                RadioItem("从壁纸取色", config.colorMode == ColorMode.FROM_WALLPAPER) {
                    config = config.copy(colorMode = ColorMode.FROM_WALLPAPER)
                    persist()
                }

                if (config.colorMode == ColorMode.CUSTOM_SEED || config.colorMode == ColorMode.FROM_WALLPAPER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        seedColors.forEach { seed ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(seed))
                                    .clickable {
                                        config = config.copy(customSeedColor = seed)
                                        persist()
                                    },
                            )
                        }
                    }
                }

                sectionTitle("壁纸")
                SwitchItem(icon = null, title = "启用壁纸", checked = config.wallpaperEnabled) {
                    config = config.copy(wallpaperEnabled = it)
                    persist()
                }
                ListItem(
                    headlineContent = { Text("导入壁纸图片") },
                    modifier = Modifier.clickable { wallpaperPicker.launch("image/*") },
                )
                if (config.hasWallpaper) {
                    ListItem(
                        headlineContent = { Text("移除壁纸") },
                        modifier = Modifier.clickable {
                            ThemeStore.deleteFile(config.wallpaperPath)
                            config = config.copy(wallpaperEnabled = false, wallpaperPath = null)
                            persist()
                        },
                    )
                }
                if (config.hasWallpaper) {
                    SettingSlider(
                        title = "壁纸模糊",
                        value = config.wallpaperBlur,
                        valueRange = 0f..48f,
                        percent = false,
                        onChange = {
                            config = config.copy(wallpaperBlur = it)
                            preview()
                        },
                        onCommit = { persist() },
                    )
                    SettingSlider(
                        title = "浅色遮罩",
                        value = config.wallpaperScrim,
                        valueRange = 0f..0.8f,
                        onChange = {
                            config = config.copy(wallpaperScrim = it)
                            preview()
                        },
                        onCommit = { persist() },
                    )
                    SettingSlider(
                        title = "深色遮罩",
                        value = config.wallpaperDarkScrim,
                        valueRange = 0f..0.9f,
                        onChange = {
                            config = config.copy(wallpaperDarkScrim = it)
                            preview()
                        },
                        onCommit = { persist() },
                    )
                }

                sectionTitle("界面透明度")
                if (config.hasWallpaper) {
                    Text(
                        "背景不透明度决定壁纸透出多少；界面不透明度同时作用于对话框、底部面板与卡片。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    SettingSlider(
                        title = "背景不透明度",
                        value = config.backgroundAlpha,
                        valueRange = 0.15f..1f,
                        onChange = {
                            config = config.copy(backgroundAlpha = it)
                            preview()
                        },
                        onCommit = { persist() },
                    )
                    SettingSlider(
                        title = "界面不透明度",
                        value = config.surfaceAlpha,
                        valueRange = 0.3f..1f,
                        onChange = {
                            config = config.copy(surfaceAlpha = it)
                            preview()
                        },
                        onCommit = { persist() },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { applyAlpha(1f, 1f) }) { Text("不透明") }
                        TextButton(onClick = { applyAlpha(0.45f, 0.85f) }) { Text("均衡") }
                        TextButton(onClick = { applyAlpha(0.25f, 0.6f) }) { Text("通透") }
                    }
                } else {
                    Text(
                        "启用壁纸后可调节壁纸模糊、遮罩与界面透明度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                sectionTitle("字体")
                ListItem(
                    headlineContent = { Text("导入字体 (TTF/OTF)") },
                    modifier = Modifier.clickable { fontPicker.launch("*/*") },
                )
                if (config.fontPath != null) {
                    ListItem(
                        headlineContent = { Text("恢复默认字体") },
                        modifier = Modifier.clickable {
                            ThemeStore.deleteFile(config.fontPath)
                            config = config.copy(fontPath = null)
                            persist()
                        },
                    )
                }
                SettingSlider(
                    title = "字号缩放",
                    value = config.fontScale,
                    valueRange = 0.8f..1.6f,
                    onChange = {
                        config = config.copy(fontScale = it)
                        preview()
                    },
                    onCommit = { persist() },
                )
                sectionTitle("图标形状")
                RadioItem("圆角", config.iconShape == IconShape.ROUND) {
                    config = config.copy(iconShape = IconShape.ROUND)
                    persist()
                }
                RadioItem("方形", config.iconShape == IconShape.SQUARE) {
                    config = config.copy(iconShape = IconShape.SQUARE)
                    persist()
                }
                RadioItem("圆形", config.iconShape == IconShape.CIRCLE) {
                    config = config.copy(iconShape = IconShape.CIRCLE)
                    persist()
                }

                sectionTitle("图标着色")
                RadioItem("跟随主题", config.iconTint == IconTint.THEME) {
                    config = config.copy(iconTint = IconTint.THEME)
                    persist()
                }
                RadioItem("单色", config.iconTint == IconTint.MONOCHROME) {
                    config = config.copy(iconTint = IconTint.MONOCHROME)
                    persist()
                }
                RadioItem("原色", config.iconTint == IconTint.ORIGINAL) {
                    config = config.copy(iconTint = IconTint.ORIGINAL)
                    persist()
                }

                sectionTitle("明暗模式")
                RadioItem("跟随系统", config.darkMode == DarkMode.FOLLOW_SYSTEM) {
                    config = config.copy(darkMode = DarkMode.FOLLOW_SYSTEM)
                    persist()
                }
                RadioItem("浅色", config.darkMode == DarkMode.LIGHT) {
                    config = config.copy(darkMode = DarkMode.LIGHT)
                    persist()
                }
                RadioItem("深色", config.darkMode == DarkMode.DARK) {
                    config = config.copy(darkMode = DarkMode.DARK)
                    persist()
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        ThemeStore.save(ThemeConfig())
                        refreshTheme.value = true
                        config = ThemeConfig()
                    }) { Text("恢复默认") }
                    Button(onClick = { showDialog.value = false }) { Text("完成") }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@Composable
private fun sectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

/** 带标题与数值显示的滑块；percent=true 时按百分比展示。 */
@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    percent: Boolean = true,
    onChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                text = if (percent) "${(value * 100).roundToInt()}%" else "${value.roundToInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onCommit,
            valueRange = valueRange,
            enabled = enabled,
        )
    }
}

private val seedColors = listOf(
    0xFF6750A4L, 0xFF9B404FL, 0xFF0061A4L, 0xFF00696EL,
    0xFF3B6939L, 0xFF7D5260L, 0xFF9A4300L, 0xFF5B5BBDL,
)

val suPathChecked: (path: String) -> Boolean = {
    it.startsWith("/") && it.trim().length > 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var suPath by remember { mutableStateOf(Natives.suPath()) }
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_reset_su_path),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 12.dp))
                        .align(Alignment.Start)
                ) {
                    OutlinedTextField(
                        value = suPath,
                        onValueChange = {
                            suPath = it
                        },
                        label = { Text(stringResource(id = R.string.setting_reset_su_new_path)) },
                        visualTransformation = VisualTransformation.None,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {

                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(enabled = suPathChecked(suPath), onClick = {
                        showDialog.value = false
                        val success = Natives.resetSuPath(suPath)
                        Toast.makeText(
                            context,
                            if (success) R.string.success else R.string.failure,
                            Toast.LENGTH_SHORT
                        ).show()
                        rootShellForResult("echo $suPath > ${APApplication.SU_PATH_FILE}")
                    }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelinuxHideWarningDialog(
    showDialog: MutableState<Boolean>,
    kernelVersion: Int?,
    isGki: Boolean,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_selinux_hide_warning_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                if ((kernelVersion ?: 0) < 510) {
                    Box(
                        Modifier
                            .padding(PaddingValues(bottom = 8.dp))
                            .align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_selinux_hide_warning_below_5_10),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!isGki) {
                    Box(
                        Modifier
                            .padding(PaddingValues(bottom = 16.dp))
                            .align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_selinux_hide_warning_non_gki),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(onClick = {
                        showDialog.value = false
                        onConfirm()
                    }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(showLanguageDialog: MutableState<Boolean>) {

    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)

    if (showLanguageDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showLanguageDialog.value = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(150.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                LazyColumn {
                    itemsIndexed(languages) { index, item ->
                        ListItem(
                            headlineContent = { Text(item) },
                            modifier = Modifier.clickable {
                                showLanguageDialog.value = false
                                if (index == 0) {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.getEmptyLocaleList()
                                    )
                                } else {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(
                                            languagesValues[index]
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
