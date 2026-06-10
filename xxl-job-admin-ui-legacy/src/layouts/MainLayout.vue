<template>
  <n-layout has-sider class="shell">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="88"
      :width="260"
      show-trigger
    >
      <div class="brand">
        <div class="brand-mark">XB</div>
        <div>
          <div class="brand-title">XXL-JOB Boost</div>
          <div class="brand-subtitle">Admin Next</div>
        </div>
      </div>
      <n-menu :options="menuOptions" :value="selectedKey" @update:value="handleSelect" />
    </n-layout-sider>
    <n-layout>
      <n-layout-header bordered class="topbar">
        <div>
          <div class="page-kicker">Phase 1</div>
          <div class="page-title">Modern Admin Console</div>
        </div>
        <div class="topbar-actions">
          <n-button quaternary @click="goLegacy">旧版控制台</n-button>
          <n-button tertiary type="error" @click="handleLogout">退出</n-button>
        </div>
      </n-layout-header>
      <div class="tabbar">
        <button
          v-for="tab in tabsStore.items"
          :key="tab.key"
          type="button"
          class="tab-chip"
          :class="{ active: tab.key === tabsStore.activeKey }"
          @click="openTab(tab.fullPath)"
        >
          <span>{{ tab.label }}</span>
          <span
            v-if="tab.closable"
            class="tab-chip-close"
            @click.stop="closeTab(tab.key)"
          >
            ×
          </span>
        </button>
      </div>
      <n-layout-content content-style="padding: 24px;">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h } from 'vue';
import {
  NButton,
  NIcon,
  NLayout,
  NLayoutContent,
  NLayoutHeader,
  NLayoutSider,
  NMenu,
  useMessage
} from 'naive-ui';
import type { MenuOption } from 'naive-ui';
import { BarChartOutline, BriefcaseOutline, DocumentTextOutline, GridOutline, HelpCircleOutline, PeopleOutline } from '@vicons/ionicons5';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useTabsStore } from '@/stores/tabs';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const tabsStore = useTabsStore();
const message = useMessage();

function renderIcon(icon: object) {
  return () => h(NIcon, null, { default: () => h(icon) });
}

const menuOptions = computed<MenuOption[]>(() => {
  const options: MenuOption[] = [
    {
      label: 'Dashboard',
      key: 'dashboard',
      icon: renderIcon(GridOutline)
    }
  ];

  if (authStore.isAdmin) {
    options.push({
      label: '执行器管理',
      key: 'executors',
      icon: renderIcon(BarChartOutline)
    });
  }

  options.push(
    {
      label: '任务管理',
      key: 'jobs',
      icon: renderIcon(BriefcaseOutline)
    },
    {
      label: '执行日志',
      key: 'logs',
      icon: renderIcon(DocumentTextOutline)
    }
  );

  if (authStore.isAdmin) {
    options.push({
      label: '用户管理',
      key: 'users',
      icon: renderIcon(PeopleOutline)
    });
  }

  options.push({
    label: '帮助',
    key: 'help',
    icon: renderIcon(HelpCircleOutline)
  });

  return options;
});

const selectedKey = computed(() => {
  if (route.name === 'logs' || route.name === 'log-detail') {
    return 'logs';
  }
  if (route.name === 'executors') {
    return 'executors';
  }
  if (route.name === 'jobs') {
    return 'jobs';
  }
  if (route.name === 'users') {
    return 'users';
  }
  if (route.name === 'help') {
    return 'help';
  }
  return 'dashboard';
});

function handleSelect(key: string) {
  if (key === 'dashboard' || key === 'executors' || key === 'jobs' || key === 'logs' || key === 'users' || key === 'help') {
    router.push({ name: key });
    return;
  }
  message.info('下一阶段会继续补任务和日志页面。');
}

function openTab(fullPath: string) {
  router.push(fullPath);
}

function closeTab(key: string) {
  const target = tabsStore.close(key);
  router.push(target);
}

function goLegacy() {
  window.location.href = '/xxl-job-admin/';
}

async function handleLogout() {
  await authStore.signOut();
  tabsStore.reset();
  message.success('已退出登录');
  router.push({ name: 'login' });
}
</script>
