<template>
  <div class="dash">
    <el-card shadow="never" class="hero">
      <div class="hero-left">
        <div class="hero-tag">学习工作台 · Workspace</div>
        <h1 class="hero-title">
          {{ greeting }}，{{ username || "同学" }}
        </h1>
        <p class="hero-sub">
          {{ heroSub }}
        </p>
        <div class="hero-actions">
          <el-button type="primary" @click="$router.push('/study')">
            生成学习计划
          </el-button>
          <el-button @click="$router.push('/tasks')">查看今日任务</el-button>
          <el-button link @click="openReport">AI 学习复盘</el-button>
        </div>
      </div>

      <div class="hero-right">
        <div class="rec-title">今日重点</div>
        <template v-if="todayTasks.length">
          <div class="rec-item">
            {{ doneToday }} / {{ todayTasks.length }} 项已完成
          </div>
          <p class="rec-desc">
            {{ nextTask
              ? `接下来：${nextTask.title}`
              : "今天的任务都完成了，可以安排一次复盘。" }}
          </p>
        </template>
        <template v-else>
          <div class="rec-item">今天还没有任务</div>
          <p class="rec-desc">
            先上传课程资料，再由 AI 生成结构化学习计划，自动拆解成每日任务。
          </p>
        </template>
        <div class="rec-foot">
          <span>待办 {{ pendingCount }} 项</span>
          <span class="arrow">→</span>
        </div>
      </div>
    </el-card>

    <div class="stats">
      <div class="stat" v-for="c in cards" :key="c.label">
        <div class="stat-num">{{ c.value }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </div>
    </div>

    <el-card shadow="never" class="trend-card">
      <template #header>
        <div class="card-head">
          <span>学习趋势</span>
          <span class="card-sub">
            近 {{ trendDays }} 天累计 {{ trendTotal.minutes }} 分钟 ·
            完成 {{ trendTotal.done }} 项
          </span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="14">14 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <TrendChart v-if="trendData.length" :data="trendData" />
      <el-empty
          v-else
          :description="`最近 ${trendDays} 天还没有学习记录，完成任务后这里会出现趋势`"
          :image-size="70"
      />
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="heat-card">
        <template #header>
          <div class="card-head">
            <span>学习记录</span>
            <span class="card-sub">最近 16 周每日学习时长</span>
          </div>
        </template>

        <div class="heat-wrap">
          <div class="heat-days">
            <span>一</span>
            <span>二</span>
            <span>三</span>
            <span>四</span>
            <span>五</span>
            <span>六</span>
            <span>日</span>
          </div>
          <div class="heat-grid">
            <div
                v-for="(cell, i) in heatCells"
                :key="i"
                class="heat-cell"
                :class="cell.level"
                :title="cell.title"
            ></div>
          </div>
        </div>

        <div class="heat-legend">
          <span>少</span>
          <i class="heat-cell"></i>
          <i class="heat-cell l1"></i>
          <i class="heat-cell l2"></i>
          <i class="heat-cell l3"></i>
          <i class="heat-cell l4"></i>
          <span>多</span>
        </div>
      </el-card>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <span>学习概况</span>
        </template>
        <div class="ov-row" v-for="r in overviewRows" :key="r.label">
          <div>
            <div class="ov-label">{{ r.label }}</div>
            <div class="ov-sub">{{ r.sub }}</div>
          </div>
          <div class="ov-value">{{ r.value }}</div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="reportVisible" title="AI 学习复盘" width="620px">
      <div class="report-range">
        <el-radio-group v-model="reportDays" size="small" @change="loadReport">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div v-loading="reportLoading" class="report-body">
        <template v-if="report">
          <div class="report-stat">
            任务 {{ report.summary.total }} 项 · 完成 {{ report.summary.done }} 项
            · 完成率 {{ report.summary.completionRate }}% · 学习
            {{ report.summary.minutes }} 分钟
          </div>
          <div class="markdown" v-html="renderMarkdown(report.report)"></div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request, { errorMessage } from "../api/request"
import { renderMarkdown } from "../utils/markdown"
import {
  toDateString, todayString, addDays, normalizeDateKey
} from "../utils/format"
import TrendChart from "../components/TrendChart.vue"

const overview = ref({})
const planCount = ref(0)
const docCount = ref(0)
const sessionCount = ref(0)
const heatCells = ref([])
const username = ref("")
const todayTasks = ref([])
const pendingCount = ref(0)
const dailyStats = ref([])
const trendData = ref([])
const trendDays = ref(14)
const trendTotal = ref({ minutes: 0, done: 0 })

const reportVisible = ref(false)
const reportDays = ref(7)
const report = ref(null)
const reportLoading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return "夜深了"
  if (hour < 12) return "早上好"
  if (hour < 18) return "下午好"
  return "晚上好"
})

const doneToday = computed(() =>
  todayTasks.value.filter((t) => t.status === "DONE").length
)

const nextTask = computed(() =>
  todayTasks.value.find((t) => t.status !== "DONE")
)

const heroSub = computed(() => {
  if (overview.value.totalTasks) {
    return `你共有 ${overview.value.totalTasks} 项任务，已完成 `
      + `${overview.value.doneTasks} 项，累计学习 `
      + `${overview.value.actualMinutes || 0} 分钟。`
  }
  return "课程资料、学习计划和 AI 对话都在这里汇总。先选一个入口开始，其余记录会自然沉淀。"
})

const cards = computed(() => [
  { label: "总任务", value: overview.value.totalTasks || 0 },
  { label: "已完成", value: overview.value.doneTasks || 0 },
  { label: "完成率", value: Math.round((overview.value.completionRate || 0) * 100) + "%" },
  { label: "学习分钟", value: overview.value.actualMinutes || 0 }
])

const overviewRows = computed(() => [
  { label: "学习计划", sub: "已创建的计划", value: planCount.value },
  {
    label: "每日任务",
    sub: "已完成 " + (overview.value.doneTasks || 0),
    value: overview.value.totalTasks || 0
  },
  { label: "学习资料", sub: "可检索的资料", value: docCount.value },
  { label: "对话会话", sub: "历史会话", value: sessionCount.value }
])

const buildHeat = (daily) => {
  const minutesByDate = new Map()
  for (const item of daily) {
    minutesByDate.set(minutesOf(item), Number(item.minutes) || 0)
  }

  const today = new Date()
  const monday = new Date(today)
  monday.setDate(today.getDate() - ((today.getDay() + 6) % 7))

  const weeks = 16
  const start = new Date(monday)
  start.setDate(monday.getDate() - (weeks - 1) * 7)

  const cells = []
  for (let w = 0; w < weeks; w++) {
    for (let d = 0; d < 7; d++) {
      const day = new Date(start)
      day.setDate(start.getDate() + w * 7 + d)
      const key = toDateString(day)
      const minutes = minutesByDate.get(key) || 0
      let level = ""
      if (minutes > 0) level = "l1"
      if (minutes >= 30) level = "l2"
      if (minutes >= 60) level = "l3"
      if (minutes >= 120) level = "l4"
      cells.push({
        level,
        title: `${key}：${minutes} 分钟`
      })
    }
  }
  return cells
}

/** 后端返回的字段可能是 taskDate 或 task_date，这里都兼容 */
const minutesOf = (item) =>
  normalizeDateKey(item.taskDate || item.task_date || item.TASKDATE)

/** 取最近 N 天并补全没有记录的日期，保证图表 X 轴连续 */
const buildTrend = (daily) => {
  const byDate = new Map()
  for (const item of daily) {
    byDate.set(minutesOf(item), {
      minutes: Number(item.minutes) || 0,
      done: Number(item.done) || 0
    })
  }
  const today = todayString()
  const result = []
  for (let i = trendDays.value - 1; i >= 0; i--) {
    const date = addDays(today, -i)
    result.push({ date, ...(byDate.get(date) || { minutes: 0, done: 0 }) })
  }
  return result
}

const loadTrend = () => {
  trendData.value = buildTrend(dailyStats.value)
  trendTotal.value = {
    minutes: trendData.value.reduce((sum, d) => sum + d.minutes, 0),
    done: trendData.value.reduce((sum, d) => sum + d.done, 0)
  }
}

const loadReport = async () => {
  reportLoading.value = true
  report.value = null
  try {
    const res = await request.get("/api/statistics/report", {
      params: { days: reportDays.value }
    })
    report.value = res.data.data
  } catch (error) {
    ElMessage.error(errorMessage(error, "生成复盘失败"))
  } finally {
    reportLoading.value = false
  }
}

const openReport = () => {
  reportVisible.value = true
  if (!report.value) loadReport()
}

onMounted(async () => {
  try {
    const [o, d, plans, docs, sessions, me, todayTasksRes, pending] =
      await Promise.all([
        request.get("/api/statistics/overview"),
        request.get("/api/statistics/daily"),
        request.get("/api/plans"),
        request.get("/api/documents"),
        request.get("/api/chat/sessions"),
        request.get("/api/users/me"),
        request.get("/api/tasks", { params: { date: todayString() } }),
        request.get("/api/tasks/upcoming", { params: { days: 0 } })
      ])

    overview.value = o.data.data || {}
    planCount.value = (plans.data.data || []).length
    docCount.value = (docs.data.data || []).length
    sessionCount.value = (sessions.data.data || []).length
    username.value = me.data.data?.nickname || me.data.data?.username || ""
    todayTasks.value = todayTasksRes.data.data || []
    pendingCount.value = (pending.data.data || []).length
    dailyStats.value = d.data.data || []
    loadTrend()
    heatCells.value = buildHeat(dailyStats.value)
  } catch (error) {
    ElMessage.error(errorMessage(error, "看板数据加载失败"))
  }
})
</script>


<style scoped>
.hero {
  margin-bottom: 18px;
}

.hero :deep(.el-card__body) {
  display: flex;
  gap: 40px;
  padding: 32px;
}

.hero-left {
  flex: 1;
}

.hero-tag {
  color: var(--ink-caption);
  font-size: 13px;
  margin-bottom: 14px;
}

.hero-title {
  font-size: 36px;
  line-height: 1.2;
  letter-spacing: -0.03em;
  margin-bottom: 14px;
}

.hero-sub {
  color: var(--ink-secondary);
  font-size: 15px;
  line-height: 1.7;
  max-width: 560px;
  margin-bottom: 22px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-right {
  width: 300px;
  flex: none;
  border-left: 1px solid var(--hairline);
  padding-left: 32px;
}

.rec-title {
  color: var(--ink-caption);
  font-size: 13px;
  margin-bottom: 12px;
}

.rec-item {
  font-size: 17px;
  font-weight: bold;
  margin-bottom: 10px;
}

.rec-desc {
  color: var(--ink-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 20px;
}

.rec-foot {
  display: flex;
  justify-content: space-between;
  color: var(--ink-caption);
  font-size: 13px;
  padding-top: 14px;
  border-top: 1px solid var(--hairline);
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.stat {
  background: var(--panel);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 20px;
}

.stat-num {
  font-size: 28px;
  font-weight: bold;
}

.stat-label {
  color: var(--ink-caption);
  margin-top: 6px;
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

.trend-card {
  margin-bottom: 18px;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-head :deep(.el-radio-group) {
  margin-left: auto;
}

.card-sub {
  color: var(--ink-caption);
  font-size: 12px;
  font-weight: normal;
}

.heat-wrap {
  display: flex;
  gap: 10px;
  padding: 6px 0 14px;
}

.heat-days {
  display: grid;
  grid-template-rows: repeat(7, 12px);
  gap: 4px;
  color: var(--ink-caption);
  font-size: 11px;
  line-height: 12px;
}

.heat-grid {
  overflow-x: auto;
}

.heat-legend {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--ink-caption);
  font-size: 12px;
}

.heat-legend i {
  display: inline-block;
}

.ov-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--hairline);
}

.ov-row:last-child {
  border-bottom: none;
}

.ov-label {
  font-size: 14px;
}

.ov-sub {
  color: var(--ink-caption);
  font-size: 12px;
  margin-top: 3px;
}

.ov-value {
  font-size: 20px;
  font-weight: bold;
}

.report-range {
  margin-bottom: 14px;
}

.report-body {
  min-height: 200px;
}

.report-stat {
  color: var(--ink-caption);
  font-size: 13px;
  margin-bottom: 10px;
}

.markdown {
  line-height: 1.8;
  font-size: 14px;
}

.markdown :deep(p) {
  margin: 0 0 8px;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 20px;
}

@media (max-width: 1100px) {
  .hero :deep(.el-card__body) {
    flex-direction: column;
    gap: 20px;
  }

  .hero-right {
    width: 100%;
    border-left: none;
    border-top: 1px solid var(--hairline);
    padding: 20px 0 0;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
