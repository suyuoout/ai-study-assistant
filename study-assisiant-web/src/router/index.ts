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
import Profile from "../views/Profile.vue"

// 这些页面体积较大或使用频率较低，按需加载，减小首屏体积
const Knowledge = () => import("../views/Knowledge.vue")
const Chat = () => import("../views/Chat.vue")
const AdminDashboard = () => import("../views/admin/AdminDashboard.vue")
const AdminUsers = () => import("../views/admin/AdminUsers.vue")
const AdminDocuments = () => import("../views/admin/AdminDocuments.vue")
const AdminPlans = () => import("../views/admin/AdminPlans.vue")


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
        },

        // 兜底：未知路径回到看板
        {
            path: "/:pathMatch(.*)*",
            redirect: "/dashboard"
        }

    ]

})


router.beforeEach((to) => {

    const token = localStorage.getItem("token")
    const role = localStorage.getItem("role")

    // 已登录用户访问登录/注册页直接进看板
    if ((to.path === "/login" || to.path === "/register") && token) {
        return "/dashboard"
    }

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
