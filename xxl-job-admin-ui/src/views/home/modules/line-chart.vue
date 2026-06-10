<script setup lang="ts">
import { watch } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

defineOptions({
  name: 'LineChart'
});

interface ChartInfo {
  triggerDayList: string[];
  triggerDayCountRunningList: number[];
  triggerDayCountSucList: number[];
  triggerDayCountFailList: number[];
}

const props = defineProps<{
  chartData: ChartInfo | null;
  loading: boolean;
}>();

const appStore = useAppStore();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.xxlJob.dashboard.trend'),
    left: 'center',
    top: 0,
    textStyle: {
      fontSize: 16,
      fontWeight: 600
    }
  },
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: [$t('page.xxlJob.dashboard.success'), $t('page.xxlJob.dashboard.failure'), $t('page.xxlJob.dashboard.running')],
    top: 28
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '22%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [] as string[]
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      color: '#22c55e',
      name: $t('page.xxlJob.dashboard.success'),
      type: 'line',
      smooth: true,
      data: [] as number[]
    },
    {
      color: '#ef4444',
      name: $t('page.xxlJob.dashboard.failure'),
      type: 'line',
      smooth: true,
      data: [] as number[]
    },
    {
      color: '#8b5cf6',
      name: $t('page.xxlJob.dashboard.running'),
      type: 'line',
      smooth: true,
      data: [] as number[]
    }
  ]
}));

function syncChart() {
  updateOptions(opts => {
    opts.xAxis.data = props.chartData?.triggerDayList ?? [];
    opts.series[0].data = props.chartData?.triggerDayCountSucList ?? [];
    opts.series[1].data = props.chartData?.triggerDayCountFailList ?? [];
    opts.series[2].data = props.chartData?.triggerDayCountRunningList ?? [];
    return opts;
  });
}

watch(
  () => [props.chartData, props.loading, appStore.locale],
  () => {
    syncChart();
  },
  { deep: true, immediate: true }
);
</script>

<template>
  <NCard :bordered="false" class="card-wrapper">
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </NCard>
</template>
