package com.luody.mkdn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luody.mkdn.data.PrefsStore
import com.luody.mkdn.ui.MkdnApp
import com.luody.mkdn.ui.theme.MkdnTheme
import com.luody.mkdn.ui.theme.MkdnThemeMode

class MainActivity : ComponentActivity() {
    // v1.6: 主题从 PrefsStore 读真实持久化值（不再写死 LIGHT）
    private var themeMode by mutableStateOf(MkdnThemeMode.LIGHT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 修复 v1.3: 提前把 context 注入到 MkdnApp 全局，供 Intent 解析使用
        com.luody.mkdn.ui.setAppContext(applicationContext)

        // v1.6: 从 prefs 读真实持久化值，避免启动时擦 prefs
        val prefs = PrefsStore(applicationContext)
        themeMode = prefs.themeMode

        setContent {
            MkdnTheme(mode = themeMode) {
                MkdnApp(
                    intent = intent,
                    themeMode = themeMode,
                    onThemeChange = { newMode ->
                        themeMode = newMode
                        // v1.6: 用户主动切主题时持久化（区别于 v1.5.2 的 LaunchedEffect 误写）
                        if (prefs.themeMode != newMode) prefs.themeMode = newMode
                    }
                )
            }
        }
    }
}
