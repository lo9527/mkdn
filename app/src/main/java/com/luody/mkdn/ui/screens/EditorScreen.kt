package com.luody.mkdn.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luody.mkdn.ui.theme.LocalMkdnColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    path: String,
    name: String,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMkdnColors.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    // v1.6: dirty 改为先 false，文件加载完后和初值比较 —— 只有真的不同才视为脏
    // 否则打开未改动的文件按"保存"按钮会被禁用（v1.5 bug）
    var initialText by remember { mutableStateOf("") }
    var dirty by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        val loaded = withContext(Dispatchers.IO) { com.luody.mkdn.util.FileReader.readText(File(path)) }
        text = loaded
        initialText = loaded
        // 首次加载时 dirty=false
        dirty = false
    }

    val tryExit: () -> Unit = {
        if (dirty) showUnsavedDialog = true
        else onCancel()
    }

    // v1.6: 系统返回键拦截 → 走 tryExit（未保存时弹确认对话框）
    BackHandler(enabled = true) {
        tryExit()
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("编辑 · $name", maxLines = 1, color = colors.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                navigationIcon = {
                    IconButton(onClick = tryExit) {
                        Icon(Icons.Default.Close, contentDescription = "取消", tint = colors.onSurface)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        // v1.6: 保存前先备份原文件（.bak），崩溃了能恢复
                                        val src = File(path)
                                        if (src.exists()) {
                                            val bak = File(path + ".bak")
                                            bak.writeText(initialText, Charsets.UTF_8)
                                        }
                                        src.writeText(text, Charsets.UTF_8)
                                    }
                                }.onSuccess {
                                    onSave()
                                }.onFailure {
                                    android.widget.Toast.makeText(context, "保存失败: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = dirty
                    ) {
                        Text("保存", color = if (dirty) colors.primary else colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = {
                text = it
                // v1.6: 与初值比较 —— 改回原内容也算干净
                dirty = (it != initialText)
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.onSurface,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text("在这里编辑 Markdown…", color = colors.onSurfaceVariant, fontSize = 16.sp)
                    }
                    inner()
                }
            }
        )
    }

    // v1.6: 未保存退出确认
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("当前文件已修改但未保存，确定放弃修改吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onCancel()
                }) { Text("放弃", color = colors.primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) { Text("继续编辑") }
            }
        )
    }
}
