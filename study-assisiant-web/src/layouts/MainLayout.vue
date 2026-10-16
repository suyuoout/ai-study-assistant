<template>
  <el-container class="layout">
    <el-aside v-if="!narrow" width="224px" class="aside">
      <div class="brand serif-display">智能学习助手</div>
      <el-menu
          :default-active="activeMenu"
          router
          background-color="#0f0f0f"
          text-color="rgba(255,255,255,0.6)"
          active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <el-badge v-if="item.badge" :value="item.badge" class="badge" />
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-drawer v-model="drawerVisible" direction="ltr" size="240px"
               :with-header="false" class="drawer">
      <div class="brand serif-display">智能学习助手</div>
      <el-menu
          :default-active="activeMenu"
          router
          background-color="#0f0f0f"
          text-color="rgba(255,255,255,0.6)"
          active-text-color="#ffffff"
          @select="drawerVisible = false"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="narrow" class="hamburger" @click="drawerVisible = true">
            <Menu />
          </el-icon>
          <div class="page-title serif-display">
            {{ $route.meta.title || "智能学习助手" }}
          </div>
        </div>

        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="28">{{ avatarText }}</el-avatar>
            <span class="username">{{ username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="isAdmin" command="admin">
                管理后台
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>


<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  ArrowDown, Calendar, ChatDotRound, Folder, Menu, Odometer, Setting,
  Tickets, User
} from "@element-plus/icons-vue"
import request from "../api/request"

const route = useRoute()
const router = useRouter()
const username = ref("")
const isAdmin = ref(localStorage.getItem("role") === "ADMIN")
const pendingCount = ref(0)
const narrow = ref(window.innerWidth < 1100)
const drawerVisible = ref(false)

const menus = computed(() => {
  const items = [
    { path: "/dashboard", label: "学习看板", icon: Odometer },
    { path: "/study", label: "学习计划", icon: Tickets },
    { path: "/tasks", label: "每日任务", icon: Calendar, badge: pendingCount.value },
    { path: "/knowledge", label: "知识库", icon: Folder },
    { path: "/chat", label: "AI 对话", icon: ChatDotRound },
    { path: "/profile", label: "个人中心", icon: User }
  ]
  if (isAdmin.value) {
    items.push({ path: "/admin", label: "管理后台", icon: Setting })
  }
  return items
})

const avatarText = computed(() => (username.value || "我").slice(0, 1))

const activeMenu = computed(() => {
  if (route.path.startsWith("/study")) return "/study"
  if (route.path.startsWith("/admin")) return "/admin"
  return route.path
})

const onResize = () => {
  narrow.value = window.innerWidth < 1100
  if (!narrow.value) drawerVisible.value = false
}

router.afterEach(() => {
  isAdmin.value = localStorage.getItem("role") === "ADMIN"
})

const onCommand = (cmd) => {
  if (cmd === "logout") {
    localStorage.removeItem("token")
    localStorage.removeItem("role")
    router.push("/login")
  } else {
    router.push("/" + cmd)
  }
}

onMounted(async () => {
  window.addEventListener("resize", onResize)
  try {
    const [me, pending] = await Promise.all([
      request.get("/api/users/me"),
      request.get("/api/tasks/upcoming", { params: { days: 0 } })
    ])
    const user = me.data.data
    username.value = user.nickname || user.username
    isAdmin.value = user.role === "ADMIN"
    pendingCount.value = (pending.data.data || []).length
  } catch (error) {
    // 仅影响展示，忽略
  }
})

onUnmounted(() => window.removeEventListener("resize", onResize))
</script>


<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #0f0f0f;
  border-right: 1px solid var(--hairline);
}

.brand {
  color: #f2f2f2;
  font-size: 19px;
  font-weight: bold;
  padding: 24px 22px;
}

.aside :deep(.el-menu),
.drawer :deep(.el-menu) {
  border-right: none;
  background: #0f0f0f;
}

.aside :deep(.el-menu-item),
.drawer :deep(.el-menu-item) {
  height: 42px;
  margin: 3px 12px;
  border-radius: 8px;
}

.aside :deep(.el-menu-item.is-active) {
  background: #1e1e1e;
  border: 1px solid var(--hairline);
  font-weight: bold;
}

.badge {
  margin-left: 8px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0f0f0f;
  border-bottom: 1px solid var(--hairline);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  font-size: 18px;
  cursor: pointer;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #f2f2f2;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: rgba(255, 255, 255, 0.75);
}

.main {
  background: #0a0a0a;
  overflow-y: auto;
}
</style>
