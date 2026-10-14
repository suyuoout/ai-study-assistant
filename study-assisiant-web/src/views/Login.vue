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

      <div class="auth-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>


<script setup>
import { ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { User, Lock } from "@element-plus/icons-vue"
import request from "../api/request"

const username = ref("")
const password = ref("")
const loading = ref(false)
const router = useRouter()

const login = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning("请输入用户名和密码")
    return
  }
  loading.value = true
  try {
    const res = await request.post("/api/users/login", {
      username: username.value,
      password: password.value
    })
    localStorage.setItem("token", res.data.data.token)
    localStorage.setItem("role", res.data.data.user.role)
    ElMessage.success("登录成功")
    router.push("/dashboard")
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "登录失败")
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
}
</style>
