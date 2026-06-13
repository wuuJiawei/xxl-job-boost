<template>
  <div class="page-stack">
    <HeaderBanner :stats="bannerStats" />
    <CardData :items="metricCards" />

    <n-card :bordered="false" class="quick-link-card">
      <div class="quick-link-header">
        <div>
          <div class="table-title">{{ $t('page.xxlJob.governance.quickActions') }}</div>
          <div class="table-subtitle">{{ $t('page.xxlJob.governance.quickActionsHint') }}</div>
        </div>
        <div class="quick-link-actions">
          <n-button type="primary" @click="router.push('/failure-aggregates')">失败聚合</n-button>
          <n-button type="primary" secondary @click="router.push('/slow-tasks')">慢任务分析</n-button>
          <n-button type="primary" tertiary @click="router.push('/audits')">审计日志</n-button>
        </div>
      </div>
    </n-card>

    <n-grid cols="1 s:1 m:2" responsive="screen" :x-gap="16" :y-gap="16">
      <n-gi>
        <n-card :bordered="false">
          <template #header>
            <div class="table-header">
              <div class="table-title">{{ $t('page.xxlJob.governance.failureTitle') }}</div>
              <div class="table-subtitle">{{ $t('page.xxlJob.governance.failureSubtitle') }}</div>
            </div>
          </template>
          <n-data-table
            :columns="failureColumns"
            :data="overview.failureTopList"
            :loading="loading"
            :pagination="false"
            :row-key="failureRowKey"
            :single-line="false"
          />
        </n-card>
      </n-gi>

      <n-gi>
        <n-card :bordered="false">
          <template #header>
            <div class="table-header">
              <div class="table-title">{{ $t('page.xxlJob.governance.slowTitle') }}</div>
              <div class="table-subtitle">{{ $t('page.xxlJob.governance.slowSubtitle') }}</div>
            </div>
          </template>
          <n-data-table
            :columns="slowColumns"
            :data="overview.slowTopList"
            :loading="loading"
            :pagination="false"
            :row-key="slowRowKey"
          />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">{{ $t('page.xxlJob.governance.auditTitle') }}</div>
          <div class="table-subtitle">{{ $t('page.xxlJob.governance.auditSubtitle') }}</div>
        </div>
      </template>
      <n-data-table
        :columns="auditColumns"
        :data="overview.recentAuditList"
        :loading="loading"
        :pagination="false"
        :row-key="auditRowKey"
        :single-line="false"
      />
    </n-card>

    <n-modal v-model:show="detailVisible" preset="card" :title="detailTitle" style="width: 760px;">
      <pre class="message-preview">{{ detailContent || '空' }}</pre>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NGrid,
  NGi,
  NModal,
  NTag,
  useMessage,
  type DataTableColumns
} from 'naive-ui';
import { $t } from '@/locales';
import {
  fetchGovernanceOverview,
  type GovernanceAuditLog,
  type GovernanceFailureAggregate,
  type GovernanceOverview,
  type GovernanceSlowAggregate
} from '@/api/governance';
import HeaderBanner from '@/views/home/modules/header-banner.vue';
import CardData from '@/views/home/modules/card-data.vue';

defineOptions({
  name: 'GovernancePage'
});

const router = useRouter();
const message = useMessage();
const loading = ref(false);
const detailVisible = ref(false);
const detailTitle = ref('');
const detailContent = ref('');

const overview = reactive<GovernanceOverview>({
  totalJobs: 0,
  taggedJobs: 0,
  ownedJobs: 0,
  auditEvents: 0,
  failureTopList: [],
  slowTopList: [],
  recentAuditList: []
});

const bannerStats = computed(() => [
  {
    id: 0,
    label: $t('page.xxlJob.governance.jobs'),
    value: String(overview.totalJobs)
  },
  {
    id: 1,
    label: $t('page.xxlJob.governance.ownedJobs'),
    value: String(overview.ownedJobs)
  },
  {
    id: 2,
    label: $t('page.xxlJob.governance.taggedJobs'),
    value: String(overview.taggedJobs)
  },
  {
    id: 3,
    label: $t('page.xxlJob.governance.auditEvents'),
    value: String(overview.auditEvents)
  }
]);

const metricCards = computed(() => [
  {
    key: 'jobs',
    title: $t('page.xxlJob.governance.jobs'),
    value: overview.totalJobs,
    hint: $t('page.xxlJob.governance.jobsHint'),
    color: { start: '#1d4ed8', end: '#3b82f6' },
    icon: 'mdi:briefcase-outline'
  },
  {
    key: 'owned',
    title: $t('page.xxlJob.governance.ownedJobs'),
    value: overview.ownedJobs,
    hint: $t('page.xxlJob.governance.ownedHint'),
    color: { start: '#0f766e', end: '#14b8a6' },
    icon: 'mdi:account-check-outline'
  },
  {
    key: 'tagged',
    title: $t('page.xxlJob.governance.taggedJobs'),
    value: overview.taggedJobs,
    hint: $t('page.xxlJob.governance.taggedHint'),
    color: { start: '#c2410c', end: '#fb923c' },
    icon: 'mdi:tag-multiple-outline'
  },
  {
    key: 'audit',
    title: $t('page.xxlJob.governance.auditEvents'),
    value: overview.auditEvents,
    hint: $t('page.xxlJob.governance.auditHint'),
    color: { start: '#7c3aed', end: '#a855f7' },
    icon: 'mdi:clipboard-text-search-outline'
  }
]);

const failureColumns: DataTableColumns<GovernanceFailureAggregate> = [
  {
    title: '任务',
    key: 'jobDesc',
    minWidth: 220,
    render: row => `#${row.jobId} ${row.jobDesc || ''}`.trim()
  },
  {
    title: '负责人',
    key: 'author',
    width: 120,
    render: row => row.author || '-'
  },
  {
    title: '标签',
    key: 'jobTag',
    minWidth: 140,
    render: row => row.jobTag || '-'
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
    width: 180,
    render: row => formatDateTime(row.lastFailureTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row =>
      h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => router.push(`/logs?jobGroup=${row.jobGroup}&jobId=${row.jobId}`)
          },
          { default: () => '日志列表' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'warning',
            ghost: true,
            disabled: !row.lastLogId,
            onClick: () => router.push(`/log-detail?id=${row.lastLogId}`)
          },
          { default: () => '最近失败' }
        )
      ])
  }
];

const slowColumns: DataTableColumns<GovernanceSlowAggregate> = [
  {
    title: '任务',
    key: 'jobDesc',
    minWidth: 220,
    render: row => `#${row.jobId} ${row.jobDesc || ''}`.trim()
  },
  {
    title: '负责人',
    key: 'author',
    width: 120,
    render: row => row.author || '-'
  },
  {
    title: '标签',
    key: 'jobTag',
    minWidth: 140,
    render: row => row.jobTag || '-'
  },
  {
    title: '慢执行',
    key: 'slowCount',
    width: 100,
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
    width: 120,
    render: row => formatDuration(row.avgDurationMs)
  },
  {
    title: '峰值耗时',
    key: 'maxDurationMs',
    width: 120,
    render: row => formatDuration(row.maxDurationMs)
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row =>
      h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => router.push(`/logs?jobGroup=${row.jobGroup}&jobId=${row.jobId}`)
          },
          { default: () => '日志列表' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'warning',
            ghost: true,
            disabled: !row.lastLogId,
            onClick: () => router.push(`/log-detail?id=${row.lastLogId}`)
          },
          { default: () => '最近一次' }
        )
      ])
  }
];

const auditColumns: DataTableColumns<GovernanceAuditLog> = [
  { title: 'ID', key: 'id', width: 90 },
  { title: '操作人', key: 'operator', width: 120 },
  {
    title: '动作',
    key: 'actionType',
    width: 180,
    render: row => h(NTag, { round: true, type: 'info' }, { default: () => row.actionType })
  },
  {
    title: '资源',
    key: 'resourceName',
    minWidth: 240,
    render: row => `${row.resourceType}${row.resourceName ? ` / ${row.resourceName}` : ''}${row.resourceId ? ` (#${row.resourceId})` : ''}`
  },
  {
    title: '请求',
    key: 'request',
    minWidth: 180,
    render: row => `${row.requestMethod || ''} ${row.requestPath || ''}`.trim() || '-'
  },
  {
    title: '时间',
    key: 'createTime',
    width: 180,
    render: row => formatDateTime(row.createTime)
  },
  {
    title: '详情',
    key: 'detail',
    width: 100,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          quaternary: true,
          onClick: () => showDetail(row)
        },
        { default: () => '查看' }
      )
  }
];

function failureRowKey(row: GovernanceFailureAggregate) {
  return `${row.jobGroup}-${row.jobId}`;
}

function slowRowKey(row: GovernanceSlowAggregate) {
  return `${row.jobGroup}-${row.jobId}`;
}

function auditRowKey(row: GovernanceAuditLog) {
  return row.id;
}

function formatDateTime(value: string | number | null | undefined) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  const second = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
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

function showDetail(row: GovernanceAuditLog) {
  detailTitle.value = `${row.actionType} 详情`;
  detailContent.value = row.detailJson || '';
  detailVisible.value = true;
}

async function loadOverview() {
  loading.value = true;
  try {
    const response = await fetchGovernanceOverview();
    if (response.code !== 200) {
      throw new Error(response.msg || '治理总览加载失败');
    }
    Object.assign(overview, response.data);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '治理总览加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadOverview();
});
</script>

<style scoped>
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-link-card {
  overflow: hidden;
}

.quick-link-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.quick-link-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.table-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.table-subtitle {
  color: rgb(100 116 139);
  font-size: 13px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.message-preview {
  max-height: 480px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgb(248 250 252);
  padding: 12px;
  border-radius: 8px;
}
</style>
