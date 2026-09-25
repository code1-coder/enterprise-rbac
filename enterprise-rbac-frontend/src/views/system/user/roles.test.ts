import { describe, expect, it } from 'vitest'
import type { UserVO } from '@/types/user'
import { userRoleIds, userRoleLabels } from './roles'

describe('user role data', () => {
  it('renders role names from the API and keeps numeric IDs for assignment', () => {
    const user = {
      id: 1,
      username: 'admin',
      status: 1,
      createTime: '2026-09-25',
      updateTime: '2026-09-25',
      roleIds: [2, 3],
      roles: ['ROLE_ADMIN', 'ROLE_AUDITOR'],
      roleNames: ['管理员', '审计员']
    } satisfies UserVO

    expect(userRoleLabels(user)).toEqual([
      { id: 2, name: '管理员' },
      { id: 3, name: '审计员' }
    ])
    expect(userRoleIds(user)).toEqual([2, 3])
  })

  it('shows no role tags and preselects nothing when roles are absent', () => {
    const user = {
      id: 2,
      username: 'guest',
      status: 1,
      createTime: '2026-09-25',
      updateTime: '2026-09-25'
    } satisfies UserVO

    expect(userRoleLabels(user)).toEqual([])
    expect(userRoleIds(user)).toEqual([])
  })
})
