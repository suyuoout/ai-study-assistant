<template>
  <el-container class="layout">
    <el-aside width="224px" class="aside">
      <div class="brand serif-display">智能学习助手</div>

      <el-menu
          :default-active="activeMenu"
          router
          background-color="#0f0f0f"
          text-color="rgba(255,255,255,0.6)"
          active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>学习看板</span>
        </el-menu-item>
        <el-menu-item index="/study">
          <el-icon><Tickets /></el-icon>
          <span>学习计划</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><Calendar /></el-icon>
          <span>每日任务</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Folder /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 对话</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title serif-display">
          {{ $route.meta.title || "智能学习助手" }}
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
import { ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import request from "../api/request"

const route = useRoute()
const router = useRouter()
const username = ref("")
const isAdmin = ref(localStorage.getItem("role") === "ADMIN")

const avatarText = computed(() =>
  (username.value || "我").slice(0, 1)
)

const activeMenu = computed(() => {
  if (route.path.startsWith("/study")) return "/study"
  if (route.path.startsWith("/admin")) return "/admin"
  return route.path
})

router.afterEach(() => {
  isAdmin.value = localStorage.getItem("role") === "ADMIN"
})

onMounted(async () => {
  try {
    const res = await request.get("/api/users/me")
    const u = res.data.data
    username.value = u.nickname || u.username
  } catch (e) {
    // 忽略：仅影响右上角昵称展示
  }
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

.aside :deep(.el-menu) {
  border-right: none;
}

.aside :deep(.el-menu-item) {
  height: 42px;
  margin: 3px 12px;
  border-radius: 8px;
}

.aside :deep(.el-menu-item.is-active) {
  background: #1e1e1e;
  border: 1px solid var(--hairline);
  font-weight: bold;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0f0f0f;
  border-bottom: 1px solid var(--hairline);
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
