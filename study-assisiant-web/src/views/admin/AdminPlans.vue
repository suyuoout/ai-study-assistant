<template>
  <el-card shadow="never">
    <el-table :data="plans" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="username" label="所属用户" min-width="120" />
      <el-table-column prop="goal" label="目标" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
              size="small"
              type="danger"
              plain
              @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request from "../../api/request"

const plans = ref([])

const load = async () => {
  const res = await request.get("/api/admin/plans")
  plans.value = res.data.data
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除计划「${row.title}」？`,
      "提示",
      { type: "warning" }
    )
    await request.delete(`/api/admin/plans/${row.id}`)
    ElMessage.success("删除成功")
    load()
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error.response?.data?.message || "删除失败")
    }
  }
}

onMounted(load)
</script>
