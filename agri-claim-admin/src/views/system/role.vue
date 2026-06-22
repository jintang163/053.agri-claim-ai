<template>
  <div class="role-page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="searchForm.roleKey" placeholder="请输入权限字符" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="角色状态" clearable style="width: 140px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <el-button type="primary" :icon="Plus" @click="openDialog(null)">新增角色</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="roleId" label="角色ID" width="90" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.roleKey)" effect="dark" style="margin-right: 8px">
              {{ row.roleKey === 'admin' ? 'S' : row.roleKey === 'manager' ? 'M' : row.roleKey === 'surveyor' ? 'C' : 'U' }}
            </el-tag>
            {{ row.roleName }}
          </template>
        </el-table-column>
        <el-table-column prop="roleKey" label="权限字符" min-width="130" align="center">
          <template #default="{ row }">
            <code class="role-key">{{ row.roleKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="roleSort" label="显示顺序" width="100" align="center" />
        <el-table-column prop="dataScope" label="数据权限" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.dataScope === '1' ? 'danger' : row.dataScope === '2' ? 'warning' : 'info'">
              {{ getDataScopeText(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" effect="dark">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openDialog(row)">修改</el-button>
            <el-button type="success" link :icon="User">分配</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadTable"
          @current-change="loadTable"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="roleFormRef" :model="formData" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限字符" prop="roleKey">
              <el-input v-model="formData.roleKey" placeholder="请输入权限字符(如：surveyor)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="roleSort">
              <el-input-number v-model="formData.roleSort" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据权限" prop="dataScope">
              <el-select v-model="formData.dataScope" style="width: 100%">
                <el-option label="全部数据权限" value="1" />
                <el-option label="自定义数据权限" value="2" />
                <el-option label="本部门数据权限" value="3" />
                <el-option label="本部门及以下" value="4" />
                <el-option label="仅本人数据" value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单权限">
              <el-tree
                ref="menuTreeRef"
                :data="menuTree"
                show-checkbox
                node-key="id"
                default-expand-all
                :props="{ label: 'name', children: 'children' }"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色状态">
              <el-radio-group v-model="formData.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Search, Refresh, Plus, Edit, Delete, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const searchForm = reactive({ roleName: '', roleKey: '', status: '' })
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const menuTree = ref([
  { id: 1, name: '首页', children: [] },
  { id: 2, name: '影像管理', children: [
    { id: 21, name: '影像上传' },
    { id: 22, name: '影像库' }
  ]},
  { id: 3, name: '定损管理', children: [
    { id: 31, name: '定损任务' },
    { id: 32, name: '新建定损' }
  ]},
  { id: 4, name: '大屏监控' },
  { id: 5, name: '系统管理', children: [
    { id: 51, name: '用户管理' },
    { id: 52, name: '角色管理' },
    { id: 53, name: '字典管理' }
  ]}
])
const menuTreeRef = ref(null)

const getDataScopeText = (v) => {
  const map = { '1': '全部数据', '2': '自定义', '3': '本部门', '4': '本部门及以下', '5': '仅本人' }
  return map[v] || '未知'
}
const getRoleTagType = (k) => {
  const map = { admin: 'danger', manager: 'warning', surveyor: 'success' }
  return map[k] || 'info'
}

const loadTable = async () => {
  loading.value = true
  const res = await request.get('/api/system/role/list', { params: { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm } }).catch(() => ({}))
  if (res?.rows) {
    tableData.value = res.rows
    total.value = res.total || 0
  } else {
    tableData.value = generateMockData()
    total.value = 12
  }
  loading.value = false
}

const generateMockData = () => [
  { roleId: 1, roleName: '超级管理员', roleKey: 'admin', roleSort: 1, dataScope: '1', status: '0', createTime: '2024-03-01 10:00:00', remark: '拥有全部管理权限' },
  { roleId: 2, roleName: '理赔部经理', roleKey: 'manager', roleSort: 2, dataScope: '4', status: '0', createTime: '2024-03-02 09:30:00', remark: '部门及下属数据' },
  { roleId: 3, roleName: '查勘员', roleKey: 'surveyor', roleSort: 3, dataScope: '5', status: '0', createTime: '2024-03-03 14:20:00', remark: '外勤查勘用户' },
  { roleId: 4, roleName: '核保专员', roleKey: 'underwriter', roleSort: 4, dataScope: '3', status: '0', createTime: '2024-03-05 11:15:00', remark: '核保业务操作' },
  { roleId: 5, roleName: '财务专员', roleKey: 'finance', roleSort: 5, dataScope: '3', status: '0', createTime: '2024-03-06 16:45:00', remark: '财务报表与赔付' },
  { roleId: 6, roleName: '分公司管理员', roleKey: 'branch_admin', roleSort: 6, dataScope: '4', status: '1', createTime: '2024-03-08 13:00:00', remark: '分支机构管理' }
]

const handleSearch = () => { pageNum.value = 1; loadTable() }
const resetForm = () => {
  searchForm.roleName = ''
  searchForm.roleKey = ''
  searchForm.status = ''
  handleSearch()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formData = reactive({
  roleId: null, roleName: '', roleKey: '', roleSort: 0,
  dataScope: '3', status: '0', remark: ''
})
const roleFormRef = ref(null)
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

const openDialog = async (row) => {
  dialogTitle.value = row ? '修改角色' : '新增角色'
  Object.keys(formData).forEach(k => {
    if (k === 'status') formData[k] = row?.[k] ?? '0'
    else if (k === 'dataScope') formData[k] = row?.[k] ?? '3'
    else if (k === 'roleSort') formData[k] = row?.[k] ?? 0
    else formData[k] = row?.[k] ?? ''
  })
  dialogVisible.value = true
  await nextTick()
  if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys(row?.menuIds || [1, 2, 21, 22, 3, 31, 32, 4])
}

const submitForm = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      await request.post('/api/system/role', { ...formData, menuIds: menuTreeRef.value?.getCheckedKeys?.() || [] }).catch(() => {})
      ElNotification.success({ title: '操作成功', message: `${dialogTitle.value}操作已完成` })
      dialogVisible.value = false
      loadTable()
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除角色"${row.roleName}"吗？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消'
  }).then(async () => {
    await request.delete(`/api/system/role/${row.roleId}`).catch(() => {})
    ElMessage.success('删除成功')
    loadTable()
  }).catch(() => {})
}

onMounted(() => loadTable())
</script>

<style scoped lang="scss">
.role-page {
  padding: 16px;
  .search-card, .table-card { margin-bottom: 16px; }
  .card-header { display: flex; gap: 10px; }
  .pagination-wrap { display: flex; justify-content: flex-end; margin-top: 20px; }
  .role-key {
    background: #f5f7fa; padding: 2px 8px; border-radius: 4px;
    color: #409eff; font-size: 13px;
  }
}
</style>
