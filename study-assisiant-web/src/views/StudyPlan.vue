<template>
  <div>
    <el-card shadow="never" class="gen-card">
      <template #header>
        <span>AI 生成学习计划</span>
      </template>
      <el-form :inline="true">
        <el-form-item label="学习目标">
          <el-input v-model="gen.goal" placeholder="如：掌握 Java Web" style="width: 220px" />
        </el-form-item>
        <el-form-item label="当前水平">
          <el-input v-model="gen.currentLevel" placeholder="如：已学完 Java 基础" style="width: 220px" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="gen.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="gen.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="每日分钟">
          <el-input-number v-model="gen.dailyMinutes" :min="10" :max="600" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="generating" @click="generatePlan">
            生成计划
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="8" v-for="item in plans" :key="item.id">
        <el-card shadow="hover" class="plan-card">
          <div class="plan-title">{{ item.title }}</div>
          <div class="plan-meta">
            <el-tag size="small">{{ item.status }}</el-tag>
            <span class="time">{{ fmtDateTime(item.createTime) }}</span>
          </div>
          <div class="plan-actions">
            <el-button size="small" type="primary" plain @click="detail(item.id)">
              查看详情
            </el-button>
            <el-button size="small" type="danger" plain @click="removePlan(item.id)">
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="plans.length === 0" description="还没有学习计划，先生成一个吧" />
  </div>
</template>


<script setup>
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import request from "../api/request"

const router = useRouter()
const plans = ref([])
const generating = ref(false)

const gen = reactive({
  goal: "",
  currentLevel: "",
  startDate: "",
  endDate: "",
  dailyMinutes: 120
})

const fmtDateTime = (value) => {
  if (!value) return "—"
  const s = String(value)
  const m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/)
  return m ? `${m[1]} ${m[2]}` : s
}

const getPlans = async () => {
  const res = await request.get("/api/plans")
  plans.value = res.data.data
}

const generatePlan = async () => {
  if (!gen.goal || !gen.startDate || !gen.endDate) {
    ElMessage.warning("请填写学习目标、开始和截止日期")
    return
  }
  generating.value = true
  try {
    const res = await request.post("/api/plans/generate", { ...gen })
    ElMessage.success("生成成功，共拆解 " + res.data.data.taskCount + " 个任务")
    getPlans()
  } catch (e) {
    const msg = e.response?.data?.message
    if (msg) {
      ElMessage.error(msg)
    } else if (e.code === "ECONNABORTED") {
      ElMessage.error("生成超时，请稍后重试")
    } else {
      ElMessage.error("生成失败，请确认网络与 AI 配置")
    }
  } finally {
    generating.value = false
  }
}

const detail = (id) => router.push(`/study/detail/${id}`)

const removePlan = async (id) => {
  try {
    await ElMessageBox.confirm("确认删除该学习计划？", "提示", { type: "warning" })
    await request.delete(`/api/ai/delete/${id}`)
    ElMessage.success("删除成功")
    getPlans()
  } catch (e) {
    if (e !== "cancel") ElMessage.error(e.response?.data?.message || "删除失败")
  }
}

onMounted(getPlans)
</script>


<style scoped>
.gen-card {
  margin-bottom: 18px;
}

.plan-card {
  margin-bottom: 16px;
}

.plan-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.plan-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink-caption);
  margin-bottom: 12px;
}

.time {
  font-size: 12px;
}

.plan-actions {
  display: flex;
  gap: 8px;
}
</style>
