<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="section-kicker">Alerts</div>
      <h2>告警渠道与投递记录</h2>
      <p>第二阶段先把失败告警通道化，支持渠道管理、任务绑定和投递记录查询，暂不扩成复杂规则引擎。</p>
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
          <n-button v-if="authStore.userInfo.isAdmin" type="primary" @click="openCreate">新增渠道</n-button>
          <n-button v-if="authStore.userInfo.isAdmin" :disabled="!selectedChannel" @click="() => void openEdit()">编辑</n-button>
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
      v-model:show="formVisible"
      preset="card"
      :title="formMode === 'create' ? '新增告警渠道' : '编辑告警渠道'"
      style="width: 760px;"
    >
      <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="120">
        <n-form-item path="name" label="渠道名称">
          <n-input v-model:value="formValue.name" placeholder="请输入渠道名称" />
        </n-form-item>
        <n-form-item path="type" label="渠道类型">
          <n-select v-model:value="formValue.type" :options="channelTypeOptions.filter(item => item.value !== '')" placeholder="请选择渠道类型" />
        </n-form-item>
        <n-form-item path="enabled" label="启用状态">
          <n-select v-model:value="formValue.enabled" :options="enabledOptions.filter(item => item.value !== -1)" />
        </n-form-item>
        <n-form-item v-if="formValue.type !== 'EMAIL'" path="endpoint" label="Webhook 地址">
          <n-input v-model:value="formValue.endpoint" placeholder="请输入 Webhook 地址" />
        </n-form-item>
        <n-form-item path="recipients" :label="formValue.type === 'EMAIL' ? '接收邮箱' : '接收人标识'">
          <n-input v-model:value="formValue.recipients" placeholder="多个值用逗号分隔，可选" />
        </n-form-item>
        <n-form-item path="headersJson" label="请求头 JSON">
          <n-input
            v-model:value="formValue.headersJson"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder='例如 {"X-Token":"abc"}，可选'
          />
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <n-input v-model:value="formValue.remark" type="textarea" :autosize="{ minRows: 3, maxRows: 5 }" placeholder="渠道说明，可选" />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitForm">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
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
  deleteAlarmChannel,
  fetchAlarmChannels,
  type AlarmChannel,
  type AlarmRecord,
  updateAlarmChannel
} from '@/api/alerts';
import {
  fetchAlarmChannelDetail,
  fetchAlarmRecords,
  fetchJobGroups,
  fetchJobMetadata,
  type JobGroupOption,
  type JobMetadata
} from '@/api/admin-next';

const authStore = useAuthStore();
const dialog = useDialog();
const message = useMessage();
const formRef = ref<FormInst | null>(null);
const formVisible = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const submitting = ref(false);

const metadata = ref<JobMetadata | null>(null);
const jobGroups = ref<JobGroupOption[]>([]);

const channelLoading = ref(false);
const channelRows = ref<AlarmChannel[]>([]);
const checkedChannelKeys = ref<number[]>([]);

const recordLoading = ref(false);
const recordRows = ref<AlarmRecord[]>([]);

const channelFilters = reactive({
  name: '',
  type: '',
  enabled: -1
});

const recordFilters = reactive({
  jobGroup: -1,
  channelType: '',
  sendStatus: -1
});

const formValue = reactive({
  id: 0,
  name: '',
  type: 'WEBHOOK',
  endpoint: '',
  recipients: '',
  headersJson: '',
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

const alarmEventLabelMap = computed<Record<string, string>>(() =>
  Object.fromEntries((metadata.value?.alarmEventTypes || []).map(item => [item.value, item.label]))
);

const recordChannelTypeOptions = computed<SelectOption[]>(() => channelTypeOptions.value);

const jobGroupOptions = computed<SelectOption[]>(() => [
  { label: '全部执行器', value: -1 },
  ...jobGroups.value.map(item => ({ label: item.title, value: item.id }))
]);

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

const rules: FormRules = {
  name: [{ required: true, message: '请输入渠道名称', trigger: ['blur', 'input'] }],
  type: [{ required: true, message: '请选择渠道类型', trigger: ['change', 'blur'] }],
  endpoint: [
    {
      validator: () => {
        if (formValue.type === 'EMAIL') {
          return true;
        }
        return /^https?:\/\//.test(formValue.endpoint.trim()) || new Error('Webhook 地址格式非法');
      },
      trigger: ['blur', 'input']
    }
  ],
  recipients: [
    {
      validator: () => {
        if (formValue.type !== 'EMAIL') {
          return true;
        }
        return formValue.recipients.trim() ? true : new Error('邮件渠道必须配置接收人');
      },
      trigger: ['blur', 'input']
    }
  ],
  headersJson: [
    {
      validator: () => {
        if (!formValue.headersJson.trim()) {
          return true;
        }
        try {
          JSON.parse(formValue.headersJson);
          return true;
        } catch {
          return new Error('请求头 JSON 格式非法');
        }
      },
      trigger: ['blur', 'input']
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
    render: row => row.type === 'EMAIL' ? row.recipients || '-' : row.endpoint || '-'
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

function channelRowKey(row: AlarmChannel) {
  return row.id;
}

function recordRowKey(row: AlarmRecord) {
  return row.id;
}

function handleChannelChecked(keys: Array<string | number>) {
  checkedChannelKeys.value = keys.map(item => Number(item));
}

function resetForm() {
  formValue.id = 0;
  formValue.name = '';
  formValue.type = 'WEBHOOK';
  formValue.endpoint = '';
  formValue.recipients = '';
  formValue.headersJson = '';
  formValue.enabled = 1;
  formValue.remark = '';
}

function buildPayload() {
  return {
    id: String(formValue.id),
    name: formValue.name.trim(),
    type: formValue.type,
    endpoint: formValue.endpoint.trim(),
    recipients: formValue.recipients.trim(),
    headersJson: formValue.headersJson.trim(),
    enabled: String(formValue.enabled),
    remark: formValue.remark.trim()
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

function openCreate() {
  formMode.value = 'create';
  resetForm();
  formVisible.value = true;
}

async function openEdit() {
  if (!selectedChannel.value) {
    return;
  }
  const response = await fetchAlarmChannelDetail(selectedChannel.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '告警渠道详情加载失败');
    return;
  }

  const channel = response.data;
  formMode.value = 'edit';
  formValue.id = channel.id;
  formValue.name = channel.name;
  formValue.type = channel.type;
  formValue.endpoint = channel.endpoint || '';
  formValue.recipients = channel.recipients || '';
  formValue.headersJson = channel.headersJson || '';
  formValue.enabled = channel.enabled;
  formValue.remark = channel.remark || '';
  formVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const payload = buildPayload();
    const response = formMode.value === 'edit' ? await updateAlarmChannel(payload) : await createAlarmChannel(payload);
    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }
    message.success(formMode.value === 'edit' ? '更新成功' : '创建成功');
    formVisible.value = false;
    await loadChannels();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    submitting.value = false;
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
    }
  });
}

onMounted(async () => {
  try {
    await Promise.all([loadMetadata(), loadJobGroups()]);
    await Promise.all([loadChannels(), loadRecords()]);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '告警页面初始化失败');
  }
});
</script>
