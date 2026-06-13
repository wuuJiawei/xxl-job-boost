<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="section-kicker">Failure Aggregates</div>
      <h2>{{ $t('page.xxlJob.failureAggregates.title') }}</h2>
      <p>{{ $t('page.xxlJob.failureAggregates.description') }}</p>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid logs-filter-grid">
        <n-select v-model:value="filters.jobGroup" :options="jobGroupOptions" placeholder="选择执行器" />
        <n-select v-model:value="filters.jobId" :options="jobOptions" placeholder="选择任务" />
        <n-input v-model:value="filters.author" placeholder="按负责人查询" clearable />
        <n-input v-model:value="filters.jobTag" placeholder="按任务标签查询" clearable />
        <n-date-picker
          v-model:value="filters.timeRange"
          type="datetimerange"
          clearable
          class="range-picker"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
        <div class="filter-actions">
          <n-button type="primary" @click="search">查询</n-button>
          <n-button @click="reset">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">{{ $t('page.xxlJob.failureAggregates.title') }}</div>
          <div class="table-subtitle">{{ $t('page.xxlJob.failureAggregates.subtitle') }}</div>
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

    <n-modal v-model:show="messageModalVisible" preset="card" :title="messageModalTitle" style="width: 720px;">
      <pre class="message-preview">{{ messageModalContent || '空' }}</pre>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
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
import { fetchFailureAggregates, type JobFailureAggregate } from '@/api/failure-aggregates';
import { fetchJobGroups, fetchJobsByGroup, type JobGroupOption, type JobOption } from '@/api/admin-next';

const router = useRouter();
const message = useMessage();
const loading = ref(false);
const rows = ref<JobFailureAggregate[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const jobs = ref<JobOption[]>([]);
const messageModalVisible = ref(false);
const messageModalTitle = ref('');
const messageModalContent = ref('');

const filters = reactive<{
  jobGroup: number;
  jobId: number;
  author: string;
  jobTag: string;
  timeRange: [number, number] | null;
}>({
  jobGroup: -1,
  jobId: 0,
  author: '',
  jobTag: '',
  timeRange: createDefaultRange()
});

const jobGroupOptions = computed<SelectOption[]>(() => [
  { label: '全部执行器', value: -1 },
  ...jobGroups.value.map(item => ({
    label: item.title,
    value: item.id
  }))
]);

const jobOptions = computed<SelectOption[]>(() => [
  { label: '全部任务', value: 0 },
  ...jobs.value.map(item => ({
    label: item.jobDesc || `任务 #${item.id}`,
    value: item.id
  }))
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

const columns: DataTableColumns<JobFailureAggregate> = [
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
    title: '任务标签',
    key: 'jobTag',
    minWidth: 160,
    render: row => row.jobTag || '-'
  },
  {
    title: '失败次数',
    key: 'failureCount',
    width: 110,
    render: row =>
      h(
        NTag,
        { type: row.failureCount >= 10 ? 'error' : row.failureCount >= 3 ? 'warning' : 'default', round: true },
        { default: () => String(row.failureCount) }
      )
  },
  {
    title: '最近失败时间',
    key: 'lastFailureTime',
    width: 180,
    render: row => formatDateTime(row.lastFailureTime)
  },
  {
    title: '最近调度结果',
    key: 'lastTriggerCode',
    width: 130,
    render: row => renderTriggerStatus(row.lastTriggerCode)
  },
  {
    title: '最近执行结果',
    key: 'lastHandleCode',
    width: 130,
    render: row => renderHandleStatus(row.lastHandleCode)
  },
  {
    title: '最近失败详情',
    key: 'messages',
    minWidth: 180,
    render: row =>
      h('div', { class: 'table-actions' }, [
        renderMessageButton('调度消息', row.lastTriggerMsg || ''),
        renderMessageButton('执行消息', row.lastHandleMsg || '')
      ])
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => openLogs(row)
          },
          { default: () => '日志列表' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'warning',
            ghost: true,
            onClick: () => openLogDetail(row)
          },
          { default: () => '最近失败' }
        )
      ])
  }
];

function rowKey(row: JobFailureAggregate) {
  return `${row.jobGroup}-${row.jobId}`;
}

function createDefaultRange(): [number, number] {
  const end = new Date();
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000);
  return [start.getTime(), end.getTime()];
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

function buildFilterTime() {
  if (!filters.timeRange?.length) {
    return '';
  }
  return `${formatDateTime(filters.timeRange[0])} - ${formatDateTime(filters.timeRange[1])}`;
}

function renderTriggerStatus(code: number) {
  if (code === 200) {
    return h(NTag, { type: 'success', round: true }, { default: () => 'SUCCESS' });
  }
  if (code > 0) {
    return h(NTag, { type: 'error', round: true }, { default: () => 'FAIL' });
  }
  return '';
}

function renderHandleStatus(code: number) {
  if (code === 200) {
    return h(NTag, { type: 'success', round: true }, { default: () => 'SUCCESS' });
  }
  if (code === 502) {
    return h(NTag, { type: 'warning', round: true }, { default: () => 'TIMEOUT' });
  }
  if (code > 0) {
    return h(NTag, { type: 'error', round: true }, { default: () => 'FAIL' });
  }
  return h(NTag, { type: 'info', round: true }, { default: () => 'RUNNING' });
}

function renderMessageButton(title: string, content: string) {
  if (!content) {
    return h('span', '空');
  }
  return h(
    NButton,
    {
      size: 'small',
      quaternary: true,
      onClick: () => showMessage(title, content)
    },
    { default: () => '查看' }
  );
}

function showMessage(title: string, content: string) {
  messageModalTitle.value = title;
  messageModalContent.value = content;
  messageModalVisible.value = true;
}

async function loadJobGroups() {
  const response = await fetchJobGroups();
  if (response.code !== 200) {
    throw new Error(response.msg || '执行器数据加载失败');
  }
  jobGroups.value = response.data;
}

async function loadJobsByGroup() {
  if (filters.jobGroup < 1) {
    jobs.value = [];
    filters.jobId = 0;
    return;
  }
  const response = await fetchJobsByGroup(filters.jobGroup);
  if (response.code !== 200) {
    throw new Error(response.msg || '任务列表加载失败');
  }
  jobs.value = response.data;
  if (!jobs.value.some(item => item.id === filters.jobId)) {
    filters.jobId = 0;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const response = await fetchFailureAggregates({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      jobGroup: filters.jobGroup,
      jobId: filters.jobId,
      author: filters.author,
      jobTag: filters.jobTag,
      filterTime: buildFilterTime()
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '失败聚合加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '失败聚合加载失败');
  } finally {
    loading.value = false;
  }
}

async function search() {
  pagination.page = 1;
  await loadData();
}

async function reset() {
  filters.jobGroup = -1;
  filters.jobId = 0;
  filters.author = '';
  filters.jobTag = '';
  filters.timeRange = createDefaultRange();
  pagination.page = 1;
  await loadJobsByGroup();
  await loadData();
}

function openLogs(row: JobFailureAggregate) {
  router.push({
    name: 'logs',
    query: {
      jobGroup: String(row.jobGroup),
      jobId: String(row.jobId)
    }
  });
}

function openLogDetail(row: JobFailureAggregate) {
  router.push({
    name: 'log-detail',
    query: {
      logId: String(row.lastLogId)
    }
  });
}

watch(
  () => filters.jobGroup,
  async () => {
    await loadJobsByGroup();
  }
);

onMounted(async () => {
  try {
    await loadJobGroups();
    await loadJobsByGroup();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '失败聚合页初始化失败');
  }
});
</script>
