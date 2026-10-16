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
          <el-progress type="circle" :percentage="percent" :width="80" />
          <div class="ov-label">完成率</div>
        </div>
        <div class="ov-item">
          <div class="ov-num">{{ overview.actualMinutes || 0 }}</div>
          <div class="ov-label">学习分钟</div>
        </div>
        <div class="ov-item pending" @click="pendingVisible = true">
          <div class="ov-num warn">{{ pending.length }}</div>
          <div class="ov-label">待办（点击查看）</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span>任务列表</span>
          <div class="toolbar-right">
            <el-radio-group v-model="viewMode" size="small" @change="loadTasks">
              <el-radio-button value="day">单日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
            </el-radio-group>
            <el-date-picker
                v-if="viewMode === 'day'"
                v-model="date"
                type="date"
                value-format="YYYY-MM-DD"
                :clearable="false"
                @change="loadTasks"
            />
            <el-button type="primary" @click="openCreate">新增任务</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column label="日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.taskDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="200"
                         show-overflow-tooltip />
        <el-table-column label="预计" width="100">
          <template #default="{ row }">
            {{ row.estimatedMinutes ? row.estimatedMinutes + " 分钟" : "—" }}
          </template>
        </el-table-column>
        <el-table-column label="实际" width="100">
          <template #default="{ row }">
            {{ row.actualMinutes ? row.actualMinutes + " 分钟" : "—" }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-select :model-value="row.status" size="small"
                       @change="(v) => changeStatus(row, v)">
              <el-option label="未开始" value="TODO" />
              <el-option label="进行中" value="DOING" />
              <el-option label="已完成" value="DONE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tasks.length === 0"
                description="这里还没有任务" />
    </el-card>

    <el-dialog v-model="createVisible" title="新增任务" width="520px">
      <el-form label-width="90px">
        <el-form-item label="所属计划" required>
          <el-select v-model="createForm.planId" placeholder="选择学习计划"
                     style="width: 100%">
            <el-option v-for="p in plans" :key="p.id" :label="p.title"
                       :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" required>
          <el-input v-model="createForm.title" placeholder="例如：复习索引原理" />
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="createForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="createForm.taskDate" type="date"
                          value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预计分钟">
          <el-input-number v-model="createForm.estimatedMinutes"
                           :min="5" :max="720" :step="15" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createTask">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑任务" width="520px">
      <el-form v-if="editForm.id" label-width="90px">
        <el-form-item label="任务标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="editForm.taskDate" type="date"
                          value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预计分钟">
          <el-input-number v-model="editForm.estimatedMinutes"
                           :min="5" :max="720" :step="15" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="未开始" value="TODO" />
            <el-option label="进行中" value="DOING" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editForm.status === 'DONE'" label="实际分钟">
          <el-input-number v-model="editForm.actualMinutes"
                           :min="0" :max="1440" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="pendingVisible" title="待办清单" size="40%">
      <el-empty v-if="pending.length === 0"
                description="没有拖欠的任务，保持住" />
      <div v-for="task in pending" :key="task.id" class="pending-item">
        <div class="pending-main">
          <div class="pending-title">{{ task.title }}</div>
          <div class="pending-sub">
            {{ formatDate(task.taskDate) }} · 预计
            {{ task.estimatedMinutes || 0 }} 分钟
          </div>
        </div>
        <el-button size="small" type="primary" plain
                   @click="changeStatus(task, 'DONE')">
          标记完成
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>


<script setup>
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../api/request"
import { todayString, addDays, formatDate } from "../utils/format"

const date = ref(todayString())
const viewMode = ref("day")
const tasks = ref([])
const plans = ref([])
const pending = ref([])
const overview = ref({})
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const editVisible = ref(false)
const pendingVisible = ref(false)

const createForm = reactive({
  planId: null,
  title: "",
  description: "",
  taskDate: todayString(),
  estimatedMinutes: 60
})

const editForm = reactive({
  id: null,
  title: "",
  description: "",
  taskDate: todayString(),
  estimatedMinutes: 60,
  status: "TODO",
  actualMinutes: 60
})

const percent = computed(() =>
  Math.round((overview.value.completionRate || 0) * 100)
)

const loadTasks = async () => {
  loading.value = true
  try {
    const params = viewMode.value === "week"
      ? { from: addDays(date.value, -6), to: date.value }
      : { date: date.value }
    const res = await request.get("/api/tasks", { params })
    tasks.value = res.data.data || []
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载任务失败"))
  } finally {
    loading.value = false
  }
}

const loadOverview = async () => {
  try {
    const res = await request.get("/api/statistics/overview")
    overview.value = res.data.data || {}
  } catch (error) {
    // 概览失败不影响主流程
  }
}

const loadPending = async () => {
  try {
    const res = await request.get("/api/tasks/upcoming", { params: { days: 0 } })
    pending.value = res.data.data || []
  } catch (error) {
    pending.value = []
  }
}

const loadPlans = async () => {
  try {
    const res = await request.get("/api/plans")
    plans.value = res.data.data || []
  } catch (error) {
    plans.value = []
  }
}

const refreshAll = () => {
  loadTasks()
  loadOverview()
  loadPending()
}

const changeStatus = async (task, status) => {
  try {
    await request.patch(`/api/tasks/${task.id}/status`, {
      status,
      actualMinutes: status === "DONE"
        ? (task.actualMinutes || task.estimatedMinutes || 0) : null
    })
    ElMessage.success("已更新")
    refreshAll()
  } catch (error) {
    ElMessage.error(errorMessage(error, "更新失败"))
  }
}

const openCreate = async () => {
  await loadPlans()
  if (plans.value.length === 0) {
    ElMessage.warning("请先在「学习计划」里生成一份计划")
    return
  }
  createForm.planId = plans.value[0].id
  createForm.title = ""
  createForm.description = ""
  createForm.taskDate = date.value
  createForm.estimatedMinutes = 60
  createVisible.value = true
}

const createTask = async () => {
  if (!createForm.planId || !createForm.title.trim()) {
    ElMessage.warning("请选择计划并填写任务标题")
    return
  }
  saving.value = true
  try {
    await request.post("/api/tasks", { ...createForm })
    ElMessage.success("任务已创建")
    createVisible.value = false
    refreshAll()
  } catch (error) {
    ElMessage.error(errorMessage(error, "创建失败"))
  } finally {
    saving.value = false
  }
}

const openEdit = (row) => {
  editForm.id = row.id
  editForm.title = row.title
  editForm.description = row.description || ""
  editForm.taskDate = formatDate(row.taskDate)
  editForm.estimatedMinutes = row.estimatedMinutes || 60
  editForm.status = row.status || "TODO"
  editForm.actualMinutes = row.actualMinutes || row.estimatedMinutes || 60
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.title.trim()) {
    ElMessage.warning("任务标题不能为空")
    return
  }
  saving.value = true
  try {
    await request.put(`/api/tasks/${editForm.id}`, {
      title: editForm.title,
      description: editForm.description,
      taskDate: editForm.taskDate,
      estimatedMinutes: editForm.estimatedMinutes
    })
    await request.patch(`/api/tasks/${editForm.id}/status`, {
      status: editForm.status,
      actualMinutes: editForm.status === "DONE"
        ? editForm.actualMinutes : null
    })
    ElMessage.success("已保存")
    editVisible.value = false
    refreshAll()
  } catch (error) {
    ElMessage.error(errorMessage(error, "保存失败"))
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除任务「${row.title}」？`, "提示",
      { type: "warning" })
    await request.delete(`/api/tasks/${row.id}`)
    ElMessage.success("删除成功")
    refreshAll()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

onMounted(refreshAll)
</script>


<style scoped>
.overview-card {
  margin-bottom: 18px;
}

.overview {
  display: flex;
  align-items: center;
  gap: 56px;
}

.ov-item {
  text-align: center;
}

.ov-item.pending {
  cursor: pointer;
}

.ov-num {
  font-size: 26px;
  font-weight: bold;
}

.ov-num.warn {
  color: #e6a23c;
}

.ov-label {
  color: var(--ink-caption);
  margin-top: 4px;
  font-size: 13px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--hairline);
}

.pending-title {
  font-size: 14px;
  margin-bottom: 4px;
}

.pending-sub {
  color: var(--ink-caption);
  font-size: 12px;
}
</style>
