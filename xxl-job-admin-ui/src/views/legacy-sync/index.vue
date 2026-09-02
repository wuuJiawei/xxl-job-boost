<script setup lang="ts">
import { computed, ref } from 'vue';
import { NAlert, NButton, NCard, NCheckbox, NForm, NFormItem, NInput, NTable, useMessage } from 'naive-ui';
import { importLegacyJobs, previewLegacyJobs, type LegacyJob, type LegacySyncPreview } from '@/api/legacy-sync';

defineOptions({ name: 'legacy-sync' });

const message = useMessage();
const loading = ref(false);
const importing = ref(false);
const preview = ref<LegacySyncPreview | null>(null);
const selectedIds = ref<number[]>([]);
const config = ref({ sourceUrl: '', username: '', password: '' });

const allSelected = computed(() => Boolean(preview.value?.jobs.length) && selectedIds.value.length === preview.value?.jobs.length);

function toggleAll(checked: boolean) {
  selectedIds.value = checked ? (preview.value?.jobs.map(job => job.id) || []) : [];
}

function toggleJob(job: LegacyJob, checked: boolean) {
  selectedIds.value = checked
    ? [...selectedIds.value, job.id]
    : selectedIds.value.filter(id => id !== job.id);
}

async function loadPreview() {
  if (!config.value.sourceUrl.trim() || !config.value.username.trim() || !config.value.password) {
    message.error('请填写旧平台地址、账号和密码');
    return;
  }
  loading.value = true;
  try {
    const response = await previewLegacyJobs({ ...config.value, sourceUrl: config.value.sourceUrl.trim(), username: config.value.username.trim() });
    if (response.code !== 200) throw new Error(response.msg || '旧平台预览失败');
    preview.value = response.data;
    selectedIds.value = response.data.jobs.map(job => job.id);
    message.success(`已读取 ${response.data.groups.length} 个执行器、${response.data.jobs.length} 个任务`);
  } catch (error) {
    message.error((error as Error).message || '旧平台预览失败');
  } finally {
    loading.value = false;
  }
}

async function importSelected() {
  if (!selectedIds.value.length) {
    message.error('请至少选择一个任务');
    return;
  }
  importing.value = true;
  try {
    const response = await importLegacyJobs(config.value, selectedIds.value);
    if (response.code !== 200) throw new Error(response.msg || '任务导入失败');
    message.success(`导入完成：新增 ${response.data.jobsCreated} 个，跳过 ${response.data.jobsSkipped} 个`);
    if (response.data.warning) message.warning(response.data.warning, { duration: 8000 });
    await loadPreview();
  } catch (error) {
    message.error((error as Error).message || '任务导入失败');
  } finally {
    importing.value = false;
  }
}
</script>

<template>
  <div class="page-stack">
    <n-card :bordered="false" title="旧平台任务同步">
      <n-form label-placement="left" label-width="110">
        <n-form-item label="旧平台地址">
          <n-input v-model:value="config.sourceUrl" placeholder="例如 http://127.0.0.1:8080/xxl-job-admin/" />
        </n-form-item>
        <n-form-item label="旧平台账号">
          <n-input v-model:value="config.username" autocomplete="username" placeholder="旧 admin 用户名" />
        </n-form-item>
        <n-form-item label="旧平台密码">
          <n-input v-model:value="config.password" type="password" show-password-on="click" autocomplete="current-password" />
        </n-form-item>
        <div class="table-actions">
          <n-button type="primary" :loading="loading" @click="loadPreview">连接并预览</n-button>
          <n-button :disabled="!preview || !selectedIds.length" :loading="importing" @click="importSelected">导入选中任务</n-button>
        </div>
      </n-form>
      <n-alert type="warning" class="mt-16px">
        账号密码只用于本次请求，不会保存。导入采用只新增策略，已有 AppName + JobHandler 会跳过；所有新任务默认停用，避免新旧平台重复执行。
      </n-alert>
    </n-card>

    <n-card v-if="preview" :bordered="false" :title="`任务预览（已选 ${selectedIds.length} / ${preview.jobs.length}）`">
      <n-table :bordered="false" :single-line="false">
        <thead>
          <tr>
            <th><n-checkbox :checked="allSelected" @update:checked="toggleAll" /></th>
            <th>执行器</th>
            <th>任务描述</th>
            <th>JobHandler</th>
            <th>调度配置</th>
            <th>参数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in preview.jobs" :key="job.id">
            <td><n-checkbox :checked="selectedIds.includes(job.id)" @update:checked="checked => toggleJob(job, checked)" /></td>
            <td>{{ job.appname }}<br /><span class="text-12px text-gray-500">{{ job.groupTitle }}</span></td>
            <td>{{ job.jobDesc }}</td>
            <td>{{ job.executorHandler }}</td>
            <td>{{ job.scheduleType }} / {{ job.scheduleConf || '-' }}</td>
            <td class="max-w-240px truncate">{{ job.executorParam || '-' }}</td>
          </tr>
        </tbody>
      </n-table>
    </n-card>
  </div>
</template>
