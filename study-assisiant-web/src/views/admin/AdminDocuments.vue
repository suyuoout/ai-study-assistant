<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="total">共 {{ total }} 份资料</span>
    </div>

    <el-table :data="documents" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="170"
                       show-overflow-tooltip />
      <el-table-column prop="username" label="上传用户" min-width="110" />
      <el-table-column prop="fileType" label="类型" width="80" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tooltip v-if="row.errorMessage" :content="row.errorMessage"
                      placement="top">
            <el-tag type="danger">解析失败</el-tag>
          </el-tooltip>
          <el-tag v-else
                  :type="row.status === 'READY' ? 'success'
                    : row.status === 'PROCESSING' ? 'warning' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="remove(row)">
            删除
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
  </el-card>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../../api/request"
import { formatDateTime } from "../../utils/format"

const documents = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await request.get("/api/admin/documents", {
      params: { page: page.value, size: size.value }
    })
    documents.value = res.data.data.list || []
    total.value = res.data.data.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载资料失败"))
  } finally {
    loading.value = false
  }
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

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除资料「${row.title}」？磁盘文件会一并删除。`, "提示",
      { type: "warning" })
    await request.delete(`/api/admin/documents/${row.id}`)
    ElMessage.success("删除成功")
    load()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

onMounted(load)
</script>


<style scoped>
.toolbar {
  margin-bottom: 14px;
}

.total {
  color: var(--ink-caption);
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
