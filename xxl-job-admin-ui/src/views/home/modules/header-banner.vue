<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({
  name: 'HeaderBanner'
});

interface StatisticData {
  id: number;
  label: string;
  value: string;
}

const props = defineProps<{
  stats: StatisticData[];
}>();

const appStore = useAppStore();
const authStore = useAuthStore();

const gap = computed(() => (appStore.isMobile ? 0 : 16));
</script>

<template>
  <NCard :bordered="false" class="card-wrapper">
    <NGrid :x-gap="gap" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 s:24 m:18">
        <div class="flex-y-center">
          <div
            class="flex-center size-72px shrink-0 rd-1/2 bg-linear-to-br from-#1d4ed8 to-#14b8a6 text-28px text-white font-700"
          >
            {{ authStore.userInfo.userName.slice(0, 1).toUpperCase() }}
          </div>
          <div class="pl-12px">
            <h3 class="text-18px font-semibold">欢迎回来，{{ authStore.userInfo.userName }}</h3>
            <p class="text-#64748b leading-30px">XXL-JOB Boost 现代化控制台已切换到 Soybean 骨架联调模式。</p>
          </div>
        </div>
      </NGi>
      <NGi span="24 s:24 m:6">
        <NSpace :size="24" justify="end">
          <NStatistic v-for="item in props.stats" :key="item.id" class="whitespace-nowrap" v-bind="item" />
        </NSpace>
      </NGi>
    </NGrid>
  </NCard>
</template>
