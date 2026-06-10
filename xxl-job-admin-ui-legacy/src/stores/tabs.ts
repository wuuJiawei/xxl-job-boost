import { defineStore } from 'pinia';

export type AdminTab = {
  key: string;
  label: string;
  fullPath: string;
  closable: boolean;
};

const HOME_TAB: AdminTab = {
  key: 'dashboard',
  label: 'Dashboard',
  fullPath: '/',
  closable: false
};

export const useTabsStore = defineStore('tabs', {
  state: () => ({
    items: [HOME_TAB] as AdminTab[],
    activeKey: HOME_TAB.key
  }),
  actions: {
    ensureTab(tab: AdminTab) {
      const existing = this.items.find((item) => item.key === tab.key);
      if (!existing) {
        this.items.push(tab);
      } else {
        existing.label = tab.label;
        existing.fullPath = tab.fullPath;
      }
      this.activeKey = tab.key;
    },
    activate(key: string) {
      this.activeKey = key;
    },
    close(key: string) {
      const targetIndex = this.items.findIndex((item) => item.key === key);
      if (targetIndex < 0) {
        return HOME_TAB.fullPath;
      }

      const target = this.items[targetIndex];
      if (!target.closable) {
        this.activeKey = target.key;
        return target.fullPath;
      }

      this.items.splice(targetIndex, 1);

      if (this.activeKey === key) {
        const next = this.items[targetIndex] || this.items[targetIndex - 1] || HOME_TAB;
        this.activeKey = next.key;
        return next.fullPath;
      }

      return this.items.find((item) => item.key === this.activeKey)?.fullPath || HOME_TAB.fullPath;
    },
    reset() {
      this.items = [HOME_TAB];
      this.activeKey = HOME_TAB.key;
    }
  }
});
