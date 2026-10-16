/**
 * SSE 流式请求客户端。
 *
 * 用 fetch 而不是 EventSource：EventSource 不能带 Authorization 头，
 * 也不支持 POST。
 */
import { errorMessage } from "./request"

const parseFrame = (frame) => {
    let event = "message"
    let data = ""
    for (const line of frame.split("\n")) {
        if (line.startsWith("event:")) {
            event = line.slice(6).trim()
        } else if (line.startsWith("data:")) {
            data += line.slice(5).trim()
        }
    }
    if (!data) return null
    try {
        return { event, data: JSON.parse(data) }
    } catch (error) {
        return { event, data: { text: data } }
    }
}

/**
 * @param {string} url 请求地址
 * @param {object} body 请求体
 * @param {object} handlers { session, delta, sources, done, error }
 * @param {AbortSignal} signal 用于中途取消
 */
export async function streamRequest(url, body, handlers = {}, signal) {
    const token = localStorage.getItem("token")
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: "Bearer " + token } : {})
        },
        body: JSON.stringify(body),
        signal
    })

    if (!response.ok) {
        let message
        try {
            const payload = await response.json()
            message = payload?.message || ""
        } catch (error) {
            message = ""
        }
        const error = new Error(message || `请求失败（${response.status}）`)
        error.status = response.status
        throw error
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder("utf-8")
    let buffer = ""

    while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        let index
        while ((index = buffer.indexOf("\n\n")) >= 0) {
            const frame = buffer.slice(0, index)
            buffer = buffer.slice(index + 2)
            const parsed = parseFrame(frame)
            if (!parsed) continue
            const handler = handlers[parsed.event]
            if (handler) handler(parsed.data)
        }
    }
}

export { errorMessage }
