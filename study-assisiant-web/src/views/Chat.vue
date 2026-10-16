<template>
  <el-card shadow="never" class="chat-card"
           :body-style="{ height: '100%', display: 'flex', flexDirection: 'column' }">
    <div class="chat-body">
      <aside class="sidebar">
        <el-button type="primary" class="new-btn" @click="newChat">
          新建对话
        </el-button>

        <el-radio-group v-model="mode" size="small" class="mode">
          <el-radio-button value="GENERAL">普通问答</el-radio-button>
          <el-radio-button value="RAG">基于资料</el-radio-button>
        </el-radio-group>

        <el-input
            v-model="keyword"
            size="small"
            clearable
            placeholder="搜索会话"
            class="search"
        />

        <div class="sessions">
          <div
              v-for="s in filteredSessions"
              :key="s.id"
              class="session"
              :class="{ active: s.id === currentSessionId }"
              @click="openSession(s)"
          >
            <span class="s-title" :title="s.title">{{ s.title || "新对话" }}</span>
            <span class="s-actions">
              <el-tag size="small" type="info">{{ s.chatType }}</el-tag>
              <el-icon class="op" title="重命名" @click.stop="renameSession(s)">
                <EditPen />
              </el-icon>
              <el-icon class="op" title="删除" @click.stop="removeSession(s)">
                <Delete />
              </el-icon>
            </span>
          </div>
          <el-empty v-if="filteredSessions.length === 0" description="暂无会话"
                    :image-size="60" />
        </div>
      </aside>

      <section class="main">
        <div class="toolbar">
          <div v-if="mode === 'RAG'" class="scope">
            <span class="scope-label">检索范围</span>
            <el-select
                v-model="selectedDocIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                size="small"
                placeholder="全部资料"
                style="width: 320px"
            >
              <el-option
                  v-for="doc in readyDocuments"
                  :key="doc.id"
                  :label="doc.title"
                  :value="doc.id"
              />
            </el-select>
          </div>
          <div v-else class="scope">
            <span class="scope-hint">普通问答不检索资料，可切换到「基于资料」提问</span>
          </div>

          <el-button
              v-if="messages.length"
              size="small"
              link
              @click="exportConversation"
          >
            导出对话
          </el-button>
        </div>

        <div class="messages" ref="messagesBox">
          <div v-if="hasMore" class="load-more">
            <el-button size="small" link :loading="loadingMore"
                       @click="loadEarlier">
              加载更早的消息
            </el-button>
          </div>

          <div
              v-for="m in messages"
              :key="m.id"
              :class="['msg', m.role === 'USER' ? 'user' : 'assistant']"
          >
            <div class="bubble">
              <div v-if="m.role === 'USER'" class="plain">{{ m.content }}</div>
              <template v-else>
                <div v-if="m.content" class="markdown"
                     v-html="renderMarkdown(m.content)"></div>
                <div v-if="m.streaming && !m.content" class="thinking">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  正在思考…
                </div>
                <span v-if="m.streaming && m.content" class="cursor">▍</span>
              </template>
            </div>

            <div v-if="m.sources && m.sources.length" class="sources">
              <el-popover
                  v-for="(s, i) in m.sources"
                  :key="i"
                  placement="top"
                  width="360"
                  trigger="click"
              >
                <template #reference>
                  <span class="src">
                    <el-tag size="small" effect="plain">
                      [{{ s.index || i + 1 }}]
                    </el-tag>
                    {{ s.documentTitle }}
                    <span v-if="s.pageNumber">第 {{ s.pageNumber }} 页</span>
                  </span>
                </template>
                <div class="src-preview">{{ s.preview || "（无预览）" }}</div>
              </el-popover>
            </div>
          </div>
        </div>

        <div class="input-bar">
          <el-input
              v-model="input"
              type="textarea"
              :rows="2"
              resize="none"
              :disabled="streaming"
              placeholder="输入问题，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="send"
          />
          <el-button v-if="streaming" @click="stop">停止</el-button>
          <el-button v-else type="primary" :disabled="!input.trim()" @click="send">
            发送
          </el-button>
        </div>
      </section>
    </div>
  </el-card>
</template>


<script setup>
import { ref, computed, onMounted, nextTick } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, EditPen, Loading } from "@element-plus/icons-vue"
import request, { errorMessage } from "../api/request"
import { streamRequest } from "../api/stream"
import { renderMarkdown } from "../utils/markdown"
import { formatDateTime } from "../utils/format"

const PAGE_SIZE = 50

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const input = ref("")
const mode = ref("GENERAL")
const streaming = ref(false)
const loadingMore = ref(false)
const messagesBox = ref(null)
const keyword = ref("")
const documents = ref([])
const selectedDocIds = ref([])
const totalMessages = ref(0)
const page = ref(1)

let controller = null

const readyDocuments = computed(() =>
  documents.value.filter((doc) => doc.status === "READY")
)

const filteredSessions = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return sessions.value
  return sessions.value.filter((s) =>
    String(s.title || "").toLowerCase().includes(key)
  )
})

const hasMore = computed(() => messages.value.length < totalMessages.value)

const loadSessions = async () => {
  const res = await request.get("/api/chat/sessions")
  sessions.value = res.data.data || []
}

const loadDocuments = async () => {
  const res = await request.get("/api/documents")
  documents.value = res.data.data || []
}

const scrollBottom = async () => {
  await nextTick()
  if (messagesBox.value) {
    messagesBox.value.scrollTop = messagesBox.value.scrollHeight
  }
}

const openSession = async (session) => {
  currentSessionId.value = session.id
  mode.value = session.chatType === "RAG" ? "RAG" : "GENERAL"
  page.value = 1
  await loadMessages()
  scrollBottom()
}

const loadMessages = async () => {
  const res = await request.get(
    `/api/chat/sessions/${currentSessionId.value}/messages`,
    { params: { page: page.value, size: PAGE_SIZE } }
  )
  const data = res.data.data
  totalMessages.value = data.total
  messages.value = (data.list || []).map((m) => ({
    ...m,
    sources: parseSources(m.sourcesJson)
  }))
}

const loadEarlier = async () => {
  loadingMore.value = true
  const box = messagesBox.value
  const previousHeight = box ? box.scrollHeight : 0
  try {
    page.value += 1
    const res = await request.get(
      `/api/chat/sessions/${currentSessionId.value}/messages`,
      { params: { page: page.value, size: PAGE_SIZE } }
    )
    const older = (res.data.data.list || []).map((m) => ({
      ...m,
      sources: parseSources(m.sourcesJson)
    }))
    const known = new Set(messages.value.map((m) => m.id))
    messages.value = [
      ...older.filter((m) => !known.has(m.id)),
      ...messages.value
    ]
    await nextTick()
    if (box) {
      box.scrollTop = box.scrollHeight - previousHeight
    }
  } finally {
    loadingMore.value = false
  }
}

const parseSources = (raw) => {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

const newChat = () => {
  if (streaming.value) stop()
  currentSessionId.value = null
  messages.value = []
  totalMessages.value = 0
  page.value = 1
}

const send = async () => {
  const text = input.value.trim()
  if (!text || streaming.value) return

  messages.value.push({
    id: `local-user-${Date.now()}`,
    role: "USER",
    content: text,
    sources: []
  })
  input.value = ""
  scrollBottom()

  messages.value.push({
    id: `local-assistant-${Date.now()}`,
    role: "ASSISTANT",
    content: "",
    sources: [],
    streaming: true
  })
  // 取数组里的响应式代理对象，回调里直接改它才能触发重渲染
  const assistant = messages.value[messages.value.length - 1]

  const isRag = mode.value === "RAG"
  const url = isRag ? "/api/ai/rag/chat/stream" : "/api/ai/chat/stream"
  const body = isRag
    ? {
        sessionId: currentSessionId.value,
        question: text,
        documentIds: selectedDocIds.value.length
          ? selectedDocIds.value : null
      }
    : { sessionId: currentSessionId.value, message: text }

  streaming.value = true
  controller = new AbortController()

  try {
    await streamRequest(url, body, {
      session: (data) => {
        currentSessionId.value = data.sessionId
      },
      sources: (data) => {
        assistant.sources = data.sources || []
      },
      delta: (data) => {
        assistant.content += data.text || ""
        scrollBottom()
      },
      done: (data) => {
        if (!assistant.content && (data.content || data.answer)) {
          assistant.content = data.content || data.answer
        }
        if (data.sources) assistant.sources = data.sources
      },
      error: (data) => {
        ElMessage.error(data.message || "生成失败，请稍后重试")
      }
    }, controller.signal)

    totalMessages.value += 2
    loadSessions()
  } catch (error) {
    if (error.name === "AbortError") {
      assistant.content += "\n\n（已停止生成）"
    } else {
      if (error.status === 401) {
        localStorage.removeItem("token")
        localStorage.removeItem("role")
        location.hash = "#/login"
      } else {
        ElMessage.error(errorMessage(error, "发送失败"))
      }
      if (!assistant.content) {
        messages.value = messages.value.filter((m) => m !== assistant)
      }
    }
  } finally {
    assistant.streaming = false
    streaming.value = false
    controller = null
  }
}

const stop = () => {
  if (controller) controller.abort()
}

const renameSession = async (session) => {
  try {
    const { value } = await ElMessageBox.prompt(
      "输入新的会话名称", "重命名会话",
      { inputValue: session.title, inputValidator: (v) => !!v?.trim() || "名称不能为空" }
    )
    await request.put(`/api/chat/sessions/${session.id}`, { title: value.trim() })
    ElMessage.success("已重命名")
    loadSessions()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "重命名失败"))
    }
  }
}

const removeSession = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确认删除会话「${session.title || "新对话"}」？该会话的消息会一并删除。`,
      "提示", { type: "warning" }
    )
    await request.delete(`/api/chat/sessions/${session.id}`)
    if (currentSessionId.value === session.id) newChat()
    ElMessage.success("删除成功")
    loadSessions()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

const exportConversation = () => {
  const title = sessions.value.find((s) => s.id === currentSessionId.value)?.title
    || "对话记录"
  const lines = [`# ${title}`, "", `导出时间：${formatDateTime(new Date().toISOString())}`, ""]
  for (const m of messages.value) {
    lines.push(m.role === "USER" ? "## 我" : "## 学习助手")
    lines.push("")
    lines.push(m.content || "")
    if (m.sources?.length) {
      lines.push("")
      lines.push("来源：" + m.sources.map((s) =>
        `${s.documentTitle}${s.pageNumber ? ` 第${s.pageNumber}页` : ""}`
      ).join("；"))
    }
    lines.push("")
  }
  const blob = new Blob([lines.join("\n")], { type: "text/markdown;charset=utf-8" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = `${title}.md`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  loadSessions()
  loadDocuments()
})
</script>


<style scoped>
.chat-card {
  height: calc(100vh - 100px);
}

.chat-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.sidebar {
  width: 240px;
  flex: none;
  border-right: 1px solid var(--hairline);
  padding: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.new-btn {
  width: 100%;
  margin-bottom: 12px;
}

.mode {
  display: flex;
  margin-bottom: 10px;
}

.search {
  margin-bottom: 10px;
}

.sessions {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 9px 8px;
  border-radius: 6px;
  cursor: pointer;
}

.session:hover,
.session.active {
  background: rgba(255, 255, 255, 0.06);
}

.s-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.s-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.op {
  cursor: pointer;
  opacity: 0.5;
  font-size: 13px;
}

.op:hover {
  opacity: 1;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--hairline);
}

.scope {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scope-label,
.scope-hint {
  color: var(--ink-caption);
  font-size: 12px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}

.load-more {
  text-align: center;
  margin-bottom: 10px;
}

.msg {
  display: flex;
  flex-direction: column;
  margin: 12px 0;
}

.msg.user {
  align-items: flex-end;
}

.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 10px;
  line-height: 1.7;
  font-size: 14px;
  overflow-x: auto;
}

.user .bubble {
  background: rgba(76, 141, 255, 0.22);
  color: #eaf0ff;
}

.assistant .bubble {
  background: #1c1c1c;
  border: 1px solid var(--hairline);
  color: rgba(255, 255, 255, 0.88);
}

.plain {
  white-space: pre-wrap;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-caption);
  font-size: 13px;
}

.cursor {
  color: var(--accent-blue);
}

.markdown :deep(p) {
  margin: 0 0 8px;
}

.markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown :deep(pre) {
  background: #0d0d0d;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 10px 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown :deep(code) {
  font-family: "Cascadia Code", Consolas, Monaco, monospace;
  font-size: 13px;
}

.markdown :deep(:not(pre) > code) {
  background: rgba(255, 255, 255, 0.08);
  padding: 1px 5px;
  border-radius: 4px;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.markdown :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}

.markdown :deep(th),
.markdown :deep(td) {
  border: 1px solid var(--hairline);
  padding: 4px 8px;
}

.markdown :deep(a) {
  color: var(--accent-blue);
}

.sources {
  margin-top: 6px;
  max-width: 78%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.src {
  font-size: 12px;
  color: var(--ink-caption);
  cursor: pointer;
}

.src-preview {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.input-bar {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--hairline);
}
</style>
