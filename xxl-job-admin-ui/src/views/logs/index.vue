<template>
  <div class="page-stack">
    <div class="page-split">
      <n-card :bordered="false" class="tree-panel-card">
        <template #header>
          <div class="table-header">
            <div class="table-title">日志树</div>
            <div class="table-subtitle">
              <span>按执行器展开任务。</span>
              <span>点任务后右侧直接切到对应日志。</span>
            </div>
          </div>
        </template>
        <template #header-extra>
          <div class="table-actions">
            <n-button text @click="refreshTree">刷新</n-button>
            <n-button text @click="resetTreeSelection">重置</n-button>
          </div>
        </template>

        <n-empty v-if="!jobGroups.length" description="暂无执行器" />
        <n-tree
          v-else
          block-line
          expand-on-click
          :data="jobTreeData"
          :expanded-keys="expandedTreeKeys"
          :selected-keys="selectedTreeKeys"
          @update:expanded-keys="handleTreeExpanded"
          @update:selected-keys="handleTreeSelected"
        />
      </n-card>

      <div class="page-split-main">
        <n-card :bordered="false" class="filter-card">
          <div class="filter-grid logs-tree-filter-grid">
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
          <div class="split-selection-bar">
            <span class="split-selection-label">当前范围</span>
            <span class="split-selection-value">{{ treeSelectionLabel }}</span>
          </div>
        </n-card>

        <n-card :bordered="false">
          <template #header>
            <div class="table-header">
              <div class="table-title">执行日志</div>
              <div class="table-subtitle">
                <span>左侧负责任务定位，右侧保留时间范围和状态筛选。</span>
                <span>滚动日志保持原入口，终止操作仅对运行中日志可用。</span>
              </div>
            </div>
          </template>
          <template #header-extra>
            <div class="table-actions">
              <n-button :disabled="!selectedRow" @click="() => void openDetail()">滚动日志</n-button>
              <n-button :disabled="!selectedRowRunning" type="warning" @click="() => void killSelected()">终止运行</n-button>
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
      </div>
    </div>

    <n-modal v-model:show="messageModalVisible" preset="card" :title="messageModalTitle" style="width: 720px;">
      <pre class="message-preview">{{ messageModalContent || '空' }}</pre>
    </n-modal>

    <n-drawer v-model:show="logDrawerVisible" :width="920" placement="right">
      <n-drawer-content :title="logDrawerTitle" closable>
        <template #header>
          <div class="log-drawer-header">
            <div class="table-header">
              <div class="table-title">{{ logDrawerTitle }}</div>
              <div class="table-subtitle">
                <span>日志 ID：{{ activeLogId || '-' }}</span>
                <span>任务 ID：{{ detailMeta?.jobId ?? '-' }}</span>
                <span>触发时间：{{ formatDateTime(detailMeta?.triggerTime) || '-' }}</span>
              </div>
            </div>
            <div class="table-actions">
              <n-button size="small" @click="() => void reloadDetail()">刷新</n-button>
            </div>
          </div>
        </template>

        <div class="table-header mb-12px">
          <div class="table-subtitle">
            <span>执行地址：{{ detailMeta?.executorAddress || '未知' }}</span>
            <span>状态：{{ detailStatusText }}</span>
          </div>
        </div>
        <pre ref="logStreamRef" class="log-stream log-drawer-stream"><code v-html="detailLogHtml"></code></pre>
        <div v-if="detailRunning" class="log-stream-running">正在持续拉取日志...</div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NModal,
  NSelect,
  NTag,
  NTree,
  useDialog,
  useMessage,
  type DataTableColumns,
  type PaginationProps,
  type SelectOption,
  type TreeOption
} from 'naive-ui';
import {
  fetchJobGroups,
  fetchJobsByGroup,
  fetchLogDetailMeta,
  type JobGroupOption,
  type JobOption,
  type LogDetailMeta
} from '@/api/admin-next';
import { fetchLogChunk, fetchLogs, killLog, type JobLog } from '@/api/logs';

defineOptions({
  name: 'logs'
});

const route = useRoute();
const dialog = useDialog();
const message = useMessage();
const loading = ref(false);
const rows = ref<JobLog[]>([]);
const checkedRowKeys = ref<number[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const jobsByGroup = ref<Record<number, JobOption[]>>({});
const messageModalVisible = ref(false);
const messageModalTitle = ref('');
const messageModalContent = ref('');
const expandedTreeKeys = ref<Array<string | number>>([]);
const selectedTreeKeys = ref<Array<string | number>>([]);
const logDrawerVisible = ref(false);
const activeLogId = ref(0);
const detailMeta = ref<LogDetailMeta | null>(null);
const detailLogHtml = ref('');
const detailRunning = ref(false);
const detailFromLineNum = ref(1);
const detailPullFailCount = ref(0);
const logStreamRef = ref<HTMLPreElement | null>(null);
let detailTimer: ReturnType<typeof setInterval> | null = null;

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

const jobTreeData = computed<TreeOption[]>(() =>
  jobGroups.value.map((group) => {
    const children = (jobsByGroup.value[group.id] || []).map((job) => ({
      key: `job-${job.id}`,
      label: `${job.jobDesc || `任务 #${job.id}`} · ${job.executorHandler || '未配置 Handler'}`,
      isLeaf: true
    }));
    return {
      key: `group-${group.id}`,
      label: `${group.title} (${children.length})`,
      children
    };
  })
);

const jobTitleMap = computed<Record<number, string>>(() =>
  Object.values(jobsByGroup.value)
    .flat()
    .reduce<Record<number, string>>((acc, item) => {
      acc[item.id] = item.jobDesc || `任务 #${item.id}`;
      return acc;
    }, {})
);

const treeSelectionLabel = computed(() => {
  if (filters.jobId > 0) {
    const job = Object.values(jobsByGroup.value)
      .flat()
      .find((item) => item.id === filters.jobId);
    if (job) {
      const group = jobGroups.value.find((item) => item.id === job.jobGroup);
      return `${group?.title || '执行器'} / ${job.jobDesc || `任务 #${job.id}`}`;
    }
  }
  if (filters.jobGroup > 0) {
    return jobGroups.value.find((item) => item.id === filters.jobGroup)?.title || '当前执行器';
  }
  return '未选择';
});

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
const selectedRowRunning = computed(() => isLogRunning(selectedRow.value));

const logDrawerTitle = computed(() => detailMeta.value?.jobDesc || `日志 #${activeLogId.value || '-'}`);

const detailStatusText = computed(() => {
  if (!detailMeta.value) {
    return '初始化中';
  }
  if (detailMeta.value.handleCode === 200) {
    return '执行成功';
  }
  if (detailMeta.value.handleCode === 502) {
    return '执行超时';
  }
  if (detailMeta.value.handleCode > 0) {
    return '执行失败';
  }
  if (detailMeta.value.triggerCode !== 200) {
    return '触发失败';
  }
  return '执行中';
});

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
        isLogRunning(row) ? h(
          NButton,
          {
            size: 'small',
            type: 'warning',
            ghost: true,
            onClick: () => killSelected(row)
          },
          { default: () => '终止' }
        ) : null
      ])
  }
];

function isLogRunning(row: JobLog | null | undefined) {
  return Boolean(row && row.triggerCode === 200 && row.handleCode === 0);
}

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

function stopDetailPolling() {
  detailRunning.value = false;
  if (detailTimer) {
    clearInterval(detailTimer);
    detailTimer = null;
  }
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

async function scrollDetailToBottom() {
  await nextTick();
  const el = logStreamRef.value;
  if (el) {
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  }
}

async function pullDetailLog() {
  if (!activeLogId.value) {
    return;
  }
  if (detailPullFailCount.value > 20) {
    stopDetailPolling();
    detailLogHtml.value += '<br><span style="color:#d03050;">[拉取失败次数过多，已停止]</span>';
    await scrollDetailToBottom();
    return;
  }
  try {
    const response = await fetchLogChunk(activeLogId.value, detailFromLineNum.value);
    if (response.code !== 200) {
      detailPullFailCount.value += 1;
      detailLogHtml.value += `<br><span style="color:#d03050;">[Rolling Log Error] ${escapeHtml(response.msg || '日志拉取失败')}</span>`;
      await scrollDetailToBottom();
      return;
    }
    const chunk = response.data;
    if (!chunk) {
      detailPullFailCount.value += 1;
      return;
    }
    if (detailFromLineNum.value !== chunk.fromLineNum) {
      detailPullFailCount.value += 1;
      return;
    }
    if (detailFromLineNum.value <= chunk.toLineNum) {
      detailFromLineNum.value = chunk.toLineNum + 1;
      detailLogHtml.value += chunk.logContent || '';
      detailPullFailCount.value = 0;
      await scrollDetailToBottom();
    }
    if (chunk.end) {
      stopDetailPolling();
      detailLogHtml.value += '<br><span style="color:#2f9e44;">[Rolling Log End]</span>';
      await scrollDetailToBottom();
    }
  } catch (error) {
    detailPullFailCount.value += 1;
    const err = error as Error;
    detailLogHtml.value += `<br><span style="color:#d03050;">[Rolling Log Error] ${escapeHtml(err.message || '日志拉取失败')}</span>`;
    await scrollDetailToBottom();
  }
}

function startDetailPolling() {
  stopDetailPolling();
  detailRunning.value = true;
  detailTimer = setInterval(() => {
    void pullDetailLog();
  }, 3000);
}

async function bootstrapDetail(logId: number) {
  stopDetailPolling();
  activeLogId.value = logId;
  detailMeta.value = null;
  detailLogHtml.value = '';
  detailFromLineNum.value = 1;
  detailPullFailCount.value = 0;

  const metaResponse = await fetchLogDetailMeta(logId);
  if (metaResponse.code !== 200) {
    throw new Error(metaResponse.msg || '日志详情加载失败');
  }
  detailMeta.value = metaResponse.data;

  if (detailMeta.value.triggerCode !== 200 && detailMeta.value.handleCode === 0) {
    detailLogHtml.value = '<span style="color:#d03050;">[调度失败，未进入执行阶段]</span>';
    detailRunning.value = false;
    await scrollDetailToBottom();
    return;
  }

  await pullDetailLog();

  if (detailMeta.value.handleCode > 0) {
    detailRunning.value = false;
    return;
  }

  startDetailPolling();
}

async function reloadDetail() {
  if (!activeLogId.value) {
    return;
  }
  try {
    await bootstrapDetail(activeLogId.value);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志详情刷新失败');
  }
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

async function loadTreeJobs(force = false) {
  if (!jobGroups.value.length) {
    jobsByGroup.value = {};
    return;
  }

  const groups = force ? jobGroups.value : jobGroups.value.filter((item) => !jobsByGroup.value[item.id]);
  if (!groups.length) {
    return;
  }

  const entries = await Promise.all(
    groups.map(async (group) => {
      const response = await fetchJobsByGroup(group.id);
      if (response.code !== 200) {
        throw new Error(response.msg || `${group.title} 任务树加载失败`);
      }
      return [group.id, response.data] as const;
    })
  );

  jobsByGroup.value = {
    ...(force ? {} : jobsByGroup.value),
    ...Object.fromEntries(entries)
  };
}

function syncTreeSelection() {
  if (filters.jobId > 0) {
    selectedTreeKeys.value = [`job-${filters.jobId}`];
    const job = Object.values(jobsByGroup.value)
      .flat()
      .find((item) => item.id === filters.jobId);
    if (job) {
      expandedTreeKeys.value = Array.from(new Set([...expandedTreeKeys.value, `group-${job.jobGroup}`]));
    }
    return;
  }
  if (filters.jobGroup > 0) {
    selectedTreeKeys.value = [`group-${filters.jobGroup}`];
    expandedTreeKeys.value = Array.from(new Set([...expandedTreeKeys.value, `group-${filters.jobGroup}`]));
    return;
  }
  selectedTreeKeys.value = [];
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
  filters.logStatus = -1;
  filters.timeRange = createDefaultRange();
  pagination.page = 1;
  await loadData();
}

function handleTreeExpanded(keys: Array<string | number>) {
  expandedTreeKeys.value = keys;
}

function handleTreeSelected(keys: Array<string | number>) {
  selectedTreeKeys.value = keys;
  const key = String(keys[0] || '');
  if (!key) {
    return;
  }

  if (key.startsWith('group-')) {
    filters.jobGroup = Number(key.slice(6));
    filters.jobId = 0;
  } else if (key.startsWith('job-')) {
    const jobId = Number(key.slice(4));
    const job = Object.values(jobsByGroup.value)
      .flat()
      .find((item) => item.id === jobId);
    if (!job) {
      return;
    }
    filters.jobGroup = job.jobGroup;
    filters.jobId = job.id;
  }

  pagination.page = 1;
  checkedRowKeys.value = [];
  void loadData();
}

async function refreshTree() {
  try {
    await loadJobGroups();
    await loadTreeJobs(true);
    syncTreeSelection();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志树刷新失败');
  }
}

function resetTreeSelection() {
  if (!jobGroups.value.length) {
    return;
  }
  filters.jobGroup = jobGroups.value[0].id;
  filters.jobId = 0;
  syncTreeSelection();
  pagination.page = 1;
  void loadData();
}

async function openDetail(row?: JobLog | null) {
  const target = row || selectedRow.value;
  if (!target) {
    return;
  }
  logDrawerVisible.value = true;
  try {
    await bootstrapDetail(target.id);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志详情加载失败');
  }
}

async function killSelected(row?: JobLog | null) {
  const target = row || selectedRow.value;
  if (!target) {
    return;
  }
  if (!isLogRunning(target)) {
    message.warning('该日志已结束，不能终止');
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
  () => [route.query.jobGroup, route.query.jobId],
  async () => {
    applyRouteQuery();
    try {
      syncTreeSelection();
      pagination.page = 1;
      await loadData();
    } catch (error) {
      const err = error as Error;
      message.error(err.message || '日志页跳转失败');
    }
  }
);

watch(logDrawerVisible, (visible) => {
  if (!visible) {
    stopDetailPolling();
  }
});

onMounted(async () => {
  try {
    applyRouteQuery();
    await loadJobGroups();
    await loadTreeJobs(true);
    syncTreeSelection();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志页初始化失败');
  }
});

onBeforeUnmount(() => {
  stopDetailPolling();
});
</script>
