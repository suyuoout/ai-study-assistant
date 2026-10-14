<template>
  <el-card shadow="never" class="profile-card">
    <template #header>
      <span>个人中心</span>
    </template>

    <el-form label-width="80px" style="max-width: 420px">
      <el-form-item label="用户名">
        <el-input :model-value="user.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="user.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="user.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存修改</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request from "../api/request"

const user = ref({})

const load = async () => {
  const res = await request.get("/api/users/me")
  user.value = res.data.data
}

const save = async () => {
  try {
    const res = await request.put("/api/users/me", {
      nickname: user.value.nickname,
      email: user.value.email
    })
    ElMessage.success(res.data.message)
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "保存失败")
  }
}

onMounted(load)
</script>


<style scoped>
.profile-card {
  max-width: 640px;
}
</style>
