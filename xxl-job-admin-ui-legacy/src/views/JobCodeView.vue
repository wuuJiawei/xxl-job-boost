<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="log-detail-header">
        <div>
          <div class="section-kicker">Web IDE</div>
          <h2>{{ detail?.jobDesc || `任务 #${jobId}` }}</h2>
          <p>
            <span>任务 ID：{{ jobId }}</span>
            <span class="muted-text">运行模式：{{ detail?.glueTypeLabel || '-' }}</span>
            <span class="muted-text">当前备注：{{ activeVersion?.glueRemark || '-' }}</span>
          </p>
        </div>
        <div class="table-actions">
          <n-button @click="reload">刷新</n-button>
          <n-button quaternary @click="goBack">返回任务列表</n-button>
        </div>
      </div>
    </div>

    <div class="job-code-layout">
      <n-card :bordered="false" class="job-code-sidebar">
        <template #header>
          <div class="table-header">
            <div class="table-title">版本切换</div>
            <div class="table-subtitle">保留当前版本和最近历史版本，便于回看与回滚编辑。</div>
          </div>
        </template>
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
      </n-card>

      <n-card :bordered="false" class="job-code-editor-card">
        <template #header>
          <div class="table-header">
            <div class="table-title">代码编辑</div>
            <div class="table-subtitle">当前版本会在保存时生成一条新的 GLUE 历史记录。</div>
          </div>
        </template>
        <div class="job-code-toolbar">
          <n-tag round :bordered="false">{{ detail?.glueTypeLabel || '未知类型' }}</n-tag>
          <span class="muted-text">{{ activeVersion?.updatedAt ? formatDateTime(activeVersion.updatedAt) : '暂无更新时间' }}</span>
        </div>
        <textarea v-model="glueSource" class="job-code-editor" spellcheck="false" />
        <div class="job-code-footer">
          <n-input
            v-model:value="glueRemark"
            placeholder="请输入本次保存备注，长度 4~100"
            maxlength="100"
          />
          <div class="table-actions">
            <n-button @click="resetToActiveVersion">重置当前编辑</n-button>
            <n-button type="primary" :loading="saving" @click="submit">保存</n-button>
          </div>
        </div>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NCard, NInput, NTag, useMessage } from 'naive-ui';
import {
  fetchJobCodeDetail,
  saveJobCode,
  type JobCodeDetail,
  type JobCodeVersion
} from '@/api/job-code';

const route = useRoute();
const router = useRouter();
const message = useMessage();

const detail = ref<JobCodeDetail | null>(null);
const activeVersionKey = ref('current');
const glueSource = ref('');
const glueRemark = ref('');
const saving = ref(false);

const jobId = computed(() => Number(route.params.jobId));

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

function switchVersion(key: string) {
  activeVersionKey.value = key;
  syncEditor(activeVersion.value);
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
  if (!jobId.value) {
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

onMounted(async () => {
  try {
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '任务代码页初始化失败');
  }
});
</script>
