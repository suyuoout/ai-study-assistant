<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          clearable
          style="width: 280px"
          @keyup.enter="search"
          @clear="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
      <span class="total">共 {{ total }} 位用户</span>
      <el-button
          class="create-btn"
          type="primary"
          plain
          @click="openCreate"
      >
        新建用户
      </el-button>
    </div>

    <el-table :data="users" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="170" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
            {{ row.role === "ADMIN" ? "管理员" : "普通用户" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? "正常" : "已禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="个人 Key" width="110">
        <template #default="{ row }">
          <span v-if="row.aiKeyMasked" class="muted">已配置</span>
          <span v-else class="muted">未配置</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button
              v-if="row.role !== 'ADMIN'"
              size="small"
              type="primary"
              plain
              @click="changeRole(row, 'ADMIN')"
          >
            设为管理员
          </el-button>
          <el-button
              v-else
              size="small"
              type="warning"
              plain
              @click="changeRole(row, 'USER')"
          >
            取消管理员
          </el-button>
          <el-button
              size="small"
              :type="row.enabled ? 'danger' : 'success'"
              plain
              @click="toggleEnabled(row)"
          >
            {{ row.enabled ? "禁用" : "启用" }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
    />

    <el-dialog v-model="createVisible" title="新建用户" width="520px">
      <el-form label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username"
                    placeholder="字母、数字、下划线，3-50 位" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="createForm.password" type="password"
                    show-password placeholder="至少 8 位，首次登录后需修改" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 160px">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createUser">
          创建
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>


<script setup>
import { ref, reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../../api/request"
import { formatDateTime } from "../../utils/format"

const users = ref([])
const keyword = ref("")
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)

const createForm = reactive({
  username: "",
  password: "",
  nickname: "",
  email: "",
  role: "USER"
})

const load = async () => {
  loading.value = true
  try {
    const res = await request.get("/api/admin/users", {
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    users.value = res.data.data.list || []
    total.value = res.data.data.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载用户失败"))
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const onPageChange = (value) => {
  page.value = value
  load()
}

const onSizeChange = (value) => {
  size.value = value
  page.value = 1
  load()
}

const changeRole = async (user, role) => {
  try {
    await request.put(`/api/admin/users/${user.id}/role`, { role })
    ElMessage.success("修改成功")
    load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "修改失败"))
  }
}

const toggleEnabled = async (user) => {
  const nextEnabled = !user.enabled
  try {
    if (!nextEnabled) {
      await ElMessageBox.confirm(
        `确认禁用「${user.username}」？禁用后该账号将无法登录。`,
        "提示", { type: "warning" })
    }
    await request.put(`/api/admin/users/${user.id}/enabled`,
      { enabled: nextEnabled })
    ElMessage.success(nextEnabled ? "已启用" : "已禁用")
    load()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "操作失败"))
    }
  }
}

const openCreate = () => {
  createForm.username = ""
  createForm.password = ""
  createForm.nickname = ""
  createForm.email = ""
  createForm.role = "USER"
  createVisible.value = true
}

const createUser = async () => {
  if (!createForm.username.trim()) {
    ElMessage.warning("请填写用户名")
    return
  }
  if (!createForm.password || createForm.password.length < 8) {
    ElMessage.warning("初始密码至少 8 位")
    return
  }
  creating.value = true
  try {
    const res = await request.post("/api/admin/users", { ...createForm })
    ElMessage.success(res.data.message || "创建成功")
    createVisible.value = false
    search()
  } catch (error) {
    ElMessage.error(errorMessage(error, "创建失败"))
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>


<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.total {
  color: var(--ink-caption);
  font-size: 13px;
}

.create-btn {
  margin-left: auto;
}

.muted {
  color: var(--ink-secondary);
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
