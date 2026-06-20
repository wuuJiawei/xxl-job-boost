<template>
  <div class="page-stack">
    <div class="page-split">
      <n-card :bordered="false" class="tree-panel-card">
        <template #header>
          <div class="table-header">
            <div class="table-title">任务树</div>
            <div class="table-subtitle">
              <span>按执行器定位任务。</span>
              <span>点击任务节点可直接聚焦单个任务。</span>
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
          <div class="filter-grid jobs-filter-grid">
            <n-select v-model:value="filters.triggerStatus" :options="statusOptions" placeholder="状态" />
            <n-input v-model:value="filters.jobDesc" placeholder="按任务描述查询" clearable />
            <n-input v-model:value="filters.executorHandler" placeholder="按 JobHandler 查询" clearable />
            <n-input v-model:value="filters.author" placeholder="按负责人查询" clearable />
            <n-input v-model:value="filters.jobTag" placeholder="按任务标签查询" clearable />
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
              <div class="table-title">任务管理</div>
              <div class="table-subtitle">
                <span>左侧按执行器或任务定位，右侧保留筛选和批量操作。</span>
                <span>单任务视图下可直接聚焦编辑、执行和日志跳转。</span>
              </div>
            </div>
          </template>
          <template #header-extra>
            <div class="table-actions">
              <n-button type="primary" @click="openCreate">新增任务</n-button>
              <n-button :disabled="!selectedRow" @click="() => void openEdit()">编辑</n-button>
              <n-button :disabled="!selectedRow" @click="() => void copySelected()">复制</n-button>
              <n-button :disabled="!selectedRow" type="error" ghost @click="() => void deleteSelected()">删除</n-button>
              <n-button :disabled="!selectedRow" @click="runSelected">执行一次</n-button>
              <n-button :disabled="!selectedRow" @click="openLogs">日志</n-button>
              <n-button :disabled="!selectedRow" @click="openCode">代码</n-button>
              <n-button :disabled="!selectedRow" @click="showRegistry">注册节点</n-button>
              <n-button :disabled="!selectedRow" @click="showNextTime">下次执行</n-button>
              <n-button :disabled="!selectedRow || selectedRow?.triggerStatus === 1" type="primary" @click="startSelected">
                启动
              </n-button>
              <n-button :disabled="!selectedRow || selectedRow?.triggerStatus === 0" type="warning" @click="stopSelected">
                停止
              </n-button>
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

    <n-modal
      v-model:show="formModalVisible"
      preset="card"
      :title="formMode === 'create' ? '新增任务' : formMode === 'edit' ? '编辑任务' : '复制任务'"
      style="width: 920px;"
    >
      <n-alert v-if="formValue.glueType !== 'BEAN'" type="warning" :show-icon="false" style="margin-bottom: 16px;">
        当前只在新控制台内完整支持 BEAN 任务。GLUE 类任务请先回到旧版控制台处理。
      </n-alert>
      <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="120">
        <div class="table-header">
          <div class="table-title">基础信息</div>
        </div>
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi path="jobGroup" label="执行器">
            <n-select v-model:value="formValue.jobGroup" :options="jobGroupOptions" placeholder="请选择执行器" />
          </n-form-item-gi>
          <n-form-item-gi path="jobDesc" label="任务描述">
            <n-input v-model:value="formValue.jobDesc" placeholder="请输入任务描述" />
          </n-form-item-gi>
          <n-form-item-gi path="author" label="负责人">
            <n-input v-model:value="formValue.author" placeholder="请输入负责人" />
          </n-form-item-gi>
          <n-form-item-gi path="jobTag" label="任务标签">
            <n-input v-model:value="formValue.jobTag" placeholder="多个标签用逗号分隔，可选" />
          </n-form-item-gi>
          <n-form-item-gi path="alarmEmail" label="报警邮件">
            <n-input v-model:value="formValue.alarmEmail" placeholder="多个邮箱用逗号分隔，可选" />
          </n-form-item-gi>
          <n-form-item-gi path="alarmChannelIds" label="告警渠道">
            <n-select
              v-model:value="formValue.alarmChannelIds"
              multiple
              :options="alarmChannelOptions"
              placeholder="选择失败告警渠道，可选"
            />
          </n-form-item-gi>
          <n-form-item-gi path="alarmEventTypes" label="告警事件">
            <n-select
              v-model:value="formValue.alarmEventTypes"
              multiple
              :options="alarmEventOptions"
              placeholder="选择需要触发告警的事件，留空表示全部失败事件"
            />
          </n-form-item-gi>
        </n-grid>

        <div class="table-header">
          <div class="table-title">调度配置</div>
        </div>
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi path="scheduleType" label="调度类型">
            <n-select v-model:value="formValue.scheduleType" :options="scheduleTypeOptions" placeholder="请选择调度类型" />
          </n-form-item-gi>
          <n-form-item-gi v-if="formValue.scheduleType !== 'NONE'" path="scheduleConf" :label="scheduleConfLabel">
            <n-input v-model:value="formValue.scheduleConf" :placeholder="scheduleConfPlaceholder" />
          </n-form-item-gi>
        </n-grid>

        <div class="table-header">
          <div class="table-title">任务配置</div>
        </div>
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi path="glueType" label="运行模式">
            <n-select
              v-model:value="formValue.glueType"
              :options="glueTypeOptions"
              placeholder="请选择运行模式"
              :disabled="formMode === 'edit'"
            />
          </n-form-item-gi>
          <n-form-item-gi path="executorHandler" label="JobHandler">
            <n-input v-model:value="formValue.executorHandler" placeholder="请输入 JobHandler" />
          </n-form-item-gi>
        </n-grid>
        <n-form-item path="executorParam" label="任务参数">
          <n-input
            v-model:value="formValue.executorParam"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 6 }"
            placeholder="请输入任务参数，可选"
          />
        </n-form-item>

        <div class="table-header">
          <div class="table-title">高级配置</div>
        </div>
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi path="executorRouteStrategy" label="路由策略">
            <n-select v-model:value="formValue.executorRouteStrategy" :options="routeStrategyOptions" placeholder="请选择路由策略" />
          </n-form-item-gi>
          <n-form-item-gi path="childJobId" label="子任务 ID">
            <n-input v-model:value="formValue.childJobId" placeholder="多个 ID 用逗号分隔，可选" />
          </n-form-item-gi>
          <n-form-item-gi path="misfireStrategy" label="调度过期策略">
            <n-select v-model:value="formValue.misfireStrategy" :options="misfireStrategyOptions" placeholder="请选择策略" />
          </n-form-item-gi>
          <n-form-item-gi path="executorBlockStrategy" label="阻塞处理策略">
            <n-select v-model:value="formValue.executorBlockStrategy" :options="blockStrategyOptions" placeholder="请选择策略" />
          </n-form-item-gi>
          <n-form-item-gi path="executorTimeout" label="超时时间">
            <n-input v-model:value="formValue.executorTimeout" placeholder="秒，留空按 0 处理" />
          </n-form-item-gi>
          <n-form-item-gi path="executorFailRetryCount" label="失败重试次数">
            <n-input v-model:value="formValue.executorFailRetryCount" placeholder="留空按 0 处理" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="formModalVisible = false">取消</n-button>
          <n-button
            :disabled="formValue.glueType !== 'BEAN'"
            type="primary"
            :loading="submitting"
            @click="submitForm"
          >
            保存
          </n-button>
          <n-button
            v-if="formValue.glueType !== 'BEAN'"
            quaternary
            @click="openLegacyForCurrentForm"
          >
            去旧版控制台处理
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="triggerModalVisible" preset="card" title="执行一次" style="width: 680px;">
      <n-form :model="triggerForm" label-placement="left" label-width="110">
        <n-form-item label="任务参数">
          <n-input
            v-model:value="triggerForm.executorParam"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 6 }"
            placeholder="请输入本次执行的任务参数"
          />
        </n-form-item>
        <n-form-item label="机器地址">
          <n-input
            v-model:value="triggerForm.addressList"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            placeholder="为空则由执行器自行选择，多个地址用逗号分隔"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="triggerModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="triggering" @click="confirmTrigger">执行</n-button>
        </div>
      </template>
    </n-modal>

    <n-drawer v-model:show="registryDrawerVisible" :width="560">
      <n-drawer-content title="注册节点">
        <n-empty v-if="!activeRegistryList.length" description="当前没有可展示的注册节点" />
        <div v-else class="registry-drawer">
          <n-tag v-for="item in activeRegistryList" :key="item" round :bordered="false">
            {{ item }}
          </n-tag>
        </div>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="nextTimeVisible" preset="card" title="下次执行时间" style="width: 520px;">
      <n-empty v-if="!nextTriggerTime.length" description="当前没有可计算的执行时间" />
      <div v-else class="registry-drawer">
        <n-tag v-for="item in nextTriggerTime" :key="item" round :bordered="false">
          {{ item }}
        </n-tag>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NForm,
  NFormItem,
  NFormItemGi,
  NGrid,
  NInput,
  NModal,
  NSelect,
  NTag,
  NTree,
  useDialog,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type PaginationProps,
  type SelectOption,
  type TreeOption
} from 'naive-ui';
import {
  fetchJobGroups,
  fetchJobsByGroup,
  fetchJobMetadata,
  type AlarmChannelOption,
  type JobGroupOption,
  type JobOption,
  type JobMetadata,
  type MetadataOption
} from '@/api/admin-next';
import {
  createJob,
  deleteJob,
  fetchJobDetail,
  fetchJobs,
  fetchNextTriggerTime,
  startJob,
  stopJob,
  triggerJob,
  updateJob,
  type JobInfo
} from '@/api/jobs';

const router = useRouter();
const dialog = useDialog();
const message = useMessage();
const loading = ref(false);
const rows = ref<JobInfo[]>([]);
const checkedRowKeys = ref<number[]>([]);
const jobGroups = ref<JobGroupOption[]>([]);
const jobTreeChildren = ref<Record<number, JobOption[]>>({});
const metadata = ref<JobMetadata | null>(null);
const registryDrawerVisible = ref(false);
const nextTimeVisible = ref(false);
const triggerModalVisible = ref(false);
const formModalVisible = ref(false);
const triggering = ref(false);
const submitting = ref(false);
const formRef = ref<FormInst | null>(null);
const activeRegistryList = ref<string[]>([]);
const nextTriggerTime = ref<string[]>([]);
const formMode = ref<'create' | 'edit' | 'copy'>('create');
const expandedTreeKeys = ref<Array<string | number>>([]);
const selectedTreeKeys = ref<Array<string | number>>([]);

const filters = reactive({
  jobGroup: -1,
  jobId: 0,
  triggerStatus: -1,
  jobDesc: '',
  executorHandler: '',
  author: '',
  jobTag: ''
});

const triggerForm = reactive({
  executorParam: '',
  addressList: ''
});

const formValue = reactive({
  id: 0,
  jobGroup: -1,
  jobDesc: '',
  author: '',
  jobTag: '',
  alarmEmail: '',
  alarmChannelIds: [] as number[],
  alarmEventTypes: [] as string[],
  scheduleType: 'CRON',
  scheduleConf: '',
  glueType: 'BEAN',
  executorHandler: '',
  executorParam: '',
  executorRouteStrategy: '',
  childJobId: '',
  misfireStrategy: 'DO_NOTHING',
  executorBlockStrategy: '',
  executorTimeout: '0',
  executorFailRetryCount: '0'
});

const statusOptions: SelectOption[] = [
  { label: '全部', value: -1 },
  { label: '停止', value: 0 },
  { label: '运行中', value: 1 }
];

const rules: FormRules = {
  jobGroup: [{ required: true, type: 'number', message: '请选择执行器', trigger: ['change', 'blur'] }],
  jobDesc: [{ required: true, message: '请输入任务描述', trigger: ['blur', 'input'] }],
  author: [{ required: true, message: '请输入负责人', trigger: ['blur', 'input'] }],
  scheduleType: [{ required: true, message: '请选择调度类型', trigger: ['change', 'blur'] }],
  scheduleConf: [
    {
      validator: () => {
        if (formValue.scheduleType === 'NONE') {
          return true;
        }
        if (!formValue.scheduleConf.trim()) {
          return new Error('请输入调度配置');
        }
        if (formValue.scheduleType === 'FIX_RATE' && !/^\d+$/.test(formValue.scheduleConf.trim())) {
          return new Error('固定频率必须是正整数秒');
        }
        return true;
      },
      trigger: ['blur', 'input', 'change']
    }
  ],
  glueType: [{ required: true, message: '请选择运行模式', trigger: ['change', 'blur'] }],
  executorHandler: [
    {
      validator: () => {
        if (formValue.glueType !== 'BEAN') {
          return true;
        }
        if (!formValue.executorHandler.trim()) {
          return new Error('请输入 JobHandler');
        }
        return true;
      },
      trigger: ['blur', 'input']
    }
  ],
  childJobId: [
    {
      validator: () => {
        const value = formValue.childJobId.trim();
        if (!value) {
          return true;
        }
        const items = value.split(',').map((item) => item.trim());
        if (!items.every((item) => /^\d+$/.test(item))) {
          return new Error('子任务 ID 需为逗号分隔的数字');
        }
        if (formMode.value === 'edit' && items.includes(String(formValue.id))) {
          return new Error('子任务 ID 不能包含自身');
        }
        return true;
      },
      trigger: ['blur', 'input']
    }
  ],
  executorTimeout: [
    {
      validator: () => /^\d*$/.test(formValue.executorTimeout.trim()) || new Error('超时时间必须是数字'),
      trigger: ['blur', 'input']
    }
  ],
  executorFailRetryCount: [
    {
      validator: () => /^\d*$/.test(formValue.executorFailRetryCount.trim()) || new Error('失败重试次数必须是数字'),
      trigger: ['blur', 'input']
    }
  ]
};

const jobGroupOptions = computed<SelectOption[]>(() =>
  jobGroups.value.map((item) => ({
    label: item.title,
    value: item.id
  }))
);

const jobTreeData = computed<TreeOption[]>(() =>
  jobGroups.value.map((group) => {
    const children = (jobTreeChildren.value[group.id] || []).map((job) => ({
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

const treeSelectionLabel = computed(() => {
  if (filters.jobId > 0) {
    const job = Object.values(jobTreeChildren.value)
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

const scheduleTypeOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.scheduleTypes || []));
const routeStrategyOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.routeStrategies || []));
const misfireStrategyOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.misfireStrategies || []));
const blockStrategyOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.blockStrategies || []));
const glueTypeOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.glueTypes || []));
const alarmChannelOptions = computed<SelectOption[]>(() =>
  (metadata.value?.alarmChannels || []).map((item: AlarmChannelOption) => ({
    label: `${item.name} [${item.type}]`,
    value: item.id
  }))
);
const alarmEventOptions = computed<SelectOption[]>(() => toSelectOptions(metadata.value?.alarmEventTypes || []));

const scheduleConfLabel = computed(() => (formValue.scheduleType === 'CRON' ? 'Cron' : '固定频率(秒)'));
const scheduleConfPlaceholder = computed(() =>
  formValue.scheduleType === 'CRON' ? '请输入 Cron 表达式' : '请输入固定频率秒数'
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

const columns: DataTableColumns<JobInfo> = [
  {
    type: 'selection',
    multiple: false
  },
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '任务描述',
    key: 'jobDesc',
    minWidth: 220
  },
  {
    title: '调度类型',
    key: 'scheduleType',
    minWidth: 180,
    render: (row) => `${row.scheduleType}${row.scheduleConf ? `: ${row.scheduleConf}` : ''}`
  },
  {
    title: '运行模式',
    key: 'glueType',
    minWidth: 180,
    render: (row) => `${row.glueType}${row.executorHandler ? `: ${row.executorHandler}` : ''}`
  },
  {
    title: '状态',
    key: 'triggerStatus',
    width: 110,
    render: (row) =>
      h(
        NTag,
        { type: row.triggerStatus === 1 ? 'success' : 'default', round: true },
        { default: () => (row.triggerStatus === 1 ? 'RUNNING' : 'STOP') }
      )
  },
  {
    title: '负责人',
    key: 'author',
    width: 120
  },
  {
    title: '任务标签',
    key: 'jobTag',
    minWidth: 180,
    render: (row) => row.jobTag || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 230,
    render: (row) =>
      h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => openEdit(row)
          },
          { default: () => '编辑' }
        ),
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => copySelected(row)
          },
          { default: () => '复制' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            ghost: true,
            onClick: () => void deleteSelected(row)
          },
          { default: () => '删除' }
        )
      ])
  }
];

function matchJobFilters(job: JobInfo) {
  if (filters.jobGroup > 0 && job.jobGroup !== filters.jobGroup) {
    return false;
  }
  if (filters.triggerStatus > -1 && job.triggerStatus !== filters.triggerStatus) {
    return false;
  }
  if (filters.jobDesc.trim() && !job.jobDesc?.toLowerCase().includes(filters.jobDesc.trim().toLowerCase())) {
    return false;
  }
  if (
    filters.executorHandler.trim() &&
    !job.executorHandler?.toLowerCase().includes(filters.executorHandler.trim().toLowerCase())
  ) {
    return false;
  }
  if (filters.author.trim() && !job.author?.toLowerCase().includes(filters.author.trim().toLowerCase())) {
    return false;
  }
  if (filters.jobTag.trim() && !(job.jobTag || '').toLowerCase().includes(filters.jobTag.trim().toLowerCase())) {
    return false;
  }
  return true;
}

function toSelectOptions(items: MetadataOption[]) {
  return items.map((item) => ({
    label: item.label,
    value: item.value
  }));
}

function rowKey(row: JobInfo) {
  return row.id;
}

function handleCheckedRowKeys(keys: Array<string | number>) {
  checkedRowKeys.value = keys.map((item) => Number(item));
}

function resetFormValue() {
  formValue.id = 0;
  formValue.jobGroup = filters.jobGroup > 0 ? filters.jobGroup : jobGroups.value[0]?.id || -1;
  formValue.jobDesc = '';
  formValue.author = '';
  formValue.jobTag = '';
  formValue.alarmEmail = '';
  formValue.alarmChannelIds = [];
  formValue.alarmEventTypes = [];
  formValue.scheduleType = metadata.value?.scheduleTypes.find((item) => item.value === 'CRON')?.value || 'CRON';
  formValue.scheduleConf = '';
  formValue.glueType = 'BEAN';
  formValue.executorHandler = '';
  formValue.executorParam = '';
  formValue.executorRouteStrategy = metadata.value?.routeStrategies[0]?.value || '';
  formValue.childJobId = '';
  formValue.misfireStrategy = metadata.value?.misfireStrategies.find((item) => item.value === 'DO_NOTHING')?.value || '';
  formValue.executorBlockStrategy = metadata.value?.blockStrategies[0]?.value || '';
  formValue.executorTimeout = '0';
  formValue.executorFailRetryCount = '0';
}

function hydrateForm(job: JobInfo) {
  formValue.id = job.id;
  formValue.jobGroup = job.jobGroup;
  formValue.jobDesc = job.jobDesc || '';
  formValue.author = job.author || '';
  formValue.jobTag = job.jobTag || '';
  formValue.alarmEmail = job.alarmEmail || '';
  formValue.alarmChannelIds = (job.alarmChannelIds || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => Number(item));
  formValue.alarmEventTypes = (job.alarmEventTypes || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
  formValue.scheduleType = job.scheduleType || 'CRON';
  formValue.scheduleConf = job.scheduleConf || '';
  formValue.glueType = job.glueType || 'BEAN';
  formValue.executorHandler = job.executorHandler || '';
  formValue.executorParam = job.executorParam || '';
  formValue.executorRouteStrategy = job.executorRouteStrategy || metadata.value?.routeStrategies[0]?.value || '';
  formValue.childJobId = job.childJobId || '';
  formValue.misfireStrategy = job.misfireStrategy || metadata.value?.misfireStrategies[0]?.value || '';
  formValue.executorBlockStrategy = job.executorBlockStrategy || metadata.value?.blockStrategies[0]?.value || '';
  formValue.executorTimeout = String(job.executorTimeout ?? 0);
  formValue.executorFailRetryCount = String(job.executorFailRetryCount ?? 0);
}

function buildPayload() {
  const payload: Record<string, string> = {
    jobGroup: String(formValue.jobGroup),
    jobDesc: formValue.jobDesc.trim(),
    author: formValue.author.trim(),
    jobTag: formValue.jobTag.trim(),
    alarmEmail: formValue.alarmEmail.trim(),
    alarmChannelIds: formValue.alarmChannelIds.join(','),
    alarmEventTypes: formValue.alarmEventTypes.join(','),
    scheduleType: formValue.scheduleType,
    scheduleConf: formValue.scheduleType === 'NONE' ? '' : formValue.scheduleConf.trim(),
    glueType: formValue.glueType,
    executorHandler: formValue.executorHandler.trim(),
    executorParam: formValue.executorParam,
    executorRouteStrategy: formValue.executorRouteStrategy,
    childJobId: normalizeChildJobId(formValue.childJobId),
    misfireStrategy: formValue.misfireStrategy,
    executorBlockStrategy: formValue.executorBlockStrategy,
    executorTimeout: normalizeNumberField(formValue.executorTimeout),
    executorFailRetryCount: normalizeNumberField(formValue.executorFailRetryCount)
  };

  if (formMode.value !== 'create') {
    payload.id = String(formValue.id);
  }

  if (formMode.value === 'create' || formMode.value === 'copy') {
    payload.glueRemark = 'Admin Next init';
    payload.glueSource = '';
  }

  return payload;
}

function normalizeNumberField(value: string) {
  const trimmed = value.trim();
  return /^\d+$/.test(trimmed) ? trimmed : '0';
}

function normalizeChildJobId(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .join(',');
}

function isBeanJob(job: JobInfo | null) {
  return !job || job.glueType === 'BEAN';
}

function ensureBeanJob(job: JobInfo | null, actionText: string) {
  if (!job) {
    return false;
  }
  if (isBeanJob(job)) {
    return true;
  }
  dialog.warning({
    title: '暂不支持',
    content: `${actionText} 目前仅支持 BEAN 任务，GLUE 类任务请先在旧版控制台处理。`,
    positiveText: '打开旧版控制台',
    negativeText: '取消',
    onPositiveClick: () => {
      window.open(`/xxl-job-admin/jobinfo?jobGroup=${job.jobGroup}`, '_blank');
    }
  });
  return false;
}

async function loadJobGroups() {
  const response = await fetchJobGroups();
  if (response.code !== 200) {
    throw new Error(response.msg || '执行器数据加载失败');
  }
  jobGroups.value = response.data;
  if (jobGroups.value.length && filters.jobGroup === -1) {
    filters.jobGroup = jobGroups.value[0].id;
  }
}

async function loadTreeJobs(force = false) {
  if (!jobGroups.value.length) {
    jobTreeChildren.value = {};
    return;
  }

  const groups = force ? jobGroups.value : jobGroups.value.filter((item) => !jobTreeChildren.value[item.id]);
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

  jobTreeChildren.value = {
    ...(force ? {} : jobTreeChildren.value),
    ...Object.fromEntries(entries)
  };
}

function syncTreeSelection() {
  if (filters.jobId > 0) {
    selectedTreeKeys.value = [`job-${filters.jobId}`];
    const job = Object.values(jobTreeChildren.value)
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

async function loadMetadata() {
  const response = await fetchJobMetadata();
  if (response.code !== 200) {
    throw new Error(response.msg || '任务元数据加载失败');
  }
  metadata.value = response.data;
}

async function loadData() {
  if (filters.jobGroup === -1) {
    return;
  }
  loading.value = true;
  try {
    if (filters.jobId > 0) {
      const detail = await fetchJobDetail(filters.jobId);
      if (detail.code !== 200) {
        throw new Error(detail.msg || '任务详情加载失败');
      }
      rows.value = matchJobFilters(detail.data) ? [detail.data] : [];
      pagination.itemCount = rows.value.length;
      checkedRowKeys.value = rows.value.length ? [rows.value[0].id] : [];
      return;
    }

    const response = await fetchJobs({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      jobGroup: filters.jobGroup,
      triggerStatus: filters.triggerStatus,
      jobDesc: filters.jobDesc,
      executorHandler: filters.executorHandler,
      author: filters.author,
      jobTag: filters.jobTag
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '任务列表加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
    checkedRowKeys.value = [];
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '任务列表加载失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.page = 1;
  void loadData();
}

function reset() {
  filters.triggerStatus = -1;
  filters.jobDesc = '';
  filters.executorHandler = '';
  filters.author = '';
  filters.jobTag = '';
  pagination.page = 1;
  void loadData();
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
    const job = Object.values(jobTreeChildren.value)
      .flat()
      .find((item) => item.id === jobId);
    if (!job) {
      return;
    }
    filters.jobGroup = job.jobGroup;
    filters.jobId = job.id;
  }

  pagination.page = 1;
  checkedRowKeys.value = filters.jobId > 0 ? [filters.jobId] : [];
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
    message.error(err.message || '任务树刷新失败');
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

function openCreate() {
  formMode.value = 'create';
  resetFormValue();
  formModalVisible.value = true;
}

async function openEdit(row?: JobInfo | null) {
  const target = row || selectedRow.value;
  if (!ensureBeanJob(target, '编辑任务')) {
    return;
  }
  const response = await fetchJobDetail(target!.id);
  if (response.code !== 200) {
    message.error(response.msg || '任务详情加载失败');
    return;
  }
  formMode.value = 'edit';
  hydrateForm(response.data);
  formModalVisible.value = true;
}

async function copySelected(row?: JobInfo | null) {
  const target = row || selectedRow.value;
  if (!ensureBeanJob(target, '复制任务')) {
    return;
  }
  const response = await fetchJobDetail(target!.id);
  if (response.code !== 200) {
    message.error(response.msg || '任务详情加载失败');
    return;
  }
  formMode.value = 'copy';
  hydrateForm(response.data);
  formValue.id = 0;
  formModalVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  if (formValue.glueType !== 'BEAN') {
    return;
  }

  submitting.value = true;
  try {
    const payload = buildPayload();
    const response =
      formMode.value === 'edit' ? await updateJob(payload) : await createJob(payload);
    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }
    message.success(formMode.value === 'edit' ? '更新成功' : '创建成功');
    formModalVisible.value = false;
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

async function deleteSelected(row?: JobInfo | null) {
  const target = row || selectedRow.value;
  if (!target) {
    return;
  }
  dialog.warning({
    title: '删除任务',
    content: `确认删除任务 #${target.id} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await deleteJob(target.id);
      if (response.code !== 200) {
        message.error(response.msg || '删除失败');
        return;
      }
      message.success('删除成功');
      await loadData();
    }
  });
}

function openLegacyForCurrentForm() {
  const groupId = formValue.jobGroup > 0 ? formValue.jobGroup : filters.jobGroup;
  window.open(`/xxl-job-admin/jobinfo?jobGroup=${groupId}`, '_blank');
}

async function startSelected() {
  if (!selectedRow.value) {
    return;
  }
  const response = await startJob(selectedRow.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '启动失败');
    return;
  }
  message.success('启动成功');
  void loadData();
}

async function stopSelected() {
  if (!selectedRow.value) {
    return;
  }
  const response = await stopJob(selectedRow.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '停止失败');
    return;
  }
  message.success('停止成功');
  void loadData();
}

function runSelected() {
  if (!selectedRow.value) {
    return;
  }
  triggerForm.executorParam = selectedRow.value.executorParam || '';
  triggerForm.addressList = '';
  triggerModalVisible.value = true;
}

async function confirmTrigger() {
  if (!selectedRow.value) {
    return;
  }
  triggering.value = true;
  try {
    const response = await triggerJob(
      selectedRow.value.id,
      triggerForm.executorParam,
      triggerForm.addressList
    );
    if (response.code !== 200) {
      throw new Error(response.msg || '执行失败');
    }
    message.success('执行成功');
    triggerModalVisible.value = false;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '执行失败');
  } finally {
    triggering.value = false;
  }
}

function openCode() {
  if (!selectedRow.value) {
    return;
  }
  if (selectedRow.value.glueType === 'BEAN') {
    message.warning('BEAN 模式任务没有 GLUE IDE');
    return;
  }
  router.push({
    name: 'job-code',
    query: {
      jobId: String(selectedRow.value.id)
    }
  });
}

function openLogs() {
  if (!selectedRow.value) {
    return;
  }
  router.push({
    name: 'logs',
    query: {
      jobGroup: String(selectedRow.value.jobGroup),
      jobId: String(selectedRow.value.id)
    }
  });
}

function showRegistry() {
  if (!selectedRow.value) {
    return;
  }
  const group = jobGroups.value.find((item) => item.id === selectedRow.value?.jobGroup);
  activeRegistryList.value = group?.registryList || [];
  registryDrawerVisible.value = true;
}

async function showNextTime() {
  if (!selectedRow.value) {
    return;
  }
  try {
    const response = await fetchNextTriggerTime(
      selectedRow.value.scheduleType,
      selectedRow.value.scheduleConf
    );
    if (response.code !== 200) {
      throw new Error(response.msg || '下次执行时间计算失败');
    }
    nextTriggerTime.value = response.data;
    nextTimeVisible.value = true;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '下次执行时间计算失败');
  }
}

onMounted(async () => {
  try {
    await loadJobGroups();
    await loadTreeJobs(true);
    await loadMetadata();
    syncTreeSelection();
    resetFormValue();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '任务页初始化失败');
  }
});
</script>
