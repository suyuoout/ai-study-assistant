import MarkdownIt from "markdown-it"
import hljs from "highlight.js/lib/common"
import DOMPurify from "dompurify"
import "highlight.js/styles/github-dark.css"

/**
 * AI 回答的 Markdown 渲染。
 *
 * 两道防线：markdown-it 关闭 HTML 直出（html: false），
 * 渲染结果再交给 DOMPurify 过滤，因此 v-html 是安全的。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(code, {
          language,
          ignoreIllegals: true
        }).value}</code></pre>`
      } catch (error) {
        // 高亮失败就退化成纯文本
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(code)}</code></pre>`
  }
})

const SAFE_CONFIG = {
  ADD_ATTR: ["target", "rel"],
  FORBID_TAGS: ["style", "iframe", "form", "input"],
  FORBID_ATTR: ["style", "onerror", "onload"]
}

export const renderMarkdown = (text) =>
  DOMPurify.sanitize(md.render(text || ""), SAFE_CONFIG)
