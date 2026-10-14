import {
    createRouter,
    createWebHashHistory
} from "vue-router"

import Login from "../views/Login.vue"
import Register from "../views/Register.vue"
import MainLayout from "../layouts/MainLayout.vue"
import AdminLayout from "../layouts/AdminLayout.vue"
import Dashboard from "../views/Dashboard.vue"
import StudyPlan from "../views/StudyPlan.vue"
import StudyPlanDetail from "../views/StudyPlanDetail.vue"
import Tasks from "../views/Tasks.vue"
import Knowledge from "../views/Knowledge.vue"
import Chat from "../views/Chat.vue"
import Profile from "../views/Profile.vue"
import AdminDashboard from "../views/admin/AdminDashboard.vue"
import AdminUsers from "../views/admin/AdminUsers.vue"
import AdminDocuments from "../views/admin/AdminDocuments.vue"
import AdminPlans from "../views/admin/AdminPlans.vue"


const router = createRouter({

    history: createWebHashHistory(),

    routes: [

        {
            path: "/login",
            component: Login
        },

        {
            path: "/register",
            component: Register
        },

        {
            path: "/",
            component: MainLayout,
            redirect: "/dashboard",
            children: [
                {
                    path: "dashboard",
                    component: Dashboard,
                    meta: { title: "学习看板" }
                },
                {
                    path: "study",
                    component: StudyPlan,
                    meta: { title: "学习计划" }
                },
                {
                    path: "study/detail/:id",
                    component: StudyPlanDetail,
                    meta: { title: "计划详情" }
                },
                {
                    path: "tasks",
                    component: Tasks,
                    meta: { title: "每日任务" }
                },
                {
                    path: "knowledge",
                    component: Knowledge,
                    meta: { title: "知识库" }
                },
                {
                    path: "chat",
                    component: Chat,
                    meta: { title: "AI 对话" }
                },
                {
                    path: "profile",
                    component: Profile,
                    meta: { title: "个人中心" }
                }
            ]
        },

        {
            path: "/admin",
            component: AdminLayout,
            redirect: "/admin/dashboard",
            children: [
                {
                    path: "dashboard",
                    component: AdminDashboard,
                    meta: { title: "数据总览" }
                },
                {
                    path: "users",
                    component: AdminUsers,
                    meta: { title: "用户管理" }
                },
                {
                    path: "documents",
                    component: AdminDocuments,
                    meta: { title: "资料管理" }
                },
                {
                    path: "plans",
                    component: AdminPlans,
                    meta: { title: "计划管理" }
                }
            ]
        }

    ]

})


router.beforeEach((to) => {

    const token = localStorage.getItem("token")
    const role = localStorage.getItem("role")

    if (to.path !== "/login"
        && to.path !== "/register"
        && !token) {
        return "/login"
    }

    if (to.path.startsWith("/admin")
        && role !== "ADMIN") {
        return "/dashboard"
    }

    return true

})


export default router
