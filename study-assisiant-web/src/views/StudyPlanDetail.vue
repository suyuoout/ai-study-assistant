<template>
  <div v-if="plan" class="detail">
    <el-card shadow="never" class="head-card">
      <div class="head">
        <div>
          <div class="title">{{ plan.title }}</div>
          <div class="sub">
            <el-tag size="small" effect="dark">{{ plan.status }}</el-tag>
            <span>创建于 {{ formatDateTime(plan.createTime) }}</span>
          </div>
        </div>
        <div class="head-actions">
          <el-button @click="addTask">新增任务</el-button>
          <el-button @click="$router.push('/study')">返回列表</el-button>
        </div>
      </div>

      <div class="facts">
        <div class="fact">
          <div class="k">学习目标</div>
          <div class="v">{{ plan.goal || "—" }}</div>
        </div>
        <div class="fact">
          <div class="k">计划周期</div>
          <div class="v">
            {{ formatDate(plan.startDate) }} ~ {{ formatDate(plan.endDate) }}
          </div>
        </div>
        <div class="fact">
          <div class="k">每日时长</div>
          <div class="v">
            {{ plan.dailyMinutes ? plan.dailyMinutes + " 分钟" : "—" }}
          </div>
        </div>
        <div class="fact">
          <div class="k">任务进度</div>
          <div class="v">{{ doneCount }} / {{ tasks.length }} 项已完成</div>
        </div>
      </div>
      <el-progress :percentage="progress" :stroke-width="8" class="bar" />
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>任务清单</span>
          <span class="card-sub">按日期编排，可在行内直接改状态</span>
        </div>
      </template>

      <div v-if="tasks.length" class="task-list">
        <div class="task" v-for="t in tasks" :key="t.id">
          <div class="task-date">{{ formatDate(t.taskDate) }}</div>
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
            <el-select :model-value="t.status" size="small" style="width: 106px"
                       @change="(v) => changeStatus(t, v)">
              <el-option label="未开始" value="TODO" />
              <el-option label="进行中" value="DOING" />
              <el-option label="已完成" value="DONE" />
            </el-select>
            <el-button size="small" text type="danger"
                       @click="removeTask(t)">删除</el-button>
          </div>
        </div>
      </div>

      <pre v-else-if="plan.content" class="raw">{{ plainContent }}</pre>

      <el-empty v-else description="暂无任务" />
    </el-card>

    <el-dialog v-model="taskVisible" title="新增任务" width="500px">
      <el-form label-width="88px">
        <el-form-item label="任务标题" required>
          <el-input v-model="taskForm.title" />
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="taskForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="taskForm.taskDate" type="date"
                          value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预计分钟">
          <el-input-number v-model="taskForm.estimatedMinutes"
                           :min="5" :max="720" :step="15" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>


<script setup>
import { ref, reactive, computed, onMounted } from "vue"
import { useRoute } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../api/request"
import { formatDate, formatDateTime, todayString } from "../utils/format"

const route = useRoute()
const plan = ref(null)
const tasks = ref([])
const taskVisible = ref(false)
const saving = ref(false)

const taskForm = reactive({
  title: "",
  description: "",
  taskDate: todayString(),
  estimatedMinutes: 60
})

const doneCount = computed(() =>
  tasks.value.filter((t) => t.status === "DONE").length
)

const progress = computed(() =>
  tasks.value.length === 0
    ? 0
    : Math.round((doneCount.value / tasks.value.length) * 100)
)

const plainContent = computed(() => {
  const content = plan.value?.content || ""
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed?.tasks)) {
      return parsed.tasks
        .map((t) => `${formatDate(t.taskDate)}  ${t.title}`)
        .join("\n")
    }
  } catch (error) {
    // 纯文本计划
  }
  return content
})

const load = async () => {
  try {
    const [p, t] = await Promise.all([
      request.get(`/api/plans/${route.params.id}`),
      request.get(`/api/plans/${route.params.id}/tasks`)
    ])
    plan.value = p.data.data
    tasks.value = t.data.data || []
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载计划失败"))
  }
}

const changeStatus = async (task, status) => {
  try {
    await request.patch(`/api/tasks/${task.id}/status`, {
      status,
      actualMinutes: status === "DONE"
        ? (task.actualMinutes || task.estimatedMinutes || 0) : null
    })
    task.status = status
    ElMessage.success("已更新")
  } catch (error) {
    ElMessage.error(errorMessage(error, "更新失败"))
  }
}

const removeTask = async (task) => {
  try {
    await ElMessageBox.confirm(`确认删除任务「${task.title}」？`, "提示",
      { type: "warning" })
    await request.delete(`/api/tasks/${task.id}`)
    ElMessage.success("删除成功")
    load()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

const addTask = () => {
  taskForm.title = ""
  taskForm.description = ""
  taskForm.taskDate = formatDate(plan.value?.startDate) || todayString()
  taskForm.estimatedMinutes = plan.value?.dailyMinutes || 60
  taskVisible.value = true
}

const saveTask = async () => {
  if (!taskForm.title.trim()) {
    ElMessage.warning("请填写任务标题")
    return
  }
  saving.value = true
  try {
    await request.post("/api/tasks", {
      planId: Number(route.params.id),
      ...taskForm
    })
    ElMessage.success("任务已创建")
    taskVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(errorMessage(error, "创建失败"))
  } finally {
    saving.value = false
  }
}

onMounted(load)
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

.head-actions {
  display: flex;
  gap: 10px;
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

.bar {
  margin-top: 18px;
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
  padding-top: 4px;
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

@media (max-width: 900px) {
  .facts {
    grid-template-columns: repeat(2, 1fr);
  }

  .task {
    flex-wrap: wrap;
  }
}
</style>
