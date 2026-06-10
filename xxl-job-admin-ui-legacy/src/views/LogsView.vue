<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div>
        <div class="section-kicker">Logs</div>
        <h2>先把执行日志查询和滚动详情迁过来，优先覆盖排障主流程。</h2>
        <p>
          当前版本覆盖筛选、列表、终止运行和滚动日志详情。日志清理等低频操作后续单独补。
        </p>
      </div>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid logs-filter-grid">
        <n-select v-model:value="filters.jobGroup" :options="jobGroupOptions" placeholder="选择执行器" />
        <n-select v-model:value="filters.jobId" :options="jobOptions" placeholder="选择任务" />
        <n-select v-model:value="filters.logStatus" :options="statusOptions" placeholder="执行状态" />
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
          <div class="table-title">执行日志</div>
          <div class="table-subtitle">先保证查日志和看滚动输出顺畅，再逐步回补低频管理动作。</div>
        </div>
      </template>
      <template #header-extra>
        <div class="table-actions">
          <n-button :disabled="!selectedRow" @click="() => openDetail()">滚动日志</n-button>
          <n-button :disabled="!selectedRow" type="warning" @click="() => void killSelected()">终止运行</n-button>
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
        @update:checked-row-keys="handleCheckedRowKeys"
      />
    </n-card>

    <n-modal v-model:show="messageModalVisible" preset="card" :title="messageModalTitle" style="width: 720px;">
      <pre class="message-preview">{{ messageModalContent || '空' }}</pre>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NModal,
  NSelect,
  NTag,
  useDialog,
  useMessage,
  type DataTableColumns,
  type PaginationProps,
  type SelectOption
} from 'naive-ui';
import { fetchJobGroups, fetchJobsByGroup, type JobGroupOption, type JobOption } from '@/api/admin-next';
import { fetchLogs, killLog, type JobLog } from '@/api/logs';

const route = useRoute();
const router = useRouter();
const dialog = useDialog();
const message = useMessage();
const loading = ref(false);
const rows = ref<JobLog[]>([]);
const checkedRowKeys = ref<number[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const jobs = ref<JobOption[]>([]);
const messageModalVisible = ref(false);
const messageModalTitle = ref('');
const messageModalContent = ref('');

const statusOptions: SelectOption[] = [
  { label: '全部', value: -1 },
  { label: '成功', value: 1 },
  { label: '失败', value: 2 },
  { label: '进行中', value: 3 }
];

const filters = reactive<{
  jobGroup: number;
  jobId: number;
  logStatus: number;
  timeRange: [number, number] | null;
}>({
  jobGroup: -1,
  jobId: 0,
  logStatus: -1,
  timeRange: createDefaultRange()
});

const jobGroupOptions = computed<SelectOption[]>(() =>
  jobGroups.value.map((item) => ({
    label: item.title,
    value: item.id
  }))
);

const jobOptions = computed<SelectOption[]>(() => [
  { label: '全部任务', value: 0 },
  ...jobs.value.map((item) => ({
    label: item.jobDesc || `任务 #${item.id}`,
    value: item.id
  }))
]);

const jobTitleMap = computed<Record<number, string>>(() =>
  jobs.value.reduce<Record<number, string>>((acc, item) => {
    acc[item.id] = item.jobDesc || `任务 #${item.id}`;
    return acc;
  }, {})
);

const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page;
    void loadData();
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    void loadData();
  }
});

const selectedRow = computed(() =>
  rows.value.find((row) => row.id === checkedRowKeys.value[0]) || null
);

const columns: DataTableColumns<JobLog> = [
  { type: 'selection', multiple: false },
  { title: 'ID', key: 'id', width: 90 },
  {
    title: '任务',
    key: 'jobId',
    minWidth: 220,
    render: (row) => `#${row.jobId} ${jobTitleMap.value[row.jobId] || ''}`.trim()
  },
  {
    title: '触发时间',
    key: 'triggerTime',
    width: 180,
    render: (row) => formatDateTime(row.triggerTime)
  },
  {
    title: '调度结果',
    key: 'triggerCode',
    width: 120,
    render: (row) => renderTriggerStatus(row.triggerCode)
  },
  {
    title: '调度消息',
    key: 'triggerMsg',
    minWidth: 140,
    render: (row) => renderMessageButton('调度消息', row.triggerMsg)
  },
  {
    title: '完成时间',
    key: 'handleTime',
    width: 180,
    render: (row) => formatDateTime(row.handleTime)
  },
  {
    title: '执行结果',
    key: 'handleCode',
    width: 120,
    render: (row) => renderHandleStatus(row.handleCode)
  },
  {
    title: '执行消息',
    key: 'handleMsg',
    minWidth: 140,
    render: (row) => renderMessageButton('执行消息', row.handleMsg)
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render: (row) =>
      h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => openDetail(row)
          },
          { default: () => '详情' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'warning',
            ghost: true,
            onClick: () => killSelected(row)
          },
          { default: () => '终止' }
        )
      ])
  }
];

function rowKey(row: JobLog) {
  return row.id;
}

function handleCheckedRowKeys(keys: Array<string | number>) {
  checkedRowKeys.value = keys.map((item) => Number(item));
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
    return '空';
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

function applyRouteQuery() {
  const routeJobGroup = Number(route.query.jobGroup || -1);
  const routeJobId = Number(route.query.jobId || 0);
  if (routeJobGroup > 0) {
    filters.jobGroup = routeJobGroup;
  }
  if (routeJobId >= 0) {
    filters.jobId = routeJobId;
  }
}

async function loadJobGroups() {
  const response = await fetchJobGroups();
  if (response.code !== 200) {
    throw new Error(response.msg || '执行器数据加载失败');
  }
  jobGroups.value = response.data;
  if (!jobGroups.value.length) {
    return;
  }
  if (!jobGroups.value.some((item) => item.id === filters.jobGroup)) {
    filters.jobGroup = jobGroups.value[0].id;
  }
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
  if (!jobs.value.some((item) => item.id === filters.jobId)) {
    filters.jobId = 0;
  }
}

async function loadData() {
  if (filters.jobGroup < 1) {
    return;
  }
  loading.value = true;
  try {
    const response = await fetchLogs({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      jobGroup: filters.jobGroup,
      jobId: filters.jobId,
      logStatus: filters.logStatus,
      filterTime: buildFilterTime()
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '日志列表加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
    checkedRowKeys.value = [];
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function search() {
  pagination.page = 1;
  await loadData();
}

async function reset() {
  filters.jobId = 0;
  filters.logStatus = -1;
  filters.timeRange = createDefaultRange();
  pagination.page = 1;
  await loadJobsByGroup();
  await loadData();
}

function openDetail(row?: JobLog | null) {
  const target = row || selectedRow.value;
  if (!target) {
    return;
  }
  router.push({
    name: 'log-detail',
    params: { logId: String(target.id) }
  });
}

async function killSelected(row?: JobLog | null) {
  const target = row || selectedRow.value;
  if (!target) {
    return;
  }
  dialog.warning({
    title: '终止运行',
    content: `确认终止日志 #${target.id} 对应的执行实例吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await killLog(target.id);
      if (response.code !== 200) {
        message.error(response.msg || '终止失败');
        return;
      }
      message.success('终止成功');
      await loadData();
    }
  });
}

watch(
  () => filters.jobGroup,
  async (value, oldValue) => {
    if (value === oldValue) {
      return;
    }
    try {
      await loadJobsByGroup();
      pagination.page = 1;
      await loadData();
    } catch (error) {
      const err = error as Error;
      message.error(err.message || '日志页刷新失败');
    }
  }
);

watch(
  () => [route.query.jobGroup, route.query.jobId],
  async () => {
    applyRouteQuery();
    try {
      await loadJobsByGroup();
      pagination.page = 1;
      await loadData();
    } catch (error) {
      const err = error as Error;
      message.error(err.message || '日志页跳转失败');
    }
  }
);

onMounted(async () => {
  try {
    applyRouteQuery();
    await loadJobGroups();
    await loadJobsByGroup();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志页初始化失败');
  }
});
</script>
