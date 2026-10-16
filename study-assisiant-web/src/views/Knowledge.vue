<template>
  <div>
    <el-card shadow="never" class="upload-card">
      <template #header>
        <div class="card-head">
          <span>上传学习资料</span>
          <span class="card-sub">支持 PDF / TXT / DOCX，单文件 20MB 以内</span>
        </div>
      </template>
      <div class="upload-row">
        <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.txt,.docx"
            :on-change="onFileChange"
        >
          <el-button>选择文件</el-button>
        </el-upload>
        <span v-if="file" class="file-name">{{ file.name }}</span>
        <el-button type="primary" :disabled="!file" :loading="uploading"
                   @click="upload">
          上传并解析
        </el-button>
        <el-progress v-if="uploading" :percentage="uploadPercent"
                     :stroke-width="6" style="width: 160px" />
      </div>
      <div class="hint">
        上传后解析在后台进行，状态会自动刷新；解析失败会显示原因。
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>我的资料</span>
          <span class="card-sub">
            共 {{ documents.length }} 份，可用
            {{ documents.filter(d => d.status === 'READY').length }} 份
          </span>
        </div>
      </template>
      <el-table :data="documents" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180"
                         show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tooltip v-if="row.errorMessage" :content="row.errorMessage"
                        placement="top">
              <el-tag type="danger">解析失败</el-tag>
            </el-tooltip>
            <el-tag v-else
                    :type="row.status === 'READY' ? 'success'
                      : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" @click="reprocess(row)">重新解析</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="rag-card">
      <template #header>
        <div class="card-head">
          <span>基于资料提问</span>
          <span class="card-sub">回答只依据你上传的资料，并标注来源</span>
        </div>
      </template>

      <div class="scope-row">
        <span class="scope-label">检索范围</span>
        <el-select
            v-model="selectedDocIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部可用资料"
            style="width: 360px"
        >
          <el-option
              v-for="doc in readyDocuments"
              :key="doc.id"
              :label="doc.title"
              :value="doc.id"
          />
        </el-select>
      </div>

      <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="例如：什么是数据库索引？"
      />
      <div class="ask-actions">
        <el-button type="primary" :loading="asking" @click="ask">
          提问
        </el-button>
        <el-button v-if="asking" @click="stopAsk">停止</el-button>
      </div>

      <div v-if="answer" class="answer">
        <div class="answer-label">回答</div>
        <div class="markdown" v-html="renderMarkdown(answer)"></div>
        <div v-if="sources.length" class="answer-sources">
          <el-popover
              v-for="(s, i) in sources"
              :key="i"
              placement="top"
              width="360"
              trigger="click"
          >
            <template #reference>
              <span class="src">
                <el-tag size="small" effect="plain">[{{ s.index || i + 1 }}]</el-tag>
                {{ s.documentTitle }}
                <span v-if="s.pageNumber">第 {{ s.pageNumber }} 页</span>
              </span>
            </template>
            <div class="src-preview">{{ s.preview || "（无预览）" }}</div>
          </el-popover>
        </div>
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" size="46%" :title="detail?.title || '资料详情'">
      <div v-if="detail" class="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="文件名">
            {{ detail.fileName }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ detail.fileType }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ statusText(detail.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ formatDateTime(detail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMessage" label="失败原因">
            {{ detail.errorMessage }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="chunk-head">
          <span>文本片段（共 {{ chunkTotal }} 段）</span>
          <el-select v-model="chunkPage" size="small" style="width: 120px"
                     @change="loadChunks">
            <el-option v-for="p in chunkPages" :key="p" :label="`第 ${p} 页`"
                       :value="p" />
          </el-select>
        </div>

        <div class="chunks" v-loading="loadingChunks">
          <div v-for="chunk in chunks" :key="chunk.id" class="chunk">
            <div class="chunk-meta">
              #{{ chunk.chunkIndex + 1 }}
              <span v-if="chunk.pageNumber">· 第 {{ chunk.pageNumber }} 页</span>
              <span>· {{ chunk.length }} 字</span>
            </div>
            <div class="chunk-text">{{ chunk.content }}</div>
          </div>
          <el-empty v-if="!loadingChunks && chunks.length === 0"
                    description="暂无片段（可能还在解析中）" :image-size="60" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>


<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../api/request"
import { streamRequest } from "../api/stream"
import { renderMarkdown } from "../utils/markdown"
import { formatDateTime } from "../utils/format"

const documents = ref([])
const file = ref(null)
const uploading = ref(false)
const uploadPercent = ref(0)
const loading = ref(false)
const question = ref("")
const asking = ref(false)
const answer = ref("")
const sources = ref([])
const selectedDocIds = ref([])

const detailVisible = ref(false)
const detail = ref(null)
const chunks = ref([])
const chunkTotal = ref(0)
const chunkPage = ref(1)
const chunkSize = 20
const loadingChunks = ref(false)

let pollTimer = null
let askController = null

const readyDocuments = computed(() =>
  documents.value.filter((doc) => doc.status === "READY")
)

const chunkPages = computed(() => {
  const pages = Math.ceil(chunkTotal.value / chunkSize)
  return Array.from({ length: Math.max(1, pages) }, (_, i) => i + 1)
})

const statusText = (status) => {
  if (status === "READY") return "可检索"
  if (status === "PROCESSING") return "解析中"
  if (status === "FAILED") return "解析失败"
  return "待解析"
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await request.get("/api/documents")
    documents.value = res.data.data || []
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载资料失败"))
  } finally {
    loading.value = false
  }
}

const onFileChange = (uploadFile) => {
  file.value = uploadFile.raw
}

const upload = async () => {
  if (!file.value) return
  const form = new FormData()
  form.append("file", file.value)
  uploading.value = true
  uploadPercent.value = 0
  try {
    const res = await request.post("/api/documents", form, {
      onUploadProgress: (event) => {
        if (event.total) {
          uploadPercent.value = Math.round((event.loaded / event.total) * 100)
        }
      }
    })
    ElMessage.success(res.data.message || "上传成功")
    file.value = null
    await loadDocuments()
    startPolling()
  } catch (error) {
    ElMessage.error(errorMessage(error, "上传失败"))
  } finally {
    uploading.value = false
  }
}

/** 有资料还在解析时轮询状态，最多约 3 分钟 */
const startPolling = () => {
  stopPolling()
  let rounds = 0
  pollTimer = setInterval(async () => {
    rounds += 1
    await loadDocuments()
    const processing = documents.value.some((d) => d.status === "PROCESSING")
    if (!processing || rounds > 90) stopPolling()
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const reprocess = async (row) => {
  try {
    await request.post(`/api/documents/${row.id}/reprocess`)
    ElMessage.success("已开始重新解析")
    await loadDocuments()
    startPolling()
  } catch (error) {
    ElMessage.error(errorMessage(error, "解析失败"))
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.title}」？`, "提示",
      { type: "warning" })
    await request.delete(`/api/documents/${row.id}`)
    ElMessage.success("删除成功")
    loadDocuments()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

const openDetail = async (row) => {
  detail.value = row
  detailVisible.value = true
  chunkPage.value = 1
  await loadChunks()
}

const loadChunks = async () => {
  if (!detail.value) return
  loadingChunks.value = true
  try {
    const res = await request.get(`/api/documents/${detail.value.id}/chunks`, {
      params: { page: chunkPage.value, size: chunkSize }
    })
    chunks.value = res.data.data.list || []
    chunkTotal.value = res.data.data.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载片段失败"))
  } finally {
    loadingChunks.value = false
  }
}

const ask = async () => {
  const text = question.value.trim()
  if (!text || asking.value) return

  answer.value = ""
  sources.value = []
  asking.value = true
  askController = new AbortController()

  try {
    await streamRequest("/api/ai/rag/chat/stream", {
      question: text,
      documentIds: selectedDocIds.value.length ? selectedDocIds.value : null
    }, {
      sources: (data) => {
        sources.value = data.sources || []
      },
      delta: (data) => {
        answer.value += data.text || ""
      },
      done: (data) => {
        if (!answer.value && data.answer) answer.value = data.answer
        if (data.sources) sources.value = data.sources
      },
      error: (data) => {
        ElMessage.error(data.message || "提问失败")
      }
    }, askController.signal)
  } catch (error) {
    if (error.name !== "AbortError") {
      ElMessage.error(errorMessage(error, "提问失败"))
    }
  } finally {
    asking.value = false
    askController = null
  }
}

const stopAsk = () => {
  if (askController) askController.abort()
}

onMounted(async () => {
  await loadDocuments()
  if (documents.value.some((d) => d.status === "PROCESSING")) startPolling()
})

onUnmounted(stopPolling)
</script>


<style scoped>
.upload-card,
.table-card {
  margin-bottom: 18px;
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

.upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-name {
  color: var(--ink-secondary);
}

.hint {
  margin-top: 10px;
  color: var(--ink-caption);
  font-size: 12px;
}

.scope-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.scope-label {
  color: var(--ink-caption);
  font-size: 13px;
}

.ask-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
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

.markdown {
  line-height: 1.8;
}

.markdown :deep(p) {
  margin: 0 0 8px;
}

.markdown :deep(pre) {
  background: #0d0d0d;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 10px 12px;
  overflow-x: auto;
}

.markdown :deep(code) {
  font-family: "Cascadia Code", Consolas, Monaco, monospace;
  font-size: 13px;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 20px;
}

.answer-sources {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.src {
  font-size: 13px;
  color: var(--ink-caption);
  cursor: pointer;
}

.src-preview {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.chunk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 10px;
}

.chunks {
  min-height: 120px;
}

.chunk {
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.chunk-meta {
  color: var(--ink-caption);
  font-size: 12px;
  margin-bottom: 6px;
}

.chunk-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-secondary);
  white-space: pre-wrap;
}
</style>
