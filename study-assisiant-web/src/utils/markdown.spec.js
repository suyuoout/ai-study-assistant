// @vitest-environment jsdom
import { describe, expect, it } from "vitest"
import { renderMarkdown } from "./markdown"

describe("renderMarkdown", () => {
  it("渲染常见 Markdown 结构", () => {
    const html = renderMarkdown("**加粗** 和 `code`\n\n- 第一项\n- 第二项")
    expect(html).toContain("<strong>加粗</strong>")
    expect(html).toContain("<code>code</code>")
    expect(html).toContain("<li>第一项</li>")
  })

  it("代码块带高亮与 hljs 容器", () => {
    const html = renderMarkdown("```java\nint a = 1;\n```")
    expect(html).toContain("hljs")
    expect(html).toContain("int")
  })

  it("过滤脚本注入，避免 XSS", () => {
    const html = renderMarkdown(
      "<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>")
    // 原始 HTML 标签必须被转义，而不是原样输出
    expect(html).not.toContain("<script")
    expect(html).not.toContain("<img")
    expect(html).toContain("&lt;script&gt;")
  })

  it("过滤 javascript: 伪协议的链接", () => {
    const html = renderMarkdown("[点我](javascript:alert(1))")
    expect(html).not.toContain("href=\"javascript")
  })

  it("空输入返回空字符串", () => {
    expect(renderMarkdown("")).toBe("")
    expect(renderMarkdown(null)).toBe("")
  })
})
