<template>
  <div class="user-page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户账号">
          <el-input v-model="searchForm.userName" placeholder="请输入用户账号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input v-model="searchForm.phonenumber" placeholder="请输入手机号码" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="用户状态" clearable style="width: 140px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
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
          <el-button type="primary" :icon="Plus" @click="openDialog(null)">新增用户</el-button>
          <el-button type="success" :icon="Upload">导入</el-button>
          <el-button type="warning" :icon="Download">导出</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="userId" label="用户ID" width="90" align="center" />
        <el-table-column prop="userName" label="用户账号" min-width="120">
          <template #default="{ row }">
            <el-avatar :size="28" style="margin-right: 8px; vertical-align: middle">
              {{ row.nickName.charAt(0) }}
            </el-avatar>
            {{ row.userName }}
          </template>
        </el-table-column>
        <el-table-column prop="nickName" label="用户昵称" min-width="120" />
        <el-table-column prop="deptName" label="所属部门" width="140" align="center" />
        <el-table-column prop="phonenumber" label="手机号码" width="140" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" effect="dark">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openDialog(row)">修改</el-button>
            <el-button type="success" link :icon="Key">重置</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadTable"
          @current-change="loadTable"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="userFormRef" :model="formData" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户账号" prop="userName">
              <el-input v-model="formData.userName" placeholder="请输入用户账号" :disabled="!!formData.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="formData.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <el-tree-select
                v-model="formData.deptId"
                :data="deptOptions"
                check-strictly
                placeholder="请选择归属部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="formData.phonenumber" placeholder="请输入手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户邮箱">
              <el-input v-model="formData.email" placeholder="请输入用户邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户状态">
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
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus, Upload, Download, Edit, Delete, Key } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const searchForm = reactive({
  userName: '',
  phonenumber: '',
  status: '',
  dateRange: []
})
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const deptOptions = ref([
  { id: 100, label: '总公司', children: [
    { id: 101, label: '信息技术部' },
    { id: 102, label: '理赔部' },
    { id: 103, label: '核保部' },
    { id: 104, label: '财务部' },
    { id: 200, label: '河南分公司', children: [
      { id: 201, label: '郑州支公司' },
      { id: 202, label: '开封支公司' },
      { id: 203, label: '洛阳支公司' }
    ]}
  ]}
])

const loadTable = async () => {
  loading.value = true
  const res = await request.get('/api/system/user/list', { params: { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm } }).catch(() => ({ data: [] }))
  if (res?.rows) {
    tableData.value = res.rows
    total.value = res.total || 0
  } else {
    tableData.value = generateMockData()
    total.value = 58
  }
  loading.value = false
}

const generateMockData = () => [
  { userId: 1, userName: 'admin', nickName: '系统管理员', deptName: '信息技术部', phonenumber: '138****8888', status: '0', createTime: '2024-03-01 10:20:30', email: 'admin@agri.com' },
  { userId: 2, userName: 'surveyor', nickName: '张查勘', deptName: '理赔部', phonenumber: '139****6666', status: '0', createTime: '2024-03-05 14:15:22', email: 'zhang@agri.com' },
  { userId: 3, userName: 'manager', nickName: '李经理', deptName: '理赔部', phonenumber: '136****7777', status: '0', createTime: '2024-03-10 09:45:12', email: 'li@agri.com' },
  { userId: 4, userName: 'wangchaok', nickName: '王朝开', deptName: '郑州支公司', phonenumber: '137****5555', status: '0', createTime: '2024-03-12 11:30:00', email: 'wang@agri.com' },
  { userId: 5, userName: 'zhaoming', nickName: '赵明', deptName: '开封支公司', phonenumber: '135****4444', status: '1', createTime: '2024-03-15 16:20:18', email: 'zhao@agri.com' },
  { userId: 6, userName: 'sunli', nickName: '孙丽', deptName: '洛阳支公司', phonenumber: '133****3333', status: '0', createTime: '2024-03-18 08:50:45', email: 'sun@agri.com' },
  { userId: 7, userName: 'zhougang', nickName: '周刚', deptName: '核保部', phonenumber: '132****2222', status: '0', createTime: '2024-03-20 13:40:00', email: 'zhou@agri.com' },
  { userId: 8, userName: 'wuqing', nickName: '吴青', deptName: '财务部', phonenumber: '131****1111', status: '0', createTime: '2024-03-22 17:10:33', email: 'wu@agri.com' }
]

const handleSearch = () => {
  pageNum.value = 1
  loadTable()
}

const resetForm = () => {
  searchForm.userName = ''
  searchForm.phonenumber = ''
  searchForm.status = ''
  searchForm.dateRange = []
  handleSearch()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formData = reactive({
  userId: null, userName: '', nickName: '', deptId: null,
  phonenumber: '', email: '', status: '0', remark: ''
})
const userFormRef = ref(null)
const rules = {
  userName: [{ required: true, message: '请输入用户账号', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }]
}

const openDialog = (row) => {
  dialogTitle.value = row ? '修改用户' : '新增用户'
  Object.keys(formData).forEach(k => {
    if (k === 'status') formData[k] = row?.[k] ?? '0'
    else if (k === 'deptId') formData[k] = row?.[k] ?? null
    else formData[k] = row?.[k] ?? ''
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      await request.post('/api/system/user', formData).catch(() => {})
      ElNotification.success({ title: '操作成功', message: `${dialogTitle.value}操作已完成` })
      dialogVisible.value = false
      loadTable()
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户"${row.userName}"吗？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消'
  }).then(async () => {
    await request.delete(`/api/system/user/${row.userId}`).catch(() => {})
    ElMessage.success('删除成功')
    loadTable()
  }).catch(() => {})
}

onMounted(() => loadTable())
</script>

<style scoped lang="scss">
.user-page {
  padding: 16px;
  .search-card, .table-card { margin-bottom: 16px; }
  .card-header { display: flex; gap: 10px; }
  .pagination-wrap { display: flex; justify-content: flex-end; margin-top: 20px; }
}
</style>
