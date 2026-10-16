<template>
  <div>
    <el-card shadow="never" class="gen-card">
      <template #header>
        <div class="card-head">
          <span>AI 生成学习计划</span>
          <span class="card-sub">生成后会按天拆解成可执行任务</span>
        </div>
      </template>
      <el-form label-width="88px" class="gen-form">
        <el-form-item label="学习目标" required>
          <el-input v-model="gen.goal" placeholder="如：掌握 Java Web 开发"
                    style="max-width: 320px" />
        </el-form-item>
        <el-form-item label="当前水平">
          <el-input v-model="gen.currentLevel" placeholder="如：已学完 Java 基础"
                    style="max-width: 320px" />
        </el-form-item>
        <el-form-item label="日期范围" required>
          <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="截止日期"
              :disabled-date="disablePast"
          />
        </el-form-item>
        <el-form-item label="每日分钟" required>
          <el-input-number v-model="gen.dailyMinutes" :min="10" :max="720"
                           :step="15" />
        </el-form-item>
        <el-form-item label="可学习日">
          <el-checkbox-group v-model="gen.availableDays">
            <el-checkbox v-for="day in weekdays" :key="day" :value="day">
              {{ day }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="generating" @click="generatePlan">
            生成计划
          </el-button>
          <span class="gen-hint">
            生成需要调用大模型，通常 10-40 秒，请勿重复点击
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="8" v-for="item in plans" :key="item.id">
        <el-card shadow="hover" class="plan-card">
          <div class="plan-title" :title="item.title">{{ item.title }}</div>
          <div class="plan-meta">
            <el-tag size="small" :type="statusType(item.status)">
              {{ item.status }}
            </el-tag>
            <span class="time">{{ formatDateTime(item.createTime) }}</span>
          </div>
          <div class="plan-goal">{{ item.goal || "未填写目标" }}</div>
          <div class="plan-actions">
            <el-button size="small" type="primary" plain @click="detail(item.id)">
              查看详情
            </el-button>
            <el-button size="small" @click="rename(item)">重命名</el-button>
            <el-dropdown size="small" trigger="click"
                         @command="(cmd) => changeStatus(item, cmd)">
              <el-button size="small">状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="进行中">进行中</el-dropdown-item>
                  <el-dropdown-item command="已完成">已完成</el-dropdown-item>
                  <el-dropdown-item command="已归档">已归档</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" plain
                       @click="removePlan(item.id)">
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && plans.length === 0"
              description="还没有学习计划，先生成一个吧" />
  </div>
</template>


<script setup>
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import request, { errorMessage } from "../api/request"
import { formatDateTime, todayString } from "../utils/format"

const router = useRouter()
const plans = ref([])
const generating = ref(false)
const loading = ref(false)
const dateRange = ref([])
const weekdays = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]

const gen = reactive({
  goal: "",
  currentLevel: "",
  dailyMinutes: 120,
  availableDays: []
})

const disablePast = (date) =>
  date.getTime() < new Date(`${todayString()}T00:00:00`).getTime()

const statusType = (status) => {
  if (status === "已完成") return "success"
  if (status === "已归档") return "info"
  return "warning"
}

const getPlans = async () => {
  loading.value = true
  try {
    const res = await request.get("/api/plans")
    plans.value = res.data.data || []
  } catch (error) {
    ElMessage.error(errorMessage(error, "加载计划失败"))
  } finally {
    loading.value = false
  }
}

const generatePlan = async () => {
  if (!gen.goal.trim()) {
    ElMessage.warning("请填写学习目标")
    return
  }
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning("请选择开始和截止日期")
    return
  }

  generating.value = true
  try {
    const res = await request.post("/api/plans/generate", {
      goal: gen.goal.trim(),
      currentLevel: gen.currentLevel,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      dailyMinutes: gen.dailyMinutes,
      availableDays: gen.availableDays
    })
    ElMessage.success(
      `生成成功，共拆解 ${res.data.data.taskCount} 个任务`)
    getPlans()
  } catch (error) {
    ElMessage.error(errorMessage(error, "生成失败，请确认 AI 配置"))
  } finally {
    generating.value = false
  }
}

const detail = (id) => router.push(`/study/detail/${id}`)

const rename = async (plan) => {
  try {
    const { value } = await ElMessageBox.prompt("输入新的计划名称", "重命名",
      {
        inputValue: plan.title,
        inputValidator: (v) => !!v?.trim() || "名称不能为空"
      })
    await request.put(`/api/plans/${plan.id}`, { title: value.trim() })
    ElMessage.success("已重命名")
    getPlans()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "重命名失败"))
    }
  }
}

const changeStatus = async (plan, status) => {
  try {
    await request.put(`/api/plans/${plan.id}`, { status })
    ElMessage.success("状态已更新")
    getPlans()
  } catch (error) {
    ElMessage.error(errorMessage(error, "更新失败"))
  }
}

const removePlan = async (id) => {
  try {
    await ElMessageBox.confirm(
      "确认删除该学习计划？计划下的每日任务会一并删除。", "提示",
      { type: "warning" })
    await request.delete(`/api/plans/${id}`)
    ElMessage.success("删除成功")
    getPlans()
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(errorMessage(error, "删除失败"))
    }
  }
}

onMounted(getPlans)
</script>


<style scoped>
.gen-card {
  margin-bottom: 18px;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-sub,
.gen-hint {
  color: var(--ink-caption);
  font-size: 12px;
  font-weight: normal;
}

.gen-hint {
  margin-left: 12px;
}

.plan-card {
  margin-bottom: 16px;
}

.plan-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink-caption);
  margin-bottom: 10px;
}

.time {
  font-size: 12px;
}

.plan-goal {
  color: var(--ink-secondary);
  font-size: 13px;
  line-height: 1.6;
  height: 42px;
  overflow: hidden;
  margin-bottom: 12px;
}

.plan-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
