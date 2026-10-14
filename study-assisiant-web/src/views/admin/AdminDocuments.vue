<template>
  <el-card shadow="never">
    <el-table :data="documents" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="username" label="上传用户" min-width="120" />
      <el-table-column prop="fileType" label="类型" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'READY' ? 'success' : 'warning'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="170" />
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

const documents = ref([])

const load = async () => {
  const res = await request.get("/api/admin/documents")
  documents.value = res.data.data
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除资料「${row.title}」？`,
      "提示",
      { type: "warning" }
    )
    await request.delete(`/api/admin/documents/${row.id}`)
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
