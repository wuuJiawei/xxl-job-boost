<script setup lang="ts">
import { createReusableTemplate } from '@vueuse/core';
import { useThemeStore } from '@/store/modules/theme';

defineOptions({
  name: 'CardData'
});

interface CardItem {
  key: string;
  title: string;
  value: number;
  hint: string;
  color: {
    start: string;
    end: string;
  };
  icon: string;
}

const props = defineProps<{
  items: CardItem[];
  title?: string;
}>();

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();
const themeStore = useThemeStore();

function getGradientColor(color: CardItem['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <div
      v-if="props.title || $slots.action"
      class="mb-12px flex items-center justify-between gap-12px max-sm:flex-wrap"
    >
      <div v-if="props.title" class="text-16px font-600">{{ props.title }}</div>
      <slot name="action" />
    </div>

    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div
        class="px-16px pb-10px pt-12px text-white"
        :style="{ backgroundImage: gradientColor, borderRadius: themeStore.themeRadius + 'px' }"
      >
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <NGrid cols="s:1 m:2 l:4" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in props.items" :key="item.key">
        <GradientBg :gradient-color="getGradientColor(item.color)" class="flex-1">
          <div class="flex items-start justify-between gap-12px">
            <div class="space-y-6px">
              <h3 class="text-16px">{{ item.title }}</h3>
              <p class="text-12px text-white/80">{{ item.hint }}</p>
            </div>
            <SvgIcon :icon="item.icon" class="text-30px" />
          </div>
          <div class="pt-18px text-30px font-700">
            <CountTo :start-value="0" :end-value="item.value" />
          </div>
        </GradientBg>
      </NGi>
    </NGrid>
  </NCard>
</template>
