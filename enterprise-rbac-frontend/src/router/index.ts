import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore, usePermissionStore } from '@/stores'
import { http } from '@/utils/http'

/**
 * 静态路由
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/system/user'
      },
      {
        path: '/system/user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: '/system/role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: '/system/menu',
        name: 'Menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫：认证和权限初始化
 */
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const permissionStore = usePermissionStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 企业权限角色分配系统` : '企业权限角色分配系统'
  
  // 公开路由直接放行
  if (to.meta.requiresAuth === false) {
    next()
    return
  }
  
  // 检查登录态
  if (!authStore.isAuthenticated) {
    // 尝试从本地恢复
    const restored = authStore.restoreAuth()
    if (!restored) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  
  // 已登录但未加载权限，先加载权限数据
  if (!permissionStore.loaded) {
    try {
      await Promise.all([
        authStore.fetchUserInfo(),
        permissionStore.loadUserPermissions()
      ])
    } catch (error) {
      console.error('Failed to load user permissions:', error)
      authStore.clearAuthState()
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  
  next()
})

/**
 * 注册认证失效回调
 */
http.setAuthFailedCallback(() => {
  const authStore = useAuthStore()
  const permissionStore = usePermissionStore()
  
  authStore.clearAuthState()
  permissionStore.clearPermissions()
  
  router.push('/login')
})

export default router
