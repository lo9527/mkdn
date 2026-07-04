package com.luody.mkdn.util

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * v1.3+: 文件读取工具
 * - 自动检测编码（UTF-8 BOM / UTF-16 BOM / UTF-8 / GBK）
 * - 移除 BOM
 *
 * v1.6: 大文件保护 —— 超过 MAX_SIZE_BYTES 直接抛错，避免 OOM
 */
object FileReader {

    private val GBK_CHARSET: Charset = Charset.forName("GBK")

    /** 10 MB —— 超过此大小直接拒绝加载并抛错 */
    const val MAX_SIZE_BYTES = 10L * 1024 * 1024

    fun readText(file: File): String {
        // v1.6: 大文件保护
        val len = file.length()
        if (len > MAX_SIZE_BYTES) {
            throw IllegalArgumentException(
                "文件过大（${len / 1024 / 1024}MB > ${MAX_SIZE_BYTES / 1024 / 1024}MB），请用桌面编辑器打开"
            )
        }
        val bytes = file.readBytes()
        return decodeBytes(bytes)
    }

    fun readText(stream: InputStream): String {
        val bytes = stream.readBytes()
        // v1.6: 流式读取也走统一阈值校验
        if (bytes.size > MAX_SIZE_BYTES) {
            throw IllegalArgumentException(
                "文件过大（${bytes.size / 1024 / 1024}MB > ${MAX_SIZE_BYTES / 1024 / 1024}MB），请用桌面编辑器打开"
            )
        }
        return decodeBytes(bytes)
    }

    private fun decodeBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        // 1. UTF-8 BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        // 2. UTF-16 LE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        // 3. UTF-16 BE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        // 4. 严格 UTF-8 解码
        return try {
            val strict = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            strict.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            // 5. fallback: GBK（Windows 写 .md 常用）
            try { String(bytes, GBK_CHARSET) } catch (e2: Exception) { String(bytes, Charsets.UTF_8) }
        }
    }
}
