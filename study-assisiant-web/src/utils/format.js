/**
 * 日期/时间格式化工具。
 *
 * 注意：不要用 toISOString() 取「今天」——那是 UTC 日期，
 * 在东八区凌晨 0-8 点会算成昨天，这里统一按本地时区计算。
 */

const pad = (value) => String(value).padStart(2, "0")

/** 本地时区的 yyyy-MM-dd */
export const toDateString = (date = new Date()) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

export const todayString = () => toDateString(new Date())

export const addDays = (dateString, days) => {
  const date = new Date(`${dateString}T00:00:00`)
  date.setDate(date.getDate() + days)
  return toDateString(date)
}

/** 后端返回 yyyy-MM-ddTHH:mm:ss，这里只保留到分钟 */
export const formatDateTime = (value) => {
  if (!value) return "—"
  const text = String(value)
  const matched = text.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/)
  return matched ? `${matched[1]} ${matched[2]}` : text
}

export const formatDate = (value) => {
  if (!value) return "—"
  return String(value).slice(0, 10)
}

/** 分钟转成「1 小时 30 分钟」这样的可读文本 */
export const formatMinutes = (minutes) => {
  const value = Number(minutes)
  if (!value || value <= 0) return "0 分钟"
  const hours = Math.floor(value / 60)
  const rest = value % 60
  if (hours === 0) return `${rest} 分钟`
  if (rest === 0) return `${hours} 小时`
  return `${hours} 小时 ${rest} 分钟`
}

export const weekdayOf = (dateString) => {
  const date = new Date(`${dateString}T00:00:00`)
  return "日一二三四五六"[date.getDay()]
}
