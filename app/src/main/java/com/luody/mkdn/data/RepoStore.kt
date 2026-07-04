package com.luody.mkdn.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * 简易 JSON 持久化：最近文件 + 高亮
 * v1.2 升级：高亮用「文本指纹」定位（前后各 24 字符 hash + offset），
 * 文件编辑后只要前后文不变就仍能定位。
 */
class RepoStore(context: Context) {
    private val dir: File = File(context.filesDir, "repo").apply { mkdirs() }
    private val recentsFile = File(dir, "recents.json")
    private val highlightsFile = File(dir, "highlights.json")

    // ---- recents ----
    fun loadRecents(): List<RecentItem> {
        if (!recentsFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(recentsFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RecentItem(
                    path = o.getString("path"),
                    name = o.getString("name"),
                    openedAt = o.getLong("openedAt")
                )
            }.sortedByDescending { it.openedAt }
        } catch (e: Exception) { emptyList() }
    }

    fun addRecent(path: String, name: String) {
        val list = loadRecents().filterNot { it.path == path }.toMutableList()
        list.add(0, RecentItem(path, name, System.currentTimeMillis()))
        val arr = JSONArray()
        list.take(50).forEach { r ->
            arr.put(JSONObject().apply {
                put("path", r.path); put("name", r.name); put("openedAt", r.openedAt)
            })
        }
        recentsFile.writeText(arr.toString())
    }

    fun removeRecent(path: String) {
        val list = loadRecents().filterNot { it.path == path }
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("path", r.path); put("name", r.name); put("openedAt", r.openedAt)
            })
        }
        recentsFile.writeText(arr.toString())
    }

    // ---- highlights ----
    fun loadHighlights(): List<HighlightItem> {
        if (!highlightsFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(highlightsFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HighlightItem(
                    id = o.getLong("id"),
                    filePath = o.getString("filePath"),
                    fileName = o.getString("fileName"),
                    startOffset = o.optInt("startOffset", 0),
                    endOffset = o.optInt("endOffset", 0),
                    text = o.getString("text"),
                    createdAt = o.getLong("createdAt"),
                    // 旧数据可能没有 fp 字段，给个 fallback
                    prefixFp = o.optString("prefixFp", ""),
                    suffixFp = o.optString("suffixFp", ""),
                    // 兼容：在原始 markdown 中的 offset（v1.2 新增；旧数据没有则用 0）
                    sourceOffset = o.optInt("sourceOffset", 0)
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) { emptyList() }
    }

    fun addHighlight(item: HighlightItem) {
        val list = loadHighlights().toMutableList()
        list.add(0, item)
        saveHighlights(list)
    }

    fun removeHighlight(id: Long) {
        saveHighlights(loadHighlights().filterNot { it.id == id })
    }

    fun highlightsForFile(path: String): List<HighlightItem> =
        loadHighlights().filter { it.filePath == path }

    private fun saveHighlights(list: List<HighlightItem>) {
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id); put("filePath", h.filePath); put("fileName", h.fileName)
                put("startOffset", h.startOffset); put("endOffset", h.endOffset)
                put("text", h.text); put("createdAt", h.createdAt)
                put("prefixFp", h.prefixFp); put("suffixFp", h.suffixFp)
                put("sourceOffset", h.sourceOffset)
            })
        }
        highlightsFile.writeText(arr.toString())
    }
}

data class RecentItem(
    val path: String,
    val name: String,
    val openedAt: Long
)

/**
 * 高亮项
 *
 * @param startOffset / endOffset 渲染后 TextView 中的位置（运行时用）
 * @param sourceOffset 原始 markdown 中的位置（指纹定位后用；编辑文件仍能找回）
 * @param prefixFp / suffixFp 文本指纹（前/后各 24 字符 SHA-1 截 8 位）
 */
data class HighlightItem(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val createdAt: Long,
    val prefixFp: String = "",
    val suffixFp: String = "",
    val sourceOffset: Int = 0
) {
    companion object {
        const val FP_CONTEXT = 24  // 前后各取 24 字符做指纹

        fun makeFingerprint(source: String, offset: Int, length: Int): Triple<String, String, Int> {
            val start = offset.coerceAtLeast(0)
            val end = (offset + length).coerceAtMost(source.length)
            val preStart = (start - FP_CONTEXT).coerceAtLeast(0)
            val sufEnd = (end + FP_CONTEXT).coerceAtMost(source.length)
            val pre = source.substring(preStart, start)
            val suf = source.substring(end, sufEnd)
            val preFp = sha1Short(pre)
            val sufFp = sha1Short(suf)
            return Triple(preFp, sufFp, start)
        }

        private fun sha1Short(s: String): String {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
            return bytes.take(4).joinToString("") { "%02x".format(it) }
        }
    }
}