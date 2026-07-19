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
    <NGrid :x-gap="gap" :y-gap="20" responsive="screen" item-responsive>
      <NGi span="24 m:6">
        <div class="h-full flex-y-center">
          <h3 class="text-18px font-semibold">欢迎回来，{{ authStore.userInfo.userName }}</h3>
        </div>
      </NGi>
      <NGi span="24 m:18">
        <NGrid cols="2 m:4" responsive="screen" :x-gap="12" :y-gap="16">
          <NGi v-for="item in props.stats" :key="item.id">
            <NStatistic class="text-center" v-bind="item" />
          </NGi>
        </NGrid>
      </NGi>
    </NGrid>
  </NCard>
</template>
