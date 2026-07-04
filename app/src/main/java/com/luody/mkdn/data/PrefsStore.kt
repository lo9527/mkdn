package com.luody.mkdn.data

import android.content.Context
import android.content.SharedPreferences
import com.luody.mkdn.ui.theme.FontSize
import com.luody.mkdn.ui.theme.MkdnThemeMode

/**
 * 全局设置（字号、主题）—— 用 SharedPreferences 存
 * 高亮/最近文件用 JSON 存
 */
class PrefsStore(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("mkdn_prefs", Context.MODE_PRIVATE)

    var fontSize: FontSize
        get() = FontSize.values().getOrElse(sp.getInt("font_size", FontSize.M.ordinal)) { FontSize.M }
        set(v) { sp.edit().putInt("font_size", v.ordinal).apply() }

    var themeMode: MkdnThemeMode
        get() = MkdnThemeMode.values().getOrElse(sp.getInt("theme", MkdnThemeMode.LIGHT.ordinal)) { MkdnThemeMode.LIGHT }
        set(v) { sp.edit().putInt("theme", v.ordinal).apply() }
}