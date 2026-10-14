<template>
  <el-container class="admin-layout">
    <el-aside width="224px" class="aside">
      <div class="logo serif-display">智能学习助手</div>
      <div class="logo-sub">管理后台</div>

      <el-menu
          :default-active="$route.path"
          router
          background-color="#0f0f0f"
          text-color="rgba(255,255,255,0.6)"
          active-text-color="#ffffff"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>数据总览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/documents">
          <el-icon><Folder /></el-icon>
          <span>资料管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/plans">
          <el-icon><Tickets /></el-icon>
          <span>计划管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title serif-display">
          {{ $route.meta.title || "管理后台" }}
        </div>
        <div class="actions">
          <el-button link @click="$router.push('/dashboard')">
            返回前台
          </el-button>
          <el-button type="danger" link @click="logout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>


<script setup>
import { useRouter } from "vue-router"

const router = useRouter()

const logout = () => {
  localStorage.removeItem("token")
  localStorage.removeItem("role")
  router.push("/login")
}
</script>


<style scoped>
.admin-layout {
  height: 100vh;
}

.aside {
  background: #0f0f0f;
  border-right: 1px solid var(--hairline);
}

.logo {
  color: #f2f2f2;
  font-size: 19px;
  font-weight: bold;
  padding: 24px 22px 4px;
}

.logo-sub {
  color: var(--ink-caption);
  font-size: 12px;
  padding: 0 22px 18px;
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

.title {
  font-size: 20px;
  font-weight: bold;
  color: #f2f2f2;
}

.main {
  background: #0a0a0a;
}
</style>
