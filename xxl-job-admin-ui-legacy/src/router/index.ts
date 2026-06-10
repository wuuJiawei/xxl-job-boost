import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useTabsStore } from '@/stores/tabs';
import DashboardView from '@/views/DashboardView.vue';
import ExecutorGroupsView from '@/views/ExecutorGroupsView.vue';
import HelpView from '@/views/HelpView.vue';
import JobCodeView from '@/views/JobCodeView.vue';
import JobsView from '@/views/JobsView.vue';
import LogDetailView from '@/views/LogDetailView.vue';
import LogsView from '@/views/LogsView.vue';
import LoginView from '@/views/LoginView.vue';
import MainLayout from '@/layouts/MainLayout.vue';
import UsersView from '@/views/UsersView.vue';

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: {
            tabKey: 'dashboard',
            tabLabel: 'Dashboard',
            closable: false
          }
        },
        {
          path: 'executors',
          name: 'executors',
          component: ExecutorGroupsView,
          meta: {
            tabKey: 'executors',
            tabLabel: '执行器管理',
            closable: true,
            adminOnly: true
          }
        },
        {
          path: 'jobs',
          name: 'jobs',
          component: JobsView,
          meta: {
            tabKey: 'jobs',
            tabLabel: '任务管理',
            closable: true
          }
        },
        {
          path: 'jobs/:jobId/code',
          name: 'job-code',
          component: JobCodeView,
          meta: {
            closable: true
          }
        },
        {
          path: 'logs',
          name: 'logs',
          component: LogsView,
          meta: {
            tabKey: 'logs',
            tabLabel: '执行日志',
            closable: true
          }
        },
        {
          path: 'logs/:logId',
          name: 'log-detail',
          component: LogDetailView,
          meta: {
            closable: true
          }
        },
        {
          path: 'users',
          name: 'users',
          component: UsersView,
          meta: {
            tabKey: 'users',
            tabLabel: '用户管理',
            closable: true,
            adminOnly: true
          }
        },
        {
          path: 'help',
          name: 'help',
          component: HelpView,
          meta: {
            tabKey: 'help',
            tabLabel: '帮助',
            closable: true
          }
        }
      ]
    }
  ]
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  await authStore.bootstrap();
  if (to.meta.public) {
    if (authStore.isAuthenticated) {
      return { name: 'dashboard' };
    }
    return true;
  }

  if (!authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.meta.adminOnly && !authStore.isAdmin) {
    return { name: 'dashboard' };
  }

  return true;
});

router.afterEach((to) => {
  if (to.meta.public) {
    return;
  }

  const tabsStore = useTabsStore();
  let key = String(to.meta.tabKey || to.name || to.fullPath);
  let label = String(to.meta.tabLabel || to.name || 'Untitled');

  if (to.name === 'log-detail') {
    const logId = String(to.params.logId || '');
    key = `log-detail-${logId}`;
    label = `日志 #${logId}`;
  }

  if (to.name === 'job-code') {
    const jobId = String(to.params.jobId || '');
    key = `job-code-${jobId}`;
    label = `代码 #${jobId}`;
  }

  tabsStore.ensureTab({
    key,
    label,
    fullPath: to.fullPath,
    closable: Boolean(to.meta.closable)
  });
});
