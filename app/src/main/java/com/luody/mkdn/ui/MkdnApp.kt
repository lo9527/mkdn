package com.luody.mkdn.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import com.luody.mkdn.data.HighlightItem
import com.luody.mkdn.data.PrefsStore
import com.luody.mkdn.data.RepoStore
import com.luody.mkdn.ui.screens.EditorScreen
import com.luody.mkdn.ui.screens.HighlightsScreen
import com.luody.mkdn.ui.screens.HomeScreen
import com.luody.mkdn.ui.screens.ReaderScreen
import com.luody.mkdn.ui.theme.MkdnTheme
import com.luody.mkdn.ui.theme.MkdnThemeMode
import com.luody.mkdn.ui.theme.LocalMkdnColors
import java.io.File

@androidx.compose.runtime.Composable
fun MkdnApp(
    intent: Intent? = null,
    themeMode: MkdnThemeMode = MkdnThemeMode.LIGHT,
    onThemeChange: (MkdnThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PrefsStore(context) }
    val repo = remember { RepoStore(context) }

    // v1.5: themeMode 由 Activity 传入（统一驱动 MkdnTheme），这里不再双向 state
    // v1.6 修复: 不再 LaunchedEffect 写 prefs,因为 MkdnApp 顶层读 prefs.themeMode 作"持久化值"
    //           如果 LaunchedEffect 写出 prefs.themeMode = LIGHT (默认值) 会擦掉用户真实设置
    //           持久化统一由 MainActivity.onThemeChange 回调里直接 prefs.themeMode = it 实现
    var fontSize by remember { mutableStateOf(prefs.fontSize) }

    val nav = rememberNavController()
    // v1.5.4 终极修复: Intent 启动 race 的真正根因是 file IO 在被重复触发
    // → 不让 HomeScreen 在 popBackStack 后重新走 IO/LaunchedEffect
    // 思路：顶层一个独立的 LaunchedEffectUnit，处理 Intent
    // 仅当 initialTarget 非空时调用一次 navigate，完事立刻"消费"(变 null)
    val pendingIntentTarget = remember { mutableStateOf<Pair<String, String>?>(null) }
    val pendingInit = remember {
        intent?.let { extractFileTarget(it) }
    }
    if (pendingIntentTarget.value == null && pendingInit != null) {
        pendingIntentTarget.value = pendingInit
    }

    // v1.5.4: 顶层 LaunchedEffect(Unit) —— 永远只启动一次（key=Unit 永远相等）
    // 当 pendingIntentTarget 有值 → navigate 一次 + 清空 → 永远不会再触发
    LaunchedEffect(Unit) {
        val target = pendingIntentTarget.value ?: return@LaunchedEffect
        pendingIntentTarget.value = null // 立刻消费掉
        // 把 Intent target 转成 path/name 后调 nav 直接跳 reader
        val (path, name) = target
        nav.navigate("reader/${Uri.encode(path)}/${Uri.encode(name)}")
    }

    Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    repo = repo,
                    onOpenFile = { path, name ->
                        nav.navigate("reader/${Uri.encode(path)}/${Uri.encode(name)}")
                    },
                    onOpenHighlights = { nav.navigate("highlights") },
                    themeMode = themeMode,
                    onThemeChange = onThemeChange
                    // v1.5.4: 顶层 LaunchedEffect 处理 Intent，HomeScreen 不再需要 initialIntentTarget
                )
            }
            composable(
                "reader/{path}/{name}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
                val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                ReaderScreen(
                    path = path,
                    name = name,
                    repo = repo,
                    fontSize = fontSize,
                    onFontSizeChange = { fontSize = it; prefs.fontSize = it },
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    onEdit = { nav.navigate("editor/${Uri.encode(path)}/${Uri.encode(name)}") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                "editor/{path}/{name}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
                val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                EditorScreen(
                    path = path,
                    name = name,
                    onSave = {
                        nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() }
                )
            }
            composable("highlights") {
                HighlightsScreen(
                    repo = repo,
                    onOpenHighlight = { h ->
                        // v1.6: 文件存在性检查，避免 reader 加载空文件卡在加载圈
                        if (java.io.File(h.filePath).exists()) {
                            nav.navigate("reader/${Uri.encode(h.filePath)}/${Uri.encode(h.fileName)}")
                        } else {
                            // v1.6: 文件丢了，提示用户并删除该 highlight 避免下次还点错
                            repo.removeHighlight(h.id)
                            android.widget.Toast.makeText(
                                context,
                                "文件已不存在，已清理该高亮：${h.fileName}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

private fun extractFileTarget(intent: Intent): Pair<String, String>? {
    if (intent.action != Intent.ACTION_VIEW) return null
    val uri = intent.data ?: return null
    val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: "未命名"
    val path = uriToPath(uri, name) ?: return null
    return path to name
}

private fun queryDisplayName(uri: Uri): String? {
    return try {
        if (uri.scheme == "content") {
            val ctx = appContext ?: return null
            val cursor = ctx.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) it.getString(idx) else null
                } else null
            }
        } else null
    } catch (e: Exception) { null }
}

private fun uriToPath(uri: Uri, displayName: String): String? {
    return try {
        when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                // 修复 v1.3: 用注入的 appContext（之前用未初始化的 mkdnAppContext → 永远 null）
                val ctx = appContext ?: return null
                val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                // v1.5.5 修复: 用 URI hash 作文件名，避免同文件多次打开变成多条 recent
                // 之前 timestamp 每次都不同 → RepoStore.addRecent 按 path 去重失败 → 主界面多个重复条目
                val uriHash = uri.toString().hashCode().toString(16)
                val out = File(ctx.filesDir, "imported/${uriHash}_$safeName")
                out.parentFile?.mkdirs()
                // 覆盖式写入：同源文件第二次打开时直接 overwrite，无需新文件
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
                out.absolutePath
            }
            else -> null
        }
    } catch (e: Exception) { null }
}