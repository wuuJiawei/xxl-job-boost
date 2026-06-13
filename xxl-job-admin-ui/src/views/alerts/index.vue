<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="section-kicker">Alerts</div>
      <h2>告警渠道、规则与投递记录</h2>
      <p>第二阶段先把失败告警通道化，再补一层轻量规则绑定，支持按执行器、任务和事件类型配置独立告警出口。</p>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid alerts-filter-grid">
        <n-input v-model:value="channelFilters.name" placeholder="按渠道名称查询" clearable />
        <n-select v-model:value="channelFilters.type" :options="channelTypeOptions" placeholder="渠道类型" clearable />
        <n-select v-model:value="channelFilters.enabled" :options="enabledOptions" placeholder="启用状态" />
        <div class="filter-actions">
          <n-button type="primary" @click="searchChannels">查询</n-button>
          <n-button @click="resetChannels">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">告警渠道</div>
          <div class="table-subtitle">支持 Email、通用 Webhook、飞书、企业微信、钉钉。</div>
        </div>
      </template>
      <template #header-extra>
        <div class="table-actions">
          <n-button v-if="authStore.userInfo.isAdmin" type="primary" @click="openChannelCreate">新增渠道</n-button>
          <n-button v-if="authStore.userInfo.isAdmin" :disabled="!selectedChannel" @click="() => void openChannelEdit()">编辑</n-button>
          <n-button
            v-if="authStore.userInfo.isAdmin"
            :disabled="!selectedChannel"
            type="error"
            ghost
            @click="() => void removeChannel()"
          >
            删除
          </n-button>
        </div>
      </template>

      <n-data-table
        remote
        :columns="channelColumns"
        :data="channelRows"
        :loading="channelLoading"
        :pagination="channelPagination"
        :row-key="channelRowKey"
        :single-line="false"
        @update:checked-row-keys="handleChannelChecked"
      />
    </n-card>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid alerts-rule-filter-grid">
        <n-select v-model:value="ruleFilters.jobGroup" :options="jobGroupOptions" placeholder="执行器" />
        <n-select v-model:value="ruleFilters.jobId" :options="ruleJobOptions" placeholder="任务" />
        <n-select v-model:value="ruleFilters.alarmEvent" :options="alarmEventOptions" placeholder="告警事件" clearable />
        <n-select v-model:value="ruleFilters.enabled" :options="enabledOptions" placeholder="启用状态" />
        <div class="filter-actions">
          <n-button type="primary" @click="searchRules">查询</n-button>
          <n-button @click="resetRules">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">告警规则</div>
          <div class="table-subtitle">按执行器、任务和事件类型绑定渠道。任务为空时表示整个执行器统一生效。</div>
        </div>
      </template>
      <template #header-extra>
        <div class="table-actions">
          <n-button v-if="authStore.userInfo.isAdmin" type="primary" @click="openRuleCreate">新增规则</n-button>
          <n-button v-if="authStore.userInfo.isAdmin" :disabled="!selectedRule" @click="() => void openRuleEdit()">编辑</n-button>
          <n-button
            v-if="authStore.userInfo.isAdmin"
            :disabled="!selectedRule"
            type="error"
            ghost
            @click="() => void removeRule()"
          >
            删除
          </n-button>
        </div>
      </template>

      <n-data-table
        remote
        :columns="ruleColumns"
        :data="ruleRows"
        :loading="ruleLoading"
        :pagination="rulePagination"
        :row-key="ruleRowKey"
        :single-line="false"
        @update:checked-row-keys="handleRuleChecked"
      />
    </n-card>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid alerts-record-filter-grid">
        <n-select v-model:value="recordFilters.jobGroup" :options="jobGroupOptions" placeholder="执行器" />
        <n-select v-model:value="recordFilters.channelType" :options="recordChannelTypeOptions" placeholder="渠道类型" clearable />
        <n-select v-model:value="recordFilters.sendStatus" :options="sendStatusOptions" placeholder="投递状态" />
        <div class="filter-actions">
          <n-button type="primary" @click="searchRecords">查询</n-button>
          <n-button @click="resetRecords">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">告警记录</div>
          <div class="table-subtitle">记录失败告警的投递状态、目标、响应码和错误信息。</div>
        </div>
      </template>
      <n-data-table
        remote
        :columns="recordColumns"
        :data="recordRows"
        :loading="recordLoading"
        :pagination="recordPagination"
        :row-key="recordRowKey"
      />
    </n-card>

    <n-modal
      v-model:show="channelFormVisible"
      preset="card"
      :title="channelFormMode === 'create' ? '新增告警渠道' : '编辑告警渠道'"
      style="width: 760px;"
    >
      <n-form ref="channelFormRef" :model="channelFormValue" :rules="channelRules" label-placement="left" label-width="120">
        <n-form-item path="name" label="渠道名称">
          <n-input v-model:value="channelFormValue.name" placeholder="请输入渠道名称" />
        </n-form-item>
        <n-form-item path="type" label="渠道类型">
          <n-select
            v-model:value="channelFormValue.type"
            :options="channelTypeOptions.filter(item => item.value !== '')"
            placeholder="请选择渠道类型"
          />
        </n-form-item>
        <n-form-item path="enabled" label="启用状态">
          <n-select v-model:value="channelFormValue.enabled" :options="enabledOptions.filter(item => item.value !== -1)" />
        </n-form-item>
        <n-form-item v-if="channelFormValue.type !== 'EMAIL'" path="endpoint" label="Webhook 地址">
          <n-input v-model:value="channelFormValue.endpoint" placeholder="请输入 Webhook 地址" />
        </n-form-item>
        <n-form-item path="recipients" :label="channelFormValue.type === 'EMAIL' ? '接收邮箱' : '接收人标识'">
          <n-input v-model:value="channelFormValue.recipients" placeholder="多个值用逗号分隔，可选" />
        </n-form-item>
        <n-form-item path="headersJson" label="请求头 JSON">
          <n-input
            v-model:value="channelFormValue.headersJson"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder='例如 {"X-Token":"abc"}，可选'
          />
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <n-input
            v-model:value="channelFormValue.remark"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder="渠道说明，可选"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="channelFormVisible = false">取消</n-button>
          <n-button type="primary" :loading="channelSubmitting" @click="submitChannelForm">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="ruleFormVisible"
      preset="card"
      :title="ruleFormMode === 'create' ? '新增告警规则' : '编辑告警规则'"
      style="width: 760px;"
    >
      <n-form ref="ruleFormRef" :model="ruleFormValue" :rules="ruleRules" label-placement="left" label-width="120">
        <n-form-item path="name" label="规则名称">
          <n-input v-model:value="ruleFormValue.name" placeholder="请输入规则名称" />
        </n-form-item>
        <n-form-item path="jobGroup" label="执行器">
          <n-select v-model:value="ruleFormValue.jobGroup" :options="jobGroupOptions.filter(item => item.value !== -1)" placeholder="请选择执行器" />
        </n-form-item>
        <n-form-item path="jobId" label="任务">
          <n-select v-model:value="ruleFormValue.jobId" :options="ruleFormJobOptions" placeholder="留空表示执行器级规则" />
        </n-form-item>
        <n-form-item path="alarmEvent" label="告警事件">
          <n-select v-model:value="ruleFormValue.alarmEvent" :options="alarmEventOptions.filter(item => item.value !== '')" placeholder="请选择告警事件" />
        </n-form-item>
        <n-form-item path="channelIds" label="绑定渠道">
          <n-select v-model:value="ruleFormValue.channelIds" multiple :options="enabledChannelOptions" placeholder="请选择一个或多个渠道" />
        </n-form-item>
        <n-form-item path="enabled" label="启用状态">
          <n-select v-model:value="ruleFormValue.enabled" :options="enabledOptions.filter(item => item.value !== -1)" />
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <n-input
            v-model:value="ruleFormValue.remark"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder="规则说明，可选"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="ruleFormVisible = false">取消</n-button>
          <n-button type="primary" :loading="ruleSubmitting" @click="submitRuleForm">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NSelect,
  NTag,
  useDialog,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type PaginationProps,
  type SelectOption
} from 'naive-ui';
import { useAuthStore } from '@/store/modules/auth';
import {
  createAlarmChannel,
  createAlarmRule,
  deleteAlarmChannel,
  deleteAlarmRule,
  fetchAlarmChannels,
  fetchAlarmRules,
  type AlarmChannel,
  type AlarmRecord,
  type AlarmRule,
  updateAlarmChannel,
  updateAlarmRule
} from '@/api/alerts';
import {
  fetchAlarmChannelDetail,
  fetchAlarmRecords,
  fetchAlarmRuleDetail,
  fetchJobGroups,
  fetchJobsByGroup,
  fetchJobMetadata,
  type JobGroupOption,
  type JobMetadata,
  type JobOption
} from '@/api/admin-next';

const authStore = useAuthStore();
const dialog = useDialog();
const message = useMessage();

const metadata = ref<JobMetadata | null>(null);
const jobGroups = ref<JobGroupOption[]>([]);
const jobsByGroup = ref<Record<number, JobOption[]>>({});

const channelFormRef = ref<FormInst | null>(null);
const ruleFormRef = ref<FormInst | null>(null);

const channelFormVisible = ref(false);
const channelFormMode = ref<'create' | 'edit'>('create');
const channelSubmitting = ref(false);

const ruleFormVisible = ref(false);
const ruleFormMode = ref<'create' | 'edit'>('create');
const ruleSubmitting = ref(false);

const channelLoading = ref(false);
const channelRows = ref<AlarmChannel[]>([]);
const checkedChannelKeys = ref<number[]>([]);

const ruleLoading = ref(false);
const ruleRows = ref<AlarmRule[]>([]);
const checkedRuleKeys = ref<number[]>([]);

const recordLoading = ref(false);
const recordRows = ref<AlarmRecord[]>([]);

const channelFilters = reactive({
  name: '',
  type: '',
  enabled: -1
});

const ruleFilters = reactive({
  jobGroup: -1,
  jobId: 0,
  alarmEvent: '',
  enabled: -1
});

const recordFilters = reactive({
  jobGroup: -1,
  channelType: '',
  sendStatus: -1
});

const channelFormValue = reactive({
  id: 0,
  name: '',
  type: 'WEBHOOK',
  endpoint: '',
  recipients: '',
  headersJson: '',
  enabled: 1,
  remark: ''
});

const ruleFormValue = reactive({
  id: 0,
  name: '',
  jobGroup: 0,
  jobId: 0,
  alarmEvent: '',
  channelIds: [] as Array<number>,
  enabled: 1,
  remark: ''
});

const enabledOptions: SelectOption[] = [
  { label: '全部', value: -1 },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const sendStatusOptions: SelectOption[] = [
  { label: '全部', value: -1 },
  { label: '成功', value: 1 },
  { label: '失败', value: 2 }
];

const channelTypeOptions = computed<SelectOption[]>(() => [
  { label: '全部', value: '' },
  ...((metadata.value?.alarmChannelTypes || []).map(item => ({ label: item.label, value: item.value })) as SelectOption[])
]);

const alarmEventOptions = computed<SelectOption[]>(() => [
  { label: '全部', value: '' },
  ...((metadata.value?.alarmEventTypes || []).map(item => ({ label: item.label, value: item.value })) as SelectOption[])
]);

const alarmEventLabelMap = computed<Record<string, string>>(() =>
  Object.fromEntries((metadata.value?.alarmEventTypes || []).map(item => [item.value, item.label]))
);

const recordChannelTypeOptions = computed<SelectOption[]>(() => channelTypeOptions.value);

const jobGroupOptions = computed<SelectOption[]>(() => [
  { label: '全部执行器', value: -1 },
  ...jobGroups.value.map(item => ({ label: item.title, value: item.id }))
]);

const enabledChannelOptions = computed<SelectOption[]>(() =>
  channelRows.value
    .filter(item => item.enabled === 1)
    .map(item => ({
      label: `${item.name} [${item.type}]`,
      value: item.id
    }))
);

const ruleJobOptions = computed<SelectOption[]>(() => {
  const jobGroup = ruleFilters.jobGroup;
  if (jobGroup < 1) {
    return [{ label: '全部任务', value: 0 }];
  }
  return [
    { label: '全部任务', value: 0 },
    ...(jobsByGroup.value[jobGroup] || []).map(item => ({
      label: item.jobDesc || `任务 #${item.id}`,
      value: item.id
    }))
  ];
});

const ruleFormJobOptions = computed<SelectOption[]>(() => {
  const jobGroup = ruleFormValue.jobGroup;
  if (jobGroup < 1) {
    return [{ label: '留空表示执行器级规则', value: 0 }];
  }
  return [
    { label: '留空表示执行器级规则', value: 0 },
    ...(jobsByGroup.value[jobGroup] || []).map(item => ({
      label: item.jobDesc || `任务 #${item.id}`,
      value: item.id
    }))
  ];
});

const channelPagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: page => {
    channelPagination.page = page;
    void loadChannels();
  },
  onUpdatePageSize: pageSize => {
    channelPagination.pageSize = pageSize;
    channelPagination.page = 1;
    void loadChannels();
  }
});

const rulePagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: page => {
    rulePagination.page = page;
    void loadRules();
  },
  onUpdatePageSize: pageSize => {
    rulePagination.pageSize = pageSize;
    rulePagination.page = 1;
    void loadRules();
  }
});

const recordPagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: page => {
    recordPagination.page = page;
    void loadRecords();
  },
  onUpdatePageSize: pageSize => {
    recordPagination.pageSize = pageSize;
    recordPagination.page = 1;
    void loadRecords();
  }
});

const selectedChannel = computed(() => channelRows.value.find(item => item.id === checkedChannelKeys.value[0]) || null);
const selectedRule = computed(() => ruleRows.value.find(item => item.id === checkedRuleKeys.value[0]) || null);

const channelRules: FormRules = {
  name: [{ required: true, message: '请输入渠道名称', trigger: ['blur', 'input'] }],
  type: [{ required: true, message: '请选择渠道类型', trigger: ['change', 'blur'] }],
  endpoint: [
    {
      validator: () => {
        if (channelFormValue.type === 'EMAIL') {
          return true;
        }
        return /^https?:\/\//.test(channelFormValue.endpoint.trim()) || new Error('Webhook 地址格式非法');
      },
      trigger: ['blur', 'input']
    }
  ],
  recipients: [
    {
      validator: () => {
        if (channelFormValue.type !== 'EMAIL') {
          return true;
        }
        return channelFormValue.recipients.trim() ? true : new Error('邮件渠道必须配置接收人');
      },
      trigger: ['blur', 'input']
    }
  ],
  headersJson: [
    {
      validator: () => {
        if (!channelFormValue.headersJson.trim()) {
          return true;
        }
        try {
          JSON.parse(channelFormValue.headersJson);
          return true;
        } catch {
          return new Error('请求头 JSON 格式非法');
        }
      },
      trigger: ['blur', 'input']
    }
  ]
};

const ruleRules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: ['blur', 'input'] }],
  jobGroup: [{ required: true, type: 'number', message: '请选择执行器', trigger: ['change', 'blur'] }],
  alarmEvent: [{ required: true, message: '请选择告警事件', trigger: ['change', 'blur'] }],
  channelIds: [
    {
      validator: () => {
        return ruleFormValue.channelIds.length > 0 ? true : new Error('请至少绑定一个告警渠道');
      },
      trigger: ['change', 'blur']
    }
  ]
};

const channelColumns: DataTableColumns<AlarmChannel> = [
  { type: 'selection', multiple: false },
  { title: '名称', key: 'name', minWidth: 180 },
  { title: '类型', key: 'type', width: 120 },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: row => h(NTag, { type: row.enabled === 1 ? 'success' : 'warning', round: true }, { default: () => (row.enabled === 1 ? '启用' : '停用') })
  },
  {
    title: '目标',
    key: 'target',
    minWidth: 280,
    render: row => (row.type === 'EMAIL' ? row.recipients || '-' : row.endpoint || '-')
  },
  { title: '备注', key: 'remark', minWidth: 220 }
];

const ruleColumns: DataTableColumns<AlarmRule> = [
  { type: 'selection', multiple: false },
  { title: '名称', key: 'name', minWidth: 180 },
  {
    title: '执行器',
    key: 'jobGroup',
    minWidth: 160,
    render: row => jobGroups.value.find(item => item.id === row.jobGroup)?.title || `执行器 #${row.jobGroup}`
  },
  {
    title: '任务',
    key: 'jobId',
    minWidth: 200,
    render: row => {
      if (!row.jobId) {
        return '执行器级';
      }
      const jobs = jobsByGroup.value[row.jobGroup] || [];
      return jobs.find(item => item.id === row.jobId)?.jobDesc || `任务 #${row.jobId}`;
    }
  },
  {
    title: '事件',
    key: 'alarmEvent',
    width: 140,
    render: row => alarmEventLabelMap.value[row.alarmEvent] || row.alarmEvent
  },
  {
    title: '渠道',
    key: 'channelIds',
    minWidth: 220,
    render: row => row.channelIds
      .split(',')
      .map(item => Number(item))
      .map(id => channelRows.value.find(channel => channel.id === id)?.name || `渠道 #${id}`)
      .join(', ')
  },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: row => h(NTag, { type: row.enabled === 1 ? 'success' : 'warning', round: true }, { default: () => (row.enabled === 1 ? '启用' : '停用') })
  },
  { title: '备注', key: 'remark', minWidth: 220 }
];

const recordColumns: DataTableColumns<AlarmRecord> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '任务', key: 'jobDesc', minWidth: 180 },
  { title: '事件', key: 'alarmEvent', width: 120, render: row => alarmEventLabelMap.value[row.alarmEvent] || row.alarmEvent },
  { title: '渠道', key: 'channelName', minWidth: 180, render: row => `${row.channelName} [${row.channelType}]` },
  { title: '目标', key: 'target', minWidth: 220 },
  {
    title: '状态',
    key: 'sendStatus',
    width: 100,
    render: row => h(NTag, { type: row.sendStatus === 1 ? 'success' : 'error', round: true }, { default: () => (row.sendStatus === 1 ? '成功' : '失败') })
  },
  { title: '响应码', key: 'responseCode', width: 100 },
  { title: '错误信息', key: 'errorMsg', minWidth: 220 }
];

watch(
  () => ruleFilters.jobGroup,
  async value => {
    ruleFilters.jobId = 0;
    if (value > 0) {
      await ensureJobsLoaded(value);
    }
  }
);

watch(
  () => ruleFormValue.jobGroup,
  async value => {
    ruleFormValue.jobId = 0;
    if (value > 0) {
      await ensureJobsLoaded(value);
    }
  }
);

function channelRowKey(row: AlarmChannel) {
  return row.id;
}

function ruleRowKey(row: AlarmRule) {
  return row.id;
}

function recordRowKey(row: AlarmRecord) {
  return row.id;
}

function handleChannelChecked(keys: Array<string | number>) {
  checkedChannelKeys.value = keys.map(item => Number(item));
}

function handleRuleChecked(keys: Array<string | number>) {
  checkedRuleKeys.value = keys.map(item => Number(item));
}

function resetChannelForm() {
  channelFormValue.id = 0;
  channelFormValue.name = '';
  channelFormValue.type = 'WEBHOOK';
  channelFormValue.endpoint = '';
  channelFormValue.recipients = '';
  channelFormValue.headersJson = '';
  channelFormValue.enabled = 1;
  channelFormValue.remark = '';
}

function resetRuleForm() {
  ruleFormValue.id = 0;
  ruleFormValue.name = '';
  ruleFormValue.jobGroup = 0;
  ruleFormValue.jobId = 0;
  ruleFormValue.alarmEvent = '';
  ruleFormValue.channelIds = [];
  ruleFormValue.enabled = 1;
  ruleFormValue.remark = '';
}

function buildChannelPayload() {
  return {
    id: String(channelFormValue.id),
    name: channelFormValue.name.trim(),
    type: channelFormValue.type,
    endpoint: channelFormValue.endpoint.trim(),
    recipients: channelFormValue.recipients.trim(),
    headersJson: channelFormValue.headersJson.trim(),
    enabled: String(channelFormValue.enabled),
    remark: channelFormValue.remark.trim()
  };
}

function buildRulePayload() {
  return {
    id: String(ruleFormValue.id),
    name: ruleFormValue.name.trim(),
    jobGroup: String(ruleFormValue.jobGroup),
    jobId: String(ruleFormValue.jobId || 0),
    alarmEvent: ruleFormValue.alarmEvent,
    channelIds: ruleFormValue.channelIds.join(','),
    enabled: String(ruleFormValue.enabled),
    remark: ruleFormValue.remark.trim()
  };
}

async function loadMetadata() {
  const response = await fetchJobMetadata();
  if (response.code !== 200) {
    throw new Error(response.msg || '告警元数据加载失败');
  }
  metadata.value = response.data;
}

async function loadJobGroups() {
  const response = await fetchJobGroups();
  if (response.code !== 200) {
    throw new Error(response.msg || '执行器数据加载失败');
  }
  jobGroups.value = response.data;
}

async function ensureJobsLoaded(jobGroup: number) {
  if (jobGroup < 1 || jobsByGroup.value[jobGroup]) {
    return;
  }
  const response = await fetchJobsByGroup(jobGroup);
  if (response.code !== 200) {
    throw new Error(response.msg || '任务列表加载失败');
  }
  jobsByGroup.value = {
    ...jobsByGroup.value,
    [jobGroup]: response.data
  };
}

async function loadChannels() {
  channelLoading.value = true;
  try {
    const response = await fetchAlarmChannels({
      offset: ((channelPagination.page as number) - 1) * (channelPagination.pageSize as number),
      pagesize: channelPagination.pageSize as number,
      name: channelFilters.name,
      type: channelFilters.type,
      enabled: channelFilters.enabled
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '告警渠道加载失败');
    }
    channelRows.value = response.data.data;
    channelPagination.itemCount = response.data.total;
    checkedChannelKeys.value = [];
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '告警渠道加载失败');
  } finally {
    channelLoading.value = false;
  }
}

async function loadRules() {
  ruleLoading.value = true;
  try {
    if (ruleFilters.jobGroup > 0) {
      await ensureJobsLoaded(ruleFilters.jobGroup);
    }
    const response = await fetchAlarmRules({
      offset: ((rulePagination.page as number) - 1) * (rulePagination.pageSize as number),
      pagesize: rulePagination.pageSize as number,
      jobGroup: ruleFilters.jobGroup,
      jobId: ruleFilters.jobId,
      alarmEvent: ruleFilters.alarmEvent,
      enabled: ruleFilters.enabled
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '告警规则加载失败');
    }
    ruleRows.value = response.data.data;
    rulePagination.itemCount = response.data.total;
    checkedRuleKeys.value = [];
    for (const row of ruleRows.value) {
      if (row.jobGroup > 0) {
        await ensureJobsLoaded(row.jobGroup);
      }
    }
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '告警规则加载失败');
  } finally {
    ruleLoading.value = false;
  }
}

async function loadRecords() {
  recordLoading.value = true;
  try {
    const response = await fetchAlarmRecords({
      offset: ((recordPagination.page as number) - 1) * (recordPagination.pageSize as number),
      pagesize: recordPagination.pageSize as number,
      jobGroup: recordFilters.jobGroup,
      channelType: recordFilters.channelType,
      sendStatus: recordFilters.sendStatus
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '告警记录加载失败');
    }
    recordRows.value = response.data.data as AlarmRecord[];
    recordPagination.itemCount = response.data.total;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '告警记录加载失败');
  } finally {
    recordLoading.value = false;
  }
}

function searchChannels() {
  channelPagination.page = 1;
  void loadChannels();
}

function resetChannels() {
  channelFilters.name = '';
  channelFilters.type = '';
  channelFilters.enabled = -1;
  channelPagination.page = 1;
  void loadChannels();
}

function searchRules() {
  rulePagination.page = 1;
  void loadRules();
}

function resetRules() {
  ruleFilters.jobGroup = -1;
  ruleFilters.jobId = 0;
  ruleFilters.alarmEvent = '';
  ruleFilters.enabled = -1;
  rulePagination.page = 1;
  void loadRules();
}

function searchRecords() {
  recordPagination.page = 1;
  void loadRecords();
}

function resetRecords() {
  recordFilters.jobGroup = -1;
  recordFilters.channelType = '';
  recordFilters.sendStatus = -1;
  recordPagination.page = 1;
  void loadRecords();
}

function openChannelCreate() {
  channelFormMode.value = 'create';
  resetChannelForm();
  channelFormVisible.value = true;
}

async function openChannelEdit() {
  if (!selectedChannel.value) {
    return;
  }
  const response = await fetchAlarmChannelDetail(selectedChannel.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '告警渠道详情加载失败');
    return;
  }
  const channel = response.data;
  channelFormMode.value = 'edit';
  channelFormValue.id = channel.id;
  channelFormValue.name = channel.name;
  channelFormValue.type = channel.type;
  channelFormValue.endpoint = channel.endpoint || '';
  channelFormValue.recipients = channel.recipients || '';
  channelFormValue.headersJson = channel.headersJson || '';
  channelFormValue.enabled = channel.enabled;
  channelFormValue.remark = channel.remark || '';
  channelFormVisible.value = true;
}

function openRuleCreate() {
  ruleFormMode.value = 'create';
  resetRuleForm();
  ruleFormVisible.value = true;
}

async function openRuleEdit() {
  if (!selectedRule.value) {
    return;
  }
  const response = await fetchAlarmRuleDetail(selectedRule.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '告警规则详情加载失败');
    return;
  }
  const rule = response.data;
  await ensureJobsLoaded(rule.jobGroup);
  ruleFormMode.value = 'edit';
  ruleFormValue.id = rule.id;
  ruleFormValue.name = rule.name;
  ruleFormValue.jobGroup = rule.jobGroup;
  ruleFormValue.jobId = rule.jobId || 0;
  ruleFormValue.alarmEvent = rule.alarmEvent;
  ruleFormValue.channelIds = (rule.channelIds || '')
    .split(',')
    .map(item => Number(item))
    .filter(item => item > 0);
  ruleFormValue.enabled = rule.enabled;
  ruleFormValue.remark = rule.remark || '';
  ruleFormVisible.value = true;
}

async function submitChannelForm() {
  await channelFormRef.value?.validate();
  channelSubmitting.value = true;
  try {
    const payload = buildChannelPayload();
    const response = channelFormMode.value === 'edit' ? await updateAlarmChannel(payload) : await createAlarmChannel(payload);
    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }
    message.success(channelFormMode.value === 'edit' ? '更新成功' : '创建成功');
    channelFormVisible.value = false;
    await loadChannels();
    await loadRules();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    channelSubmitting.value = false;
  }
}

async function submitRuleForm() {
  await ruleFormRef.value?.validate();
  ruleSubmitting.value = true;
  try {
    const payload = buildRulePayload();
    const response = ruleFormMode.value === 'edit' ? await updateAlarmRule(payload) : await createAlarmRule(payload);
    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }
    message.success(ruleFormMode.value === 'edit' ? '更新成功' : '创建成功');
    ruleFormVisible.value = false;
    await loadRules();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    ruleSubmitting.value = false;
  }
}

async function removeChannel() {
  if (!selectedChannel.value) {
    return;
  }
  dialog.warning({
    title: '删除告警渠道',
    content: `确认删除渠道 ${selectedChannel.value.name} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await deleteAlarmChannel(selectedChannel.value!.id);
      if (response.code !== 200) {
        message.error(response.msg || '删除失败');
        return;
      }
      message.success('删除成功');
      await loadChannels();
      await loadRules();
    }
  });
}

async function removeRule() {
  if (!selectedRule.value) {
    return;
  }
  dialog.warning({
    title: '删除告警规则',
    content: `确认删除规则 ${selectedRule.value.name} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await deleteAlarmRule(selectedRule.value!.id);
      if (response.code !== 200) {
        message.error(response.msg || '删除失败');
        return;
      }
      message.success('删除成功');
      await loadRules();
    }
  });
}

onMounted(async () => {
  try {
    await Promise.all([loadMetadata(), loadJobGroups(), loadChannels()]);
    await Promise.all([loadRules(), loadRecords()]);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '告警页面初始化失败');
  }
});
</script>
