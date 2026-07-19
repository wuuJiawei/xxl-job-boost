<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useMessage, type FormInst, type FormRules } from 'naive-ui';
import { fetchEmailSettings, updateEmailSettings, type EmailSettings } from '@/api/system-settings';

defineOptions({
  name: 'system-settings'
});

const message = useMessage();
const loading = ref(false);
const submitting = ref(false);
const formRef = ref<FormInst | null>(null);

const formValue = reactive<EmailSettings>({
  enabled: false,
  host: 'smtp.qq.com',
  port: 25,
  username: '',
  from: '',
  password: '',
  passwordConfigured: false,
  smtpAuth: true,
  starttlsEnabled: true,
  starttlsRequired: true,
  sslEnabled: false
});

const passwordPlaceholder = computed(() =>
  formValue.passwordConfigured ? '已配置，留空则保持不变' : '请输入 SMTP 密码或授权码'
);

const rules: FormRules = {
  host: {
    validator: () => !formValue.enabled || Boolean(formValue.host.trim()),
    message: '启用邮件后必须填写 SMTP 服务器',
    trigger: ['blur', 'input']
  },
  port: {
    type: 'number',
    required: true,
    validator: (_, value: number) => Number.isInteger(value) && value >= 1 && value <= 65535,
    message: 'SMTP 端口必须在 1 到 65535 之间',
    trigger: ['blur', 'change']
  },
  username: {
    validator: () => !formValue.enabled || !formValue.smtpAuth || Boolean(formValue.username.trim()),
    message: '启用 SMTP 认证后必须填写用户名',
    trigger: ['blur', 'input']
  },
  from: {
    validator: () => !formValue.enabled || Boolean(formValue.from.trim()),
    message: '启用邮件后必须填写发件人地址',
    trigger: ['blur', 'input']
  },
  password: {
    validator: () =>
      !formValue.enabled || !formValue.smtpAuth || formValue.passwordConfigured || Boolean(formValue.password),
    message: '启用 SMTP 认证后必须填写密码或授权码',
    trigger: ['blur', 'input']
  }
};

function applySettings(settings: EmailSettings) {
  Object.assign(formValue, settings, { password: '' });
}

async function loadSettings() {
  loading.value = true;
  try {
    const response = await fetchEmailSettings();
    if (response.code !== 200) {
      throw new Error(response.msg || '邮箱设置加载失败');
    }
    applySettings(response.data);
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '邮箱设置加载失败');
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const response = await updateEmailSettings({
      ...formValue,
      host: formValue.host.trim(),
      username: formValue.username.trim(),
      from: formValue.from.trim()
    });
    if (response.code !== 200) {
      throw new Error(response.msg || '邮箱设置保存失败');
    }
    applySettings(response.data);
    message.success('邮箱设置已保存');
  } catch (error) {
    const err = error as Error;
    message.error(err.message || '邮箱设置保存失败');
  } finally {
    submitting.value = false;
  }
}

watch(
  () => formValue.starttlsEnabled,
  (enabled) => {
    if (enabled) {
      formValue.sslEnabled = false;
    } else {
      formValue.starttlsRequired = false;
    }
  }
);

watch(
  () => formValue.sslEnabled,
  (enabled) => {
    if (enabled) {
      formValue.starttlsEnabled = false;
      formValue.starttlsRequired = false;
    }
  }
);

onMounted(() => {
  void loadSettings();
});
</script>

<template>
  <div class="h-full overflow-auto">
    <n-card title="系统设置" size="small" :bordered="false">
      <n-tabs type="line" animated default-value="email">
        <n-tab-pane name="email" tab="邮箱设置">
          <n-spin :show="loading">
            <n-form
              ref="formRef"
              :model="formValue"
              :rules="rules"
              label-placement="left"
              label-width="150"
              require-mark-placement="right-hanging"
              class="email-settings-form"
            >
              <n-form-item label="启用邮件告警" path="enabled">
                <n-switch v-model:value="formValue.enabled" />
              </n-form-item>

              <n-divider title-placement="left">服务器配置</n-divider>

              <n-form-item label="SMTP 服务器" path="host">
                <n-input v-model:value="formValue.host" placeholder="例如 smtp.qq.com" />
              </n-form-item>
              <n-form-item label="SMTP 端口" path="port">
                <n-input-number v-model:value="formValue.port" :min="1" :max="65535" :precision="0" />
              </n-form-item>
              <n-form-item label="用户名" path="username">
                <n-input v-model:value="formValue.username" placeholder="请输入 SMTP 用户名" />
              </n-form-item>
              <n-form-item label="发件人地址" path="from">
                <n-input v-model:value="formValue.from" placeholder="请输入邮件发件人地址" />
              </n-form-item>
              <n-form-item label="密码 / 授权码" path="password">
                <n-input
                  v-model:value="formValue.password"
                  type="password"
                  show-password-on="click"
                  :placeholder="passwordPlaceholder"
                  autocomplete="new-password"
                />
              </n-form-item>

              <n-divider title-placement="left">安全选项</n-divider>

              <n-form-item label="SMTP 认证" path="smtpAuth">
                <n-switch v-model:value="formValue.smtpAuth" />
              </n-form-item>
              <n-form-item label="STARTTLS" path="starttlsEnabled">
                <n-switch v-model:value="formValue.starttlsEnabled" />
              </n-form-item>
              <n-form-item label="强制 STARTTLS" path="starttlsRequired">
                <n-switch v-model:value="formValue.starttlsRequired" :disabled="!formValue.starttlsEnabled" />
              </n-form-item>
              <n-form-item label="SSL" path="sslEnabled">
                <n-switch v-model:value="formValue.sslEnabled" />
              </n-form-item>

              <n-form-item :show-label="false">
                <n-button type="primary" :loading="submitting" @click="submit">
                  <template #icon>
                    <icon-mdi-content-save-outline />
                  </template>
                  保存设置
                </n-button>
              </n-form-item>
            </n-form>
          </n-spin>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<style scoped>
.email-settings-form {
  width: min(100%, 760px);
  padding-top: 8px;
}

.email-settings-form :deep(.n-input-number) {
  width: 180px;
}

</style>
