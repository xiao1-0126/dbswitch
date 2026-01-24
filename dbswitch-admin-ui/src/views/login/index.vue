<template>
  <div class="login-container">
    <div class="login-background"></div>
    <div class="login-wrapper">
      <div class="login-card">
        <!-- Logo和标题 -->
        <div class="login-header">
          <div class="logo-container">
            <img src="../../assets/logo.png"
                 alt="DBSwitch Logo"
                 class="logo">
          </div>
          <h1 class="login-title">DBSwitch</h1>
          <p class="login-subtitle">数据迁移同步工具</p>
        </div>

        <!-- 登录表单 -->
        <el-form :model="ruleForm2"
                 :rules="rules2"
                 status-icon
                 ref="ruleForm2"
                 label-position="left"
                 label-width="0px"
                 class="login-form">
          <el-form-item prop="username">
            <el-input type="text"
                      v-model="ruleForm2.username"
                      auto-complete="off"
                      placeholder="用户名"
                      class="login-input"
                      prefix-icon="el-icon-user">
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input type="password"
                      v-model="ruleForm2.password"
                      auto-complete="off"
                      placeholder="密码"
                      class="login-input"
                      prefix-icon="el-icon-lock"
                      show-password>
            </el-input>
          </el-form-item>

          <!-- 记住密码 -->
          <div class="login-options">
            <el-checkbox v-model="checked"
                         class="remember-me">记住密码</el-checkbox>
          </div>

          <el-form-item>
            <el-button type="primary"
                       style="width:100%;"
                       @click="handleSubmit"
                       :loading="logining"
                       class="login-button"
                       @keyup.enter="handleSubmit">登录</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 页脚信息 -->
      <div class="login-footer">
        <p>© {{ currentYear }} https://gitee.com/inrgihc/dbswitch</p>
      </div>
    </div>
  </div>
</template>

<script>
import qs from "qs";

export default {
  data () {
    return {
      logining: false,
      ruleForm2: {
        username: "",
        password: ""
      },
      rules2: {
        username: [
          {
            required: true,
            message: "请输入用户名",
            trigger: "blur"
          }
        ],
        password: [
          {
            required: true,
            message: "请输入密码",
            trigger: "blur"
          }
        ]
      },
      checked: false
    };
  },
  computed: {
    currentYear () {
      return new Date().getFullYear();
    }
  },
  created () {
    // 按enter键提交功能参考：https://www.cnblogs.com/cristina-guan/p/9440035.html
    var lett = this;
    document.onkeydown = function (e) {
      var key = window.event.keyCode;
      if (key == 13) {
        lett.handleSubmit(e);
      }
    };

    // 从localStorage读取记住的密码
    const savedUsername = localStorage.getItem('rememberedUsername');
    const savedPassword = localStorage.getItem('rememberedPassword');
    if (savedUsername && savedPassword) {
      this.ruleForm2.username = savedUsername;
      this.ruleForm2.password = savedPassword;
      this.checked = true;
    }
  },
  methods: {
    resetLoading () {
      this.loading = false;
    },
    showMessageBox (msg) {
      this.$alert(msg, '异常提示', {
        type: 'error',
        confirmButtonText: '确定'
      });
    },
    handleSubmit () {
      this.$refs.ruleForm2.validate(valid => {
        if (valid) {
          this.$http({
            method: 'POST',
            url: '/dbswitch/admin/api/v1/authentication/login',
            data: qs.stringify({
              username: this.ruleForm2.username,
              password: this.ruleForm2.password
            }),
          }).then(res => {
            if (0 === res.data.code) {
              this.logining = true;

              // 保存token和用户信息
              window.sessionStorage.setItem('token', res.data.data.accessToken);
              window.sessionStorage.setItem('username', this.ruleForm2.username);
              window.sessionStorage.setItem('realname', res.data.data.realName);
              window.sessionStorage.setItem('version', res.data.data.version);

              // 处理记住密码
              if (this.checked) {
                localStorage.setItem('rememberedUsername', this.ruleForm2.username);
                localStorage.setItem('rememberedPassword', this.ruleForm2.password);
              } else {
                localStorage.removeItem('rememberedUsername');
                localStorage.removeItem('rememberedPassword');
              }

              this.$router.push({ path: '/dashboard' });
            } else {
              this.resetLoading();
              this.showMessageBox("登录异常," + res.data.message);
            }
          }).catch(error => {
            this.resetLoading();
            this.showMessageBox("登录异常," + (error || "地址无法联通"));
          })
        }
      });
    }
  }
}
</script>

<style scoped>
/* 全局容器 */
.login-container {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f3f4; /* 与系统主背景色一致 */
}

/* 背景效果 */
.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #f0f3f4; /* 与系统主背景色一致 */
  z-index: 1;
}

.login-background::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(54, 163, 247, 0.1) 0%,
    transparent 70%
  ); /* 使用系统蓝色调 */
  animation: backgroundAnimation 20s ease-in-out infinite;
}

@keyframes backgroundAnimation {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(20%, -20%) rotate(90deg);
  }
  50% {
    transform: translate(0, -40%) rotate(180deg);
  }
  75% {
    transform: translate(-20%, -20%) rotate(270deg);
  }
}

/* 登录包装器 */
.login-wrapper {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 登录卡片 */
.login-card {
  background: #fff; /* 与系统卡片背景一致 */
  border-radius: 12px; /* 调整为更保守的圆角 */
  padding: 40px 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); /* 调整阴影效果 */
  width: 100%;
  animation: cardSlideIn 0.6s ease-out;
  border: 1px solid #e4e7ed; /* 添加边框，与Element UI一致 */
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 登录头部 */
.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-container {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: #36a3f7; /* 使用系统主蓝色 */
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 6px 20px rgba(54, 163, 247, 0.4); /* 调整阴影 */
  overflow: hidden;
}

.logo {
  width: 60px;
  height: 60px;
  object-fit: contain;
  background: white;
  border-radius: 50%;
  padding: 5px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133; /* Element UI标题颜色 */
  margin: 0 0 8px 0;
  background: linear-gradient(
    135deg,
    #36a3f7 0%,
    #1890ff 100%
  ); /* 使用系统蓝色渐变 */
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 14px;
  color: #606266; /* Element UI描述文字颜色 */
  margin: 0;
  font-weight: 400;
}

/* 登录表单 */
.login-form {
  width: 100%;
}

/* 输入框样式 */
.login-input {
  border-radius: 4px; /* Element UI默认圆角 */
  border: 1px solid #dcdfe6; /* Element UI边框颜色 */
  height: 40px; /* Element UI默认高度 */
  font-size: 14px;
  transition: all 0.3s ease;
  background: #fff;
}

.login-input:hover {
  border-color: #c6e2ff;
  box-shadow: 0 0 0 2px rgba(54, 163, 247, 0.1);
}

.login-input:focus {
  border-color: #36a3f7;
  box-shadow: 0 0 0 2px rgba(54, 163, 247, 0.2);
  outline: none;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 13px;
}

.remember-me {
  color: #606266;
  cursor: pointer;
  transition: color 0.3s ease;
}

.remember-me:hover {
  color: #36a3f7;
}

/* 登录按钮 */
.login-button {
  height: 40px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  background: #36a3f7; /* 使用系统主蓝色 */
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px 0 rgba(54, 163, 247, 0.3);
}

.login-button:hover {
  background: #1890ff; /* Element UI蓝色hover效果 */
  box-shadow: 0 4px 16px 0 rgba(54, 163, 247, 0.4);
  transform: translateY(-1px);
}

.login-button:active {
  transform: translateY(0);
  background: #096dd9;
}

.login-button:focus {
  box-shadow: 0 0 0 2px rgba(54, 163, 247, 0.2);
}

/* 页脚 */
.login-footer {
  margin-top: 30px;
  text-align: center;
}

.login-footer p {
  color: #909399; /* Element UI辅助文字颜色 */
  font-size: 12px;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }

  .login-title {
    font-size: 24px;
  }

  .logo-container {
    width: 60px;
    height: 60px;
  }

  .logo {
    width: 45px;
    height: 45px;
  }
}
</style>
