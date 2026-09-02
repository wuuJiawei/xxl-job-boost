<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';

defineOptions({ name: 'ForceChangePassword' });

const authStore = useAuthStore();
const { formRef, validate } = useNaiveForm();
const submitting = ref(false);
const model = reactive({ oldPassword: '', password: '', confirmPassword: '' });

const rules = computed(() => {
  const { createConfirmPwdRule } = useFormRules();
  return {
    oldPassword: { required: true, message: '请输入当前密码', trigger: ['blur', 'input'] },
    password: {
      required: true,
      validator: (_rule: unknown, value: string) => {
        if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z\d])\S{12,64}$/.test(value)) {
          return new Error('密码必须为12-64位，并同时包含大写字母、小写字母、数字和特殊字符，不能包含空白字符。弱密码可能导致管理后台被攻击。');
        }
        if (value.toLowerCase().includes(authStore.userInfo.userName.toLowerCase())) {
          return new Error('新密码不能包含用户名');
        }
        return true;
      },
      trigger: ['blur', 'input']
    },
    confirmPassword: createConfirmPwdRule(model.password)
  };
});

async function submit() {
  await validate();
  submitting.value = true;
  try {
    await authStore.changePassword(model.oldPassword, model.password);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="relative size-full flex-center overflow-hidden bg-#fef2f2">
    <NCard :bordered="false" class="w-460px lt-sm:w-320px">
      <NSpace vertical :size="18">
        <div>
          <h2 class="text-22px font-semibold text-#991b1b">必须立即修改管理员密码</h2>
          <p class="mt-10px text-14px leading-22px text-#7f1d1d">
            当前密码属于弱密码，继续使用会增加暴力破解和管理后台被攻击的风险。未完成修改前，其他管理功能将被禁止访问。
          </p>
        </div>
        <NForm ref="formRef" :model="model" :rules="rules" label-placement="top" @keyup.enter="submit">
          <NFormItem path="oldPassword" label="当前密码">
            <NInput v-model:value="model.oldPassword" type="password" show-password-on="click" autocomplete="current-password" />
          </NFormItem>
          <NFormItem path="password" label="新密码">
            <NInput v-model:value="model.password" type="password" show-password-on="click" autocomplete="new-password" />
          </NFormItem>
          <NFormItem path="confirmPassword" label="确认新密码">
            <NInput v-model:value="model.confirmPassword" type="password" show-password-on="click" autocomplete="new-password" />
          </NFormItem>
          <NButton type="error" block :loading="submitting" @click="submit">修改密码并继续</NButton>
        </NForm>
      </NSpace>
    </NCard>
  </div>
</template>
