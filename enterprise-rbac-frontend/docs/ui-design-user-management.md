# 用户管理界面设计规范

> 实现进度（2026-09-25）：`src/views/system/user/index.vue` 已实现查询、增删改、批量删除、角色选择/名称回显及重置密码；本文件中的示例是设计参考，实际交互以当前组件为准。角色标签显示 `roleNames`，角色分配提交数字型 `roleIds`。关键表单和真实后端联调仍需补测。

## 概述

用户管理模块负责系统用户的全生命周期管理，包括用户查询、创建、编辑、删除、角色分配和密码操作。界面设计遵循数据密集型后台管理系统的设计原则，强调信息组织的清晰性和操作流程的高效性。

---

## 页面路由与权限

- **路由路径**: `/system/user`
- **页面组件**: `src/views/system/user/index.vue`
- **所需权限**: 已登录（导航可见需 `system:user:list`）

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
  <el-form-item label="用户名">
    <el-input v-model="queryForm.username" placeholder="请输入用户名" clearable />
  </el-form-item>
  
  <el-form-item label="状态">
    <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
      <el-option label="启用" :value="1" />
      <el-option label="禁用" :value="0" />
    </el-select>
  </el-form-item>
  
  <el-form-item>
    <el-button type="primary" @click="handleQuery">查询</el-button>
    <el-button @click="handleReset">重置</el-button>
  </el-form-item>
  
  <!-- 右侧：操作按钮（使用 flex 推到右侧）-->
  <el-form-item style="margin-left: auto">
    <el-button 
      type="primary" 
      :icon="Plus" 
      @click="handleCreate"
      v-if="hasPermission('system:user:add')"
    >
      新增用户
    </el-button>
    <el-button 
      type="danger" 
      :icon="Delete" 
      @click="handleBatchDelete"
      :disabled="selectedIds.length === 0"
      v-if="hasPermission('system:user:delete')"
    >
      批量删除
    </el-button>
  </el-form-item>
</el-form>
```

---

## 数据表格

### 表格列定义

| 列标题 | 字段 | 宽度 | 说明 |
|--------|------|------|------|
| 复选框 | - | 55px | 多选列，用于批量操作 |
| 用户名 | `username` | 120px | 唯一标识，不可重复 |
| 昵称 | `nickname` | 120px | 可为空 |
| 邮箱 | `email` | 180px | 可为空 |
| 手机号 | `phone` | 130px | 可为空 |
| 状态 | `status` | 80px | 标签组件：1=启用(绿色) 0=禁用(灰色) |
| 角色 | `roles` | 180px | 角色名称列表，用逗号分隔或标签组显示 |
| 创建时间 | `createTime` | 160px | 格式 `yyyy-MM-dd HH:mm:ss` |
| 操作 | - | 280px | 操作按钮组 |

### 表格实现示例

```html
<el-table 
  :data="tableData" 
  @selection-change="handleSelectionChange"
  v-loading="loading"
  border
  stripe
>
  <el-table-column type="selection" width="55" />
  
  <el-table-column prop="username" label="用户名" width="120" />
  
  <el-table-column prop="nickname" label="昵称" width="120">
    <template #default="{ row }">
      {{ row.nickname || '-' }}
    </template>
  </el-table-column>
  
  <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
  
  <el-table-column prop="phone" label="手机号" width="130" />
  
  <el-table-column prop="status" label="状态" width="80">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'info'">
        {{ row.status === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </el-table-column>
  
  <el-table-column label="角色" width="180">
    <template #default="{ row }">
      <el-tag 
        v-for="role in row.roles" 
        :key="role.id" 
        size="small" 
        style="margin-right: 4px"
      >
        {{ role.roleName }}
      </el-tag>
      <span v-if="!row.roles || row.roles.length === 0">-</span>
    </template>
  </el-table-column>
  
  <el-table-column prop="createTime" label="创建时间" width="160" />
  
  <el-table-column label="操作" width="280" fixed="right">
    <template #default="{ row }">
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleEdit(row)"
        v-if="hasPermission('system:user:edit')"
      >
        编辑
      </el-button>
      
      <el-button 
        link 
        type="primary" 
        size="small" 
        @click="handleAssignRole(row)"
        v-if="hasPermission('system:user:role')"
      >
        分配角色
      </el-button>
      
      <el-button 
        link 
        type="warning" 
        size="small" 
        @click="handleResetPassword(row)"
        v-if="hasPermission('system:user:resetPwd')"
      >
        重置密码
      </el-button>
      
      <el-button 
        link 
        type="danger" 
        size="small" 
        @click="handleDelete(row)"
        v-if="hasPermission('system:user:delete')"
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

**布局**: 居右对齐，显示总数、每页条数选择器、页码跳转

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

## 对话框：创建/编辑用户

### 对话框属性

- **宽度**: `600px`
- **标题**: 创建时显示"新增用户"，编辑时显示"编辑用户"
- **关闭方式**: 点击遮罩层不关闭（`close-on-click-modal="false"`）

### 表单字段（创建用户）

```typescript
interface UserFormData {
  username: string       // 必填，唯一
  password: string       // 必填（仅创建时）
  nickname?: string      // 可选
  email?: string         // 可选
  phone?: string         // 可选
  status: 0 | 1          // 必填，默认 1
  roleIds?: number[]     // 可选，角色多选
}
```

### 表单校验规则

```typescript
const rules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度不少于 6 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
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
    <el-form-item label="用户名" prop="username">
      <el-input 
        v-model="formData.username" 
        placeholder="请输入用户名"
        :disabled="isEdit"
      />
    </el-form-item>
    
    <el-form-item label="密码" prop="password" v-if="!isEdit">
      <el-input 
        v-model="formData.password" 
        type="password" 
        placeholder="请输入密码"
        show-password
      />
    </el-form-item>
    
    <el-form-item label="昵称" prop="nickname">
      <el-input v-model="formData.nickname" placeholder="请输入昵称" />
    </el-form-item>
    
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="formData.email" placeholder="请输入邮箱" />
    </el-form-item>
    
    <el-form-item label="手机号" prop="phone">
      <el-input v-model="formData.phone" placeholder="请输入手机号" />
    </el-form-item>
    
    <el-form-item label="状态" prop="status">
      <el-radio-group v-model="formData.status">
        <el-radio :value="1">启用</el-radio>
        <el-radio :value="0">禁用</el-radio>
      </el-radio-group>
    </el-form-item>
    
    <el-form-item label="分配角色" prop="roleIds" v-if="!isEdit">
      <el-select 
        v-model="formData.roleIds" 
        multiple 
        placeholder="请选择角色"
        style="width: 100%"
      >
        <el-option 
          v-for="role in roleOptions" 
          :key="role.id" 
          :label="role.roleName" 
          :value="role.id" 
        />
      </el-select>
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
- 编辑模式下，`username` 字段禁用（不可修改）
- 编辑模式下不显示密码字段（密码修改走专门的密码重置接口）
- 创建时可同时分配角色；编辑时角色分配走独立的"分配角色"对话框

---

## 对话框：分配角色

### 对话框属性

- **宽度**: `500px`
- **标题**: "分配角色 - {username}"

### 表单内容

```html
<el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
  <el-form>
    <el-form-item label="用户名">
      <el-input v-model="currentUser.username" disabled />
    </el-form-item>
    
    <el-form-item label="选择角色">
      <el-select 
        v-model="selectedRoleIds" 
        multiple 
        placeholder="请选择角色"
        style="width: 100%"
      >
        <el-option 
          v-for="role in roleOptions" 
          :key="role.id" 
          :label="role.roleName" 
          :value="role.id" 
        />
      </el-select>
    </el-form-item>
  </el-form>
  
  <template #footer>
    <el-button @click="roleDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSaveRoles" :loading="submitting">
      确定
    </el-button>
  </template>
</el-dialog>
```

**数据加载**:
- 打开对话框时调用 `GET /api/roles/list` 获取所有角色选项
- 调用 `GET /api/users/{id}` 获取用户当前角色，用于回显选中状态
- 提交时调用 `PUT /api/users/{id}/roles`，传递 `{ roleIds: number[] }`

---

## 对话框：重置密码

### 对话框属性

- **宽度**: `450px`
- **标题**: "重置密码 - {username}"

### 表单内容

```html
<el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="450px">
  <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
    <el-form-item label="用户名">
      <el-input v-model="currentUser.username" disabled />
    </el-form-item>
    
    <el-form-item label="新密码" prop="newPassword">
      <el-input 
        v-model="pwdForm.newPassword" 
        type="password" 
        placeholder="请输入新密码"
        show-password
      />
    </el-form-item>
    
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input 
        v-model="pwdForm.confirmPassword" 
        type="password" 
        placeholder="请再次输入新密码"
        show-password
      />
    </el-form-item>
  </el-form>
  
  <template #footer>
    <el-button @click="resetPwdDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmitResetPwd" :loading="submitting">
      确定
    </el-button>
  </template>
</el-dialog>
```

**校验规则**:
```typescript
const pwdRules = {
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度不少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
```

**API 调用**: `PUT /api/users/{id}/reset-password`，传递 `{ newPassword: string }`

---

## 操作确认

### 删除确认

使用 `ElMessageBox.confirm` 进行二次确认：

```typescript
const handleDelete = async (row: UserVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户"${row.username}"吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await usersApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
```

### 批量删除确认

```typescript
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.length} 个用户吗？此操作不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await usersApi.batchDelete(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
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
const tableData = ref<UserVO[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const roleOptions = ref<RoleOption[]>([])

const queryForm = reactive<UserQueryDTO>({
  page: 1,
  size: 10,
  username: '',
  status: undefined
})
```

### 数据加载流程

```typescript
const loadData = async () => {
  loading.value = true
  try {
    const result = await usersApi.getList(queryForm)
    tableData.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadRoleOptions = async () => {
  try {
    roleOptions.value = await rolesApi.getList()
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  }
}

onMounted(() => {
  loadData()
  loadRoleOptions()
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
| 查看列表 | `system:user:list` | 导航菜单可见 |
| 新增用户 | `system:user:add` | "新增用户"按钮 |
| 编辑用户 | `system:user:edit` | 表格"编辑"按钮 |
| 删除用户 | `system:user:delete` | "删除"按钮、"批量删除"按钮 |
| 分配角色 | `system:user:role` | "分配角色"按钮 |
| 重置密码 | `system:user:resetPwd` | "重置密码"按钮 |

---

## 样式规范

### 颜色变量

- **主色调**: Element Plus 默认 `--el-color-primary` (#409EFF)
- **成功**: `--el-color-success` (#67C23A)
- **警告**: `--el-color-warning` (#E6A23C)
- **危险**: `--el-color-danger` (#F56C6C)
- **信息**: `--el-color-info` (#909399)

### 间距规范

- 工具栏与表格间距: `16px`
- 表格与分页间距: `16px`
- 表单项间距: Element Plus 默认（`18px`）
- 按钮组间距: `8px`

### 响应式适配

- 最小宽度: `1200px` (表格需要足够宽度展示所有列)
- 小于该宽度时，操作列使用固定定位（`fixed="right"`）
- 长文本列使用 `show-overflow-tooltip` 显示省略号

---

## 交互反馈

### 加载状态

- 列表加载: `el-table` 使用 `v-loading` 指令
- 按钮操作: 提交按钮使用 `loading` 属性

### 成功反馈

```typescript
ElMessage.success('操作成功')
```

### 错误处理

```typescript
try {
  await usersApi.create(formData)
  ElMessage.success('创建成功')
} catch (error: any) {
  ElMessage.error(error.message || '操作失败')
}
```

### 空状态

当表格无数据时，显示 Element Plus 默认空状态提示。

---

## 文件组织建议

```
src/views/system/user/
├── index.vue              # 主页面组件
├── components/
│   ├── UserForm.vue       # 创建/编辑用户表单（可选拆分）
│   ├── RoleAssign.vue     # 角色分配对话框（可选拆分）
│   └── ResetPassword.vue  # 重置密码对话框（可选拆分）
└── types.ts               # 页面级类型定义（可选）
```

**建议**: 初期实现可将所有逻辑放在 `index.vue`；当单个组件超过 500 行或对话框逻辑复杂时，再拆分为独立子组件。
