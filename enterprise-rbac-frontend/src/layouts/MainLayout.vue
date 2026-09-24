<template>
  <el-container class="main-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h2>权限管理系统</h2>
      </div>
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
          <h3>{{ pageTitle }}</h3>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
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
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4d;
}

.logo h2 {
  margin: 0;
  font-size: 16px;
  color: #fff;
}

.sidebar-menu {
  border: none;
  background-color: #304156;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  color: #bfcbd9;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #263445 !important;
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
