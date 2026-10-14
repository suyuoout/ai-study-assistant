<template>
  <el-card shadow="never" class="chat-card" :body-style="{ height: '100%', display: 'flex', flexDirection: 'column' }">
    <div class="chat-body">
      <aside class="sidebar">
        <el-button type="primary" class="new-btn" @click="newChat">新建对话</el-button>

        <el-radio-group v-model="mode" size="small" class="mode">
          <el-radio-button value="GENERAL">普通问答</el-radio-button>
          <el-radio-button value="RAG">基于资料</el-radio-button>
        </el-radio-group>

        <div class="sessions">
          <div
              v-for="s in sessions"
              :key="s.id"
              class="session"
              :class="{ active: s.id === currentSessionId }"
              @click="openSession(s.id)"
          >
            <span class="s-title">{{ s.title || "新对话" }}</span>
            <el-tag size="small" type="info">{{ s.chatType }}</el-tag>
          </div>
        </div>
      </aside>

      <section class="main">
        <div class="messages" ref="messagesBox">
          <div
              v-for="m in messages"
              :key="m.id"
              :class="['msg', m.role === 'USER' ? 'user' : 'assistant']"
          >
            <div class="bubble">{{ m.content }}</div>
            <div v-if="m.sources && m.sources.length" class="sources">
              <div v-for="(s, i) in m.sources" :key="i" class="src">
                <el-tag size="small" effect="plain">来源</el-tag>
                {{ s.documentTitle }}
                <span v-if="s.pageNumber">第 {{ s.pageNumber }} 页</span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-bar">
          <el-input
              v-model="input"
              type="textarea"
              :rows="2"
              resize="none"
              placeholder="输入问题，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" @click="send">发送</el-button>
        </div>
      </section>
    </div>
  </el-card>
</template>


<script setup>
import { ref, onMounted, nextTick } from "vue"
import { ElMessage } from "element-plus"
import request from "../api/request"

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const input = ref("")
const mode = ref("GENERAL")
const messagesBox = ref(null)

const loadSessions = async () => {
  const res = await request.get("/api/chat/sessions")
  sessions.value = res.data.data
}

const openSession = async (id) => {
  currentSessionId.value = id
  const res = await request.get(`/api/chat/sessions/${id}/messages`)
  messages.value = res.data.data.map((m) => ({
    ...m,
    sources: m.sourcesJson ? JSON.parse(m.sourcesJson) : []
  }))
  scrollBottom()
}

const newChat = () => {
  currentSessionId.value = null
  messages.value = []
}

const scrollBottom = async () => {
  await nextTick()
  if (messagesBox.value) {
    messagesBox.value.scrollTop = messagesBox.value.scrollHeight
  }
}

const send = async () => {
  const text = input.value.trim()
  if (!text) return

  messages.value.push({ id: Date.now(), role: "USER", content: text })
  input.value = ""
  scrollBottom()

  try {
    const url = mode.value === "RAG" ? "/api/ai/rag/chat" : "/api/ai/chat"
    const body = mode.value === "RAG"
      ? { sessionId: currentSessionId.value, question: text }
      : { sessionId: currentSessionId.value, message: text }

    const res = await request.post(url, body)
    const data = res.data.data
    currentSessionId.value = data.sessionId
    messages.value.push({
      id: Date.now() + 1,
      role: "ASSISTANT",
      content: data.reply || data.answer,
      sources: data.sources || []
    })
    scrollBottom()
    loadSessions()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "发送失败")
  }
}

onMounted(loadSessions)
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
  border-right: 1px solid var(--hairline);
  padding: 12px;
  overflow-y: auto;
}

.new-btn {
  width: 100%;
  margin-bottom: 12px;
}

.mode {
  display: flex;
  margin-bottom: 12px;
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 8px;
  border-radius: 6px;
  cursor: pointer;
}

.session:hover,
.session.active {
  background: rgba(255, 255, 255, 0.06);
}

.s-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}

.msg {
  display: flex;
  flex-direction: column;
  margin: 10px 0;
}

.msg.user {
  align-items: flex-end;
}

.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 10px;
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 14px;
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

.sources {
  margin-top: 6px;
  max-width: 75%;
}

.src {
  font-size: 12px;
  color: var(--ink-caption);
}

.input-bar {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--hairline);
}
</style>
