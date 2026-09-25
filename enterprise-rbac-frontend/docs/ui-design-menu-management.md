# 菜单管理界面设计规范

> 实现进度（2026-09-25）：`src/views/system/menu/index.vue` 已有树形表格及目录、菜单、按钮、接口的创建、编辑和删除页面；本文代码段为设计示例，当前实现以组件和后端接口为准。页面级路由权限校验、完整组件测试及真实环境联调仍待完善。

## 概述

菜单管理模块负责系统菜单权限的树形管理，包括目录（M）、菜单（C）、按钮（F）和接口（A）四种类型的创建、编辑和删除。菜单树是权限体系的核心数据结构，界面设计需要清晰展示层级关系并支持高效的树形操作。

---

## 页面路由与权限

- **路由路径**: `/system/menu`
- **页面组件**: `src/views/system/menu/index.vue`
- **所需权限**: 已登录（导航可见需 `system:menu:list`）

---

## 整体布局

### 页面结构

```
┌─────────────────────────────────────────────────────────┐
│ 工具栏区域                                                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 树形表格区域                                              │
│ (el-table + tree-props)                                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 工具栏

**组件**: 简单按钮组

**布局**: 右对齐，只包含"新增菜单"按钮

```html
<div class="toolbar" style="display: flex; justify-content: flex-end; margin-bottom: 16px">
  <el-button 
    type="primary" 
    :icon="Plus" 
    @click="handleCreate"
    v-if="hasPermission('system:menu:add')"
  >
    新增菜单
  </el-button>
</div>
```

**注意**: 菜单管理通常不需要搜索过滤，因为树形结构本身就是最好的导航方式，且菜单总量有限。

---

## 树形表格

### 表格特性

- 使用 `el-table` 配合 `row-key` 和 `tree-props` 实现树形展示
- 支持展开/折叠节点
- 默认展开第一层（系统管理等顶级目录）
- 行内显示操作按钮

### 表格列定义

| 列标题 | 字段 | 宽度 | 说明 |
|--------|------|------|------|
| 菜单名称 | `menuName` | 250px | 树形展示，左侧有展开/折叠图标 |
| 类型 | `menuType` | 80px | M=目录 C=菜单 F=按钮 A=接口，用标签区分 |
| 图标 | `icon` | 80px | 可选，显示图标名称或预览 |
| 路径 | `path` | 180px | 前端路由路径，目录和菜单必填 |
| 组件路径 | `component` | 180px | 前端组件路径，菜单类型必填 |
| 权限标识 | `permission` | 180px | 如 `system:user:add`，按钮和接口必填 |
| 排序 | `sort` | 70px | 数字，同级排序 |
| 状态 | `status` | 80px | 标签：1=启用(绿色) 0=禁用(灰色) |
| 操作 | - | 180px | 操作按钮组 |

### 表格实现示例

```html
<el-table
  :data="tableData"
  v-loading="loading"
  row-key="id"
  :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
  :default-expand-all="false"
  border
>
  <el-table-column prop="menuName" label="菜单名称" width="250" show-overflow-tooltip />
  
  <el-table-column prop="menuType" label="类型" width="80" align="center">
    <template #default="{ row }">
      <el-tag :type="getMenuTypeTag(row.menuType)" size="small">
        {{ getMenuTypeLabel(row.menuType) }}
      </el-tag>
    </template>
  </el-table-column>
  
  <el-table-column prop="icon" label="图标" width="80" align="center">
    <template #default="{ row }">
      <el-icon v-if="row.icon" :size="18">
        <component :is="row.icon" />
      </el-icon>
      <span v-else>-</span>
    </template>
  </el-table-column>
  
  <el-table-column prop="path" label="路径" width="180" show-overflow-tooltip>
    <template #default="{ row }">
      {{ row.path || '-' }}
    </template>
  </el-table-column>
  
  <el-table-column prop="component" label="组件路径" width="180" show-overflow-tooltip>
    <template #default="{ row }">
      {{ row.component || '-' }}
    </template>
  </el-table-column>
  
  <el-table-column prop="permission" label="权限标识" width="180" show-overflow-tooltip>
    <template #default="{ row }">
      <el-tag v-if="row.permission" size="small" type="info">
        {{ row.permission }}
      </el-tag>
      <span v-else>-</span>
    </template>
  </el-table-column>
  
  <el-table-column prop="sort" label="排序" width="70" align="center" />
  
  <el-table-column prop="status" label="状态" width="80" align="center">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
        {{ row.status === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </el-table-column>
  
  <el-table-column label="操作" width="180" fixed="right">
    <template #default="{ row }">
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleEdit(row)"
        v-if="hasPermission('system:menu:edit')"
      >
        编辑
      </el-button>
      
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleAddChild(row)"
        v-if="hasPermission('system:menu:add')"
      >
        新增下级
      </el-button>
      
      <el-button 
        link 
        type="danger" 
        size="small" 
        @click="handleDelete(row)"
        v-if="hasPermission('system:menu:delete')"
      >
        删除
      </el-button>
    </template>
  </el-table-column>
</el-table>
```

### 菜单类型标识

```typescript
const getMenuTypeTag = (type: MenuType) => {
  const tagMap: Record<MenuType, string> = {
    M: '',           // 目录 - 默认色
    C: 'success',    // 菜单 - 绿色
    F: 'warning',    // 按钮 - 橙色
    A: 'info'        // 接口 - 灰色
  }
  return tagMap[type]
}

const getMenuTypeLabel = (type: MenuType) => {
  const labelMap: Record<MenuType, string> = {
    M: '目录',
    C: '菜单',
    F: '按钮',
    A: '接口'
  }
  return labelMap[type]
}
```

---

## 对话框：创建/编辑菜单

### 对话框属性

- **宽度**: `700px`
- **标题**: 创建时显示"新增菜单"，编辑时显示"编辑菜单"
- **关闭方式**: 点击遮罩层不关闭

### 表单字段

```typescript
interface MenuFormData {
  parentId: number       // 必填，0 表示顶级
  menuName: string       // 必填
  menuType: MenuType     // 必填，M/C/F/A
  path?: string          // M 和 C 必填
  component?: string     // C 必填
  permission?: string    // F 和 A 必填
  icon?: string          // 可选
  sort?: number          // 可选，默认 0
  visible?: 0 | 1        // 可选，默认 1
  status?: 0 | 1         // 可选，默认 1
  remark?: string        // 可选
}
```

### 表单校验规则

```typescript
const rules = computed(() => ({
  menuName: [
    { required: true, message: '菜单名称不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '菜单名称长度在 2-50 个字符', trigger: 'blur' }
  ],
  menuType: [
    { required: true, message: '菜单类型不能为空', trigger: 'change' }
  ],
  parentId: [
    { required: true, message: '上级菜单不能为空', trigger: 'change' }
  ],
  path: [
    { 
      required: formData.menuType === 'M' || formData.menuType === 'C', 
      message: '路径不能为空', 
      trigger: 'blur' 
    }
  ],
  component: [
    { 
      required: formData.menuType === 'C', 
      message: '组件路径不能为空', 
      trigger: 'blur' 
    }
  ],
  permission: [
    { 
      required: formData.menuType === 'F' || formData.menuType === 'A', 
      message: '权限标识不能为空', 
      trigger: 'blur' 
    }
  ]
}))
```

### 表单布局示例

```html
<el-dialog 
  v-model="dialogVisible" 
  :title="dialogTitle" 
  width="700px"
  :close-on-click-modal="false"
>
  <el-form 
    ref="formRef" 
    :model="formData" 
    :rules="rules" 
    label-width="100px"
  >
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="formData.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      
      <el-col :span="12">
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="formData.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
            <el-radio value="A">接口</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="菜单名称" prop="menuName">
          <el-input 
            v-model="formData.menuName" 
            placeholder="请输入菜单名称"
          />
        </el-form-item>
      </el-col>
      
      <el-col :span="12">
        <el-form-item label="图标" prop="icon">
          <el-input 
            v-model="formData.icon" 
            placeholder="请输入图标名称"
          >
            <template #append>
              <el-button @click="handleSelectIcon">选择</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item 
          label="路由路径" 
          prop="path"
          v-if="formData.menuType === 'M' || formData.menuType === 'C'"
        >
          <el-input 
            v-model="formData.path" 
            placeholder="如：/system/user"
          />
        </el-form-item>
      </el-col>
      
      <el-col :span="12">
        <el-form-item 
          label="组件路径" 
          prop="component"
          v-if="formData.menuType === 'C'"
        >
          <el-input 
            v-model="formData.component" 
            placeholder="如：system/user/index"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item 
          label="权限标识" 
          prop="permission"
          v-if="formData.menuType === 'F' || formData.menuType === 'A'"
        >
          <el-input 
            v-model="formData.permission" 
            placeholder="如：system:user:add"
          />
        </el-form-item>
      </el-col>
      
      <el-col :span="12">
        <el-form-item label="排序" prop="sort">
          <el-input-number 
            v-model="formData.sort" 
            :min="0" 
            :max="9999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="显示状态" prop="visible">
          <el-radio-group v-model="formData.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      
      <el-col :span="12">
        <el-form-item label="菜单状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>
    
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

### 表单动态逻辑

1. **上级菜单选择**:
   - 使用 `el-tree-select` 组件展示菜单树
   - `parentId=0` 表示顶级菜单
   - 编辑时需要过滤掉当前节点及其所有子节点（防止循环引用）
   - `check-strictly` 允许选择任意节点，不受父子关联限制

2. **字段显示联动**:
   - **目录（M）**: 显示 `path`，隐藏 `component` 和 `permission`
   - **菜单（C）**: 显示 `path` 和 `component`，隐藏 `permission`
   - **按钮（F）**: 显示 `permission`，隐藏 `path` 和 `component`
   - **接口（A）**: 显示 `permission`，隐藏 `path` 和 `component`

3. **图标选择器（可选增强）**:
   - 点击"选择"按钮打开图标选择对话框
   - 展示常用 Element Plus 图标或自定义图标库
   - 选中后回填到输入框

---

## 树形数据处理

### 数据加载

```typescript
const loadData = async () => {
  loading.value = true
  try {
    // 后端返回扁平数组或已构建的树结构
    const data = await menusApi.getTree()
    tableData.value = data
  } catch (error) {
    ElMessage.error('加载菜单树失败')
  } finally {
    loading.value = false
  }
}
```

### 构建上级菜单选项

编辑时需要排除当前节点及其子节点：

```typescript
const buildMenuTreeOptions = (editId?: number) => {
  const options = [
    { id: 0, menuName: '顶级菜单', children: [] }
  ]
  
  // 深拷贝原始树
  const clonedTree = JSON.parse(JSON.stringify(tableData.value))
  
  // 如果是编辑模式，过滤掉当前节点及其子孙
  if (editId) {
    const filteredTree = filterTreeNode(clonedTree, editId)
    options[0].children = filteredTree
  } else {
    options[0].children = clonedTree
  }
  
  return options
}

// 递归过滤树节点
const filterTreeNode = (tree: MenuTreeVO[], excludeId: number): MenuTreeVO[] => {
  return tree
    .filter(node => node.id !== excludeId)
    .map(node => ({
      ...node,
      children: node.children ? filterTreeNode(node.children, excludeId) : []
    }))
}
```

### 新增下级菜单

```typescript
const handleAddChild = (row: MenuTreeVO) => {
  formData.value = {
    parentId: row.id,  // 父级设为当前节点
    menuName: '',
    menuType: 'C',     // 默认为菜单类型
    sort: 0,
    visible: 1,
    status: 1
  }
  
  dialogTitle.value = '新增菜单'
  isEdit.value = false
  menuTreeOptions.value = buildMenuTreeOptions()
  dialogVisible.value = true
}
```

---

## 操作确认

### 删除确认

删除菜单前需检查是否有子节点：

```typescript
const handleDelete = async (row: MenuTreeVO) => {
  // 检查是否有子节点
  if (row.children && row.children.length > 0) {
    ElMessage.warning('该菜单包含下级菜单，无法删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除菜单"${row.menuName}"吗？删除后关联角色将失去该权限。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await menusApi.delete(row.id)
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
const tableData = ref<MenuTreeVO[]>([])
const menuTreeOptions = ref<MenuTreeVO[]>([])

const formData = ref<MenuFormData>({
  parentId: 0,
  menuName: '',
  menuType: 'M',
  sort: 0,
  visible: 1,
  status: 1
})
```

### 初始化加载

```typescript
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
| 查看列表 | `system:menu:list` | 导航菜单可见 |
| 新增菜单 | `system:menu:add` | "新增菜单"按钮、"新增下级"按钮 |
| 编辑菜单 | `system:menu:edit` | "编辑"按钮 |
| 删除菜单 | `system:menu:delete` | "删除"按钮 |

---

## 样式规范

### 颜色变量

沿用 Element Plus 默认主题色。

### 树形表格样式

```css
/* 树形缩进 */
:deep(.el-table__indent) {
  padding-left: 20px;
}

/* 展开图标 */
:deep(.el-table__expand-icon) {
  font-size: 14px;
}

/* 树节点文字对齐 */
:deep(.el-table__row .cell) {
  display: flex;
  align-items: center;
}
```

---

## 交互反馈

### 加载状态

- 列表加载: `v-loading`
- 提交按钮: `loading` 属性

### 成功反馈

```typescript
ElMessage.success('操作成功')
```

### 错误处理

```typescript
try {
  await menusApi.create(formData)
  ElMessage.success('创建成功')
} catch (error: any) {
  ElMessage.error(error.message || '操作失败')
}
```

### 特殊提示

1. **删除拦截**: 有子节点时提示"该菜单包含下级菜单，无法删除"
2. **循环引用**: 编辑时上级菜单选择器自动过滤当前节点及其子节点

---

## 可选增强功能

### 图标选择器

创建独立的图标选择对话框，展示常用图标供用户点选：

```html
<el-dialog v-model="iconDialogVisible" title="选择图标" width="800px">
  <div class="icon-grid">
    <div 
      v-for="icon in iconList" 
      :key="icon" 
      class="icon-item"
      :class="{ active: selectedIcon === icon }"
      @click="selectedIcon = icon"
    >
      <el-icon :size="24">
        <component :is="icon" />
      </el-icon>
      <span>{{ icon }}</span>
    </div>
  </div>
  
  <template #footer>
    <el-button @click="iconDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleConfirmIcon">
      确定
    </el-button>
  </template>
</el-dialog>
```

图标列表可从 Element Plus 图标库或项目自定义图标集中获取。

### 拖拽排序（高级）

使用 Sortable.js 或 Element Plus 的拖拽指令实现同级菜单拖拽排序，提升操作效率。实现复杂度较高，建议在基础功能稳定后再考虑。

---

## 文件组织建议

```
src/views/system/menu/
├── index.vue              # 主页面组件
├── components/
│   ├── MenuForm.vue       # 创建/编辑菜单表单（可选拆分）
│   └── IconSelector.vue   # 图标选择器（可选增强）
└── types.ts               # 页面级类型定义（可选）
```

**建议**: 初期实现可将所有逻辑放在 `index.vue`；当表单逻辑复杂或图标选择器需要复用时，再拆分为独立组件。

---

## 设计要点总结

1. **树形展示**: 使用 `el-table` 的树形属性清晰展示菜单层级关系
2. **类型区分**: 用颜色标签直观区分 M/C/F/A 四种菜单类型
3. **动态表单**: 根据菜单类型动态显示/隐藏相关字段
4. **循环引用防护**: 编辑时上级菜单选择器自动过滤当前节点及其后代
5. **删除保护**: 有子节点时拦截删除操作
6. **快捷操作**: "新增下级"按钮快速在当前节点下添加子菜单
7. **权限控制**: 所有操作按钮根据用户权限动态显示
