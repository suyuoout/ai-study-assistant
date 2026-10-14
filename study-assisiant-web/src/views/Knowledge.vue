<template>
  <div>
    <el-card shadow="never" class="upload-card">
      <template #header>
        <span>上传学习资料</span>
      </template>
      <div class="upload-row">
        <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.txt,.docx"
            :on-change="onFileChange"
        >
          <el-button>选择文件（PDF / TXT / DOCX）</el-button>
        </el-upload>
        <span v-if="file" class="file-name">{{ file.name }}</span>
        <el-button type="primary" :disabled="!file" :loading="uploading" @click="upload">
          上传并解析
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <span>我的资料</span>
      </template>
      <el-table :data="documents" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'READY' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="reprocess(row)">重新解析</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="rag-card">
      <template #header>
        <span>基于资料提问</span>
      </template>
      <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="例如：什么是数据库索引？"
      />
      <el-button type="primary" class="ask-btn" :loading="asking" @click="ask">
        提问
      </el-button>

      <div v-if="answer" class="answer">
        <div class="answer-label">回答</div>
        <div class="answer-text">{{ answer }}</div>
        <div v-if="sources.length" class="answer-sources">
          <div v-for="(s, i) in sources" :key="i" class="src">
            <el-tag size="small" effect="plain">来源</el-tag>
            {{ s.documentTitle }}
            <span v-if="s.pageNumber">第 {{ s.pageNumber }} 页</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>


<script setup>
import { ref, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request from "../api/request"

const documents = ref([])
const file = ref(null)
const uploading = ref(false)
const question = ref("")
const asking = ref(false)
const answer = ref("")
const sources = ref([])

const loadDocuments = async () => {
  const res = await request.get("/api/documents")
  documents.value = res.data.data
}

const onFileChange = (uploadFile) => {
  file.value = uploadFile.raw
}

const upload = async () => {
  const form = new FormData()
  form.append("file", file.value)
  uploading.value = true
  try {
    const res = await request.post("/api/documents", form)
    ElMessage.success(res.data.message)
    file.value = null
    loadDocuments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "上传失败")
  } finally {
    uploading.value = false
  }
}

const reprocess = async (row) => {
  try {
    await request.post(`/api/documents/${row.id}/reprocess`)
    ElMessage.success("重新解析完成")
    loadDocuments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "解析失败")
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.title}」？`, "提示", { type: "warning" })
    await request.delete(`/api/documents/${row.id}`)
    ElMessage.success("删除成功")
    loadDocuments()
  } catch (e) {
    if (e !== "cancel") ElMessage.error(e.response?.data?.message || "删除失败")
  }
}

const ask = async () => {
  if (!question.value.trim()) return
  asking.value = true
  try {
    const res = await request.post("/api/ai/rag/chat", { question: question.value })
    answer.value = res.data.data.answer
    sources.value = res.data.data.sources || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "提问失败")
  } finally {
    asking.value = false
  }
}

onMounted(loadDocuments)
</script>


<style scoped>
.upload-card,
.table-card {
  margin-bottom: 18px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-name {
  color: var(--ink-secondary);
}

.ask-btn {
  margin-top: 10px;
}

.answer {
  margin-top: 16px;
  background: var(--panel-inset);
  border: 1px solid var(--hairline);
  padding: 16px;
  border-radius: 8px;
}

.answer-label {
  font-weight: bold;
  margin-bottom: 8px;
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.8;
}

.answer-sources {
  margin-top: 10px;
}

.src {
  font-size: 13px;
  color: var(--ink-caption);
  margin-top: 4px;
}
</style>
