<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="section-kicker">Audit Logs</div>
      <h2>{{ $t('page.xxlJob.audits.title') }}</h2>
      <p>{{ $t('page.xxlJob.audits.description') }}</p>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid alerts-record-filter-grid">
        <n-input v-model:value="filters.operator" placeholder="按操作人查询" clearable />
        <n-select v-model:value="filters.actionType" :options="actionOptions" placeholder="动作类型" clearable />
        <n-select v-model:value="filters.resourceType" :options="resourceOptions" placeholder="资源类型" clearable />
        <n-select v-model:value="filters.jobGroup" :options="jobGroupOptions" placeholder="执行器" />
        <div class="filter-actions">
          <n-button type="primary" @click="search">查询</n-button>
          <n-button @click="reset">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">{{ $t('page.xxlJob.audits.title') }}</div>
          <div class="table-subtitle">{{ $t('page.xxlJob.audits.subtitle') }}</div>
        </div>
      </template>

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
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
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NModal,
  NSelect,
  NTag,
  useMessage,
  type DataTableColumns,
  type PaginationProps,
  type SelectOption
} from 'naive-ui';
import { $t } from '@/locales';
import { fetchJobGroups, type JobGroupOption } from '@/api/admin-next';
import { fetchAuditLogs, type AuditLog } from '@/api/audit-logs';

const message = useMessage();
const loading = ref(false);
const rows = ref<AuditLog[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const detailVisible = ref(false);
const detailTitle = ref('');
const detailContent = ref('');

const filters = reactive({
  operator: '',
  actionType: '',
  resourceType: '',
  jobGroup: -1
});

const actionOptions: SelectOption[] = [
  { label: '全部动作', value: '' },
  { label: '新增任务', value: 'jobinfo-save' },
  { label: '更新任务', value: 'jobinfo-update' },
  { label: '删除任务', value: 'jobinfo-remove' },
  { label: '启动任务', value: 'jobinfo-start' },
  { label: '停止任务', value: 'jobinfo-stop' },
  { label: '手动触发任务', value: 'jobinfo-trigger' },
  { label: '更新任务代码', value: 'jobcode-update' },
  { label: '新增执行器', value: 'jobgroup-insert' },
  { label: '更新执行器', value: 'jobgroup-update' },
  { label: '删除执行器', value: 'jobgroup-delete' },
  { label: '新增用户', value: 'user-insert' },
  { label: '更新用户', value: 'user-update' },
  { label: '删除用户', value: 'user-delete' },
  { label: '新增告警渠道', value: 'alarmchannel-insert' },
  { label: '更新告警渠道', value: 'alarmchannel-update' },
  { label: '删除告警渠道', value: 'alarmchannel-delete' }
];

const resourceOptions: SelectOption[] = [
  { label: '全部资源', value: '' },
  { label: '任务', value: 'job' },
  { label: '任务代码', value: 'job-code' },
  { label: '执行器', value: 'job-group' },
  { label: '用户', value: 'user' },
  { label: '告警渠道', value: 'alarm-channel' }
];

const jobGroupOptions = computed<SelectOption[]>(() => [
  { label: '全部执行器', value: -1 },
  ...jobGroups.value.map(item => ({ label: item.title, value: item.id }))
]);

const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: page => {
    pagination.page = page;
    void loadData();
  },
  onUpdatePageSize: pageSize => {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    void loadData();
  }
});

const columns: DataTableColumns<AuditLog> = [
  { title: 'ID', key: 'id', width: 90 },
  { title: '操作人', key: 'operator', width: 120 },
  {
    title: '动作类型',
    key: 'actionType',
    width: 180,
    render: row => h(NTag, { round: true, type: 'info' }, { default: () => row.actionType })
  },
  {
    title: '资源',
    key: 'resourceName',
    minWidth: 220,
    render: row => `${row.resourceType}${row.resourceName ? ` / ${row.resourceName}` : ''}${row.resourceId ? ` (#${row.resourceId})` : ''}`
  },
  {
    title: '执行器',
    key: 'jobGroup',
    width: 100,
    render: row => row.jobGroup || '-'
  },
  {
    title: '来源',
    key: 'source',
    width: 120,
    render: row => row.source || '-'
  },
  {
    title: '请求',
    key: 'request',
    minWidth: 220,
    render: row => `${row.requestMethod || ''} ${row.requestPath || ''}`.trim() || '-'
  },
  {
    title: '客户端 IP',
    key: 'clientIp',
    width: 140,
    render: row => row.clientIp || '-'
  },
  {
    title: '时间',
    key: 'createTime',
    width: 180,
    render: row => formatDateTime(row.createTime)
  },
  {
    title: '详情',
    key: 'detailJson',
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

function rowKey(row: AuditLog) {
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

function showDetail(row: AuditLog) {
  detailTitle.value = `${row.actionType} 详情`;
  detailContent.value = row.detailJson || '';
  detailVisible.value = true;
}

async function loadJobGroups() {
  const response = await fetchJobGroups();
  if (response.code !== 200) {
    throw new Error(response.msg || '执行器数据加载失败');
  }
  jobGroups.value = response.data;
}

async function loadData() {
  loading.value = true;
  try {
    const response = await fetchAuditLogs({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      operator: filters.operator,
      actionType: filters.actionType,
      resourceType: filters.resourceType,
      jobGroup: filters.jobGroup
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '审计日志加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '审计日志加载失败');
  } finally {
    loading.value = false;
  }
}

async function search() {
  pagination.page = 1;
  await loadData();
}

async function reset() {
  filters.operator = '';
  filters.actionType = '';
  filters.resourceType = '';
  filters.jobGroup = -1;
  pagination.page = 1;
  await loadData();
}

onMounted(async () => {
  try {
    await loadJobGroups();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '审计页初始化失败');
  }
});
</script>
