<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="item in cards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-head">
          <span>系统运行概况</span>
          <span class="card-sub">数据实时统计</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户总数">
          {{ overview.users || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="学习资料">
          {{ overview.documents || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="学习计划">
          {{ overview.plans || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="每日任务">
          {{ overview.tasks || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="对话会话">
          {{ overview.chatSessions || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="对话消息">
          {{ overview.chatMessages || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="解析中资料">
          <el-tag v-if="overview.processingDocuments" type="warning">
            {{ overview.processingDocuments }}
          </el-tag>
          <span v-else>0</span>
        </el-descriptions-item>
        <el-descriptions-item label="解析失败资料">
          <el-tag v-if="overview.failedDocuments" type="danger">
            {{ overview.failedDocuments }}
          </el-tag>
          <span v-else>0</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-head">
          <span>AI 用量</span>
          <span class="card-sub">
            近 {{ usageDays }} 天共 {{ usage.totalCalls || 0 }} 次调用
          </span>
          <el-radio-group v-model="usageDays" size="small" @change="loadUsage">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="14">14 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="usage-grid">
        <div class="usage-bars">
          <div v-for="row in usage.daily || []" :key="row.day" class="usage-row">
            <span class="usage-day">{{ formatDate(row.day) }}</span>
            <div class="usage-bar-wrap">
              <div class="usage-bar"
                   :style="{ width: barWidth(row.calls) }"></div>
            </div>
            <span class="usage-count">{{ row.calls }} 次</span>
          </div>
          <el-empty v-if="!(usage.daily || []).length"
                    description="暂无调用记录" :image-size="60" />
        </div>

        <div class="usage-top">
          <div class="usage-top-title">调用最多的用户</div>
          <div v-for="(u, i) in usage.topUsers || []" :key="u.userId"
               class="usage-top-row">
            <span class="rank">{{ i + 1 }}</span>
            <span class="name">{{ u.username }}</span>
            <span class="count">{{ u.calls }} 次</span>
          </div>
          <el-empty v-if="!(usage.topUsers || []).length"
                    description="暂无数据" :image-size="60" />
        </div>
      </div>
    </el-card>
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request, { errorMessage } from "../../api/request"
import { formatDate } from "../../utils/format"

const overview = ref({})
const usage = ref({})
const usageDays = ref(14)

const cards = computed(() => [
  { label: "用户", value: overview.value.users || 0 },
  { label: "资料", value: overview.value.documents || 0 },
  { label: "计划", value: overview.value.plans || 0 },
  { label: "任务", value: overview.value.tasks || 0 },
  { label: "会话", value: overview.value.chatSessions || 0 },
  { label: "今日 AI 调用", value: overview.value.aiCallsToday || 0 }
])

const maxCalls = computed(() =>
  Math.max(1, ...(usage.value.daily || []).map((row) => Number(row.calls) || 0))
)

const barWidth = (calls) =>
  `${Math.round(((Number(calls) || 0) / maxCalls.value) * 100)}%`

const loadOverview = async () => {
  try {
    const res = await request.get("/api/admin/overview")
    overview.value = res.data.data || {}
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载总览失败"))
  }
}

const loadUsage = async () => {
  try {
    const res = await request.get("/api/admin/ai-usage", {
      params: { days: usageDays.value }
    })
    usage.value = res.data.data || {}
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载 AI 用量失败"))
  }
}

onMounted(() => {
  loadOverview()
  loadUsage()
})
</script>


<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
}

.stat-label {
  color: var(--ink-caption);
  margin-top: 6px;
  font-size: 13px;
}

.card {
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
  flex: 1;
}

.usage-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.usage-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
}

.usage-day {
  width: 92px;
  color: var(--ink-caption);
}

.usage-bar-wrap {
  flex: 1;
  height: 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 5px;
  overflow: hidden;
}

.usage-bar {
  height: 100%;
  background: var(--accent-blue);
  border-radius: 5px;
}

.usage-count {
  width: 60px;
  text-align: right;
  color: var(--ink-secondary);
}

.usage-top-title {
  font-size: 13px;
  color: var(--ink-caption);
  margin-bottom: 10px;
}

.usage-top-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--hairline);
}

.rank {
  width: 18px;
  color: var(--ink-caption);
}

.name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  color: var(--ink-secondary);
}

@media (max-width: 1100px) {
  .usage-grid {
    grid-template-columns: 1fr;
  }
}
</style>
