package com.luody.mkdn.render

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import com.luody.mkdn.data.HighlightItem
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.SpanFactory
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import org.commonmark.node.Heading
import org.commonmark.node.Link

/**
 * Markdown 渲染器 v1.3 —— 主流审美重排版
 *
 * 排版规范：
 * - 行高 1.65（中文友好）
 * - 标题层级用 RelativeSizeSpan + Bold + 中等字重
 * - 链接加下划线
 * - 行内代码用半透明灰背景
 * - H1/H2 大字号 + 上下间距
 * - 高亮段（v1.2）：HighlightPlugin
 */
object MarkdownRenderer {

    @Volatile private var markwonRef: Markwon? = null

    fun get(context: Context): Markwon {
        markwonRef?.let { return it }
        synchronized(this) {
            markwonRef?.let { return it }
            val prism4j = Prism4j(MkdnGrammarLocator())
            val markwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDefault.create()))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            // v1.3 排版：链接蓝色 + 默认下划线
                            .linkColor(Color.parseColor("#0969DA"))
                            .isLinkUnderlined(true)
                            // 代码块配色（GitHub 风）
                            .codeTextColor(Color.parseColor("#CF222E"))
                            .codeBlockTextColor(Color.parseColor("#1F2328"))
                            .codeBlockBackgroundColor(Color.parseColor("#F6F8FA"))
                            // 引用块
                            .blockQuoteWidth(4)
                            .blockQuoteColor(Color.parseColor("#54A0FF"))
                            // 标题：不要横线分割
                            .headingBreakHeight(0)
                            // 列表
                            .bulletWidth(8)
                            .listItemColor(Color.parseColor("#1F2328"))
                            .bulletListItemStrokeWidth(2)
                    }
                })
                .usePlugin(object : AbstractMarkwonPlugin() {
                    // v1.3 排版：标题按层级分尺寸
                    override fun configureSpansFactory(builder: io.noties.markwon.MarkwonSpansFactory.Builder) {
                        builder.setFactory(Heading::class.java, SpanFactory { _, node ->
                            val h = node as Heading
                            val rel = when (h.level) {
                                1 -> 1.85f
                                2 -> 1.55f
                                3 -> 1.32f
                                4 -> 1.18f
                                5 -> 1.08f
                                else -> 1.0f
                            }
                            val sb = SpannableBuilder()
                            val spans: Array<Any> = arrayOf(
                                StyleSpan(Typeface.BOLD),
                                RelativeSizeSpan(rel),
                                TypefaceSpan("sans-serif-medium")
                            )
                            for (span in spans) {
                                SpannableBuilder.setSpans(sb, span, 0, 0)
                            }
                            sb
                        })
                    }
                })
                .build()
            markwonRef = markwon
            return markwon
        }
    }

    fun renderTo(
        textView: TextView,
        markdown: String,
        source: String = markdown,
        highlights: List<HighlightItem> = emptyList(),
        highlightColor: Int = Color.parseColor("#FFF59D")
    ) {
        // v1.3 排版：行高 1.65（中文友好）
        textView.setLineSpacing(0f, 1.65f)
        val builder = Markwon.builder(textView.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(textView.context))
            .usePlugin(TaskListPlugin.create(textView.context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SyntaxHighlightPlugin.create(
                Prism4j(MkdnGrammarLocator()),
                Prism4jThemeDefault.create()
            ))
            .usePlugin(HighlightPlugin(highlights, highlightColor, source))
        builder.build().setMarkdown(textView, markdown)
    }
}
