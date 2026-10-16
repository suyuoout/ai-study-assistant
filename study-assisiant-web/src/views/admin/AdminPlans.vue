<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="total">共 {{ total }} 份计划</span>
    </div>

    <el-table :data="plans" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="170"
                       show-overflow-tooltip />
      <el-table-column prop="username" label="所属用户" min-width="110" />
      <el-table-column prop="goal" label="目标" min-width="180"
                       show-overflow-tooltip />
      <el-table-column prop="taskCount" label="任务数" width="90" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createTime" label="创建时间" min-width="160">
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

const plans = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await request.get("/api/admin/plans", {
      params: { page: page.value, size: size.value }
    })
    plans.value = res.data.data.list || []
    total.value = res.data.data.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载计划失败"))
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
      `确认删除计划「${row.title}」？该计划下的任务会一并删除。`, "提示",
      { type: "warning" })
    await request.delete(`/api/admin/plans/${row.id}`)
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
