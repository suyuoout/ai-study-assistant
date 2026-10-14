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

  <el-card shadow="never" class="ai-card">
    <template #header>
      <span>我的 AI Key</span>
    </template>

    <p class="tip">
      不填则使用系统默认 Key（每天有调用次数限制）；填上你自己的 DeepSeek Key
      后，AI 调用走你自己的额度，且不受系统次数限制。
    </p>

    <el-form label-width="80px" style="max-width: 560px">
      <el-form-item label="当前">
        <el-tag v-if="user.aiKeyMasked" type="success">
          已配置：{{ user.aiKeyMasked }}
        </el-tag>
        <el-tag v-else type="info">未配置（使用系统 Key）</el-tag>
      </el-form-item>
      <el-form-item label="API Key">
        <el-input
            v-model="aiKey"
            placeholder="sk-... 留空保存可清除"
            show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveAiKey">保存 Key</el-button>
        <el-button @click="clearAiKey">清除</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request from "../api/request"

const user = ref({})
const aiKey = ref("")

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

const saveAiKey = async () => {
  try {
    const res = await request.put("/api/users/me/ai-key", {
      aiKey: aiKey.value.trim()
    })
    ElMessage.success(res.data.message)
    aiKey.value = ""
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "保存失败")
  }
}

const clearAiKey = async () => {
  aiKey.value = ""
  try {
    const res = await request.put("/api/users/me/ai-key", { aiKey: "" })
    ElMessage.success(res.data.message)
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "操作失败")
  }
}

onMounted(load)
</script>


<style scoped>
.profile-card {
  max-width: 760px;
  margin-bottom: 18px;
}

.ai-card {
  max-width: 760px;
}

.tip {
  color: var(--ink-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 16px;
}
</style>
