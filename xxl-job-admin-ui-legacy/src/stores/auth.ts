import { defineStore } from 'pinia';
import { login, logout } from '@/api/auth';
import { fetchSession } from '@/api/admin-next';

type Credentials = {
  userName: string;
  password: string;
  ifRemember: boolean;
};

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthenticated: false,
    userName: '',
    isAdmin: false,
    initialized: false
  }),
  actions: {
    async syncSession() {
      const response = await fetchSession();
      if (response.code !== 200) {
        throw new Error(response.msg || '会话加载失败');
      }
      this.isAuthenticated = true;
      this.userName = response.data.userName;
      this.isAdmin = response.data.isAdmin;
    },
    async bootstrap() {
      if (this.initialized) {
        return;
      }
      try {
        await this.syncSession();
      } catch {
        this.isAuthenticated = false;
        this.userName = '';
        this.isAdmin = false;
      } finally {
        this.initialized = true;
      }
    },
    async signIn(credentials: Credentials) {
      const response = await login(credentials);
      if (response.code !== 200) {
        throw new Error(response.msg || '登录失败');
      }
      await this.syncSession();
      this.initialized = true;
    },
    async signOut() {
      await logout();
      this.isAuthenticated = false;
      this.userName = '';
      this.isAdmin = false;
      this.initialized = true;
    }
  }
});
