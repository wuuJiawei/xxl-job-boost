<template>
  <div class="page-stack">


    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid logs-filter-grid">
        <n-select v-model:value="filters.jobGroup" :options="jobGroupOptions" placeholder="选择执行器" />
        <n-select v-model:value="filters.jobId" :options="jobOptions" placeholder="选择任务" />
        <n-input-number v-model:value="filters.minDurationSeconds" :min="1" :max="3600" placeholder="慢任务阈值(秒)" />
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

      </template>

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :scroll-x="1500"
      />
    </n-card>
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
  NInputNumber,
  NSelect,
  NTag,
  useMessage,
  type DataTableColumns,
  type PaginationProps,
  type SelectOption
} from 'naive-ui';
import { $t } from '@/locales';
import { fetchSlowTasks, type JobSlowAggregate } from '@/api/slow-tasks';
import { fetchJobGroups, fetchJobsByGroup, type JobGroupOption, type JobOption } from '@/api/admin-next';

defineOptions({
  name: 'slow-tasks'
});

const router = useRouter();
const message = useMessage();
const loading = ref(false);
const rows = ref<JobSlowAggregate[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const jobs = ref<JobOption[]>([]);

const filters = reactive<{
  jobGroup: number;
  jobId: number;
  minDurationSeconds: number;
  author: string;
  jobTag: string;
  timeRange: [number, number] | null;
}>({
  jobGroup: -1,
  jobId: 0,
  minDurationSeconds: 30,
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

const columns: DataTableColumns<JobSlowAggregate> = [
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
    title: '慢执行次数',
    key: 'slowCount',
    width: 120,
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
    width: 130,
    render: row => formatDuration(row.avgDurationMs)
  },
  {
    title: '峰值耗时',
    key: 'maxDurationMs',
    width: 130,
    render: row => formatDuration(row.maxDurationMs)
  },
  {
    title: '最近一次慢执行',
    key: 'lastSlowTime',
    width: 180,
    render: row => formatDateTime(row.lastSlowTime)
  },
  {
    title: '最近耗时',
    key: 'lastDurationMs',
    width: 130,
    render: row => formatDuration(row.lastDurationMs)
  },
  {
    title: '最近执行结果',
    key: 'lastHandleCode',
    width: 130,
    render: row => renderHandleStatus(row.lastHandleCode)
  },
  {
    title: '操作',
    key: 'actions',
    fixed: 'right',
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
          { default: () => '最近一次' }
        )
      ])
  }
];

function rowKey(row: JobSlowAggregate) {
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

function formatDuration(durationMs: number) {
  if (!durationMs) {
    return '0 ms';
  }
  if (durationMs < 1000) {
    return `${durationMs} ms`;
  }
  return `${(durationMs / 1000).toFixed(durationMs >= 10_000 ? 0 : 1)} s`;
}

function buildFilterTime() {
  if (!filters.timeRange?.length) {
    return '';
  }
  return `${formatDateTime(filters.timeRange[0])} - ${formatDateTime(filters.timeRange[1])}`;
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
    const response = await fetchSlowTasks({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      jobGroup: filters.jobGroup,
      jobId: filters.jobId,
      minDurationSeconds: filters.minDurationSeconds,
      author: filters.author,
      jobTag: filters.jobTag,
      filterTime: buildFilterTime()
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '慢任务分析加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '慢任务分析加载失败');
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
  filters.minDurationSeconds = 30;
  filters.author = '';
  filters.jobTag = '';
  filters.timeRange = createDefaultRange();
  pagination.page = 1;
  await loadJobsByGroup();
  await loadData();
}

function openLogs(row: JobSlowAggregate) {
  router.push({
    name: 'logs',
    query: {
      jobGroup: String(row.jobGroup),
      jobId: String(row.jobId)
    }
  });
}

function openLogDetail(row: JobSlowAggregate) {
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
    message.error(err.message || '慢任务分析页初始化失败');
  }
});
</script>
