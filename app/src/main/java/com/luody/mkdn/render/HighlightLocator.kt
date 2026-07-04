package com.luody.mkdn.render

import com.luody.mkdn.data.HighlightItem
import java.security.MessageDigest

/**
 * v1.2 高亮定位（在 markdown 原文用指纹定位 + 渲染后近似 offset）
 *
 * 简化策略：
 * - 用户在 TextView 选中文字 → 拿到 rendered offset [s, e)
 * - 我们要回存的是 markdown 原文 offset [ms, me) = (s, e) 的近似（多数情况 1:1）
 *   因为 Markwon 渲染时纯文本段几乎是 1:1 复制，控制符（*、`、# 等）被吞掉不影响高亮常见位置
 * - 重新打开文件时，先在 markdown 原文用 fingerprint 找回 sourceOffset
 *   再用 mapSourceToRendered 映射成 rendered offset 应用 span
 *
 * 失败处理：fingerprint 找不到 → 不显示该条高亮（不卡定位）
 */
object HighlightLocator {

    /**
     * 在 markdown 原文中用 fingerprint 找位置
     * @return sourceOffset (>= 0) 或 -1 (失败)
     */
    fun locateInSource(source: String, fp: HighlightItem): Int {
        if (fp.text.isEmpty()) return -1
        val length = fp.text.length
        if (length > source.length) return -1
        // 快速路径：原 offset 还匹配 + 指纹通过
        if (fp.sourceOffset in 0..(source.length - length)) {
            if (source.regionMatches(fp.sourceOffset, fp.text, 0, length)) {
                if (fingerprintMatches(source, fp.sourceOffset, length, fp.prefixFp, fp.suffixFp)) {
                    android.util.Log.d("MKDN_HL", "[locateInSource] 快速路径命中: srcOffset=${fp.sourceOffset} text='${fp.text}'")
                    return fp.sourceOffset
                }
            }
        }
        // 全文扫描：先指纹匹配
        if (fp.prefixFp.isNotEmpty() || fp.suffixFp.isNotEmpty()) {
            var idx = 0
            while (idx <= source.length - length) {
                if (source.regionMatches(idx, fp.text, 0, length)) {
                    if (fingerprintMatches(source, idx, length, fp.prefixFp, fp.suffixFp)) {
                        android.util.Log.d("MKDN_HL", "[locateInSource] 指纹扫描命中: srcOffset=$idx (原=${fp.sourceOffset}) text='${fp.text}'")
                        return idx
                    }
                }
                idx++
            }
        }
        // 弱匹配：忽略指纹
        var idx = 0
        while (idx <= source.length - length) {
            if (source.regionMatches(idx, fp.text, 0, length)) {
                android.util.Log.d("MKDN_HL", "[locateInSource] 弱匹配命中: srcOffset=$idx text='${fp.text}'")
                return idx
            }
            idx++
        }
        android.util.Log.w("MKDN_HL", "[locateInSource] 全部失败! text='${fp.text}' 原srcOffset=${fp.sourceOffset}")
        return -1
    }

    /**
     * 把 markdown 原文 offset 映射到渲染后 TextView offset
     * 简化算法：扫描 markdown，跳过 markdown 控制符（`*`、`#`、`` ` ``、`[`、`]`、`(`、`)`、`!`、`>`、`-`、数字+.、转义符）
     * 返回的 rendered offset 是「跳过控制符后的字符计数」，与 markwon 渲染结果近似
     */
    fun mapSourceToRendered(source: String, sourceOffset: Int): Int {
        if (sourceOffset <= 0) return 0
        if (sourceOffset >= source.length) return source.length
        var ren = 0
        var i = 0
        // 标记代码块边界
        while (i < sourceOffset) {
            val c = source[i]
            when {
                // 代码块 ```...```
                c == '`' && i + 2 < source.length && source[i + 1] == '`' && source[i + 2] == '`' -> {
                    val end = source.indexOf("```", i + 3)
                    if (end > 0) {
                        val blockLen = (end - i) + 3
                        if (i + blockLen <= sourceOffset) {
                            // 整块在前面：markwon 会渲染 ``` 和代码内容（控制符 + 文本都渲染），所以 ren += blockLen
                            ren += blockLen
                            i += blockLen
                        } else {
                            // offset 在块内：渲染长度 = 源长度
                            ren += (sourceOffset - i)
                            return ren
                        }
                    } else {
                        ren++; i++
                    }
                }
                // 行内代码 `text`
                c == '`' -> {
                    val end = source.indexOf('`', i + 1)
                    if (end > 0) {
                        val blockLen = (end - i) + 1
                        if (i + blockLen <= sourceOffset) {
                            ren += blockLen
                            i += blockLen
                        } else {
                            ren += (sourceOffset - i)
                            return ren
                        }
                    } else {
                        ren++; i++
                    }
                }
                // 强调 */_**/_/__ —— 简化处理：成对匹配，整段不计入 ren
                (c == '*' || c == '_') && i + 1 < source.length && (source[i + 1] == c) -> {
                    val end = findPairEnd(source, i + 2, c, c.toString().repeat(2))
                    if (end > 0) {
                        val blockLen = (end - i) + 2
                        if (i + blockLen <= sourceOffset) {
                            // 强调控制符 + 文本都渲染
                            ren += blockLen
                            i += blockLen
                        } else {
                            ren += (sourceOffset - i)
                            return ren
                        }
                    } else {
                        ren++; i++
                    }
                }
                (c == '*' || c == '_') -> {
                    val end = findPairEnd(source, i + 1, c, c.toString())
                    if (end > 0) {
                        val blockLen = (end - i) + 1
                        if (i + blockLen <= sourceOffset) {
                            ren += blockLen
                            i += blockLen
                        } else {
                            ren += (sourceOffset - i)
                            return ren
                        }
                    } else {
                        ren++; i++
                    }
                }
                // 标题 # - 列表标记 -、+、数字.、引用 >  在行首
                c == '#' || c == '>' -> {
                    // 简化：吞掉直到行尾（这些控制符 markwon 会渲染成图标或留空）
                    @Suppress("UNUSED_VARIABLE")
                    val lineEnd = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                    // 标题：行内 # 都被吞掉，文本正常
                    if (i + blockHeaderCount(source, i) <= sourceOffset) {
                        ren += (sourceOffset - i)
                        return ren
                    } else {
                        // 跳到第一个非控制符
                        i += blockHeaderCount(source, i)
                    }
                }
                // 链接 [text](url) - text 渲染，url 隐藏
                c == '[' -> {
                    val closeBracket = source.indexOf(']', i)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < source.length && source[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen > 0) source.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && openParen > 0 && closeParen > 0) {
                        val textLen = closeBracket - i - 1  // 去掉 [ 和 ]
                        val totalLen = (closeParen - i) + 1
                        if (i + totalLen <= sourceOffset) {
                            // 文本部分计入 ren，控制符不计入
                            ren += textLen
                            i += totalLen
                        } else if (sourceOffset <= closeBracket) {
                            // 在 [text 范围内，渲染 textLen 长度
                            ren += (sourceOffset - i - 1).coerceAtLeast(0)
                            return ren
                        } else {
                            // 在 (url) 范围内，渲染 0（url 隐藏）
                            ren += textLen
                            return ren
                        }
                    } else {
                        ren++; i++
                    }
                }
                // 图片 ![alt](url) - alt 渲染，!+url 隐藏
                c == '!' -> {
                    if (i + 1 < source.length && source[i + 1] == '[') {
                        val closeBracket = source.indexOf(']', i + 2)
                        val openParen = if (closeBracket > 0 && closeBracket + 1 < source.length && source[closeBracket + 1] == '(') closeBracket + 1 else -1
                        val closeParen = if (openParen > 0) source.indexOf(')', openParen) else -1
                        if (closeBracket > 0 && openParen > 0 && closeParen > 0) {
                            val altLen = closeBracket - i - 2
                            val totalLen = (closeParen - i) + 1
                            if (i + totalLen <= sourceOffset) {
                                ren += altLen
                                i += totalLen
                            } else {
                                ren += altLen
                                return ren
                            }
                        } else {
                            ren++; i++
                        }
                    } else {
                        ren++; i++
                    }
                }
                // 普通字符
                else -> { ren++; i++ }
            }
        }
        return ren
    }

    /**
     * v1.7.1 新增：把渲染后 TextView offset 反算回 markdown 原文 offset
     * 用途：用户在 TextView 选中文字（rendered offset）→ 反算到 source offset
     * 算法：和正向 mapSourceToRendered 镜像，但累计 ren 直到达到目标 renderedOffset
     *
     * 注意：本函数假设 markwon 渲染时文本几乎 1:1 复制（控制符被吞掉），
     * 所以 rendered offset N 对应的 source offset N+（前面所有被跳过的控制符数）
     */
    fun mapRenderedToSource(source: String, renderedOffset: Int): Int {
        if (renderedOffset <= 0) return 0
        if (source.isEmpty()) return 0
        var src = 0
        var ren = 0
        while (src < source.length && ren < renderedOffset) {
            val c = source[src]
            // ren 还需要多少字符到目标
            val need = renderedOffset - ren
            when {
                // 代码块 ```...```
                c == '`' && src + 2 < source.length && source[src + 1] == '`' && source[src + 2] == '`' -> {
                    val end = source.indexOf("```", src + 3)
                    if (end > 0) {
                        val blockLen = (end - src) + 3
                        // 代码块在 markwon 渲染时控制符 + 文本都保留 → 渲染长度 = source 长度
                        if (blockLen <= need) {
                            // 整个代码块在前面
                            ren += blockLen
                            src += blockLen
                        } else {
                            // offset 落在代码块内
                            src += need
                            return src
                        }
                    } else {
                        ren++; src++
                    }
                }
                // 行内代码 `text`
                c == '`' -> {
                    val end = source.indexOf('`', src + 1)
                    if (end > 0) {
                        val blockLen = (end - src) + 1
                        if (blockLen <= need) {
                            ren += blockLen
                            src += blockLen
                        } else {
                            src += need
                            return src
                        }
                    } else {
                        ren++; src++
                    }
                }
                // 强调 **/**/__/__（成对匹配）
                (c == '*' || c == '_') && src + 1 < source.length && (source[src + 1] == c) -> {
                    val end = findPairEnd(source, src + 2, c, c.toString().repeat(2))
                    if (end > 0) {
                        val blockLen = (end - src) + 2
                        if (blockLen <= need) {
                            ren += blockLen
                            src += blockLen
                        } else {
                            src += need
                            return src
                        }
                    } else {
                        ren++; src++
                    }
                }
                (c == '*' || c == '_') -> {
                    val end = findPairEnd(source, src + 1, c, c.toString())
                    if (end > 0) {
                        val blockLen = (end - src) + 1
                        if (blockLen <= need) {
                            ren += blockLen
                            src += blockLen
                        } else {
                            src += need
                            return src
                        }
                    } else {
                        ren++; src++
                    }
                }
                // 标题 # 引用 > 在行首
                c == '#' || c == '>' -> {
                    val prefixLen = blockHeaderCount(source, src)
                    // 标题/引用在 markwon 里渲染时控制符被吞掉，文本保留
                    // 但前缀部分（# 空格）不渲染 → 控制符跳过
                    // 简化处理：如果前缀完全在前面，整个前缀都不计入 ren
                    if (prefixLen <= need) {
                        // 整个标题前缀 + 文本都按 1:1 走
                        // 但前缀的 # 字符本身在 markwon 里不渲染（被吞）
                        // 这里简化：标题行整体按文本长度渲染（前缀跳过）
                        val lineEnd = source.indexOf('\n', src).let { if (it < 0) source.length else it }
                        val lineLen = lineEnd - src
                        if (lineLen <= need) {
                            ren += lineLen
                            src += lineLen
                        } else {
                            // offset 落在标题行内
                            // 但要跳过前缀的 # 和空格
                            src += prefixLen + need
                            return src.coerceAtMost(lineEnd)
                        }
                    } else {
                        // offset 落在标题前缀内（用户实际不会选到这，因为前缀不可见）
                        // 保守：返回 src
                        return src
                    }
                }
                // 链接 [text](url)
                c == '[' -> {
                    val closeBracket = source.indexOf(']', src)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < source.length && source[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen > 0) source.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && openParen > 0 && closeParen > 0) {
                        val textLen = closeBracket - src - 1  // text 长度
                        val totalLen = (closeParen - src) + 1
                        // 整个链接的渲染长度 = textLen（url 不渲染）
                        if (totalLen <= need) {
                            ren += textLen
                            src += totalLen
                        } else if (need <= textLen) {
                            // 在 text 部分内（rendered offset 在 text 内）
                            // src 当前是 [，need 是 text 内的偏移
                            // 对应 src 是 [ 后 need 个字符
                            src += 1 + need
                            return src
                        } else {
                            // 在 url 部分内（不应该发生，url 不可见）
                            return closeBracket + 1
                        }
                    } else {
                        ren++; src++
                    }
                }
                // 图片 ![alt](url)
                c == '!' -> {
                    if (src + 1 < source.length && source[src + 1] == '[') {
                        val closeBracket = source.indexOf(']', src + 2)
                        val openParen = if (closeBracket > 0 && closeBracket + 1 < source.length && source[closeBracket + 1] == '(') closeBracket + 1 else -1
                        val closeParen = if (openParen > 0) source.indexOf(')', openParen) else -1
                        if (closeBracket > 0 && openParen > 0 && closeParen > 0) {
                            val altLen = closeBracket - src - 2
                            val totalLen = (closeParen - src) + 1
                            // 图片渲染长度 = altLen
                            if (totalLen <= need) {
                                ren += altLen
                                src += totalLen
                            } else if (need <= altLen) {
                                // 在 alt 文本内
                                src += 2 + need
                                return src
                            } else {
                                return closeBracket + 1
                            }
                        } else {
                            ren++; src++
                        }
                    } else {
                        ren++; src++
                    }
                }
                // 普通字符 1:1
                else -> { ren++; src++ }
            }
        }
        return src
    }

    private fun findPairEnd(source: String, from: Int, ch: Char, seq: String): Int {
        var idx = from
        while (idx < source.length) {
            if (source[idx] == ch) {
                // 检查是否成对
                if (seq.length == 1) return idx
                if (idx + 1 < source.length && source[idx + 1] == ch) return idx
            }
            idx++
        }
        return -1
    }

    private fun blockHeaderCount(source: String, i: Int): Int {
        var j = i
        while (j < source.length && (source[j] == '#' || source[j] == ' ')) j++
        // 跳过所有 # 和空格
        var k = i
        while (k < source.length && source[k] == '#') k++
        while (k < source.length && source[k] == ' ') k++
        return k - i
    }

    private fun fingerprintMatches(
        source: String, offset: Int, length: Int, prefixFp: String, suffixFp: String
    ): Boolean {
        if (prefixFp.isEmpty() && suffixFp.isEmpty()) return true
        val preStart = (offset - HighlightItem.FP_CONTEXT).coerceAtLeast(0)
        val sufEnd = (offset + length + HighlightItem.FP_CONTEXT).coerceAtMost(source.length)
        val pre = source.substring(preStart, offset)
        val suf = source.substring(offset + length, sufEnd)
        return sha1Short(pre) == prefixFp && sha1Short(suf) == suffixFp
    }

    private fun sha1Short(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.take(4).joinToString("") { "%02x".format(it) }
    }
}