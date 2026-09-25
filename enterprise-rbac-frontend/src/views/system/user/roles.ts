import type { UserVO } from '@/types/user'

export function userRoleLabels(user: UserVO): Array<{ id: number; name: string }> {
  return (user.roleNames ?? []).map((name, index) => ({
    id: user.roleIds?.[index] ?? index,
    name
  }))
}

export function userRoleIds(user: UserVO): number[] {
  return user.roleIds ?? []
}
