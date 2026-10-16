import axios from 'axios'


const request = axios.create({

    // 用相对地址，前端由后端同源提供时无需关心主机名/IP
    baseURL:"",

    timeout:120000

})


// 请求拦截器
request.interceptors.request.use(

    config=>{


        const token = localStorage.getItem("token")


        if(token){

            config.headers.Authorization =
                "Bearer " + token

        }


        return config

    },


    error=>{

        return Promise.reject(error)

    }

)


// 响应拦截器：统一处理登录过期（令牌失效直接回登录页）
request.interceptors.response.use(

    response => response,

    error => {

        const status = error.response?.status

        if (status === 401) {

            localStorage.removeItem("token")
            localStorage.removeItem("role")

            if (!location.hash.startsWith("#/login")) {
                location.hash = "#/login"
            }
        }

        return Promise.reject(error)

    }

)


export default request
