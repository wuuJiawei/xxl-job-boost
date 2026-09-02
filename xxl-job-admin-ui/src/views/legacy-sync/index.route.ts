import type { RouteRecordRaw } from 'vue-router';

const route: RouteRecordRaw = {
  name: 'legacy-sync',
  path: '/legacy-sync',
  component: () => import('./index.vue'),
  meta: {
    title: 'legacy-sync',
    i18nKey: 'route.legacy-sync',
    icon: 'mdi:database-sync-outline',
    order: 10,
    roles: ['R_SUPER']
  }
};

export default route;
