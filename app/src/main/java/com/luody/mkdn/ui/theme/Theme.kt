package com.luody.mkdn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * v1.3 重设计：默认白底 + 主流审美排版
 *
 * 三套主题：
 * - LIGHT  纯白底（默认）— 干净、像微信读书 / One Markdown
 * - SEPIA  护眼米黄 — 长时间阅读
 * - DARK   深色
 */
data class MkdnColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val highlight: Color,
    val linkColor: Color,
    val codeBg: Color,
    val codeText: Color,
    val border: Color,
    val quoteBar: Color,
    val quoteBg: Color,
)

// === 主流审美配色（参考 One Markdown / Bear / 微信读书） ===
val LightMkdn = MkdnColors(
    background = Color(0xFFFFFFFF),     // 纯白底
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),      // 主文字：近黑
    onSurfaceVariant = Color(0xFF59636E), // 次文字：深灰
    primary = Color(0xFF0969DA),         // 主题色：GitHub 蓝（链接/强调）
    highlight = Color(0xFFFFF59D),       // 高亮：暖黄
    linkColor = Color(0xFF0969DA),       // 链接：蓝色
    codeBg = Color(0xFFF6F8FA),          // 行内代码：浅灰（GitHub 风）
    codeText = Color(0xFFCF222E),        // 行内代码：红
    border = Color(0xFFE1E4E8),          // 浅边框
    quoteBar = Color(0xFF54A0FF),
    quoteBg = Color(0xFFF6F8FA),
)

val DarkMkdn = MkdnColors(
    background = Color(0xFF0D1117),      // GitHub dark
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    primary = Color(0xFF58A6FF),
    highlight = Color(0xFF7D5108),
    linkColor = Color(0xFF58A6FF),
    codeBg = Color(0xFF161B22),
    codeText = Color(0xFFFF7B72),
    border = Color(0xFF30363D),
    quoteBar = Color(0xFF58A6FF),
    quoteBg = Color(0xFF161B22),
)

val SepiaMkdn = MkdnColors(
    background = Color(0xFFFAF3DF),      // 护眼米黄
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF3D2F1F),
    onSurfaceVariant = Color(0xFF7A6A50),
    primary = Color(0xFF8B4513),
    highlight = Color(0xFFFFD580),
    linkColor = Color(0xFF1565C0),
    codeBg = Color(0xFFEFE6CC),
    codeText = Color(0xFFB0311A),
    border = Color(0xFFD9CFB3),
    quoteBar = Color(0xFF8B4513),
    quoteBg = Color(0xFFEFE6CC),
)

enum class MkdnThemeMode { LIGHT, DARK, SEPIA }

private val LightScheme = lightColorScheme(
    primary = LightMkdn.primary,
    background = LightMkdn.background,
    surface = LightMkdn.surface,
    onSurface = LightMkdn.onSurface,
    onBackground = LightMkdn.onSurface,
)

private val DarkScheme = darkColorScheme(
    primary = DarkMkdn.primary,
    background = DarkMkdn.background,
    surface = DarkMkdn.surface,
    onSurface = DarkMkdn.onSurface,
    onBackground = DarkMkdn.onSurface,
)

@Composable
fun MkdnTheme(
    mode: MkdnThemeMode = MkdnThemeMode.LIGHT,  // v1.3: 默认纯白
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        MkdnThemeMode.LIGHT -> LightScheme
        MkdnThemeMode.DARK -> DarkScheme
        MkdnThemeMode.SEPIA -> LightScheme
    }
    val mkdnColors = when (mode) {
        MkdnThemeMode.LIGHT -> LightMkdn
        MkdnThemeMode.DARK -> DarkMkdn
        MkdnThemeMode.SEPIA -> SepiaMkdn
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMkdnColors provides mkdnColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}

val LocalMkdnColors = staticCompositionLocalOf { LightMkdn }

/**
 * v1.3 字号档位（sp）— 加更多档位，覆盖小屏到大屏、不同视力
 *
 * 参考微信读书档位（4 档）+ 加细（每档 2sp 步进）
 */
enum class FontSize(val sp: Int) {
    XS(13),   // 极小：技术文档紧凑浏览
    S(15),    // 小：默认
    M(17),    // 中：推荐（v1.0 默认）
    L(19),    // 大
    XL(22),   // 特大
    XXL(26),  // 极大
    XXXL(30)  // 极特大：老人 / 视力辅助
}

/**
 * v1.3 行高倍率（基于主流审美）
 *  - 1.6 是 One Markdown / Bear / Typora 通用值
 *  - 中文需要比英文稍宽，行高 1.65 比较合适
 */
object Layout {
    const val LINE_HEIGHT_MULT = 1.65f
    const val PARAGRAPH_SPACING_DP = 12
    const val HEADING_SPACING_DP = 16
    const val LIST_INDENT_DP = 20
    const val HORIZONTAL_PADDING_DP = 20
    const val CODE_BG_ALPHA = 0.5f
}
