import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createDiscreteApi } from 'naive-ui';
import App from './App.vue';
import { router } from './router';
import './styles.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.mount('#app');

const { message } = createDiscreteApi(['message']);

window.addEventListener('unhandledrejection', () => {
  message.error('请求失败，请稍后重试。');
});
