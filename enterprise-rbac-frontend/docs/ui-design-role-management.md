# 角色管理界面设计规范

## 概述

角色管理模块负责系统角色的创建、编辑、删除以及菜单权限的分配。角色是 RBAC 权限模型的核心，连接用户与菜单权限，界面设计需要清晰展示角色数据并提供高效的权限配置流程。

---

## 页面路由与权限

- **路由路径**: `/system/role`
- **页面组件**: `src/views/system/role/index.vue`
- **所需权限**: 已登录（导航可见需 `system:role:list`）

---

## 整体布局

### 页面结构

```
┌─────────────────────────────────────────────────────────┐
│ 搜索栏区域（工具栏）                                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 数据表格区域                                              │
│ (el-table)                                              │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ 分页控件（el-pagination）                                │
└─────────────────────────────────────────────────────────┘
```

### 工具栏（顶部搜索栏）

**组件**: `el-form` inline 模式

**布局**: Flex 布局，左侧搜索条件，右侧操作按钮

```html
<el-form :inline="true" :model="queryForm">
  <!-- 左侧：搜索条件 -->
  <el-form-item label="角色名称">
    <el-input v-model="queryForm.roleName" placeholder="请输入角色名称" clearable />
  </el-form-item>
  
  <el-form-item>
    <el-button type="primary" @click="handleQuery">查询</el-button>
    <el-button @click="handleReset">重置</el-button>
  </el-form-item>
  
  <!-- 右侧：操作按钮 -->
  <el-form-item style="margin-left: auto">
    <el-button 
      type="primary" 
      :icon="Plus" 
      @click="handleCreate"
      v-if="hasPermission('system:role:add')"
    >
      新增角色
    </el-button>
  </el-form-item>
</el-form>
```

---

## 数据表格

### 表格列定义

| 列标题 | 字段 | 宽度 | 说明 |
|--------|------|------|------|
| 角色名称 | `roleName` | 150px | 如"超级管理员" |
| 角色编码 | `roleCode` | 150px | 如"ROLE_ADMIN" |
| 排序 | `sort` | 80px | 数字，用于显示顺序 |
| 状态 | `status` | 80px | 标签：1=启用(绿色) 0=禁用(灰色) |
| 备注 | `remark` | 200px | 可为空，显示省略号 |
| 创建时间 | `createTime` | 160px | 格式 `yyyy-MM-dd HH:mm:ss` |
| 操作 | - | 220px | 操作按钮组 |

### 表格实现示例

```html
<el-table 
  :data="tableData" 
  v-loading="loading"
  border
  stripe
>
  <el-table-column prop="roleName" label="角色名称" width="150" />
  
  <el-table-column prop="roleCode" label="角色编码" width="150">
    <template #default="{ row }">
      <el-tag size="small" type="info">{{ row.roleCode }}</el-tag>
    </template>
  </el-table-column>
  
  <el-table-column prop="sort" label="排序" width="80" align="center" />
  
  <el-table-column prop="status" label="状态" width="80">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'info'">
        {{ row.status === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </el-table-column>
  
  <el-table-column prop="remark" label="备注" width="200" show-overflow-tooltip>
    <template #default="{ row }">
      {{ row.remark || '-' }}
    </template>
  </el-table-column>
  
  <el-table-column prop="createTime" label="创建时间" width="160" />
  
  <el-table-column label="操作" width="220" fixed="right">
    <template #default="{ row }">
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleEdit(row)"
        v-if="hasPermission('system:role:edit')"
      >
        编辑
      </el-button>
      
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleAssignPermission(row)"
        v-if="hasPermission('system:role:assign')"
      >
        分配权限
      </el-button>
      
      <el-button 
        link 
        type="danger" 
        size="small" 
        @click="handleDelete(row)"
        v-if="hasPermission('system:role:delete')"
      >
        删除
      </el-button>
    </template>
  </el-table-column>
</el-table>
```

---

## 分页控件

**组件**: `el-pagination`

```html
<el-pagination
  v-model:current-page="queryForm.page"
  v-model:page-size="queryForm.size"
  :page-sizes="[10, 20, 50, 100]"
  :total="total"
  layout="total, sizes, prev, pager, next, jumper"
  @size-change="handleQuery"
  @current-change="handleQuery"
  style="margin-top: 16px; justify-content: flex-end"
/>
```

**默认值**: `page=1`, `size=10`

---

## 对话框：创建/编辑角色

### 对话框属性

- **宽度**: `600px`
- **标题**: 创建时显示"新增角色"，编辑时显示"编辑角色"
- **关闭方式**: 点击遮罩层不关闭

### 表单字段

```typescript
interface RoleFormData {
  roleName: string       // 必填，唯一
  roleCode: string       // 必填，唯一，如 ROLE_MANAGER
  status: 0 | 1          // 必填，默认 1
  sort?: number          // 可选，默认 0
  remark?: string        // 可选
}
```

### 表单校验规则

```typescript
const rules = {
  roleName: [
    { required: true, message: '角色名称不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '角色名称长度在 2-50 个字符', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '角色编码不能为空', trigger: 'blur' },
    { 
      pattern: /^ROLE_[A-Z_]+$/, 
      message: '角色编码格式为 ROLE_ 开头的大写字母和下划线', 
      trigger: 'blur' 
    }
  ],
  status: [
    { required: true, message: '状态不能为空', trigger: 'change' }
  ]
}
```

### 表单布局示例

```html
<el-dialog 
  v-model="dialogVisible" 
  :title="dialogTitle" 
  width="600px"
  :close-on-click-modal="false"
>
  <el-form 
    ref="formRef" 
    :model="formData" 
    :rules="rules" 
    label-width="90px"
  >
    <el-form-item label="角色名称" prop="roleName">
      <el-input 
        v-model="formData.roleName" 
        placeholder="请输入角色名称，如：部门经理"
      />
    </el-form-item>
    
    <el-form-item label="角色编码" prop="roleCode">
      <el-input 
        v-model="formData.roleCode" 
        placeholder="请输入角色编码，如：ROLE_MANAGER"
        :disabled="isEdit"
      />
      <div style="font-size: 12px; color: #909399; margin-top: 4px">
        格式：ROLE_ 开头的大写字母和下划线
      </div>
    </el-form-item>
    
    <el-form-item label="排序" prop="sort">
      <el-input-number 
        v-model="formData.sort" 
        :min="0" 
        :max="9999"
        controls-position="right"
      />
    </el-form-item>
    
    <el-form-item label="状态" prop="status">
      <el-radio-group v-model="formData.status">
        <el-radio :value="1">启用</el-radio>
        <el-radio :value="0">禁用</el-radio>
      </el-radio-group>
    </el-form-item>
    
    <el-form-item label="备注" prop="remark">
      <el-input 
        v-model="formData.remark" 
        type="textarea" 
        :rows="3"
        placeholder="请输入备注信息"
        maxlength="200"
        show-word-limit
      />
    </el-form-item>
  </el-form>
  
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit" :loading="submitting">
      确定
    </el-button>
  </template>
</el-dialog>
```

**注意事项**:
- 编辑模式下，`roleCode` 字段禁用（不可修改）
- `roleCode` 必须以 `ROLE_` 开头，后接大写字母和下划线
- 排序值越小越靠前显示

---

## 对话框：分配权限（核心功能）

### 对话框属性

- **宽度**: `700px`
- **标题**: "分配权限 - {roleName}"
- **高度**: 内容区建议高度 `500px`，支持滚动

### 权限树结构

权限以菜单树形式展示，包括目录（M）、菜单（C）、按钮（F）和接口（A）四种类型。

```html
<el-dialog 
  v-model="permissionDialogVisible" 
  :title="`分配权限 - ${currentRole.roleName}`" 
  width="700px"
  :close-on-click-modal="false"
>
  <el-form>
    <el-form-item label="角色名称">
      <el-input v-model="currentRole.roleName" disabled />
    </el-form-item>
    
    <el-form-item label="菜单权限">
      <div style="border: 1px solid #dcdfe6; border-radius: 4px; padding: 12px; max-height: 400px; overflow-y: auto">
        <div style="margin-bottom: 12px">
          <el-checkbox 
            v-model="expandAll" 
            @change="handleExpandChange"
          >
            展开/折叠全部
          </el-checkbox>
          <el-checkbox 
            v-model="checkAll" 
            @change="handleCheckAllChange"
          >
            全选/全不选
          </el-checkbox>
          <el-checkbox 
            v-model="checkStrictly"
          >
            父子联动
          </el-checkbox>
        </div>
        
        <el-tree
          ref="treeRef"
          :data="menuTree"
          :props="treeProps"
          :default-expand-all="expandAll"
          :check-strictly="!checkStrictly"
          node-key="id"
          show-checkbox
          @check="handleTreeCheck"
        >
          <template #default="{ node, data }">
            <span class="tree-node-label">
              <el-tag size="small" :type="getMenuTypeTag(data.menuType)">
                {{ getMenuTypeLabel(data.menuType) }}
              </el-tag>
              <span style="margin-left: 8px">{{ data.menuName }}</span>
              <span v-if="data.permission" style="margin-left: 8px; color: #909399; font-size: 12px">
                ({{ data.permission }})
              </span>
            </span>
          </template>
        </el-tree>
      </div>
    </el-form-item>
  </el-form>
  
  <template #footer>
    <el-button @click="permissionDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSavePermissions" :loading="submitting">
      确定
    </el-button>
  </template>
</el-dialog>
```

### 树形控件配置

```typescript
const treeProps = {
  label: 'menuName',
  children: 'children'
}

const expandAll = ref(false)
const checkAll = ref(false)
const checkStrictly = ref(true)  // 默认父子联动

// 菜单类型标签颜色
const getMenuTypeTag = (type: MenuType) => {
  const tagMap = {
    M: '',           // 目录 - 默认色
    C: 'success',    // 菜单 - 绿色
    F: 'warning',    // 按钮 - 橙色
    A: 'info'        // 接口 - 灰色
  }
  return tagMap[type]
}

// 菜单类型文字
const getMenuTypeLabel = (type: MenuType) => {
  const labelMap = {
    M: '目录',
    C: '菜单',
    F: '按钮',
    A: '接口'
  }
  return labelMap[type]
}
```

### 树操作逻辑

```typescript
// 展开/折叠全部
const handleExpandChange = (val: boolean) => {
  const tree = treeRef.value
  if (!tree) return
  
  const allNodes = tree.store.nodesMap
  for (const key in allNodes) {
    allNodes[key].expanded = val
  }
}

// 全选/全不选
const handleCheckAllChange = (val: boolean) => {
  const tree = treeRef.value
  if (!tree) return
  
  if (val) {
    tree.setCheckedNodes(menuTree.value)
  } else {
    tree.setCheckedKeys([])
  }
}

// 树节点选中状态变化
const handleTreeCheck = () => {
  // 可用于实时统计已选权限数量
  const checkedCount = treeRef.value?.getCheckedKeys().length || 0
  console.log('已选择权限数量:', checkedCount)
}
```

### 数据加载与保存

```typescript
// 打开权限分配对话框
const handleAssignPermission = async (row: RoleVO) => {
  currentRole.value = row
  
  try {
    // 加载菜单树
    menuTree.value = await menusApi.getTree()
    
    // 加载角色已分配的菜单 ID
    const result = await rolesApi.getPermissions(row.id)
    
    // 回显选中状态
    nextTick(() => {
      treeRef.value?.setCheckedKeys(result.menuIds)
    })
    
    permissionDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载权限数据失败')
  }
}

// 保存权限分配
const handleSavePermissions = async () => {
  const tree = treeRef.value
  if (!tree) return
  
  // 获取选中的节点 ID（包括半选状态的父节点）
  const checkedKeys = tree.getCheckedKeys() as number[]
  const halfCheckedKeys = tree.getHalfCheckedKeys() as number[]
  const menuIds = [...checkedKeys, ...halfCheckedKeys]
  
  submitting.value = true
  try {
    await rolesApi.assignPermissions(currentRole.value.id, { menuIds })
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '权限分配失败')
  } finally {
    submitting.value = false
  }
}
```

**重要提示**:
- 保存时需要同时提交全选（`getCheckedKeys`）和半选（`getHalfCheckedKeys`）的节点 ID
- 半选状态表示父节点的部分子节点被选中，父节点也应包含在权限列表中
- 后端存储的是菜单 ID 列表，不是单独的 permission 字段

---

## 操作确认

### 删除确认

```typescript
const handleDelete = async (row: RoleVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色"${row.roleName}"吗？删除后关联用户将失去该角色权限。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await rolesApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
```

---

## 数据加载与状态管理

### 组件状态定义

```typescript
const loading = ref(false)
const submitting = ref(false)
const tableData = ref<RoleVO[]>([])
const total = ref(0)
const menuTree = ref<MenuTreeVO[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()

const queryForm = reactive<RoleQueryDTO>({
  page: 1,
  size: 10,
  roleName: ''
})
```

### 数据加载流程

```typescript
const loadData = async () => {
  loading.value = true
  try {
    const result = await rolesApi.getList(queryForm)
    tableData.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
```

---

## 权限控制

### 权限检查函数

```typescript
import { usePermissionStore } from '@/stores'

const permissionStore = usePermissionStore()
const hasPermission = (permission: string) => {
  return permissionStore.hasPermission(permission)
}
```

### 权限标识映射

| 操作 | 权限标识 | UI 元素 |
|------|----------|---------|
| 查看列表 | `system:role:list` | 导航菜单可见 |
| 新增角色 | `system:role:add` | "新增角色"按钮 |
| 编辑角色 | `system:role:edit` | 表格"编辑"按钮 |
| 删除角色 | `system:role:delete` | "删除"按钮 |
| 分配权限 | `system:role:assign` | "分配权限"按钮 |
| 查看权限 | `system:role:query` | 权限分配对话框数据加载 |

---

## 样式规范

### 颜色变量

沿用 Element Plus 默认主题色。

### 树形控件样式

```css
.tree-node-label {
  display: flex;
  align-items: center;
}

/* 树节点悬停效果 */
:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

/* 树节点复选框与文字间距 */
:deep(.el-tree-node__content) {
  padding: 4px 0;
}
```

---

## 交互反馈

### 加载状态

- 列表加载: `v-loading`
- 提交按钮: `loading` 属性
- 权限树加载: 对话框打开前显示加载动画

### 成功反馈

```typescript
ElMessage.success('操作成功')
```

### 错误处理

```typescript
try {
  await rolesApi.create(formData)
  ElMessage.success('创建成功')
} catch (error: any) {
  ElMessage.error(error.message || '操作失败')
}
```

---

## 文件组织建议

```
src/views/system/role/
├── index.vue                  # 主页面组件
├── components/
│   ├── RoleForm.vue           # 创建/编辑角色表单（可选拆分）
│   └── PermissionTree.vue     # 权限树选择器（可选拆分）
└── types.ts                   # 页面级类型定义（可选）
```

**建议**: 初期实现可将所有逻辑放在 `index.vue`；当权限树逻辑复杂或需要复用时，再拆分为独立组件。
