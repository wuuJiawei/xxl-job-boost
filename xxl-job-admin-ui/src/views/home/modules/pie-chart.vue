<script setup lang="ts">
import { watch } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

defineOptions({
  name: 'PieChart'
});

interface ChartInfo {
  triggerCountRunningTotal: number;
  triggerCountSucTotal: number;
  triggerCountFailTotal: number;
}

const props = defineProps<{
  chartData: ChartInfo | null;
  loading: boolean;
}>();

const appStore = useAppStore();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.xxlJob.dashboard.distribution'),
    left: 'center',
    top: 0,
    textStyle: {
      fontSize: 16,
      fontWeight: 600
    }
  },
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: '2%',
    left: 'center'
  },
  series: [
    {
      color: ['#22c55e', '#ef4444', '#8b5cf6'],
      name: $t('page.xxlJob.dashboard.distribution'),
      type: 'pie',
      radius: ['45%', '72%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '12'
        }
      },
      labelLine: {
        show: false
      },
      data: [] as { name: string; value: number }[]
    }
  ]
}));

function syncChart() {
  updateOptions(opts => {
    opts.series[0].data = [
      {
        name: $t('page.xxlJob.dashboard.success'),
        value: props.chartData?.triggerCountSucTotal ?? 0
      },
      {
        name: $t('page.xxlJob.dashboard.failure'),
        value: props.chartData?.triggerCountFailTotal ?? 0
      },
      {
        name: $t('page.xxlJob.dashboard.running'),
        value: props.chartData?.triggerCountRunningTotal ?? 0
      }
    ];
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
