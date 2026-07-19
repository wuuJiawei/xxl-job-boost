<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NTag, useMessage, type DataTableColumns } from 'naive-ui';
import {
  fetchGovernanceOverview,
  type GovernanceFailureAggregate,
  type GovernanceOverview,
  type GovernanceSlowAggregate
} from '@/api/governance';
import { $t } from '@/locales';
import { request } from '@/service/request';
import { useAuthStore } from '@/store/modules/auth';
import HeaderBanner from './modules/header-banner.vue';
import CardData from './modules/card-data.vue';
import LineChart from './modules/line-chart.vue';
import PieChart from './modules/pie-chart.vue';

defineOptions({
  name: 'home'
});

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

const router = useRouter();
const message = useMessage();
const authStore = useAuthStore();

const summary = ref<DashboardSummary>({
  jobInfoCount: 0,
  jobLogCount: 0,
  jobLogSuccessCount: 0,
  executorCount: 0
});

const governance = reactive<GovernanceOverview>({
  totalJobs: 0,
  taggedJobs: 0,
  ownedJobs: 0,
  auditEvents: 0,
  failureTopList: [],
  slowTopList: [],
  recentAuditList: []
});

const chartInfo = ref<ChartInfo | null>(null);
const dashboardLoading = ref(false);
const governanceLoading = ref(false);
const rangeDays = ref(7);

const showGovernance = computed(() => authStore.userInfo.isAdmin);

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

const governanceStats = computed(() => {
  const total = governance.totalJobs;

  return [
    {
      key: 'owned',
      label: $t('page.xxlJob.governance.ownedJobs'),
      value: governance.ownedJobs,
      suffix: `/ ${total}`,
      rate: calculateRate(governance.ownedJobs, total),
      icon: 'mdi:account-check-outline',
      color: '#0f766e'
    },
    {
      key: 'tagged',
      label: $t('page.xxlJob.governance.taggedJobs'),
      value: governance.taggedJobs,
      suffix: `/ ${total}`,
      rate: calculateRate(governance.taggedJobs, total),
      icon: 'mdi:tag-multiple-outline',
      color: '#c2410c'
    },
    {
      key: 'audit',
      label: $t('page.xxlJob.governance.auditEvents'),
      value: governance.auditEvents,
      suffix: '',
      rate: null,
      icon: 'mdi:clipboard-text-search-outline',
      color: '#7c3aed'
    }
  ];
});

const failureColumns: DataTableColumns<GovernanceFailureAggregate> = [
  {
    title: '任务',
    key: 'jobDesc',
    minWidth: 200,
    render: row => `#${row.jobId} ${row.jobDesc || ''}`.trim()
  },
  {
    title: '负责人',
    key: 'author',
    width: 110,
    render: row => row.author || '-'
  },
  {
    title: '失败次数',
    key: 'failureCount',
    width: 100,
    render: row =>
      h(
        NTag,
        { type: row.failureCount >= 10 ? 'error' : row.failureCount >= 3 ? 'warning' : 'default', round: true },
        { default: () => String(row.failureCount) }
      )
  },
  {
    title: '最近失败',
    key: 'lastFailureTime',
    width: 170,
    render: row => formatDateTime(row.lastFailureTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          quaternary: true,
          onClick: () => router.push(`/logs?jobGroup=${row.jobGroup}&jobId=${row.jobId}`)
        },
        { default: () => '查看日志' }
      )
  }
];

const slowColumns: DataTableColumns<GovernanceSlowAggregate> = [
  {
    title: '任务',
    key: 'jobDesc',
    minWidth: 200,
    render: row => `#${row.jobId} ${row.jobDesc || ''}`.trim()
  },
  {
    title: '负责人',
    key: 'author',
    width: 110,
    render: row => row.author || '-'
  },
  {
    title: '慢执行',
    key: 'slowCount',
    width: 90,
    render: row =>
      h(
        NTag,
        { type: row.slowCount >= 10 ? 'error' : row.slowCount >= 3 ? 'warning' : 'default', round: true },
        { default: () => String(row.slowCount) }
      )
  },
  {
    title: '平均耗时',
    key: 'avgDurationMs',
    width: 110,
    render: row => formatDuration(row.avgDurationMs)
  },
  {
    title: '峰值耗时',
    key: 'maxDurationMs',
    width: 110,
    render: row => formatDuration(row.maxDurationMs)
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          quaternary: true,
          onClick: () => router.push(`/logs?jobGroup=${row.jobGroup}&jobId=${row.jobId}`)
        },
        { default: () => '查看日志' }
      )
  }
];

function calculateRate(value: number, total: number) {
  return total > 0 ? Math.round((value / total) * 100) : 0;
}

function failureRowKey(row: GovernanceFailureAggregate) {
  return `${row.jobGroup}-${row.jobId}`;
}

function slowRowKey(row: GovernanceSlowAggregate) {
  return `${row.jobGroup}-${row.jobId}`;
}

function formatDateTime(value: string | number | null | undefined) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const pad = (part: number) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDuration(durationMs: number | null | undefined) {
  if (!durationMs) {
    return '0 ms';
  }
  if (durationMs < 1000) {
    return `${durationMs} ms`;
  }
  return `${(durationMs / 1000).toFixed(durationMs >= 10000 ? 0 : 1)} s`;
}

function formatDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');

  return (
    [date.getFullYear(), pad(date.getMonth() + 1), pad(date.getDate())].join('-') +
    ' ' +
    [pad(date.getHours()), pad(date.getMinutes()), pad(date.getSeconds())].join(':')
  );
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
  dashboardLoading.value = true;
  try {
    rangeDays.value = days;
    await Promise.all([loadSummary(), loadChartInfo(days)]);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '调度数据加载失败');
  } finally {
    dashboardLoading.value = false;
  }
}

async function loadGovernance() {
  governanceLoading.value = true;
  try {
    const response = await fetchGovernanceOverview();
    if (response.code !== 200) {
      throw new Error(response.msg || '治理数据加载失败');
    }
    Object.assign(governance, response.data);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '治理数据加载失败');
  } finally {
    governanceLoading.value = false;
  }
}

onMounted(() => {
  void loadDashboard();
  if (showGovernance.value) {
    void loadGovernance();
  }
});
</script>

<template>
  <NSpace vertical :size="16">
    <HeaderBanner :stats="bannerStats" />

    <CardData :items="metricCards" :title="$t('page.xxlJob.dashboard.title')">
      <template #action>
        <NButtonGroup>
          <NButton
            :type="rangeDays === 7 ? 'primary' : 'default'"
            :loading="dashboardLoading && rangeDays === 7"
            @click="loadDashboard(7)"
          >
            {{ $t('page.xxlJob.dashboard.recent7Days') }}
          </NButton>
          <NButton
            :type="rangeDays === 30 ? 'primary' : 'default'"
            :loading="dashboardLoading && rangeDays === 30"
            @click="loadDashboard(30)"
          >
            {{ $t('page.xxlJob.dashboard.recent30Days') }}
          </NButton>
        </NButtonGroup>
      </template>
    </CardData>

    <NGrid cols="1 s:1 m:2" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi>
        <LineChart :chart-data="chartInfo" :loading="dashboardLoading" />
      </NGi>
      <NGi>
        <PieChart :chart-data="chartInfo" :loading="dashboardLoading" />
      </NGi>
    </NGrid>

    <template v-if="showGovernance">
      <NCard :bordered="false" size="small">
        <template #header>
          <div class="section-header">
            <div>
              <div class="section-title">{{ $t('page.xxlJob.governance.title') }}</div>
              <div class="section-subtitle">{{ $t('page.xxlJob.governance.description') }}</div>
            </div>
            <div class="quick-actions">
              <NButton secondary type="primary" @click="router.push('/failure-aggregates')">
                <template #icon><SvgIcon icon="mdi:alert-decagram-outline" /></template>
                {{ $t('route.failure-aggregates') }}
              </NButton>
              <NButton secondary type="primary" @click="router.push('/slow-tasks')">
                <template #icon><SvgIcon icon="mdi:timer-alert-outline" /></template>
                {{ $t('route.slow-tasks') }}
              </NButton>
              <NButton secondary type="primary" @click="router.push('/audits')">
                <template #icon><SvgIcon icon="mdi:clipboard-text-search-outline" /></template>
                {{ $t('route.audits') }}
              </NButton>
            </div>
          </div>
        </template>

        <NSpin :show="governanceLoading">
          <NGrid cols="1 s:1 m:3" responsive="screen" :x-gap="24" :y-gap="20">
            <NGi v-for="item in governanceStats" :key="item.key">
              <div class="governance-stat">
                <div class="stat-heading">
                  <div class="stat-icon" :style="{ color: item.color, backgroundColor: `${item.color}14` }">
                    <SvgIcon :icon="item.icon" />
                  </div>
                  <span>{{ item.label }}</span>
                </div>
                <div class="stat-value">
                  <span>{{ item.value }}</span>
                  <small v-if="item.suffix">
                    {{ item.suffix }}
                    <template v-if="item.rate !== null">· {{ item.rate }}%</template>
                  </small>
                </div>
                <NProgress
                  v-if="item.rate !== null"
                  type="line"
                  :percentage="item.rate"
                  :show-indicator="false"
                  :height="6"
                  :border-radius="3"
                  :color="item.color"
                  :rail-color="`${item.color}1f`"
                />
                <div v-else class="stat-hint">{{ $t('page.xxlJob.governance.auditHint') }}</div>
              </div>
            </NGi>
          </NGrid>
        </NSpin>
      </NCard>

      <NGrid cols="1 s:1 xl:2" responsive="screen" :x-gap="16" :y-gap="16">
        <NGi>
          <NCard :bordered="false" class="h-full">
            <template #header>
              <div>
                <div class="section-title">{{ $t('page.xxlJob.governance.failureTitle') }}</div>
                <div class="section-subtitle">{{ $t('page.xxlJob.governance.failureSubtitle') }}</div>
              </div>
            </template>
            <NDataTable
              :columns="failureColumns"
              :data="governance.failureTopList"
              :loading="governanceLoading"
              :pagination="false"
              :row-key="failureRowKey"
              :scroll-x="710"
              :single-line="false"
            />
          </NCard>
        </NGi>

        <NGi>
          <NCard :bordered="false" class="h-full">
            <template #header>
              <div>
                <div class="section-title">{{ $t('page.xxlJob.governance.slowTitle') }}</div>
                <div class="section-subtitle">{{ $t('page.xxlJob.governance.slowSubtitle') }}</div>
              </div>
            </template>
            <NDataTable
              :columns="slowColumns"
              :data="governance.slowTopList"
              :loading="governanceLoading"
              :pagination="false"
              :row-key="slowRowKey"
              :scroll-x="750"
              :single-line="false"
            />
          </NCard>
        </NGi>
      </NGrid>
    </template>
  </NSpace>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.section-subtitle {
  margin-top: 4px;
  color: rgb(100 116 139);
  font-size: 13px;
}

.quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.governance-stat {
  min-height: 112px;
  padding: 4px 8px;
}

.stat-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgb(71 85 105);
  font-size: 13px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  font-size: 18px;
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 12px 0 8px;
  color: rgb(15 23 42);
  font-size: 26px;
  font-weight: 700;
}

.stat-value small {
  color: rgb(100 116 139);
  font-size: 13px;
  font-weight: 400;
}

.stat-hint {
  color: rgb(100 116 139);
  font-size: 12px;
}

:global(.dark) .section-subtitle,
:global(.dark) .stat-heading,
:global(.dark) .stat-value small,
:global(.dark) .stat-hint {
  color: rgb(148 163 184);
}

:global(.dark) .stat-value {
  color: rgb(241 245 249);
}
</style>
