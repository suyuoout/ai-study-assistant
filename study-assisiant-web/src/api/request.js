import axios from "axios"
import { ElMessage } from "element-plus"

const request = axios.create({
    // 用相对地址，前端由后端同源提供时无需关心主机名/IP
    baseURL: "",
    // 普通问答与资料解析可能较慢，保留较长超时
    timeout: 120000
})

request.interceptors.request.use(
    config => {
        const token = localStorage.getItem("token")
        if (token) {
            config.headers.Authorization = "Bearer " + token
        }
        return config
    },
    error => Promise.reject(error)
)

let redirecting = false

/** 统一取出后端返回的可读错误信息 */
export const errorMessage = (error, fallback = "操作失败") => {
    if (error?.response?.data?.message) {
        return error.response.data.message
    }
    if (error?.code === "ECONNABORTED") {
        return "请求超时，请稍后重试"
    }
    if (error?.message === "Network Error") {
        return "网络异常，请检查后端服务是否已启动"
    }
    return error?.message || fallback
}

// 响应拦截器：登录过期统一处理
request.interceptors.response.use(
    response => response,
    error => {
        const status = error.response?.status
        const message = errorMessage(error)

        if (status === 401) {
            localStorage.removeItem("token")
            localStorage.removeItem("role")
            if (!redirecting) {
                redirecting = true
                ElMessage.warning(message || "登录状态已过期，请重新登录")
                const redirect = encodeURIComponent(
                    location.hash.replace(/^#/, "") || "/dashboard")
                location.hash = `#/login?redirect=${redirect}`
                setTimeout(() => {
                    redirecting = false
                }, 1000)
            }
        }

        return Promise.reject(error)
    }
)

export default request
