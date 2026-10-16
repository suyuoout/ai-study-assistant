<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="auth-title serif-display">智能学习助手</div>
      <div class="auth-sub">登录你的学习空间</div>

      <el-form @submit.prevent>
        <el-form-item>
          <el-input
              v-model="username"
              placeholder="用户名"
              size="large"
              :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="password"
              type="password"
              show-password
              placeholder="密码"
              size="large"
              :prefix-icon="Lock"
              @keyup.enter="login"
          />
        </el-form-item>
        <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            @click="login"
        >
          登 录
        </el-button>
      </el-form>

      <div v-if="allowRegister" class="auth-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
      <div v-else class="auth-link muted">
        本环境未开放注册，需要账号请联系管理员开通
      </div>
    </el-card>
  </div>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import { User, Lock } from "@element-plus/icons-vue"
import request, { errorMessage } from "../api/request"

const username = ref("")
const password = ref("")
const loading = ref(false)
const allowRegister = ref(true)
const route = useRoute()
const router = useRouter()

onMounted(async () => {
  try {
    const res = await request.get("/api/public/config")
    allowRegister.value = res.data.data?.allowRegister !== false
  } catch (error) {
    // 拿不到配置就保持默认（显示注册入口）
  }
})

const login = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning("请输入用户名和密码")
    return
  }
  loading.value = true
  try {
    const res = await request.post("/api/users/login", {
      username: username.value.trim(),
      password: password.value
    })
    const data = res.data.data
    localStorage.setItem("token", data.token)
    localStorage.setItem("role", data.user.role)
    ElMessage.success("登录成功")

    if (data.user.mustChangePassword) {
      await ElMessageBox.alert(
        "当前使用的是初始密码，请先修改密码。", "安全提示",
        { type: "warning", confirmButtonText: "去修改" }
      )
      router.push("/profile")
      return
    }

    const redirect = route.query.redirect
    router.push(typeof redirect === "string" && redirect.startsWith("/")
      ? redirect : "/dashboard")
  } catch (error) {
    ElMessage.error(errorMessage(error, "登录失败"))
  } finally {
    loading.value = false
  }
}
</script>


<style scoped>
.auth-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a0a;
}

.auth-card {
  width: 400px;
  border-radius: 16px;
  padding: 12px 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
}

.auth-title {
  text-align: center;
  font-size: 26px;
  font-weight: bold;
  color: #f2f2f2;
}

.auth-sub {
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  margin: 8px 0 26px;
}

.submit {
  width: 100%;
}

.auth-link {
  text-align: center;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.auth-link.muted {
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.6;
}
</style>
