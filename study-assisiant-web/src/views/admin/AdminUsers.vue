<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          clearable
          style="width: 280px"
      />
    </div>

    <el-table :data="filtered" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
            {{ row.role === "ADMIN" ? "管理员" : "普通用户" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" min-width="170" />
      <el-table-column label="操作" width="140" fixed="right">
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
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request from "../../api/request"

const users = ref([])
const keyword = ref("")

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter((u) =>
    [u.username, u.nickname, u.email]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(k))
  )
})

const load = async () => {
  const res = await request.get("/api/admin/users")
  users.value = res.data.data
}

const changeRole = async (user, role) => {
  try {
    await request.put(`/api/admin/users/${user.id}/role`, { role })
    ElMessage.success("修改成功")
    load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "修改失败")
  }
}

onMounted(load)
</script>


<style scoped>
.toolbar {
  margin-bottom: 14px;
}
</style>
