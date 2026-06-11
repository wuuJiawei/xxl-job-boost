import type { RouteRecordRaw } from 'vue-router';

const route: RouteRecordRaw = {
  name: 'alerts',
  path: '/alerts',
  component: () => import('./index.vue'),
  meta: {
    title: 'alerts',
    i18nKey: 'route.alerts',
    icon: 'mdi:bell-alert-outline',
    order: 5,
    roles: ['R_SUPER']
  }
};

export default route;
