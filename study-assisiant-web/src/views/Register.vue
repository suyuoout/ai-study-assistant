<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="auth-title serif-display">创建账号</div>
      <div class="auth-sub">开启你的智能学习之旅</div>

      <el-alert
          v-if="!allowRegister"
          type="warning"
          show-icon
          :closable="false"
          title="本环境未开放注册"
          description="请联系管理员在「管理后台 → 用户管理 → 新建用户」里为你开通账号。"
          class="notice"
      />

      <el-form @submit.prevent>
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" show-password placeholder="密码（至少8位）" size="large" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="nickname" placeholder="昵称" size="large" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="email" placeholder="邮箱（选填）" size="large" :prefix-icon="Message" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit"
                   :loading="loading" :disabled="!allowRegister"
                   @click="register">
          注 册
        </el-button>
      </el-form>

      <div class="auth-link">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { User, Lock, Avatar, Message } from "@element-plus/icons-vue"
import request from "../api/request"

const username = ref("")
const password = ref("")
const nickname = ref("")
const email = ref("")
const loading = ref(false)
const allowRegister = ref(true)
const router = useRouter()

onMounted(async () => {
  try {
    const res = await request.get("/api/public/config")
    allowRegister.value = res.data.data?.allowRegister !== false
  } catch (error) {
    // 拿不到配置就保持默认
  }
})

const register = async () => {
  if (!allowRegister.value) {
    ElMessage.warning("本环境未开放注册，请联系管理员开通账号")
    return
  }
  if (!username.value.trim()) {
    ElMessage.warning("请输入用户名")
    return
  }
  if (!password.value || password.value.length < 8) {
    ElMessage.warning("密码至少需要8位")
    return
  }
  loading.value = true
  try {
    const res = await request.post("/api/users/register", {
      username: username.value,
      password: password.value,
      nickname: nickname.value,
      email: email.value
    })
    ElMessage.success(res.data.message)
    router.push("/login")
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "注册失败")
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
  margin: 8px 0 24px;
}

.submit {
  width: 100%;
}

.auth-link {
  text-align: center;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.5);
}

.notice {
  margin-bottom: 18px;
}
</style>
