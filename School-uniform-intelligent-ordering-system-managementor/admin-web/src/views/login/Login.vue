<template>
  <div class="page-shell login-page">
    <section class="login-page__panel section-card">
      <div class="login-page__hero">
        <div class="login-page__logo">SU</div>
        <h1>校服智能订购管理后台</h1>
        <p>统一处理校服、订单、评论审核与基础数据维护。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入管理员密码"
            show-password
            type="password"
            :prefix-icon="Lock"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-button class="login-page__submit" type="primary" :loading="loading" @click="handleSubmit">
          登录后台
        </el-button>
      </el-form>

    </section>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }

  loading.value = true
  try {
    await authStore.loginAction(form)
    ElMessage.success('登录成功')
    await router.replace(String(route.query.redirect || '/'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: grid;
  place-items: center;
  padding: 24px;
  min-height: 100vh;
  background: var(--app-bg);
}

.login-page__panel {
  width: min(100%, 460px);
  padding: 34px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.login-page__hero {
  margin-bottom: 26px;
}

.login-page__logo {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
  background: var(--brand);
  font-weight: 700;
}

.login-page__hero h1 {
  margin: 18px 0 10px;
  font-size: 26px;
  line-height: 1.25;
  letter-spacing: 0;
  color: var(--text-primary);
}

.login-page__hero p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.login-page__submit {
  width: 100%;
  height: 44px;
  margin-top: 10px;
  border: 0;
  border-radius: var(--radius-md);
}

:deep(.el-form-item__label) {
  color: var(--text-primary);
  font-weight: 600;
}
</style>
