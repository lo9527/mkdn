package com.luody.mkdn.render

import com.luody.mkdn.data.HighlightItem
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.core.CoreProps
import org.commonmark.node.Node
import org.commonmark.node.Text

/**
 * Markwon 插件：在 markdown 渲染时把"已保存的高亮段"用 BackgroundColorSpan 标出
 *
 * 工作机制（v1.2）：
 * 1. 用 sourceOffset 在 markdown 原文中定位高亮段（已通过 fingerprint 重定位）
 * 2. 渲染时维护一个"源 offset → 渲染后 offset"的累积映射
 * 3. 在 Text 节点 append 到 SpannableBuilder 之后，对属于高亮段的字符范围加 BackgroundColorSpan
 *
 * 简化策略：markwon 渲染时 Text 节点内容原样追加（控制符已被 commonmark 解析掉），
 * 所以 Text 节点字符串在源 markdown 中的位置 = 当前已扫描过的源字符数。
 * 我们把 source offset 转换为「已访问 Text 节点的字符串累积长度」，逐 Text 节点扫描。
 */
class HighlightPlugin(
    private val highlights: List<HighlightItem>,
    private val color: Int,
    private val sourceText: String
) : AbstractMarkwonPlugin() {

    override fun afterRender(node: Node, visitor: MarkwonVisitor) {
        // v1.7.2-hide: 软隐藏高亮渲染（标重点 UI 入口已关闭，主人决定不显示高亮）
        // 保留代码逻辑 + HighlightItem 数据，仅不渲染 span；恢复时删此行 return 即可
        return
    }
}
