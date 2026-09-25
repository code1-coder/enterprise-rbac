import type { MenuTreeVO } from '@/types/menu'

export function checkedKeysForDisplay(
  menus: MenuTreeVO[],
  assignedIds: number[],
  linked: boolean
): number[] {
  if (!linked) return assignedIds

  const assigned = new Set(assignedIds)
  const checkedLeaves: number[] = []

  const collect = (nodes: MenuTreeVO[]) => {
    nodes.forEach(node => {
      if (node.children?.length) {
        collect(node.children)
      } else if (assigned.has(node.id)) {
        checkedLeaves.push(node.id)
      }
    })
  }

  collect(menus)
  return checkedLeaves
}
