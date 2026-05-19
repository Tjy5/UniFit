<template>
  <div class="change-password-container">
    <el-card class="change-password-card">
      <div class="title-block">
        <span class="eyebrow">账户安全</span>
        <div class="title">修改密码</div>
        <p>更新登录凭据，保护你的订单、收藏和个人资料。</p>
      </div>
      <el-form :model="passwordForm" :rules="rules" ref="passwordForm" label-width="0px">
        <el-form-item prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请输入旧密码"
          ></el-input>
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请输入新密码"
          ></el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请确认新密码"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword" :loading="loading" class="submit-button">
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import api from '../api/api';

export default {
  name: 'ChangePassword',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };

    return {
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      },
      loading: false
    };
  },
  methods: {
    handleChangePassword() {
      this.$refs.passwordForm.validate(async (valid) => {
        if (valid) {
          this.loading = true;
          try {
            const response = await api.changePassword({
              oldPassword: this.passwordForm.oldPassword,
              newPassword: this.passwordForm.newPassword
            });
            
            if (response.data) {
              this.$message.success('密码修改成功，请重新登录');
              // 清除登录信息并跳转到登录页
              localStorage.removeItem('token');
              localStorage.removeItem('userId');
              this.$router.push('/login');
            }
          } catch (error) {
            console.error('修改密码失败:', error);
            this.$message.error(error.response?.data?.message || '修改密码失败，请稍后重试');
          } finally {
            this.loading = false;
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.change-password-container {
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.change-password-card {
  width: min(100%, 460px);
  padding: 36px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.title-block {
  margin-bottom: 24px;
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 0;
}

.title {
  text-align: left;
  font-size: 34px;
  font-weight: bold;
  margin: 16px 0 10px;
  color: var(--text-primary);
  letter-spacing: 0;
}

.title-block p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.submit-button {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-md);
}
</style>
