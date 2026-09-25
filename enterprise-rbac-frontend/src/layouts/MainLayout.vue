<template>
  <el-container class="main-layout">
    <el-aside width="232px" class="sidebar">
      <div class="logo">
        <span class="logo-mark" aria-hidden="true">R</span>
        <div><strong>权限管理</strong><small>RBAC 管理端</small></div>
      </div>
      <div class="sidebar-label">系统管理</div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <template v-for="menu in menuTree" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.id)">
            <template #title>
              <el-icon v-if="menu.icon">
                <component :is="menu.icon" />
              </el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.id"
              :index="child.path"
            >
              {{ child.menuName }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon v-if="menu.icon">
              <component :is="menu.icon" />
            </el-icon>
            <span>{{ menu.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-path">系统管理 <span>/</span> {{ pageTitle }}</div>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <span class="user-avatar" aria-hidden="true">{{ (authStore.userInfo?.nickname || authStore.userInfo?.username || '用').slice(0, 1) }}</span>
              {{ authStore.userInfo?.nickname || authStore.userInfo?.username }}
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore, usePermissionStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 页面标题
const pageTitle = computed(() => route.meta.title || '')

// 菜单树
const menuTree = computed(() => permissionStore.menus)

/**
 * 退出登录
 */
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: #f5f7f9;
}

.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  color: #eaf2ff;
  background: #162b3d;
}

.logo {
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 22px;
  border-bottom: 1px solid #9ac7d31c;
}
.logo-mark { display: grid; place-items: center; flex: 0 0 34px; height: 34px; border-radius: 7px; color: #123145; background: #9bd4cc; font-size: 19px; font-weight: 700; }
.logo strong { display: block; font-size: 14px; letter-spacing: .04em; }
.logo small { display: block; margin-top: 5px; color: #99acbb; font-size: 10px; }
.sidebar-label { margin: 30px 28px 12px; color: #8499a8; font-size: 11px; }
.sidebar-menu { flex: 1; padding: 0 11px; border: none; background: transparent; }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { height: 44px; margin-bottom: 4px; border-radius: 7px; color: #b2c2ce; font-size: 13px; transition: background .2s, color .2s, transform .2s; }
:deep(.el-sub-menu .el-menu) { background: transparent; }
:deep(.el-sub-menu .el-menu-item) { min-width: 0; padding-left: 44px !important; font-size: 12px; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: #ffffff12 !important; color: #fff; }
:deep(.el-menu-item.is-active) { background: #9bd4cc1d !important; color: #b5e7de; font-weight: 600; }
:deep(.el-menu-item.is-active)::before { content: ''; position: absolute; left: 0; width: 3px; height: 20px; border-radius: 3px; background: #9bd4cc; }
.header { height: 64px; display: flex; justify-content: space-between; align-items: center; padding: 0 clamp(22px, 3vw, 48px); border-bottom: 1px solid #e5e9ed; background: #fff; }
.header-path { color: #78899a; font-size: 12px; }
.header-path span { margin: 0 7px; color: #bfc9d6; }
.header-right { display: flex; align-items: center; gap: 22px; }
.user-info { display: flex; align-items: center; gap: 10px; color: #3b5064; cursor: pointer; font-size: 12px; font-weight: 600; }
.user-avatar { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 7px; color: #375d88; background: #e9eef3; font-size: 14px; }
.main-content { min-width: 0; padding: clamp(22px, 3vw, 42px); background: #f5f7f9; }
@media (max-width: 800px) {
  .sidebar { width: 184px !important; }
  .logo { margin: 0 14px; }
  .sidebar-label { margin-right: 18px; margin-left: 18px; }
  .main-content { padding: 18px; }
}
@media (prefers-reduced-motion: reduce) {
  :deep(.el-menu-item), :deep(.el-sub-menu__title) { transition: none; }
}
</style>
