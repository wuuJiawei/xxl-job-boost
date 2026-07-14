<template>
  <div class="page-stack">
    <n-card :bordered="false" class="help-card">
      <div class="help-stack">
        <div class="table-header">
          <div class="table-title">{{ helpInfo?.productName || 'XXL-JOB' }}</div>
          <div class="table-subtitle">文档与项目资源入口</div>
        </div>
        <div class="help-links">
          <a class="help-link" :href="helpInfo?.documentUrl" target="_blank" rel="noopener noreferrer">
            XXL-JOB 官方文档
          </a>
          <a class="help-link" :href="helpInfo?.githubUrl" target="_blank" rel="noopener noreferrer">
            XXL-JOB 官方仓库
          </a>
          <a class="help-link" :href="helpInfo?.boostGithubUrl" target="_blank" rel="noopener noreferrer">
            XXL-JOB Boost GitHub 仓库
          </a>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NCard, useMessage } from 'naive-ui';
import { fetchHelpInfo, type HelpInfo } from '@/api/help';

defineOptions({
  name: 'help'
});

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
