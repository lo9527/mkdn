package com.luody.mkdn.ui.screens

import android.text.Layout as TextLayout
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.luody.mkdn.data.HighlightItem
import com.luody.mkdn.data.RepoStore
import com.luody.mkdn.render.HighlightLocator
import com.luody.mkdn.render.MarkdownRenderer
import com.luody.mkdn.ui.theme.FontSize
import com.luody.mkdn.ui.theme.Layout
import com.luody.mkdn.ui.theme.LocalMkdnColors
import com.luody.mkdn.ui.theme.MkdnThemeMode
import com.luody.mkdn.util.FileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    path: String,
    name: String,
    repo: RepoStore,
    fontSize: FontSize,
    onFontSizeChange: (FontSize) -> Unit,
    @Suppress("UNUSED_PARAMETER") themeMode: MkdnThemeMode,
    onThemeChange: (MkdnThemeMode) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalMkdnColors.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var content by remember { mutableStateOf("") }
    var toc by remember { mutableStateOf<List<TocItem>>(emptyList()) }
    var showToc by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSizeLocal by remember(fontSize) { mutableStateOf(fontSize) }

    LaunchedEffect(path) {
        val text = withContext(Dispatchers.IO) { FileReader.readText(File(path)) }
        content = text
        toc = parseToc(text)
        repo.addRecent(path, name)
    }

    val savedHighlightsBase = remember(path) { repo.highlightsForFile(path) }
    val highlightColor = colors.highlight.toArgb()

    // v1.6: 标重点后触发 update 重新渲染（让 HighlightPlugin 注入）
    var refreshTrigger by remember { mutableStateOf(0) }
    // 标重点后重新从 repo 读 highlights（repo.addHighlight 已更新数据）
    val savedHighlights = if (refreshTrigger > 0) repo.highlightsForFile(path) else savedHighlightsBase
    // v1.5 修复: 拿到 AndroidView 里的 TextView 句柄，供搜索/目录跳转 setSelection() 用
    // v1.7.7: 上移到 searchMatches 之前（之前 searchMatches 用到 textViewRef.value）
    val textViewRef = remember { mutableStateOf<android.widget.TextView?>(null) }
    // v1.2: 搜索匹配（按 markdown 原文 case-insensitive 搜索）
    // v1.7.7: 在 TextView 渲染文本里搜关键词，返回 rendered offset
    // 关键修复：之前用 source offset 当 rendered offset 传给 scrollToCharTarget，错位
    val textViewText = textViewRef.value?.text?.toString()
    val searchMatches = remember(searchQuery, textViewText, content) {
        if (searchQuery.isBlank()) emptyList()
        else {
            // 优先用 TextView 实际渲染文本（rendered offset，精准）；fallback 到 source
            val renderedText = textViewText ?: content
            findMatches(renderedText, searchQuery)
        }
    }
    var currentMatchIdx by remember { mutableStateOf(0) }

    // v1.5 修复: 跳转工具（搜索/目录）— 用 TextView 的 Layout 算精确 Y 坐标 + Selection 高亮
    val scrollToCharScope: (Int, Int) -> Unit = { charOffset, selectLen ->
        scrollToCharTarget(
            textView = textViewRef.value,
            scrollState = scroll,
            scope = scope,
            charOffset = charOffset,
            selectLen = selectLen,
            contentPaddingTop = 0
        )
    }

    // v1.5.2 修复: 回退到 v1.5 之前的 nav.popBackStack() 行为，
    // v1.5.1 用 activity.finish() 会全 App 退出，太粗暴；
    // v1.4/v1.5 "闪一下又回原文件" 的根因在 HomeScreen 的 LaunchedEffect 重入栈，
    // 下面 HomeScreen 已加 800ms 防重入锁，这里只走 popBackStack


    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        name,
                        color = colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; searchQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索", tint = colors.onSurface)
                    }
                    IconButton(onClick = { showToc = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "目录", tint = colors.onSurface)
                    }
                    IconButton(onClick = { showFontMenu = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "字号", tint = colors.onSurface)
                    }
                    DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                        FontSize.values().forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.name}  (${f.sp}sp)") },
                                onClick = {
                                    fontSizeLocal = f
                                    onFontSizeChange(f)
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 搜索栏
            if (showSearch) {
                Surface(color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; currentMatchIdx = 0 },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("搜索 ${name}...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                cursorColor = colors.primary
                            )
                        )
                        if (searchMatches.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text("${currentMatchIdx + 1}/${searchMatches.size}", color = colors.onSurfaceVariant, fontSize = 13.sp)
                            IconButton(onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIdx = (currentMatchIdx - 1 + searchMatches.size) % searchMatches.size
                                    val idx = searchMatches[currentMatchIdx]
                                    scrollToCharScope(idx, searchQuery.length)
                                }
                            }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上一个") }
                            IconButton(onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIdx = (currentMatchIdx + 1) % searchMatches.size
                                    val idx = searchMatches[currentMatchIdx]
                                    scrollToCharScope(idx, searchQuery.length)
                                }
                            }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下一个") }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (content.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(
                                horizontal = Layout.HORIZONTAL_PADDING_DP.dp,
                                vertical = Layout.PARAGRAPH_SPACING_DP.dp
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                android.widget.TextView(ctx).apply {
                                    setTextColor(colors.onSurface.toArgb())
                                    textSize = fontSizeLocal.sp.toFloat()
                                    // v1.3 排版：行高 1.65
                                    setLineSpacing(0f, Layout.LINE_HEIGHT_MULT)
                                    setPadding(0, 0, 0, 32)
                                    setTextIsSelectable(true)
                                    // v1.5 修复: 把 TextView 句柄存出去，供搜索/目录跳转 setSelection 用
                                    textViewRef.value = this
                                    customSelectionActionModeCallback = object : ActionMode.Callback {
                                        // v1.7.2-hide: 软隐藏标重点功能（v1.7.1 修复未生效，主人决定隐藏）
                                        // 保留所有代码和数据，仅 UI 入口关闭；恢复时改下面 4 处
                                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                                            // return false → 不添加自定义菜单项（系统默认菜单如复制/搜索仍可用）
                                            return false
                                        }
                                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
                                        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean = false
                                        override fun onDestroyActionMode(mode: ActionMode) {}
                                    }
                                }
                            },
                            update = { tv ->
                                tv.setTextColor(colors.onSurface.toArgb())
                                tv.textSize = fontSizeLocal.sp.toFloat()
                                // v1.3 排版：行高 1.65
                                tv.setLineSpacing(0f, Layout.LINE_HEIGHT_MULT)
                                if (content.isNotEmpty()) {
                                    // v1.2: 渲染时把已保存的高亮注入
                                    MarkdownRenderer.renderTo(
                                        tv, content,
                                        source = content,
                                        highlights = savedHighlights,
                                        highlightColor = highlightColor
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showToc) {
        AlertDialog(
            onDismissRequest = { showToc = false },
            title = { Text("目录") },
            text = {
                // v1.7.3 修复: 加 verticalScroll 让 42 个标题能完整滚动显示
                // 之前 Column 没滚动条，AlertDialog 默认 text 高度有限，靠后标题看不到
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (toc.isEmpty()) {
                        Text("本文档没有标题")
                    } else {
                        toc.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showToc = false
                                        // v1.7.6: 关键词定位 — 调用方需要传入 content（source 文本）
                                        scrollToTocByKeyword(textViewRef.value, scroll, scope, item, content)
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "  ".repeat(item.level - 1) + item.text,
                                    color = colors.onSurface,
                                    fontSize = (fontSizeLocal.sp - 2).sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToc = false }) { Text("关闭") }
            }
        )
    }
}

data class TocItem(val level: Int, val text: String, val sourceOffset: Int = 0, val orderIndex: Int = 0)

private fun parseToc(md: String): List<TocItem> {
    val out = mutableListOf<TocItem>()
    // v1.7.6: 关键词定位方案 —— 不再算 rendered offset
    // 跳转时直接用 textView.text.indexOf(item.text, lastFoundOffset) 动态找位置
    var cursor = 0
    var orderIndex = 0
    md.lineSequence().forEach { line ->
        val m = Regex("^(#{1,6})\\s+(.*)").find(line)
        if (m != null) {
            val level = m.groupValues[1].length
            val text = m.groupValues[2].trim()
            val sourceOffset = md.indexOf(line, cursor, ignoreCase = false)
            if (sourceOffset >= 0) {
                val item = TocItem(level = level, text = text, sourceOffset = sourceOffset, orderIndex = orderIndex)
                out.add(item)
                android.util.Log.d("MKDN_TOC", "[parseToc] #$orderIndex H$level '$text' sourceOffset=$sourceOffset")
                cursor = sourceOffset + line.length
                orderIndex++
            }
        } else {
            val idx = md.indexOf(line, cursor, ignoreCase = false)
            if (idx >= 0) cursor = idx + line.length
        }
    }
    android.util.Log.d("MKDN_TOC", "[parseToc] 共解析 ${out.size} 个标题")
    return out
}

/**
 * v1.5 修复: 精准跳转 + 高亮
 * - 用 TextView.getLayout() 精确算出目标字符的 Y 坐标（PX）
 * - 把字符行 Y 转成 ScrollState 的值并用 scope.launch 滚动
 * - Selection.setSelection() 同时给系统选中高亮（搜索场景）
 * - setSelection(int, int) 在 TextView 是 protected，所以走 Selection 静态工具
 */
private fun scrollToCharTarget(
    textView: android.widget.TextView?,
    scrollState: androidx.compose.foundation.ScrollState,
    scope: kotlinx.coroutines.CoroutineScope,
    charOffset: Int,
    selectLen: Int,
    contentPaddingTop: Int = 0
) {
    if (textView == null) {
        android.util.Log.w("MKDN_TOC", "[scrollToCharTarget] textView=null (AndroidView 还没初始化?)")
        return
    }
    val max = textView.text?.length ?: 0
    if (max == 0) {
        android.util.Log.w("MKDN_TOC", "[scrollToCharTarget] textView.text 空 (渲染还没完成?)")
        return
    }
    val safe = charOffset.coerceIn(0, max)
    val end = if (selectLen > 0) (safe + selectLen).coerceAtMost(max) else safe
    android.util.Log.d("MKDN_TOC", "[scrollToCharTarget] charOffset=$charOffset → safe=$safe (max=$max) selectLen=$selectLen")
    textView.post {
        val layout: TextLayout? = textView.layout
        if (layout == null) {
            android.util.Log.w("MKDN_TOC", "[scrollToCharTarget] textView.layout=null (布局还没好?)")
            return@post
        }
        val line = layout.getLineForOffset(safe)
        val yPx = layout.getLineTop(line).toFloat()
        val lineH = layout.getLineBottom(line) - layout.getLineTop(line)
        // v1.7.3 修复: viewport 应该是屏幕可视高度，不是 textView.height
        // 之前 textView.height=39168 是整篇内容的高度（Compose verticalScroll + AndroidView 已知坑）
        // 用 LocalConfiguration.screenHeightDp - topBar 估算 viewport ≈ 2000px
        val viewportPx = 2000
        val targetPx = (yPx - viewportPx / 3f).toInt().coerceAtLeast(0)
        android.util.Log.d("MKDN_TOC", "[scrollToCharTarget] safe=$safe → line=$line yPx=$yPx viewportPx=$viewportPx targetPx=$targetPx (textView.height=${textView.height})")
        // Compose ScrollState 单位是 PX，scrollTo 是 suspend，需在协程里调
        scope.launch {
            scrollState.scrollTo(targetPx)
        }
        // 系统级选中高亮（光标 + 蓝把 handle）
        val text = textView.text
        if (text is Spannable && safe <= end) {
            Selection.setSelection(text, safe, end)
        }
    }
}

/**
 * v1.7.6 关键词定位：markwon 渲染后用 textView.text.indexOf(item.text) 找标题位置
 *
 * 为什么用关键词：
 * - 之前算 source→rendered offset 受 emoji/中文 + mapSourceToRendered 简化算法影响
 * - markwon 渲染 markdown 时，标题文字部分几乎 1:1 保留（只是前面的 # 字符被吞掉）
 * - 用 textView.text 全文搜索 item.text 关键词 = 100% 准
 *
 * 同名标题处理：用上次找到的位置作 fromIndex，保证顺序匹配
 */
private fun scrollToTocByKeyword(
    textView: android.widget.TextView?,
    scrollState: androidx.compose.foundation.ScrollState,
    scope: kotlinx.coroutines.CoroutineScope,
    item: TocItem,
    source: String  // markdown 原文（用作 search fallback）
) {
    if (textView == null) {
        android.util.Log.w("MKDN_TOC", "[scrollToTocByKeyword] textView=null")
        return
    }
    val max = textView.text?.length ?: 0
    if (max == 0) {
        android.util.Log.w("MKDN_TOC", "[scrollToTocByKeyword] textView.text 空")
        return
    }
    android.util.Log.d("MKDN_TOC", "[scrollToTocByKeyword] H${item.level} '#${item.orderIndex}' '${item.text}' orderIndex=${item.orderIndex} (max=$max)")
    textView.post {
        val layout: TextLayout? = textView.layout
        if (layout == null) {
            android.util.Log.w("MKDN_TOC", "[scrollToTocByKeyword] textView.layout=null")
            return@post
        }
        val renderedText = textView.text.toString()
        // ★ 关键词定位：在 TextView 渲染文本里搜 item.text
        // 从 orderIndex 字符开始找（粗略从前一个标题位置往后，避免匹配到前面同名的标题）
        val fromIndex = if (item.orderIndex == 0) 0 else (item.orderIndex * 50).coerceAtMost(max - 1)
        var foundOffset = renderedText.indexOf(item.text, fromIndex)
        if (foundOffset < 0) {
            // 找不到就从头找
            foundOffset = renderedText.indexOf(item.text)
        }
        if (foundOffset < 0) {
            android.util.Log.w("MKDN_TOC", "[scrollToTocByKeyword] 关键词 '${item.text}' 在 TextView 里找不到！")
            return@post
        }
        val safe = foundOffset.coerceIn(0, max)
        val line = layout.getLineForOffset(safe)
        val yPx = layout.getLineTop(line).toFloat()
        val lineH = layout.getLineBottom(line) - layout.getLineTop(line)
        val viewportPx = 2000
        // 标题行中点停在屏幕 3/8 高度位置
        val targetPx = (yPx + lineH / 2f - viewportPx / 2f - viewportPx / 8f).toInt().coerceAtLeast(0)
        android.util.Log.d("MKDN_TOC", "[scrollToTocByKeyword] found='${item.text}' at safe=$safe → line=$line yPx=$yPx lineH=$lineH targetPx=$targetPx")
        scope.launch {
            scrollState.scrollTo(targetPx)
        }
    }
}

/** 在 markdown 中查找关键字所有出现位置（offset 列表） */
private fun findMatches(source: String, query: String): List<Int> {
    if (query.isBlank()) return emptyList()
    val out = mutableListOf<Int>()
    var idx = 0
    while (idx <= source.length - query.length) {
        val found = source.indexOf(query, idx, ignoreCase = true)
        if (found < 0) break
        out.add(found)
        idx = found + query.length
    }
    return out
}

// v1.6: 高亮 id 生成器 —— nanoTime 高位 48 位 + 自增 counter 低位 16 位
// 避免 System.currentTimeMillis() 在毫秒级冲突（连续标记多个高亮时 id 重复）
private val highlightIdCounter = java.util.concurrent.atomic.AtomicLong(0)
private fun generateHighlightId(): Long {
    val nanos = System.nanoTime() and 0xFFFFFFFFFFFFL  // 48 bits
    val counter = highlightIdCounter.incrementAndGet() and 0xFFFFL  // 16 bits
    return (nanos shl 16) or counter
}