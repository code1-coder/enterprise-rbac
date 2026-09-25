<template>
  <div class="login-container">
    <section class="login-story" aria-label="系统介绍">
      <div class="story-brand"><span class="brand-mark">R</span> 权限管理 <span>/ RBAC</span></div>
      <div class="story-copy">
        <div class="eyebrow">用户 · 角色 · 菜单</div>
        <h1>把访问权限<br /><em>管清楚。</em></h1>
        <p>按职责分配角色，按需要开放菜单。<br />日常管理，从这里开始。</p>
      </div>
      <div class="orbit" aria-hidden="true">
        <div class="orbit-ring outer"></div><div class="orbit-ring inner"></div>
        <div class="orbit-core"><span>R</span><small>RBAC</small></div>
        <span class="orbit-node node-user">用户</span>
        <span class="orbit-node node-role">角色</span>
        <span class="orbit-node node-menu">菜单</span>
      </div>
      <div class="story-foot">企业权限管理系统</div>
    </section>
    <section class="login-panel" aria-label="登录">
      <div class="panel-top">账号登录</div>
      <div class="login-card">
        <h2>登录管理端</h2>
        <p class="card-description">请输入账号和密码，继续管理权限。</p>
      <el-form :model="loginForm" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <div class="field-label">用户名</div>
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
          />
        </el-form-item>
        <div class="field-label">密码</div>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item class="submit-item">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      </div>
      <div class="panel-foot">RBAC <span>权限管理系统</span></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore, usePermissionStore } from '@/stores'

const router = useRouter()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await authStore.login(loginForm)
      await permissionStore.loadUserPermissions()
      
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(430px, .88fr);
  min-height: 100vh;
  overflow: hidden;
  background: #0a1528;
}
.login-story {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100vh;
  padding: 48px clamp(36px, 6vw, 100px) 42px;
  overflow: hidden;
  color: #e9f3ff;
  background: radial-gradient(circle at 70% 68%, #24597952, transparent 33%), radial-gradient(circle at 12% 8%, #354b9550, transparent 34%), #0a1528;
}
.login-story::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: .16;
  background-image: linear-gradient(#74a1b522 1px, transparent 1px), linear-gradient(90deg, #74a1b522 1px, transparent 1px);
  background-size: 54px 54px;
  mask-image: linear-gradient(90deg, #000, transparent);
  pointer-events: none;
}
.story-brand, .story-foot, .panel-top, .panel-foot, .eyebrow { font-family: 'Segoe UI', Arial, sans-serif; letter-spacing: .08em; font-size: 11px; font-weight: 600; }
.story-brand { z-index: 1; display: flex; align-items: center; gap: 12px; font-size: 14px; letter-spacing: .12em; }
.story-brand > span:last-child { color: #9bb2d3; font-weight: 400; }
.brand-mark { display: grid; place-items: center; width: 34px; height: 34px; color: #091a31; background: #82e2d3; border-radius: 8px; font-size: 19px; font-weight: 700; }
.story-copy { position: relative; z-index: 1; margin-top: 7vh; }
.eyebrow { color: #8ee3d5; }
.story-copy h1 { margin: 24px 0; font-size: clamp(40px, 4.2vw, 68px); line-height: 1.22; letter-spacing: -.05em; font-weight: 600; animation: login-enter .36s .04s ease-out both; }
.story-copy h1 em { color: #88dfd1; font-style: normal; }
.story-copy p { color: #a9b9cb; line-height: 1.9; font-size: 15px; animation: login-enter .42s .13s ease-out both; }
.story-foot { z-index: 1; color: #7188a5; font-size: 10px; }
.orbit { position: relative; align-self: center; width: min(30vw, 330px, 38vh); height: min(30vw, 330px, 38vh); margin-top: -40px; }
.orbit-ring { position: absolute; border: 1px solid #7ad9dc35; border-radius: 50%; }
.outer { inset: 1%; box-shadow: inset 0 0 55px #65b9ca0b; animation: breathe 9s ease-in-out infinite; }
.inner { inset: 19%; border-style: dashed; animation: orbit-turn 50s linear infinite; }
.orbit-core { position: absolute; inset: 32%; display: grid; place-content: center; text-align: center; border: 1px solid #8debdc80; border-radius: 50%; background: #17364a; box-shadow: 0 0 0 14px #83dbde0b; }
.orbit-core span { color: #93e6d8; font-size: 45px; font-weight: 650; line-height: 1; }
.orbit-core small { margin-top: 8px; color: #a4d5db; font-size: 9px; letter-spacing: .22em; }
.orbit-node { position: absolute; display: flex; align-items: center; padding: 8px 12px; border: 1px solid #96c9d447; border-radius: 6px; color: #c8e8ed; background: #112b40dc; font-size: 11px; letter-spacing: .08em; animation: float 7s ease-in-out infinite; }
.node-user { top: 8%; left: 9%; }
.node-role { top: 27%; right: -6%; animation-delay: -2s; }
.node-menu { bottom: 12%; left: -5%; animation-delay: -4s; }
.login-panel { display: flex; flex-direction: column; justify-content: space-between; min-height: 100vh; padding: 48px clamp(36px, 6vw, 100px) 42px; background: #f6f8fa; color: #233251; }
.panel-top { display: flex; align-items: center; gap: 9px; color: #536c8b; }
.login-card { width: min(100%, 420px); margin: 34px auto; }
.login-card h2 { margin: 0 0 10px; color: #182a48; font-size: clamp(32px, 3vw, 42px); letter-spacing: -.04em; animation: login-enter .36s .08s ease-out both; }
.card-description { margin-bottom: 38px; color: #7587a1; font-size: 14px; line-height: 1.7; animation: login-enter .4s .16s ease-out both; }
.login-card .el-form { animation: login-enter .44s .22s ease-out both; }
.field-label { margin-bottom: 9px; color: #334662; font-size: 13px; font-weight: 600; }
.login-card :deep(.el-form-item) { margin-bottom: 25px; }
.login-card :deep(.el-input__wrapper) { min-height: 51px; padding: 0 17px; border: 1px solid #dce5f0; border-radius: 10px; background: #fff; box-shadow: none; transition: border-color .2s, box-shadow .2s; }
.login-card :deep(.el-input__wrapper:hover), .login-card :deep(.el-input__wrapper.is-focus) { border-color: #7899e1; box-shadow: 0 0 0 4px #5479d215; }
.login-card :deep(.el-input__inner) { font-size: 14px; }
.login-card :deep(.submit-item) { margin-top: 34px; margin-bottom: 0; }
.login-card :deep(.el-button) { height: 52px; border-color: #375d88; border-radius: 8px; background: #375d88; font-weight: 600; transition: background-color .2s, transform .2s; }
.login-card :deep(.el-button:hover) { background: #27476c; }
.login-card :deep(.el-button:active) { transform: translateY(1px); }
.panel-foot { display: flex; justify-content: space-between; gap: 12px; color: #a0acc0; font-size: 9px; }
@keyframes breathe { 50% { transform: scale(1.055); opacity: .55; } }
@keyframes orbit-turn { to { transform: rotate(360deg); } }
@keyframes float { 50% { transform: translateY(-8px); } }
@keyframes login-enter { from { opacity: .6; transform: translateY(7px); } }
@media (max-width: 900px) {
  .login-container { grid-template-columns: 1fr; }
  .login-story { min-height: 270px; padding: 28px 30px; }
  .story-copy { margin: 28px 0 0; }
  .story-copy h1 { margin: 14px 0 0; font-size: 34px; }
  .story-copy p, .story-foot, .orbit { display: none; }
  .login-panel { min-height: calc(100vh - 270px); padding: 28px 30px; }
  .login-card { margin: 30px auto; }
}
@media (max-width: 480px) {
  .login-story { min-height: 240px; }
  .login-panel { min-height: calc(100vh - 240px); }
  .panel-foot { font-size: 8px; }
}
@media (prefers-reduced-motion: reduce) {
  .orbit-ring, .orbit-node, .story-copy h1, .story-copy p, .login-card h2, .card-description, .login-card .el-form { animation: none; }
  .login-card :deep(.el-button) { transition: none; }
}
</style>
