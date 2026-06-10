<template>
  <div class="login-screen">
    <div class="login-hero">
      <div class="hero-pill">XXL-JOB Boost</div>
      <h1>Modern console for pragmatic job operations.</h1>
      <p>
        第一阶段先把旧控制台迁移过来。当前版本先接登录链路和 Dashboard，
        保持与旧后台并存。
      </p>
    </div>

    <n-card class="login-card" :bordered="false">
      <template #header>
        <div class="login-card-header">
          <div class="login-card-title">Admin Next</div>
          <div class="login-card-subtitle">使用现有管理员账号登录</div>
        </div>
      </template>

      <n-form ref="formRef" :model="formValue" :rules="rules" @submit.prevent="handleSubmit">
        <n-form-item path="userName" label="用户名">
          <n-input v-model:value="formValue.userName" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="formValue.password"
            type="password"
            show-password-on="click"
            placeholder="请输入密码"
          />
        </n-form-item>
        <div class="login-actions">
          <n-checkbox v-model:checked="formValue.ifRemember">记住我</n-checkbox>
          <n-button type="primary" attr-type="submit" :loading="submitting">登录</n-button>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import {
  NButton,
  NCard,
  NCheckbox,
  NForm,
  NFormItem,
  NInput,
  useMessage
} from 'naive-ui';
import type { FormInst, FormRules } from 'naive-ui';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const formRef = ref<FormInst | null>(null);
const message = useMessage();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const submitting = ref(false);

const formValue = ref({
  userName: '',
  password: '',
  ifRemember: true
});

const rules: FormRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: ['blur', 'input'] },
    { min: 4, message: '用户名至少 4 位', trigger: ['blur', 'input'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'input'] },
    { min: 4, message: '密码至少 4 位', trigger: ['blur', 'input'] }
  ]
};

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    await authStore.signIn(formValue.value);
    message.success('登录成功');
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
    router.push(redirect);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '登录失败');
  } finally {
    submitting.value = false;
  }
}
</script>
