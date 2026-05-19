<template>
  <div class="auth-page register-container">
    <el-card class="register-card">
      <div class="auth-page__copy">
        <span class="auth-page__eyebrow">创建订购账号</span>
        <h1>创建新账户</h1>
        <p>建立属于你的校服预订空间，后续可保存身高体重与常用地址。</p>
      </div>
      <el-form ref="registerForm" :model="registerForm" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="auth-page__submit" type="primary" @click="register">注册</el-button>
        </el-form-item>
      </el-form>

      <div class="auth-page__link login-link">
        <el-link type="primary" @click="goToLogin">已有账号？去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'UserRegister',
  data() {
    return {
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }],
      },
    };
  },
  methods: {
    async register() {
      if (!this.registerForm.username || !this.registerForm.password || !this.registerForm.confirmPassword) {
        this.$message.error('请填写所有字段');
        return;
      }

      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        this.$message.error('两次密码输入不一致');
        return;
      }

      try {
        const response = await axios.post('/api/users/register', {
          userAccount: this.registerForm.username,
          userPassword: this.registerForm.password,
        });

        this.$message.success(response.data.message || '注册成功');
        this.$router.push('/login'); // 注册成功后跳转到登录页面
      } catch (error) {
        const errorMessage = error.response?.data?.message || '注册失败，请稍后再试';
        this.$message.error(errorMessage);
      }
    },
    goToLogin() {
      this.$router.push('/login');
    },
  },
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--app-bg);
}

.register-card {
  width: min(100%, 480px);
  padding: 36px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.auth-page__copy {
  margin-bottom: 24px;
}

.auth-page__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 0;
}

.auth-page__copy h1 {
  margin: 16px 0 10px;
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1.2;
  letter-spacing: 0;
}

.auth-page__copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.auth-page__submit {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-md);
}

.auth-page__link {
  margin-top: 15px;
  text-align: center;
}

:deep(.el-form-item__label) {
  color: var(--text-primary);
  font-weight: 600;
}
</style>
