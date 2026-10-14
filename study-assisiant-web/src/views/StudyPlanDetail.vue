<template>
  <div v-if="plan" class="detail">
    <el-card shadow="never" class="head-card">
      <div class="head">
        <div>
          <div class="title serif-display">{{ plan.title }}</div>
          <div class="sub">
            <el-tag size="small" effect="dark">{{ plan.status }}</el-tag>
            <span>创建于 {{ fmtDateTime(plan.createTime) }}</span>
          </div>
        </div>
        <el-button @click="$router.push('/study')">返回列表</el-button>
      </div>

      <div class="facts">
        <div class="fact">
          <div class="k">学习目标</div>
          <div class="v">{{ plan.goal || "—" }}</div>
        </div>
        <div class="fact">
          <div class="k">计划周期</div>
          <div class="v">
            {{ plan.startDate || "—" }} ~ {{ plan.endDate || "—" }}
          </div>
        </div>
        <div class="fact">
          <div class="k">每日时长</div>
          <div class="v">
            {{ plan.dailyMinutes ? plan.dailyMinutes + " 分钟" : "—" }}
          </div>
        </div>
        <div class="fact">
          <div class="k">任务数量</div>
          <div class="v">{{ displayTasks.length }} 项</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span>任务清单</span>
      </template>

      <div v-if="displayTasks.length" class="task-list">
        <div class="task" v-for="(t, i) in displayTasks" :key="i">
          <div class="task-date">{{ fmtDate(t.taskDate) }}</div>
          <div class="task-main">
            <div class="task-title">{{ t.title }}</div>
            <div class="task-desc" v-if="t.description">
              {{ t.description }}
            </div>
          </div>
          <div class="task-right">
            <span class="task-min">
              {{ t.estimatedMinutes ? t.estimatedMinutes + " 分钟" : "—" }}
            </span>
            <el-tag size="small" :type="statusType(t.status)">
              {{ statusText(t.status) }}
            </el-tag>
          </div>
        </div>
      </div>

      <pre v-else-if="plan.content" class="raw">{{ plainContent }}</pre>

      <el-empty v-else description="暂无任务" />
    </el-card>
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import { useRoute } from "vue-router"
import request from "../api/request"

const route = useRoute()
const plan = ref(null)
const tasks = ref([])

const fmtDateTime = (value) => {
  if (!value) return "—"
  const s = String(value)
  const m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/)
  return m ? `${m[1]} ${m[2]}` : s
}

const fmtDate = (value) => {
  if (!value) return "—"
  return String(value).slice(0, 10)
}

// 从计划内容里尝试解析出任务（兼容旧数据结构）
const contentTasks = computed(() => {
  const content = plan.value?.content
  if (!content) return []
  try {
    const obj = JSON.parse(content)
    if (obj && Array.isArray(obj.tasks)) return obj.tasks
  } catch (e) {
    // 纯文本计划，忽略
  }
  return []
})

const displayTasks = computed(() =>
  tasks.value.length ? tasks.value : contentTasks.value
)

const plainContent = computed(() => {
  const content = plan.value?.content || ""
  try {
    const obj = JSON.parse(content)
    if (obj && Array.isArray(obj.tasks)) {
      return obj.tasks
        .map((t) => `${fmtDate(t.taskDate)}  ${t.title}`)
        .join("\n")
    }
  } catch (e) {
    // 纯文本
  }
  return content
})

const statusText = (status) => {
  if (status === "DONE") return "已完成"
  if (status === "DOING") return "进行中"
  return "未开始"
}

const statusType = (status) => {
  if (status === "DONE") return "success"
  if (status === "DOING") return "warning"
  return "info"
}

onMounted(async () => {
  const [p, t] = await Promise.all([
    request.get(`/api/plans/${route.params.id}`),
    request.get(`/api/plans/${route.params.id}/tasks`)
  ])
  plan.value = p.data.data
  tasks.value = t.data.data || []
})
</script>


<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 10px;
}

.sub {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-caption);
  font-size: 13px;
}

.facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid var(--hairline);
}

.fact .k {
  color: var(--ink-caption);
  font-size: 12px;
  margin-bottom: 6px;
}

.fact .v {
  font-size: 15px;
}

.task-list {
  display: flex;
  flex-direction: column;
}

.task {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid var(--hairline);
}

.task:last-child {
  border-bottom: none;
}

.task-date {
  width: 92px;
  flex: none;
  color: var(--ink-caption);
  font-size: 13px;
  padding-top: 2px;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 6px;
}

.task-desc {
  color: var(--ink-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.task-right {
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-min {
  color: var(--ink-caption);
  font-size: 13px;
}

.raw {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.9;
  font-family: inherit;
  color: var(--ink-secondary);
}
</style>
