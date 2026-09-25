<template>
  <div class="menu-management">
    <div class="section-intro">
      <div><h1>菜单管理</h1><p>维护菜单层级、路径与权限标识。</p></div>
    </div>
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button 
        type="primary" 
        :icon="Plus" 
        @click="handleCreate"
        v-if="hasPermission('system:menu:add')"
      >
        新增菜单
      </el-button>
    </div>

    <!-- 树形表格 -->
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
          <span v-if="row.icon">{{ row.icon }}</span>
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

    <!-- 创建/编辑菜单对话框 -->
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
              />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { menusApi } from '@/api/menus'
import { usePermissionStore } from '@/stores/permission'
import type { MenuTreeVO, MenuCreateDTO, MenuUpdateDTO, MenuType } from '@/types/menu'

const permissionStore = usePermissionStore()
const hasPermission = (permission: string) => {
  return permissionStore.hasPermission(permission)
}

// 表格数据
const loading = ref(false)
const tableData = ref<MenuTreeVO[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number>()

const formData = reactive<MenuCreateDTO>({
  parentId: 0,
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  permission: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
  remark: ''
})

// 菜单树选项
const menuTreeOptions = ref<any[]>([])

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

// 表单校验规则（动态计算）
const rules = computed<FormRules>(() => ({
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

// 加载菜单树
const loadData = async () => {
  loading.value = true
  try {
    const data = await menusApi.getTree()
    tableData.value = data
  } catch (error: any) {
    ElMessage.error(error.message || '加载菜单树失败')
  } finally {
    loading.value = false
  }
}

// 构建上级菜单选项（过滤当前节点及其子节点）
const buildMenuTreeOptions = (editId?: number) => {
  const options = [
    { id: 0, menuName: '顶级菜单', children: [] as any[] }
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

// 新增
const handleCreate = () => {
  dialogTitle.value = '新增菜单'
  isEdit.value = false
  currentEditId.value = undefined
  Object.assign(formData, {
    parentId: 0,
    menuName: '',
    menuType: 'M',
    path: '',
    component: '',
    permission: '',
    icon: '',
    sort: 0,
    visible: 1,
    status: 1,
    remark: ''
  })
  menuTreeOptions.value = buildMenuTreeOptions()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: MenuTreeVO) => {
  dialogTitle.value = '编辑菜单'
  isEdit.value = true
  currentEditId.value = row.id
  Object.assign(formData, {
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path || '',
    component: row.component || '',
    permission: row.permission || '',
    icon: row.icon || '',
    sort: row.sort,
    visible: row.visible,
    status: row.status,
    remark: ''
  })
  menuTreeOptions.value = buildMenuTreeOptions(row.id)
  dialogVisible.value = true
}

// 新增下级菜单
const handleAddChild = (row: MenuTreeVO) => {
  dialogTitle.value = '新增菜单'
  isEdit.value = false
  currentEditId.value = undefined
  Object.assign(formData, {
    parentId: row.id,
    menuName: '',
    menuType: 'C',
    path: '',
    component: '',
    permission: '',
    icon: '',
    sort: 0,
    visible: 1,
    status: 1,
    remark: ''
  })
  menuTreeOptions.value = buildMenuTreeOptions()
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      // 清理不需要的字段
      const submitData: MenuCreateDTO | MenuUpdateDTO = {
        parentId: formData.parentId,
        menuName: formData.menuName,
        menuType: formData.menuType,
        sort: formData.sort,
        visible: formData.visible,
        status: formData.status,
        remark: formData.remark || undefined
      }
      
      // 根据类型添加对应字段
      if (formData.menuType === 'M' || formData.menuType === 'C') {
        submitData.path = formData.path
        submitData.icon = formData.icon || undefined
      }
      if (formData.menuType === 'C') {
        submitData.component = formData.component
      }
      if (formData.menuType === 'F' || formData.menuType === 'A') {
        submitData.permission = formData.permission
      }
      
      if (isEdit.value && currentEditId.value) {
        await menusApi.update(currentEditId.value, submitData)
        ElMessage.success('更新成功')
      } else {
        await menusApi.create(submitData)
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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.menu-management {
  padding: 0;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0;
}

:deep(.el-table__indent) {
  padding-left: 20px;
}

:deep(.el-table__expand-icon) {
  font-size: 14px;
}

:deep(.el-table__row .cell) {
  display: flex;
  align-items: center;
}
</style>
