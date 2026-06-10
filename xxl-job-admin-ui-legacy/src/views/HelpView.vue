<template>
  <div class="page-stack">
    <div class="dashboard-hero">
      <div>
        <div class="section-kicker">Help</div>
        <h2>保留原帮助页内容，先给新控制台一个稳定的文档入口。</h2>
        <p>
          当前版本提供官方仓库和官方文档的快捷跳转，后续再逐步补 Boost 自己的运维和接入文档。
        </p>
      </div>
    </div>

    <n-card :bordered="false" class="help-card">
      <div class="help-stack">
        <div class="table-header">
          <div class="table-title">{{ helpInfo?.productName || 'XXL-JOB' }}</div>
          <div class="table-subtitle">官方资源入口</div>
        </div>
        <div class="help-links">
          <a class="help-link" :href="helpInfo?.githubUrl" target="_blank" rel="noreferrer">Github</a>
          <a class="help-link" :href="helpInfo?.documentUrl" target="_blank" rel="noreferrer">官方文档</a>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NCard, useMessage } from 'naive-ui';
import { fetchHelpInfo, type HelpInfo } from '@/api/help';

const message = useMessage();
const helpInfo = ref<HelpInfo | null>(null);

onMounted(async () => {
  try {
    const response = await fetchHelpInfo();
    if (response.code !== 200) {
      throw new Error(response.msg || '帮助信息加载失败');
    }
    helpInfo.value = response.data;
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '帮助信息加载失败');
  }
});
</script>
