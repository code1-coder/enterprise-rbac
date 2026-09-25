import { describe, expect, it } from 'vitest'
import TreeStore from 'element-plus/es/components/tree/src/model/tree-store.mjs'
import type { MenuTreeVO } from '@/types/menu'
import { checkedKeysForDisplay } from './permissionSelection'

const menus = [
  {
    id: 1,
    children: [
      { id: 2, children: [{ id: 3 }, { id: 4 }] },
      { id: 5, children: [{ id: 6 }, { id: 7 }] }
    ]
  }
] as MenuTreeVO[]

const createTree = (checkStrictly: boolean) => {
  const tree = new TreeStore({
    data: menus,
    key: 'id',
    props: { children: 'children' },
    checkStrictly,
    lazy: false,
    checkDescendants: false,
    autoExpandParent: false,
    defaultExpandAll: false
  })
  tree.initialize()
  return tree
}

describe('role permission selection', () => {
  it('restores partially selected branches without granting unchecked buttons or APIs', () => {
    const tree = createTree(false)
    const assignedIds = [1, 2, 3, 5, 6]

    tree.setCheckedKeys(checkedKeysForDisplay(menus, assignedIds, true))

    expect(tree.getCheckedKeys()).toEqual([3, 6])
    expect(tree.getHalfCheckedKeys()).toEqual([1, 2, 5])
    expect(tree.getCheckedKeys()).not.toContain(4)
    expect(tree.getCheckedKeys()).not.toContain(7)

    const savedIds = [...tree.getCheckedKeys(), ...tree.getHalfCheckedKeys()] as number[]
    tree.setCheckedKeys(checkedKeysForDisplay(menus, savedIds, true))
    expect(tree.getCheckedKeys()).toEqual([3, 6])
  })

  it('retains explicit parent selection when linkage is disabled', () => {
    const tree = createTree(true)
    tree.setCheckedKeys(checkedKeysForDisplay(menus, [2], false))

    expect(tree.getCheckedKeys()).toEqual([2])
    expect(tree.getHalfCheckedKeys()).toEqual([])
  })

  it('clears the previous role selection when the next role has none', () => {
    const tree = createTree(false)
    tree.setCheckedKeys(checkedKeysForDisplay(menus, [1, 2, 3], true))
    tree.setCheckedKeys(checkedKeysForDisplay(menus, [], true))

    expect(tree.getCheckedKeys()).toEqual([])
    expect(tree.getHalfCheckedKeys()).toEqual([])
  })
})
