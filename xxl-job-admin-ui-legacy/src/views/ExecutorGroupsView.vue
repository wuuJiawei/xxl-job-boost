<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div>
        <div class="section-kicker">Executors</div>
        <h2>先把执行器管理页迁过来，作为第一张业务页。</h2>
        <p>
          当前页面复用原 `/jobgroup/pageList` 接口，先覆盖查询和列表展示。增删改下一步接弹窗表单。
        </p>
      </div>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid">
        <n-input v-model:value="filters.appname" placeholder="按 AppName 查询" clearable />
        <n-input v-model:value="filters.title" placeholder="按执行器名称查询" clearable />
        <div class="filter-actions">
          <n-button type="primary" @click="search">查询</n-button>
          <n-button @click="reset">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">执行器管理</div>
          <div class="table-subtitle">先完成列表迁移，再补新增、编辑、删除弹窗。</div>
        </div>
      </template>
      <template #header-extra>
        <div class="table-actions">
          <n-button type="primary" @click="openCreate">新增执行器</n-button>
          <n-button :disabled="!selectedRow" @click="() => openEdit()">编辑</n-button>
          <n-button :disabled="!selectedRow" type="error" ghost @click="confirmDelete">删除</n-button>
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

    <n-modal v-model:show="formModalVisible" preset="card" :title="formMode === 'create' ? '新增执行器' : '编辑执行器'" style="width: 720px;">
      <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="110">
        <n-form-item path="appname" label="AppName">
          <n-input v-model:value="formValue.appname" placeholder="请输入 AppName" />
        </n-form-item>
        <n-form-item path="title" label="名称">
          <n-input v-model:value="formValue.title" placeholder="请输入执行器名称" />
        </n-form-item>
        <n-form-item path="addressType" label="注册方式">
          <n-radio-group v-model:value="formValue.addressType">
            <n-space>
              <n-radio :value="0">自动注册</n-radio>
              <n-radio :value="1">手动录入</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item path="addressList" label="机器地址">
          <n-input
            v-model:value="formValue.addressList"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 6 }"
            :placeholder="formValue.addressType === 0 ? '自动注册模式下不需要填写' : '多个地址用逗号分隔'"
            :disabled="formValue.addressType === 0"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="formModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitForm">保存</n-button>
        </div>
      </template>
    </n-modal>

    <n-drawer v-model:show="registryDrawerVisible" :width="560">
      <n-drawer-content title="在线节点">
        <n-empty v-if="!activeRegistryList.length" description="当前没有可展示的在线节点" />
        <div v-else class="registry-drawer">
          <n-tag v-for="item in activeRegistryList" :key="item" round :bordered="false">
            {{ item }}
          </n-tag>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type PaginationProps
} from 'naive-ui';
import {
  createExecutorGroup,
  deleteExecutorGroup,
  fetchExecutorGroups,
  updateExecutorGroup,
  type ExecutorGroup
} from '@/api/executors';

const message = useMessage();
const loading = ref(false);
const rows = ref<ExecutorGroup[]>([]);
const checkedRowKeys = ref<number[]>([]);
const formRef = ref<FormInst | null>(null);
const formModalVisible = ref(false);
const registryDrawerVisible = ref(false);
const submitting = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const activeRegistryList = ref<string[]>([]);

const filters = reactive({
  appname: '',
  title: ''
});

const formValue = reactive({
  id: 0,
  appname: '',
  title: '',
  addressType: 0,
  addressList: ''
});

const rules: FormRules = {
  appname: [
    { required: true, message: '请输入 AppName', trigger: ['blur', 'input'] },
    { min: 4, max: 64, message: 'AppName 长度限制为 4~64', trigger: ['blur', 'input'] },
    {
      validator: (_, value: string) => /^[a-z][a-zA-Z0-9-]*$/.test(value),
      message: '限制以小写字母开头，由小写字母、数字和中划线组成',
      trigger: ['blur', 'input']
    }
  ],
  title: [
    { required: true, message: '请输入名称', trigger: ['blur', 'input'] },
    { min: 4, max: 12, message: '名称长度限制为 4~12', trigger: ['blur', 'input'] }
  ],
  addressList: [
    {
      validator: () => {
        if (formValue.addressType === 0) {
          return true;
        }
        const value = formValue.addressList.trim();
        if (!value) {
          return new Error('手动录入注册方式，机器地址不可为空');
        }
        const items = value.split(',').map((item) => item.trim()).filter(Boolean);
        const valid = items.every((item) => /^https?:\/\//.test(item));
        if (!valid) {
          return new Error('机器地址格式非法');
        }
        return true;
      },
      trigger: ['blur', 'input']
    }
  ]
};

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

const columns: DataTableColumns<ExecutorGroup> = [
  {
    type: 'selection',
    multiple: false
  },
  {
    title: 'AppName',
    key: 'appname',
    minWidth: 180
  },
  {
    title: '执行器名称',
    key: 'title',
    minWidth: 180
  },
  {
    title: '地址类型',
    key: 'addressType',
    width: 120,
    render: (row) =>
      h(
        NTag,
        { type: row.addressType === 0 ? 'success' : 'warning', round: true },
        { default: () => (row.addressType === 0 ? '自动注册' : '手动录入') }
      )
  },
  {
    title: '在线地址',
    key: 'registryList',
    minWidth: 320,
    render: (row) => {
      const values = row.registryList?.length ? row.registryList : row.addressList?.split(',') || [];
      if (!values.length) {
        return '空';
      }
      return h(
        NButton,
        {
          size: 'small',
          quaternary: true,
          onClick: () => showRegistry(row)
        },
        { default: () => `查看 (${values.length})` }
      );
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
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
            onClick: () => showRegistry(row)
          },
          { default: () => '节点' }
        )
      ])
  }
];

function rowKey(row: ExecutorGroup) {
  return row.id;
}

function handleCheckedRowKeys(keys: Array<string | number>) {
  checkedRowKeys.value = keys.map((item) => Number(item));
}

function resetForm() {
  formValue.id = 0;
  formValue.appname = '';
  formValue.title = '';
  formValue.addressType = 0;
  formValue.addressList = '';
}

function openCreate() {
  formMode.value = 'create';
  resetForm();
  formModalVisible.value = true;
}

function openEdit(row?: ExecutorGroup | null) {
  const current = row || selectedRow.value;
  if (!current) {
    message.warning('请先选择一条执行器数据');
    return;
  }
  formMode.value = 'edit';
  formValue.id = current.id;
  formValue.appname = current.appname;
  formValue.title = current.title;
  formValue.addressType = current.addressType;
  formValue.addressList = current.addressList || '';
  formModalVisible.value = true;
}

function showRegistry(row: ExecutorGroup) {
  activeRegistryList.value = row.registryList?.length ? row.registryList : row.addressList?.split(',').filter(Boolean) || [];
  registryDrawerVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const response =
      formMode.value === 'create'
        ? await createExecutorGroup({
            appname: formValue.appname.trim(),
            title: formValue.title.trim(),
            addressType: formValue.addressType,
            addressList: formValue.addressType === 0 ? '' : formValue.addressList.trim()
          })
        : await updateExecutorGroup({
            id: formValue.id,
            appname: formValue.appname.trim(),
            title: formValue.title.trim(),
            addressType: formValue.addressType,
            addressList: formValue.addressType === 0 ? '' : formValue.addressList.trim()
          });

    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }

    message.success(formMode.value === 'create' ? '新增成功' : '更新成功');
    formModalVisible.value = false;
    void loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

async function confirmDelete() {
  if (!selectedRow.value) {
    message.warning('请先选择一条执行器数据');
    return;
  }

  loading.value = true;
  try {
    const response = await deleteExecutorGroup(selectedRow.value.id);
    if (response.code !== 200) {
      throw new Error(response.msg || '删除失败');
    }
    message.success('删除成功');
    checkedRowKeys.value = [];
    void loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '删除失败');
  } finally {
    loading.value = false;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const response = await fetchExecutorGroups({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      appname: filters.appname || undefined,
      title: filters.title || undefined
    });

    if (response.code !== 200) {
      throw new Error(response.msg || '执行器列表加载失败');
    }

    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
    checkedRowKeys.value = [];
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '执行器列表加载失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.page = 1;
  void loadData();
}

function reset() {
  filters.appname = '';
  filters.title = '';
  pagination.page = 1;
  void loadData();
}

onMounted(() => {
  void loadData();
});
</script>
