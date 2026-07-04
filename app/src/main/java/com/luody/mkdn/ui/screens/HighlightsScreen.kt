package com.luody.mkdn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luody.mkdn.data.HighlightItem
import com.luody.mkdn.data.RepoStore
import com.luody.mkdn.ui.theme.LocalMkdnColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsScreen(
    repo: RepoStore,
    onOpenHighlight: (HighlightItem) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalMkdnColors.current
    var items by remember { mutableStateOf(repo.loadHighlights()) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("我的高亮", color = colors.onSurface, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有高亮的段落\n阅读时选中文字即可标重点",
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { h ->
                        HighlightRow(
                            h = h,
                            onClick = { onOpenHighlight(h) },
                            onDelete = {
                                repo.removeHighlight(h.id)
                                items = repo.loadHighlights()
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
private fun HighlightRow(
    h: HighlightItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMkdnColors.current
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Bookmark, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                h.text,
                color = colors.onSurface,
                fontSize = 15.sp,
                maxLines = 3
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${h.fileName}  ·  ${df.format(Date(h.createdAt))}",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.onSurfaceVariant)
        }
    }
}