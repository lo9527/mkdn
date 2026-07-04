package com.luody.mkdn.ui

import android.content.Context

/**
 * v1.3 修复：用 setter 注入 context，替代之前的全局 var
 * MainActivity.onCreate 时注入 applicationContext
 */
@Volatile internal var appContext: Context? = null
    private set

fun setAppContext(ctx: Context) {
    appContext = ctx.applicationContext
}
