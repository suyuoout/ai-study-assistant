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


export default request
