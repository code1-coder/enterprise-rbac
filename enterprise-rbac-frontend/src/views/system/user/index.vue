<template>
  <div class="user-management">
    <div class="section-intro">
      <div><h1>用户管理</h1><p>管理账号信息、状态与角色分配。</p></div>
    </div>
    <el-form :inline="true" :model="queryForm" class="toolbar-form">
      <el-form-item label="用户名">
        <el-input v-model="queryForm.username" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
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
      <el-form-item style="margin-left: auto">
        <el-button v-if="hasPermission('system:user:add')" type="primary" @click="handleCreate">新增用户</el-button>
        <el-button v-if="hasPermission('system:user:delete')" type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120">
        <template #default="{ row }">{{ row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <template v-if="row.roles && row.roles.length > 0">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right: 4px">{{ role.roleName }}</el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="hasPermission('system:user:edit')" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="hasPermission('system:user:role')" link type="primary" size="small" @click="handleAssignRole(row)">分配角色</el-button>
          <el-button v-if="hasPermission('system:user:resetPwd')" link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button v-if="hasPermission('system:user:delete')" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleQuery" @current-change="handleQuery" style="margin-top: 16px; justify-content: flex-end" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
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
        <el-form-item v-if="!isEdit" label="分配角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="`分配角色 - ${currentUser.username}`" width="500px">
      <el-form>
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="选择角色">
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveRoles">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdDialogVisible" :title="`重置密码 - ${currentUser.username}`" width="450px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { usersApi } from '@/api/users'
import { rolesApi } from '@/api/roles'
import { usePermissionStore } from '@/stores/permission'
import type { UserVO, UserQueryDTO, UserCreateDTO, UserUpdateDTO } from '@/types/user'
import type { RoleOption } from '@/types/role'

const permissionStore = usePermissionStore()
const hasPermission = (permission: string) => permissionStore.hasPermission(permission)

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryForm = reactive<UserQueryDTO>({
  page: 1,
  size: 10,
  username: '',
  status: undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<UserCreateDTO>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})

const roleOptions = ref<RoleOption[]>([])
const roleDialogVisible = ref(false)
const currentUser = ref<Partial<UserVO>>({})
const selectedRoleIds = ref<number[]>([])
const resetPwdDialogVisible = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = ref({
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度不少于 6 个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const pwdRules: FormRules = {
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度不少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const result = await usersApi.getList(queryForm)
    tableData.value = result.records
    total.value = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadRoleOptions = async () => {
  try {
    roleOptions.value = await rolesApi.getAllRoles()
  } catch (error: any) {
    ElMessage.error(error.message || '加载角色列表失败')
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadData()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.status = undefined
  handleQuery()
}

const handleSelectionChange = (selection: UserVO[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleCreate = () => {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  formData.value = {
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    status: 1,
    roleIds: []
  }
  dialogVisible.value = true
}

const handleEdit = async (row: UserVO) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  currentUser.value = row
  formData.value = {
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    status: row.status,
    roleIds: []
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        const updateData: UserUpdateDTO = {
          nickname: formData.value.nickname,
          email: formData.value.email,
          phone: formData.value.phone,
          status: formData.value.status
        }
        await usersApi.update(currentUser.value.id!, updateData)
        ElMessage.success('更新成功')
      } else {
        await usersApi.create(formData.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleAssignRole = async (row: UserVO) => {
  currentUser.value = row
  try {
    const user = await usersApi.getById(row.id)
    selectedRoleIds.value = user.roles?.map(r => r.id) || []
    roleDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载用户角色失败')
  }
}

const handleSaveRoles = async () => {
  submitting.value = true
  try {
    await usersApi.assignRoles(currentUser.value.id!, { roleIds: selectedRoleIds.value })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '角色分配失败')
  } finally {
    submitting.value = false
  }
}

const handleResetPassword = (row: UserVO) => {
  currentUser.value = row
  pwdForm.value = {
    newPassword: '',
    confirmPassword: ''
  }
  resetPwdDialogVisible.value = true
}

const handleSubmitResetPwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await usersApi.resetPassword(currentUser.value.id!, { newPassword: pwdForm.value.newPassword })
      ElMessage.success('密码重置成功')
      resetPwdDialogVisible.value = false
    } catch (error: any) {
      ElMessage.error(error.message || '密码重置失败')
    } finally {
      submitting.value = false
    }
  })
}

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
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个用户吗？此操作不可恢复。`,
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
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadRoleOptions()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}
.toolbar-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 0;
}
</style>
