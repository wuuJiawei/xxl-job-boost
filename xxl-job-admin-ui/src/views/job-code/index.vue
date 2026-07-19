<template>
  <div class="job-code-page">
    <n-card :bordered="false" class="job-code-editor-card">
      <template #header>
        <div class="job-code-page-header">
          <div class="job-code-identity">
            <div class="job-code-title">{{ detail?.jobDesc || `任务 #${jobId}` }}</div>
            <div class="job-code-meta">
              <span>#{{ jobId }}</span>
              <n-tag size="small" :bordered="false">{{ detail?.glueTypeLabel || '未知类型' }}</n-tag>
              <span>{{ activeVersion?.updatedAt ? formatDateTime(activeVersion.updatedAt) : '暂无更新时间' }}</span>
              <n-tag v-if="isDirty" size="small" type="warning" :bordered="false">未保存</n-tag>
            </div>
          </div>
          <div class="table-actions job-code-page-actions">
            <n-button @click="versionDrawerVisible = true">
              <template #icon><SvgIcon icon="mdi:history" /></template>
              版本 {{ versionOptions.length }}
            </n-button>
            <n-button @click="reload">
              <template #icon><SvgIcon icon="mdi:refresh" /></template>
              刷新
            </n-button>
            <n-button quaternary @click="goBack">
              <template #icon><SvgIcon icon="mdi:arrow-left" /></template>
              返回
            </n-button>
          </div>
        </div>
      </template>
      <CodeEditor v-model="glueSource" :glue-type="detail?.glueType || ''" @save="submit" />
      <div class="job-code-footer">
        <n-input
          v-model:value="glueRemark"
          placeholder="请输入本次保存备注，长度 4~100"
          maxlength="100"
        />
        <div class="table-actions">
          <n-button @click="resetToActiveVersion">重置</n-button>
          <n-button type="primary" :loading="saving" :disabled="!isDirty" @click="submit">保存</n-button>
        </div>
      </div>
    </n-card>

    <n-drawer v-model:show="versionDrawerVisible" placement="right" :width="360">
      <n-drawer-content title="代码版本" closable>
        <div class="job-code-version-list">
          <button
            v-for="item in versionOptions"
            :key="item.key"
            type="button"
            class="job-code-version"
            :class="{ active: item.key === activeVersionKey }"
            @click="switchVersion(item.key)"
          >
            <span class="job-code-version-title">{{ item.title }}</span>
            <span class="job-code-version-meta">{{ item.meta }}</span>
          </button>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
import { NButton, NCard, NDrawer, NDrawerContent, NInput, NTag, useDialog, useMessage } from 'naive-ui';
import {
  fetchJobCodeDetail,
  saveJobCode,
  type JobCodeDetail,
  type JobCodeVersion
} from '@/api/job-code';
import CodeEditor from './modules/code-editor.vue';

defineOptions({
  name: 'job-code'
});

const route = useRoute();
const router = useRouter();
const dialog = useDialog();
const message = useMessage();

const detail = ref<JobCodeDetail | null>(null);
const activeVersionKey = ref('current');
const glueSource = ref('');
const glueRemark = ref('');
const saving = ref(false);
const versionDrawerVisible = ref(false);

const jobId = computed(() => Number(route.query.jobId || 0));

const versionOptions = computed(() => {
  const options: Array<{ key: string; title: string; meta: string }> = [];
  if (detail.value?.currentVersion) {
    options.push({
      key: 'current',
      title: '当前版本',
      meta: detail.value.currentVersion.glueRemark || '无备注'
    });
  }
  (detail.value?.historyVersions || []).forEach((item) => {
    options.push({
      key: `history-${item.id}`,
      title: formatDateTime(item.updatedAt) || `版本 #${item.id}`,
      meta: item.glueRemark || '无备注'
    });
  });
  return options;
});

const activeVersion = computed<JobCodeVersion | null>(() => {
  if (!detail.value) {
    return null;
  }
  if (activeVersionKey.value === 'current') {
    return detail.value.currentVersion;
  }
  const id = Number(activeVersionKey.value.replace('history-', ''));
  return detail.value.historyVersions.find((item) => item.id === id) || null;
});

const isDirty = computed(
  () =>
    glueSource.value !== (activeVersion.value?.glueSource || '') ||
    glueRemark.value !== (activeVersion.value?.glueRemark || '')
);

function formatDateTime(value: string | null | undefined) {
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

function syncEditor(version: JobCodeVersion | null) {
  glueSource.value = version?.glueSource || '';
  glueRemark.value = version?.glueRemark || '';
}

function applyVersion(key: string) {
  activeVersionKey.value = key;
  syncEditor(activeVersion.value);
  versionDrawerVisible.value = false;
}

function switchVersion(key: string) {
  if (key === activeVersionKey.value) {
    versionDrawerVisible.value = false;
    return;
  }
  if (!isDirty.value) {
    applyVersion(key);
    return;
  }
  dialog.warning({
    title: '放弃未保存修改',
    content: '切换版本会丢失当前未保存的代码和备注。',
    positiveText: '继续切换',
    negativeText: '取消',
    onPositiveClick: () => applyVersion(key)
  });
}

function resetToActiveVersion() {
  syncEditor(activeVersion.value);
}

async function bootstrap() {
  if (!jobId.value) {
    return;
  }
  const response = await fetchJobCodeDetail(jobId.value);
  if (response.code !== 200) {
    throw new Error(response.msg || '任务代码加载失败');
  }
  detail.value = response.data;
  activeVersionKey.value = 'current';
  syncEditor(detail.value.currentVersion);
}

async function reload() {
  try {
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '任务代码刷新失败');
  }
}

async function submit() {
  if (!jobId.value || saving.value || !isDirty.value) {
    return;
  }
  if (!glueSource.value.trim()) {
    message.error('请输入 GLUE 源代码');
    return;
  }
  if (glueRemark.value.trim().length < 4 || glueRemark.value.trim().length > 100) {
    message.error('保存备注长度限制为 4~100');
    return;
  }

  saving.value = true;
  try {
    const response = await saveJobCode(jobId.value, glueSource.value, glueRemark.value.trim());
    if (response.code !== 200) {
      throw new Error(response.msg || '保存失败');
    }
    message.success('保存成功');
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push({ name: 'jobs' });
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!isDirty.value) {
    return;
  }
  event.preventDefault();
  event.returnValue = '';
}

onBeforeRouteLeave((_to, _from, next) => {
  if (!isDirty.value) {
    next();
    return;
  }
  let resolved = false;
  const finishNavigation = (leave: boolean) => {
    if (resolved) {
      return;
    }
    resolved = true;
    if (leave) {
      next();
    } else {
      next(false);
    }
  };
  dialog.warning({
    title: '离开代码编辑',
    content: '当前代码尚未保存，离开后修改会丢失。',
    positiveText: '离开',
    negativeText: '继续编辑',
    onPositiveClick: () => finishNavigation(true),
    onNegativeClick: () => finishNavigation(false),
    onClose: () => finishNavigation(false)
  });
});

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload);
  try {
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '任务代码页初始化失败');
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});
</script>
