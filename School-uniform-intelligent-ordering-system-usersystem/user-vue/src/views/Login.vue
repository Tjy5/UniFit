<template>
  <div class="auth-page login-container">
    <el-card class="login-card">
      <div class="auth-page__copy">
        <span class="auth-page__eyebrow">校服订购账号</span>
        <h1>欢迎回来</h1>
        <p>继续浏览穿搭灵感、完善尺码信息并完成校服预订。</p>
      </div>
      <el-form ref="loginForm" :model="loginForm" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="auth-page__submit" type="primary" @click="login">登录</el-button>
        </el-form-item>
      </el-form>

      <div class="auth-page__link register-link">
        <el-link type="primary" @click="goToRegister">没有账号？去注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'UserLogin',
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
    };
  },
  methods: {
    async login() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.$message.error('请输入用户名和密码');
        return;
      }

      try {
        // 登录请求
        const response = await axios.post('/api/users/login', {
          userAccount: this.loginForm.username,
          userPassword: this.loginForm.password,
        });

        const body = response.data;
        const data = body?.data || body;
        localStorage.setItem('token', data.token); // 存储 Token
        this.$message.success(body?.message || '登录成功');

        // 登录成功后，自动获取用户信息
        await this.fetchUserInfo();

        // 跳转到商城页面
        const redirectUrl = this.$route.query.redirect || '/home';
        this.$router.push(redirectUrl);
      } catch (error) {
        const errorMessage = error.response?.data?.message || '登录失败，请稍后再试';
        this.$message.error(errorMessage);
      }
    },

    async fetchUserInfo() {
      const token = localStorage.getItem('token');
      if (!token) {
        this.$message.error('请先登录');
        return;
      }

      try {
        // 获取用户信息
        const response = await axios.get('/api/users/info', {
          headers: { Authorization: `Bearer ${token}` },
        });

        const userData = response.data.data; // 解析用户信息
        localStorage.setItem('userId', userData.userId); // 存储用户ID
        localStorage.setItem('userAccount', userData.userAccount); // 存储用户账号
        this.$message.success('用户状态正常'); // ✅ 修改弹窗内容
      } catch (error) {
        this.$message.error('获取用户信息失败，请重新登录');
      }
    },

    goToRegister() {
      this.$router.push('/register');
    },
  },
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--app-bg);
}

.login-card {
  width: min(100%, 460px);
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
