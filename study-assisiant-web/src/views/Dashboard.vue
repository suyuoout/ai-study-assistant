<template>
  <div class="dash">
    <el-card shadow="never" class="hero">
      <div class="hero-left">
        <div class="hero-tag">学习工作台 · Workspace</div>
        <h1 class="hero-title">把今天放回学习里。</h1>
        <p class="hero-sub">
          课程资料、学习计划和 AI 对话都在这里汇总。先选一个入口开始，其余记录会自然沉淀。
        </p>
        <div class="hero-actions">
          <el-button type="primary" @click="$router.push('/study')">
            生成学习计划
          </el-button>
          <el-button @click="$router.push('/chat')">打开 AI 对话</el-button>
          <el-button link @click="$router.push('/knowledge')">我的资料</el-button>
        </div>
      </div>

      <div class="hero-right">
        <div class="rec-title">推荐入口</div>
        <div class="rec-item">把资料变成具体任务</div>
        <p class="rec-desc">
          先上传课程资料，再由 AI 生成结构化学习计划，并自动拆解成每日任务。
        </p>
        <div class="rec-foot">
          <span>{{ overview.totalTasks || 0 }} 个每日任务已就位</span>
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

    <div class="grid">
      <el-card shadow="never" class="heat-card">
        <template #header>
          <div class="card-head">
            <span>学习记录</span>
            <span class="card-sub">按周展示每日学习时长</span>
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
                :class="cell"
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
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import request from "../api/request"

const overview = ref({})
const planCount = ref(0)
const docCount = ref(0)
const sessionCount = ref(0)
const heatCells = ref([])

const cards = computed(() => [
  { label: "总任务", value: overview.value.totalTasks || 0 },
  { label: "已完成", value: overview.value.doneTasks || 0 },
  { label: "完成率", value: Math.round((overview.value.completionRate || 0) * 100) + "%" },
  { label: "学习分钟", value: overview.value.actualMinutes || 0 }
])

const overviewRows = computed(() => [
  { label: "学习计划", sub: "已创建的计划", value: planCount.value },
  { label: "每日任务", sub: "已完成 " + (overview.value.doneTasks || 0), value: overview.value.totalTasks || 0 },
  { label: "学习资料", sub: "可检索的资料", value: docCount.value },
  { label: "对话会话", sub: "历史会话", value: sessionCount.value }
])

const pad = (n) => String(n).padStart(2, "0")
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const buildHeat = (daily) => {
  const minutesByDate = new Map()
  for (const d of daily) {
    minutesByDate.set(d.taskDate, d.minutes || 0)
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
      const dt = new Date(start)
      dt.setDate(start.getDate() + w * 7 + d)
      const minutes = minutesByDate.get(fmt(dt)) || 0
      let level = ""
      if (minutes > 0) level = "l1"
      if (minutes >= 30) level = "l2"
      if (minutes >= 60) level = "l3"
      if (minutes >= 120) level = "l4"
      cells.push(level)
    }
  }
  return cells
}

onMounted(async () => {
  const [o, d, plans, docs, sessions] = await Promise.all([
    request.get("/api/statistics/overview"),
    request.get("/api/statistics/daily"),
    request.get("/api/plans"),
    request.get("/api/documents"),
    request.get("/api/chat/sessions")
  ])
  overview.value = o.data.data
  planCount.value = plans.data.data.length
  docCount.value = docs.data.data.length
  sessionCount.value = sessions.data.data.length
  heatCells.value = buildHeat(d.data.data || [])
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
  font-size: 40px;
  line-height: 1.15;
  letter-spacing: -0.03em;
  margin-bottom: 14px;
}

.hero-sub {
  color: var(--ink-secondary);
  font-size: 15px;
  line-height: 1.7;
  max-width: 520px;
  margin-bottom: 22px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.hero-right {
  width: 300px;
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
</style>
