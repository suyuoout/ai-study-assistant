<template>
  <div class="profile">
    <el-alert
        v-if="user.mustChangePassword"
        type="warning"
        show-icon
        :closable="false"
        title="请先修改初始密码"
        description="当前账号使用的是系统分配的初始密码，修改后其他设备需要重新登录。"
        class="alert"
    />

    <el-card shadow="never" class="profile-card">
      <template #header>
        <span>个人资料</span>
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
          <el-button type="primary" :loading="savingProfile" @click="save">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="profile-card">
      <template #header>
        <span>修改密码</span>
      </template>

      <el-form label-width="80px" style="max-width: 420px">
        <el-form-item label="当前密码">
          <el-input v-model="password.oldPassword" type="password"
                    show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="password.newPassword" type="password"
                    show-password placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="password.confirm" type="password"
                    show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPassword"
                     @click="changePassword">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="profile-card">
      <template #header>
        <div class="card-head">
          <span>我的 AI Key</span>
          <span class="card-sub">
            今天已用系统额度 {{ usage.systemKeyCallsToday || 0 }}
            <template v-if="usage.dailyLimit > 0">
              / {{ usage.dailyLimit }} 次
            </template>
            <template v-else>（不限制）</template>
          </span>
        </div>
      </template>

      <p class="tip">
        不填则使用系统默认 Key（每天有调用次数限制）；填上你自己的 DeepSeek Key 后，
        AI 调用走你自己的额度，且不受系统次数限制。Key 会加密后保存，页面只显示掩码。
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
          <el-button type="primary" :loading="savingKey" @click="saveAiKey">
            保存 Key
          </el-button>
          <el-button @click="clearAiKey">清除</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>


<script setup>
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import request, { errorMessage } from "../api/request"

const router = useRouter()
const user = ref({})
const aiKey = ref("")
const usage = ref({})
const savingProfile = ref(false)
const savingPassword = ref(false)
const savingKey = ref(false)

const password = reactive({
  oldPassword: "",
  newPassword: "",
  confirm: ""
})

const load = async () => {
  try {
    const [me, aiUsage] = await Promise.all([
      request.get("/api/users/me"),
      request.get("/api/users/me/ai-usage")
    ])
    user.value = me.data.data || {}
    usage.value = aiUsage.data.data || {}
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载个人信息失败"))
  }
}

const save = async () => {
  savingProfile.value = true
  try {
    const res = await request.put("/api/users/me", {
      nickname: user.value.nickname,
      email: user.value.email
    })
    ElMessage.success(res.data.message || "保存成功")
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "保存失败"))
  } finally {
    savingProfile.value = false
  }
}

const changePassword = async () => {
  if (!password.oldPassword) {
    ElMessage.warning("请输入当前密码")
    return
  }
  if (!password.newPassword || password.newPassword.length < 8) {
    ElMessage.warning("新密码至少 8 位")
    return
  }
  if (password.newPassword !== password.confirm) {
    ElMessage.warning("两次输入的新密码不一致")
    return
  }

  savingPassword.value = true
  try {
    const res = await request.put("/api/users/me/password", {
      oldPassword: password.oldPassword,
      newPassword: password.newPassword
    })
    ElMessage.success(res.data.message || "密码修改成功")
    localStorage.removeItem("token")
    localStorage.removeItem("role")
    router.push("/login")
  } catch (error) {
    ElMessage.error(errorMessage(error, "修改失败"))
  } finally {
    savingPassword.value = false
  }
}

const saveAiKey = async () => {
  savingKey.value = true
  try {
    const res = await request.put("/api/users/me/ai-key", {
      aiKey: aiKey.value.trim()
    })
    ElMessage.success(res.data.message || "保存成功")
    aiKey.value = ""
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "保存失败"))
  } finally {
    savingKey.value = false
  }
}

const clearAiKey = async () => {
  aiKey.value = ""
  try {
    const res = await request.put("/api/users/me/ai-key", { aiKey: "" })
    ElMessage.success(res.data.message || "已清除")
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "操作失败"))
  }
}

onMounted(load)
</script>


<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 780px;
}

.alert {
  margin-bottom: 0;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-sub {
  color: var(--ink-caption);
  font-size: 12px;
  font-weight: normal;
}

.tip {
  color: var(--ink-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 16px;
}
</style>
