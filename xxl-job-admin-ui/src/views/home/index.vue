<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useMessage } from 'naive-ui';
import { $t } from '@/locales';
import { request } from '@/service/request';
import HeaderBanner from './modules/header-banner.vue';
import CardData from './modules/card-data.vue';
import LineChart from './modules/line-chart.vue';
import PieChart from './modules/pie-chart.vue';

interface DashboardSummary {
  jobInfoCount: number;
  jobLogCount: number;
  jobLogSuccessCount: number;
  executorCount: number;
}

interface ChartInfo {
  triggerDayList: string[];
  triggerDayCountRunningList: number[];
  triggerDayCountSucList: number[];
  triggerDayCountFailList: number[];
  triggerCountRunningTotal: number;
  triggerCountSucTotal: number;
  triggerCountFailTotal: number;
}

type FlatResult<T> = Awaited<ReturnType<typeof request<T>>>;

const message = useMessage();

const summary = ref<DashboardSummary>({
  jobInfoCount: 0,
  jobLogCount: 0,
  jobLogSuccessCount: 0,
  executorCount: 0
});

const chartInfo = ref<ChartInfo | null>(null);
const loading = ref(false);
const rangeDays = ref(7);

const bannerStats = computed(() => {
  const chart = chartInfo.value;

  return [
    {
      id: 0,
      label: $t('page.xxlJob.dashboard.jobs'),
      value: String(summary.value.jobInfoCount)
    },
    {
      id: 1,
      label: $t('page.xxlJob.dashboard.logs'),
      value: String(summary.value.jobLogCount)
    },
    {
      id: 2,
      label: $t('page.xxlJob.dashboard.executors'),
      value: String(summary.value.executorCount)
    },
    {
      id: 3,
      label: $t('page.xxlJob.dashboard.success'),
      value: chart ? String(chart.triggerCountSucTotal) : '--'
    }
  ];
});

const metricCards = computed(() => {
  const chart = chartInfo.value;

  return [
    {
      key: 'jobs',
      title: $t('page.xxlJob.dashboard.jobs'),
      value: summary.value.jobInfoCount,
      hint: $t('page.xxlJob.dashboard.summaryHint'),
      color: { start: '#1d4ed8', end: '#3b82f6' },
      icon: 'mdi:briefcase-outline'
    },
    {
      key: 'logs',
      title: $t('page.xxlJob.dashboard.logs'),
      value: summary.value.jobLogCount,
      hint: $t('page.xxlJob.dashboard.logHint', { count: summary.value.jobLogSuccessCount }),
      color: { start: '#0f766e', end: '#14b8a6' },
      icon: 'mdi:file-document-outline'
    },
    {
      key: 'running',
      title: $t('page.xxlJob.dashboard.running'),
      value: chart?.triggerCountRunningTotal ?? 0,
      hint: $t('page.xxlJob.dashboard.executorHint'),
      color: { start: '#7c3aed', end: '#a855f7' },
      icon: 'mdi:play-circle-outline'
    },
    {
      key: 'failure',
      title: $t('page.xxlJob.dashboard.failure'),
      value: chart?.triggerCountFailTotal ?? 0,
      hint: $t('page.xxlJob.dashboard.executorHint'),
      color: { start: '#b91c1c', end: '#ef4444' },
      icon: 'mdi:alert-circle-outline'
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

function unwrapResult<T>(result: FlatResult<T>) {
  if (result.error) {
    throw result.error;
  }

  return result.data;
}

async function loadSummary() {
  const result = await request<DashboardSummary>({
    url: '/api/admin-next/dashboard/summary'
  });
  summary.value = unwrapResult(result);
}

async function loadChartInfo(days: number) {
  const end = new Date();
  const start = new Date();

  start.setDate(end.getDate() - (days - 1));
  start.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 59, 0);

  const form = new URLSearchParams();
  form.set('startDate', formatDate(start));
  form.set('endDate', formatDate(end));

  const result = await request<ChartInfo>({
    url: '/chartInfo',
    method: 'post',
    data: form,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  chartInfo.value = unwrapResult(result);
}

async function loadDashboard(days = rangeDays.value) {
  loading.value = true;
  try {
    rangeDays.value = days;
    await Promise.all([loadSummary(), loadChartInfo(days)]);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '调度数据加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadDashboard();
});
</script>

<template>
  <NSpace vertical :size="16">
    <HeaderBanner :stats="bannerStats" />
    <div class="flex justify-end">
      <NSpace>
        <NButton :type="rangeDays === 7 ? 'primary' : 'default'" :loading="loading && rangeDays === 7" @click="loadDashboard(7)">
          {{ $t('page.xxlJob.dashboard.recent7Days') }}
        </NButton>
        <NButton
          :type="rangeDays === 30 ? 'primary' : 'default'"
          :loading="loading && rangeDays === 30"
          @click="loadDashboard(30)"
        >
          {{ $t('page.xxlJob.dashboard.recent30Days') }}
        </NButton>
      </NSpace>
    </div>
    <CardData :items="metricCards" />

    <NGrid cols="1 s:1 m:2" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi>
        <LineChart :chart-data="chartInfo" :loading="loading" />
      </NGi>
      <NGi>
        <PieChart :chart-data="chartInfo" :loading="loading" />
      </NGi>
    </NGrid>
  </NSpace>
</template>
