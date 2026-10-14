<template>
  <div>
    <el-row :gutter="16">
      <el-col
          v-for="item in cards"
          :key="item.label"
          :span="4"
      >
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="welcome">
      <h2>系统运行概况</h2>
      <p>这里展示系统整体数据。你可以通过左侧菜单管理用户、学习资料和学习计划。</p>
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
      </el-descriptions>
    </el-card>
  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue"
import request from "../../api/request"

const overview = ref({})

const cards = computed(() => [
  { label: "用户", value: overview.value.users || 0 },
  { label: "资料", value: overview.value.documents || 0 },
  { label: "计划", value: overview.value.plans || 0 },
  { label: "任务", value: overview.value.tasks || 0 },
  { label: "会话", value: overview.value.chatSessions || 0 },
  { label: "消息", value: overview.value.chatMessages || 0 }
])

onMounted(async () => {
  const res = await request.get("/api/admin/overview")
  overview.value = res.data.data
})
</script>


<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  color: var(--ink-caption);
  margin-top: 6px;
}

.welcome h2 {
  margin-top: 0;
}
</style>
