import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePermissionStore } from './permission'

const mocks = vi.hoisted(() => ({
  getMyMenus: vi.fn(),
  getMyPermissions: vi.fn()
}))

vi.mock('@/api/menus', () => ({ menusApi: mocks }))

describe('permission refresh', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('removes revoked buttons and menus after reloading the current account', async () => {
    mocks.getMyMenus
      .mockResolvedValueOnce([{ id: 1, menuName: '用户管理' }])
      .mockResolvedValueOnce([])
    mocks.getMyPermissions
      .mockResolvedValueOnce(['system:user:list', 'system:user:delete'])
      .mockResolvedValueOnce(['system:user:list'])

    const store = usePermissionStore()
    await store.loadUserPermissions()
    expect(store.hasPermission('system:user:delete')).toBe(true)
    expect(store.menus).toHaveLength(1)

    await store.loadUserPermissions()
    expect(store.hasPermission('system:user:delete')).toBe(false)
    expect(store.menus).toEqual([])
  })
})
