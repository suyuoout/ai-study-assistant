<template>
  <div ref="chartRef" class="trend"></div>
</template>


<script setup>
import { onMounted, onUnmounted, ref, watch } from "vue"
import * as echarts from "echarts/core"
import { BarChart, LineChart } from "echarts/charts"
import {
  GridComponent, LegendComponent, TooltipComponent
} from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"

echarts.use([
  LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent,
  CanvasRenderer
])

const props = defineProps({
  /** [{ date, minutes, done }] */
  data: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

const render = () => {
  if (!chart) return
  const dates = props.data.map((item) => String(item.date).slice(5))
  const minutes = props.data.map((item) => Number(item.minutes) || 0)
  const done = props.data.map((item) => Number(item.done) || 0)

  chart.setOption({
    backgroundColor: "transparent",
    tooltip: { trigger: "axis" },
    legend: {
      data: ["学习分钟", "完成任务"],
      textStyle: { color: "rgba(255,255,255,0.5)" },
      right: 0,
      top: 0
    },
    grid: { left: 40, right: 40, top: 36, bottom: 28 },
    xAxis: {
      type: "category",
      data: dates,
      axisLine: { lineStyle: { color: "rgba(255,255,255,0.15)" } },
      axisLabel: { color: "rgba(255,255,255,0.5)", fontSize: 11 }
    },
    yAxis: [
      {
        type: "value",
        name: "分钟",
        nameTextStyle: { color: "rgba(255,255,255,0.4)", fontSize: 11 },
        splitLine: { lineStyle: { color: "rgba(255,255,255,0.06)" } },
        axisLabel: { color: "rgba(255,255,255,0.5)", fontSize: 11 }
      },
      {
        type: "value",
        name: "任务",
        nameTextStyle: { color: "rgba(255,255,255,0.4)", fontSize: 11 },
        splitLine: { show: false },
        axisLabel: { color: "rgba(255,255,255,0.5)", fontSize: 11 }
      }
    ],
    series: [
      {
        name: "学习分钟",
        type: "line",
        smooth: true,
        symbolSize: 6,
        data: minutes,
        itemStyle: { color: "#4c8dff" },
        areaStyle: { color: "rgba(76,141,255,0.18)" }
      },
      {
        name: "完成任务",
        type: "bar",
        yAxisIndex: 1,
        barWidth: 10,
        data: done,
        itemStyle: { color: "rgba(219,164,96,0.75)", borderRadius: 4 }
      }
    ]
  })
}

const resize = () => chart?.resize()

onMounted(() => {
  chart = echarts.init(chartRef.value)
  render()
  window.addEventListener("resize", resize)
})

onUnmounted(() => {
  window.removeEventListener("resize", resize)
  chart?.dispose()
  chart = null
})

watch(() => props.data, render, { deep: true })
</script>


<style scoped>
.trend {
  width: 100%;
  height: 220px;
}
</style>
