<template>
  <div class="dashboard-page">
    <div class="dashboard-hero">
      <div>
        <div class="section-kicker">Dashboard</div>
        <h2>先把旧报表链路迁移过来，再谈增强。</h2>
        <p>
          当前页面直接复用现有 `/chartInfo` 接口，先保证新旧控制台看到的是同一套调度数据。
        </p>
      </div>
      <div class="range-actions">
        <n-button @click="setRange(7)">近 7 天</n-button>
        <n-button @click="setRange(30)">近 30 天</n-button>
      </div>
    </div>

    <div class="metric-grid">
      <n-card v-for="item in metrics" :key="item.label" class="metric-card" :bordered="false">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-hint">{{ item.hint }}</div>
      </n-card>
    </div>

    <div class="chart-grid">
      <n-card title="调度趋势" :bordered="false">
        <div ref="lineChartRef" class="chart"></div>
      </n-card>
      <n-card title="结果分布" :bordered="false">
        <div ref="pieChartRef" class="chart"></div>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { NButton, NCard, useMessage } from 'naive-ui';
import { fetchDashboardSummary } from '@/api/admin-next';
import { fetchChartInfo, type ChartInfo } from '@/api/dashboard';

const message = useMessage();
const lineChartRef = ref<HTMLDivElement | null>(null);
const pieChartRef = ref<HTMLDivElement | null>(null);
const chartData = ref<ChartInfo | null>(null);
const summary = ref({
  jobInfoCount: 0,
  jobLogCount: 0,
  jobLogSuccessCount: 0,
  executorCount: 0
});

let lineChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;

const metrics = computed(() => {
  const data = chartData.value;
  if (!data) {
    return [
      { label: '成功次数', value: '--', hint: '加载中' },
      { label: '失败次数', value: '--', hint: '加载中' },
      { label: '运行中次数', value: '--', hint: '加载中' }
    ];
  }

  return [
    {
      label: '任务总数',
      value: String(summary.value.jobInfoCount),
      hint: '复用现有 dashboardInfo'
    },
    {
      label: '调度总数',
      value: String(summary.value.jobLogCount),
      hint: `成功 ${summary.value.jobLogSuccessCount}`
    },
    {
      label: '执行器数量',
      value: String(summary.value.executorCount),
      hint: '与旧控制台同口径'
    }
  ];
});

function formatDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ' ' + [pad(date.getHours()), pad(date.getMinutes()), pad(date.getSeconds())].join(':');
}

async function loadRange(days: number) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - (days - 1));
  start.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 59, 0);

  try {
    const response = await fetchChartInfo(formatDate(start), formatDate(end));
    if (response.code !== 200) {
      throw new Error(response.msg || '报表加载失败');
    }
    chartData.value = response.data;
    await nextTick();
    renderCharts();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '报表加载失败');
  }
}

async function loadSummary() {
  try {
    const response = await fetchDashboardSummary();
    if (response.code !== 200) {
      throw new Error(response.msg || '摘要加载失败');
    }
    summary.value = response.data;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '摘要加载失败');
  }
}

function renderCharts() {
  if (!chartData.value || !lineChartRef.value || !pieChartRef.value) {
    return;
  }

  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value);
  }
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value);
  }

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败', '运行中'] },
    grid: { left: 24, right: 12, top: 40, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.value.triggerDayList
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '成功',
        type: 'line',
        smooth: true,
        data: chartData.value.triggerDayCountSucList
      },
      {
        name: '失败',
        type: 'line',
        smooth: true,
        data: chartData.value.triggerDayCountFailList
      },
      {
        name: '运行中',
        type: 'line',
        smooth: true,
        data: chartData.value.triggerDayCountRunningList
      }
    ]
  });

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '72%'],
        itemStyle: {
          borderRadius: 12,
          borderColor: '#fff',
          borderWidth: 4
        },
        data: [
          { value: chartData.value.triggerCountSucTotal, name: '成功' },
          { value: chartData.value.triggerCountFailTotal, name: '失败' },
          { value: chartData.value.triggerCountRunningTotal, name: '运行中' }
        ]
      }
    ]
  });
}

function setRange(days: number) {
  void loadRange(days);
}

function handleResize() {
  lineChart?.resize();
  pieChart?.resize();
}

onMounted(() => {
  void loadSummary();
  void loadRange(7);
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  lineChart?.dispose();
  pieChart?.dispose();
});
</script>
