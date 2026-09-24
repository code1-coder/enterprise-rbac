<template>
  <div class="role-management">
    <!-- 工具栏 -->
    <el-form :inline="true" :model="queryForm" class="toolbar-form">
      <el-form-item label="角色名称">
        <el-input 
          v-model="queryForm.roleName" 
          placeholder="请输入角色名称" 
          clearable 
          style="width: 200px"
        />
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
      
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

    <!-- 数据表格 -->
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

    <!-- 分页 -->
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

    <!-- 创建/编辑角色对话框 -->
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

    <!-- 分配权限对话框 -->
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
              <template #default="{ data }">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { rolesApi } from '@/api/roles'
import { menusApi } from '@/api/menus'
import { usePermissionStore } from '@/stores/permission'
import type { RoleVO, RoleQueryDTO, RoleCreateDTO, RoleUpdateDTO } from '@/types/role'
import type { MenuTreeVO, MenuType } from '@/types/menu'

const permissionStore = usePermissionStore()
const hasPermission = (permission: string) => {
  return permissionStore.hasPermission(permission)
}

// 查询表单
const queryForm = reactive<RoleQueryDTO>({
  page: 1,
  size: 10,
  roleName: ''
})

// 表格数据
const loading = ref(false)
const tableData = ref<RoleVO[]>([])
const total = ref(0)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<RoleCreateDTO | RoleUpdateDTO>({
  roleName: '',
  roleCode: '',
  status: 1,
  sort: 0,
  remark: ''
})

// 表单校验规则
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

// 权限分配对话框
const permissionDialogVisible = ref(false)
const currentRole = ref<RoleVO>({} as RoleVO)
const menuTree = ref<MenuTreeVO[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const expandAll = ref(false)
const checkAll = ref(false)
const checkStrictly = ref(true)

const treeProps = {
  label: 'menuName',
  children: 'children'
}

// 菜单类型标签颜色
const getMenuTypeTag = (type: MenuType) => {
  const tagMap: Record<MenuType, string> = {
    M: '',
    C: 'success',
    F: 'warning',
    A: 'info'
  }
  return tagMap[type]
}

// 菜单类型文字
const getMenuTypeLabel = (type: MenuType) => {
  const labelMap: Record<MenuType, string> = {
    M: '目录',
    C: '菜单',
    F: '按钮',
    A: '接口'
  }
  return labelMap[type]
}

// 加载角色列表
const loadData = async () => {
  loading.value = true
  try {
    const result = await rolesApi.getList(queryForm)
    tableData.value = result.records
    total.value = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryForm.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.page = 1
  queryForm.size = 10
  queryForm.roleName = ''
  loadData()
}

// 新增
const handleCreate = () => {
  dialogTitle.value = '新增角色'
  isEdit.value = false
  Object.assign(formData, {
    roleName: '',
    roleCode: '',
    status: 1,
    sort: 0,
    remark: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: RoleVO) => {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  Object.assign(formData, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    status: row.status,
    sort: row.sort,
    remark: row.remark || ''
  })
  currentRole.value = row
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (isEdit.value) {
        await rolesApi.update(currentRole.value.id, formData as RoleUpdateDTO)
        ElMessage.success('更新成功')
      } else {
        await rolesApi.create(formData as RoleCreateDTO)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadData()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 删除
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

// 分配权限
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
  } catch (error: any) {
    ElMessage.error(error.message || '加载权限数据失败')
  }
}

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
    // 获取所有节点ID
    const getAllNodeIds = (nodes: MenuTreeVO[]): number[] => {
      let ids: number[] = []
      nodes.forEach(node => {
        ids.push(node.id)
        if (node.children && node.children.length > 0) {
          ids = ids.concat(getAllNodeIds(node.children))
        }
      })
      return ids
    }
    tree.setCheckedKeys(getAllNodeIds(menuTree.value))
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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.toolbar-form {
  background: #fff;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
}

.tree-node-label {
  display: flex;
  align-items: center;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

:deep(.el-tree-node__content) {
  padding: 4px 0;
}
</style>
