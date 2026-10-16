<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="total">共 {{ total }} 份资料</span>
      <el-tooltip
          content="上传目录挂在持久化存储、但数据库被重置后，用这个按钮把磁盘上已有的文件重新写回数据库"
          placement="top"
      >
        <el-button size="small" :loading="reindexing" @click="reindex">
          从挂载盘重建资料
        </el-button>
      </el-tooltip>
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
const reindexing = ref(false)

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

const reindex = async () => {
  try {
    await ElMessageBox.confirm(
      "将扫描上传目录，把磁盘上存在但数据库里没有的文件重新入库并解析。确认继续？",
      "重建资料索引", { type: "warning" })
  } catch (error) {
    return
  }

  reindexing.value = true
  try {
    const res = await request.post("/api/admin/documents/reindex")
    const data = res.data.data
    await ElMessageBox.alert(
      `扫描 ${data.scanned} 个文件：新恢复 ${data.recovered} 个、`
      + `已存在 ${data.skipped} 个、失败 ${data.failed} 个。`
      + "\n新恢复的资料正在后台解析，稍后刷新即可看到。",
      "重建完成", { type: "success" })
    load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "重建失败"))
  } finally {
    reindexing.value = false
  }
}

onMounted(load)
</script>


<style scoped>
.toolbar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
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
