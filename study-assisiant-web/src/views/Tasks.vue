<template>
  <div>
    <el-card shadow="never" class="overview-card">
      <div class="overview">
        <div class="ov-item">
          <div class="ov-num">{{ overview.totalTasks || 0 }}</div>
          <div class="ov-label">总任务</div>
        </div>
        <div class="ov-item">
          <div class="ov-num">{{ overview.doneTasks || 0 }}</div>
          <div class="ov-label">已完成</div>
        </div>
        <div class="ov-item">
          <el-progress
              type="circle"
              :percentage="percent"
              :width="80"
          />
          <div class="ov-label">完成率</div>
        </div>
        <div class="ov-item">
          <div class="ov-num">{{ overview.actualMinutes || 0 }}</div>
          <div class="ov-label">学习分钟</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span>任务列表</span>
          <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              @change="loadTasks"
          />
        </div>
      </template>

      <el-table :data="tasks" stripe>
        <el-table-column prop="title" label="任务" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="estimatedMinutes" label="预计(分钟)" width="110" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-select :model-value="row.status" size="small" @change="(v) => changeStatus(row, v)">
              <el-option label="未开始" value="TODO" />
              <el-option label="进行中" value="DOING" />
              <el-option label="已完成" value="DONE" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="tasks.length === 0" description="当天没有任务" />
    </el-card>
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import request from "../api/request"

const date = ref(new Date().toISOString().slice(0, 10))
const tasks = ref([])
const overview = ref({})

const percent = computed(() =>
  Math.round((overview.value.completionRate || 0) * 100)
)

const loadTasks = async () => {
  const res = await request.get("/api/tasks", { params: { date: date.value } })
  tasks.value = res.data.data
}

const loadOverview = async () => {
  const res = await request.get("/api/statistics/overview")
  overview.value = res.data.data
}

const changeStatus = async (task, status) => {
  try {
    await request.patch(`/api/tasks/${task.id}/status`, {
      status,
      actualMinutes: status === "DONE" ? task.estimatedMinutes : null
    })
    ElMessage.success("已更新")
    loadTasks()
    loadOverview()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "更新失败")
  }
}

onMounted(() => {
  loadTasks()
  loadOverview()
})
</script>


<style scoped>
.overview-card {
  margin-bottom: 18px;
}

.overview {
  display: flex;
  align-items: center;
  gap: 60px;
}

.ov-item {
  text-align: center;
}

.ov-num {
  font-size: 26px;
  font-weight: bold;
}

.ov-label {
  color: var(--ink-caption);
  margin-top: 4px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
