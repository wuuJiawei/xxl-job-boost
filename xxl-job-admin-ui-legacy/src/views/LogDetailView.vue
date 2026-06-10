<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div class="log-detail-header">
        <div>
          <div class="section-kicker">Rolling Log</div>
          <h2>{{ meta?.jobDesc || `日志 #${logId}` }}</h2>
          <p>
            <span>日志 ID：{{ logId }}</span>
            <span class="muted-text">任务 ID：{{ meta?.jobId ?? '-' }}</span>
            <span class="muted-text">触发时间：{{ formatDateTime(meta?.triggerTime) || '-' }}</span>
          </p>
        </div>
        <div class="table-actions">
          <n-button @click="reload">刷新</n-button>
          <n-button quaternary @click="goBack">返回日志列表</n-button>
        </div>
      </div>
    </div>

    <n-card :bordered="false">
      <template #header>
        <div class="table-header">
          <div class="table-title">滚动日志输出</div>
          <div class="table-subtitle">
            <span>执行地址：{{ meta?.executorAddress || '未知' }}</span>
            <span class="muted-text">状态：{{ statusText }}</span>
          </div>
        </div>
      </template>
      <pre class="log-stream"><code v-html="logHtml"></code></pre>
      <div v-if="running" class="log-stream-running">正在持续拉取日志...</div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NCard, useMessage } from 'naive-ui';
import { fetchLogDetailMeta, type LogDetailMeta } from '@/api/admin-next';
import { fetchLogChunk } from '@/api/logs';

const route = useRoute();
const router = useRouter();
const message = useMessage();
const meta = ref<LogDetailMeta | null>(null);
const logHtml = ref('');
const running = ref(false);
const fromLineNum = ref(1);
const pullFailCount = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const logId = computed(() => Number(route.params.logId));

const statusText = computed(() => {
  if (!meta.value) {
    return '初始化中';
  }
  if (meta.value.handleCode === 200) {
    return '执行成功';
  }
  if (meta.value.handleCode === 502) {
    return '执行超时';
  }
  if (meta.value.handleCode > 0) {
    return '执行失败';
  }
  if (meta.value.triggerCode !== 200) {
    return '触发失败';
  }
  return '执行中';
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

function stopPolling() {
  running.value = false;
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

async function pullLog() {
  if (!logId.value) {
    return;
  }
  if (pullFailCount.value > 20) {
    stopPolling();
    logHtml.value += '<br><span style="color:#d03050;">[拉取失败次数过多，已停止]</span>';
    return;
  }
  try {
    const response = await fetchLogChunk(logId.value, fromLineNum.value);
    if (response.code !== 200) {
      pullFailCount.value += 1;
      logHtml.value += `<br><span style="color:#d03050;">[Rolling Log Error] ${escapeHtml(response.msg || '日志拉取失败')}</span>`;
      return;
    }
    const chunk = response.data;
    if (!chunk) {
      pullFailCount.value += 1;
      return;
    }
    if (fromLineNum.value !== chunk.fromLineNum) {
      pullFailCount.value += 1;
      return;
    }
    if (fromLineNum.value <= chunk.toLineNum) {
      fromLineNum.value = chunk.toLineNum + 1;
      logHtml.value += chunk.logContent || '';
      pullFailCount.value = 0;
      requestAnimationFrame(() => {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
      });
    }
    if (chunk.end) {
      stopPolling();
      logHtml.value += '<br><span style="color:#2f9e44;">[Rolling Log End]</span>';
    }
  } catch (error) {
    pullFailCount.value += 1;
    const err = error as Error;
    logHtml.value += `<br><span style="color:#d03050;">[Rolling Log Error] ${escapeHtml(err.message || '日志拉取失败')}</span>`;
  }
}

function startPolling() {
  stopPolling();
  running.value = true;
  timer = setInterval(() => {
    void pullLog();
  }, 3000);
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

async function bootstrap() {
  if (!logId.value) {
    return;
  }
  stopPolling();
  meta.value = null;
  logHtml.value = '';
  fromLineNum.value = 1;
  pullFailCount.value = 0;

  const metaResponse = await fetchLogDetailMeta(logId.value);
  if (metaResponse.code !== 200) {
    throw new Error(metaResponse.msg || '日志详情加载失败');
  }
  meta.value = metaResponse.data;

  if (meta.value.triggerCode !== 200 && meta.value.handleCode === 0) {
    logHtml.value = '<span style="color:#d03050;">[调度失败，未进入执行阶段]</span>';
    running.value = false;
    return;
  }

  await pullLog();

  if (meta.value.handleCode > 0) {
    running.value = false;
    return;
  }

  startPolling();
}

async function reload() {
  try {
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志详情刷新失败');
  }
}

function goBack() {
  router.push({ name: 'logs' });
}

watch(
  () => route.params.logId,
  async () => {
    try {
      await bootstrap();
    } catch (error) {
      const err = error as Error;
      message.error(err.message || '日志详情切换失败');
    }
  }
);

onMounted(async () => {
  try {
    await bootstrap();
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '日志详情初始化失败');
  }
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>
