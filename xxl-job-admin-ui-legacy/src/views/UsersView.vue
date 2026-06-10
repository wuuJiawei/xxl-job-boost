<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div>
        <div class="section-kicker">Users</div>
        <h2>先把用户管理页迁过来，保持管理员的日常维护动作可用。</h2>
        <p>
          当前版本覆盖筛选、列表、新增、编辑、删除以及执行器权限分配。
        </p>
      </div>
    </div>

    <n-card :bordered="false" class="filter-card">
      <div class="filter-grid">
        <n-select v-model:value="filters.role" :options="roleOptionsWithAll" placeholder="按角色筛选" />
        <n-input v-model:value="filters.username" placeholder="按用户名查询" clearable />
        <div class="filter-actions">
          <n-button type="primary" @click="search">查询</n-button>
          <n-button @click="reset">重置</n-button>
        </div>
      </div>
    </n-card>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">用户管理</div>
          <div class="table-subtitle">保留原权限模型，先保证账号和执行器权限维护可用。</div>
        </div>
      </template>
      <template #header-extra>
        <div class="table-actions">
          <n-button type="primary" @click="openCreate">新增用户</n-button>
          <n-button :disabled="!selectedRow" @click="() => void openEdit()">编辑</n-button>
          <n-button :disabled="!selectedRow" type="error" ghost @click="() => void deleteSelected()">删除</n-button>
        </div>
      </template>

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        @update:checked-row-keys="handleCheckedRowKeys"
      />
    </n-card>

    <n-modal
      v-model:show="formModalVisible"
      preset="card"
      :title="formMode === 'create' ? '新增用户' : '编辑用户'"
      style="width: 760px;"
    >
      <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="110">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="formValue.username" :disabled="formMode === 'edit'" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="formValue.password"
            type="password"
            show-password-on="mousedown"
            :placeholder="formMode === 'create' ? '请输入密码' : '留空表示不修改密码'"
          />
        </n-form-item>
        <n-form-item path="role" label="角色">
          <n-radio-group v-model:value="formValue.role">
            <n-space>
              <n-radio v-for="item in roleOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item path="permissions" label="执行器权限">
          <n-checkbox-group v-model:value="formValue.permissions" :disabled="formValue.role === 1">
            <n-space vertical>
              <n-checkbox v-for="group in groups" :key="group.id" :value="String(group.id)">
                {{ group.title }} : {{ group.appname }}
              </n-checkbox>
            </n-space>
          </n-checkbox-group>
        </n-form-item>
      </n-form>
      <template #action>
        <div class="table-actions">
          <n-button @click="formModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitForm">保存</n-button>
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
  NCheckbox,
  NCheckboxGroup,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NTag,
  useDialog,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type PaginationProps,
  type SelectOption
} from 'naive-ui';
import { createUser, deleteUser, fetchUserDetail, fetchUserMetadata, fetchUsers, updateUser, type UserInfo, type UserMetadata } from '@/api/users';

const dialog = useDialog();
const message = useMessage();
const loading = ref(false);
const rows = ref<UserInfo[]>([]);
const checkedRowKeys = ref<number[]>([]);
const formRef = ref<FormInst | null>(null);
const formModalVisible = ref(false);
const submitting = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const metadata = ref<UserMetadata | null>(null);

const filters = reactive({
  username: '',
  role: -1
});

const formValue = reactive({
  id: 0,
  username: '',
  password: '',
  role: 0,
  permissions: [] as string[]
});

const roleOptions = computed<SelectOption[]>(() =>
  (metadata.value?.roles || []).map((item) => ({
    label: item.label,
    value: item.value
  }))
);

const roleOptionsWithAll = computed<SelectOption[]>(() => [
  { label: '全部角色', value: -1 },
  ...roleOptions.value
]);

const groups = computed(() => metadata.value?.groups || []);

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['blur', 'input'] },
    { min: 4, max: 20, message: '用户名长度限制为 4~20', trigger: ['blur', 'input'] },
    {
      validator: (_, value: string) => /^[a-z][a-z0-9]*$/.test(value),
      message: '用户名需以小写字母开头，只能包含小写字母和数字',
      trigger: ['blur', 'input']
    }
  ],
  password: [
    {
      validator: () => {
        const value = formValue.password.trim();
        if (formMode.value === 'edit' && !value) {
          return true;
        }
        if (value.length < 4 || value.length > 20) {
          return new Error('密码长度限制为 4~20');
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

const columns: DataTableColumns<UserInfo> = [
  { type: 'selection', multiple: false },
  {
    title: '用户名',
    key: 'username',
    minWidth: 180
  },
  {
    title: '密码',
    key: 'password',
    width: 120,
    render: () => '******'
  },
  {
    title: '角色',
    key: 'role',
    width: 140,
    render: (row) =>
      h(
        NTag,
        { type: row.role === 1 ? 'warning' : 'info', round: true },
        { default: () => roleOptions.value.find((item) => item.value === row.role)?.label || String(row.role) }
      )
  },
  {
    title: '执行器权限',
    key: 'permission',
    minWidth: 280,
    render: (row) => {
      if (row.role === 1) {
        return '全部';
      }
      if (!row.permission) {
        return '空';
      }
      const permissionSet = new Set(row.permission.split(',').filter(Boolean));
      return groups.value
        .filter((group) => permissionSet.has(String(group.id)))
        .map((group) => group.title)
        .join(' / ') || '空';
    }
  }
];

function rowKey(row: UserInfo) {
  return row.id;
}

function handleCheckedRowKeys(keys: Array<string | number>) {
  checkedRowKeys.value = keys.map((item) => Number(item));
}

function resetForm() {
  formValue.id = 0;
  formValue.username = '';
  formValue.password = '';
  formValue.role = 0;
  formValue.permissions = [];
}

async function loadMetadata() {
  const response = await fetchUserMetadata();
  if (response.code !== 200) {
    throw new Error(response.msg || '用户元数据加载失败');
  }
  metadata.value = response.data;
}

async function loadData() {
  loading.value = true;
  try {
    const response = await fetchUsers({
      offset: ((pagination.page as number) - 1) * (pagination.pageSize as number),
      pagesize: pagination.pageSize as number,
      username: filters.username,
      role: filters.role
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '用户列表加载失败');
    }
    rows.value = response.data.data;
    pagination.itemCount = response.data.total;
    checkedRowKeys.value = [];
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '用户列表加载失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.page = 1;
  void loadData();
}

function reset() {
  filters.username = '';
  filters.role = -1;
  pagination.page = 1;
  void loadData();
}

function openCreate() {
  formMode.value = 'create';
  resetForm();
  formModalVisible.value = true;
}

async function openEdit() {
  if (!selectedRow.value) {
    return;
  }
  const response = await fetchUserDetail(selectedRow.value.id);
  if (response.code !== 200) {
    message.error(response.msg || '用户详情加载失败');
    return;
  }
  const user = response.data;
  formMode.value = 'edit';
  formValue.id = user.id;
  formValue.username = user.username;
  formValue.password = '';
  formValue.role = user.role;
  formValue.permissions = user.permission ? user.permission.split(',').filter(Boolean) : [];
  formModalVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const payload: Record<string, string | number> = {
      username: formValue.username.trim(),
      role: formValue.role,
      permission: formValue.role === 1 ? '' : formValue.permissions.join(',')
    };
    if (formMode.value === 'edit') {
      payload.id = formValue.id;
      if (formValue.password.trim()) {
        payload.password = formValue.password.trim();
      }
      const response = await updateUser(payload);
      if (response.code !== 200) {
        throw new Error(response.msg || '更新失败');
      }
    } else {
      payload.password = formValue.password.trim();
      const response = await createUser(payload);
      if (response.code !== 200) {
        throw new Error(response.msg || '创建失败');
      }
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

async function deleteSelected() {
  if (!selectedRow.value) {
    return;
  }
  dialog.warning({
    title: '删除用户',
    content: `确认删除用户 ${selectedRow.value.username} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await deleteUser(selectedRow.value!.id);
      if (response.code !== 200) {
        message.error(response.msg || '删除失败');
        return;
      }
      message.success('删除成功');
      await loadData();
    }
  });
}

watch(
  () => formValue.role,
  (role) => {
    if (role === 1) {
      formValue.permissions = [];
    }
  }
);

onMounted(async () => {
  try {
    await loadMetadata();
    await loadData();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '用户页初始化失败');
  }
});
</script>
