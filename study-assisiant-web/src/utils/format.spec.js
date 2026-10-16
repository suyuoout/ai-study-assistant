import { describe, expect, it } from "vitest"
import {
  addDays, formatDate, formatDateTime, formatMinutes, toDateString,
  todayString
} from "./format"

describe("format 日期工具", () => {
  it("toDateString 使用本地时区而不是 UTC", () => {
    // 本地时间凌晨 00:30 的一天，用 toISOString 会算成前一天
    const localEarlyMorning = new Date(2026, 0, 5, 0, 30)
    expect(toDateString(localEarlyMorning)).toBe("2026-01-05")

    const localLateNight = new Date(2026, 0, 5, 23, 30)
    expect(toDateString(localLateNight)).toBe("2026-01-05")
  })

  it("todayString 返回 10 位日期", () => {
    expect(todayString()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it("addDays 支持跨月与跨年", () => {
    expect(addDays("2026-01-31", 1)).toBe("2026-02-01")
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01")
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28")
  })

  it("formatDateTime 截断到分钟", () => {
    expect(formatDateTime("2026-10-16T08:05:11")).toBe("2026-10-16 08:05")
    expect(formatDateTime(null)).toBe("—")
  })

  it("formatDate 兼容带时间的字符串", () => {
    expect(formatDate("2026-10-16T00:00:00")).toBe("2026-10-16")
  })

  it("formatMinutes 输出可读文本", () => {
    expect(formatMinutes(0)).toBe("0 分钟")
    expect(formatMinutes(45)).toBe("45 分钟")
    expect(formatMinutes(60)).toBe("1 小时")
    expect(formatMinutes(95)).toBe("1 小时 35 分钟")
  })
})
