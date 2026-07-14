import type { RouteRecordRaw } from 'vue-router';

const route: RouteRecordRaw = {
  name: 'governance',
  path: '/governance',
  component: () => import('./index.vue'),
  meta: {
    title: 'governance',
    i18nKey: 'route.governance',
    icon: 'mdi:view-dashboard-outline',
    order: 1.5,
    roles: ['R_SUPER']
  }
};

export default route;
