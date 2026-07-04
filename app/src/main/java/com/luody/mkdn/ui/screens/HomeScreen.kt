package com.luody.mkdn.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luody.mkdn.data.PrefsStore
import com.luody.mkdn.data.RecentItem
import com.luody.mkdn.data.RepoStore
import com.luody.mkdn.ui.theme.LocalMkdnColors
import com.luody.mkdn.ui.theme.MkdnThemeMode
import com.luody.mkdn.ui.theme.FontSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun HomeScreen(
    repo: RepoStore,
    onOpenFile: (String, String) -> Unit,
    onOpenHighlights: () -> Unit,
    themeMode: MkdnThemeMode,
    onThemeChange: (MkdnThemeMode) -> Unit
    // v1.5.4: 删 initialIntentTarget，Intent 处理已移到 MkdnApp 顶层 LaunchedEffect
) {
    val context = LocalContext.current
    val colors = LocalMkdnColors.current
    val prefs = remember { PrefsStore(context) }
    var recents by remember { mutableStateOf(repo.loadRecents()) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(prefs.fontSize) }

    // 系统文件选择器
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = queryName(context, uri) ?: "未命名.md"
            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            // v1.5.5 修复: 用 URI hash 作文件名，避免同文件多次打开变成多条 recent
            val uriHash = uri.toString().hashCode().toString(16)
            val out = java.io.File(context.filesDir, "imported/${uriHash}_$safeName")
            out.parentFile?.mkdirs()
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    // v1.3: 用 FileReader 自动检测编码
                    val text = com.luody.mkdn.util.FileReader.readText(input)
                    out.parentFile?.mkdirs()
                    out.writeText(text, Charsets.UTF_8)
                }
                repo.addRecent(out.absolutePath, name)
                recents = repo.loadRecents()
                onOpenFile(out.absolutePath, name)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "打开失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // v1.5.4: Intent 处理已移到 MkdnApp 顶层 LaunchedEffect，
    // HomeScreen 只负责正常文件选择流程


    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("mkdn", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onSurface
                ),
                actions = {
                    IconButton(onClick = { showFontMenu = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "字号", tint = colors.onSurface)
                    }
                    DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                        FontSize.values().forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.name}  (${f.sp}sp)") },
                                onClick = {
                                    fontSize = f
                                    prefs.fontSize = f
                                    showFontMenu = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "主题", tint = colors.onSurface)
                    }
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        DropdownMenuItem(text = { Text("浅色") }, onClick = { onThemeChange(MkdnThemeMode.LIGHT); showThemeMenu = false })
                        DropdownMenuItem(text = { Text("护眼（米黄）") }, onClick = { onThemeChange(MkdnThemeMode.SEPIA); showThemeMenu = false })
                        DropdownMenuItem(text = { Text("深色") }, onClick = { onThemeChange(MkdnThemeMode.DARK); showThemeMenu = false })
                    }
                    // v1.7.2-hide: 软隐藏高亮入口（主人决定隐藏标重点功能）
                    // 恢复时取消下面 3 行的注释即可
                    /*
                    IconButton(onClick = onOpenHighlights) {
                        Icon(Icons.Default.Bookmark, contentDescription = "高亮", tint = colors.onSurface)
                    }
                    */
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 大打开按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        picker.launch(arrayOf("text/markdown", "text/plain", "text/x-markdown", "application/octet-stream", "*/*"))
                    },
                color = colors.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("打开 Markdown / 文本文件", color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("支持 .md  .markdown  .txt", color = colors.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("最近文件", color = colors.onSurfaceVariant, fontSize = 14.sp)
            }

            if (recents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有打开过文件\n点击上方按钮打开 .md / .txt 文件",
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recents, key = { it.path }) { r ->
                        RecentRow(r,
                            onClick = {
                                if (java.io.File(r.path).exists()) onOpenFile(r.path, r.name)
                                else {
                                    android.widget.Toast.makeText(context, "文件不存在：${r.path}", android.widget.Toast.LENGTH_SHORT).show()
                                    repo.removeRecent(r.path)
                                    recents = repo.loadRecents()
                                }
                            },
                            onRemove = {
                                repo.removeRecent(r.path)
                                recents = repo.loadRecents()
                            }
                        )
                        HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    r: RecentItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalMkdnColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Description, contentDescription = null, tint = colors.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(r.name, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                r.path,
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "移除", tint = colors.onSurfaceVariant)
        }
    }
}

private fun queryName(context: android.content.Context, uri: Uri): String? {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else null
            } else null
        }
    } catch (e: Exception) { null }
}