package com.luody.mkdn.render;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * 启用 prism4j-bundler 的代码语法高亮
 *
 * kapt 处理后会在 com.luody.mkdn.render.MkdnGrammarLocator 生成
 * 一个 GrammarLocator 实现（包含所有 prism4j 标准语言的 grammar）。
 */
@PrismBundle(
    includeAll = true,
    grammarLocatorClassName = ".MkdnGrammarLocator"
)
public class MkdnPrismBundleInit {
}
