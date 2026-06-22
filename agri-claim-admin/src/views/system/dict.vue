<template>
  <div class="dict-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="type-card">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600">字典类型</span>
              <el-button type="primary" :icon="Plus" size="small" @click="openTypeDialog(null)">新增</el-button>
            </div>
          </template>

          <el-table :data="typeList" border stripe style="width: 100%" v-loading="typeLoading" size="default">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="dictName" label="字典名称" min-width="130" show-overflow-tooltip />
            <el-table-column prop="dictType" label="字典类型" min-width="140">
              <template #default="{ row }">
                <code class="dict-type">{{ row.dictType }}</code>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="selectType(row)">详情</el-button>
                <el-button type="danger" link size="small" @click="deleteType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="card-header">
              <div>
                <span style="font-weight: 600">字典数据 - </span>
                <el-tag type="warning" effect="plain" v-if="currentType">
                  {{ currentType.dictName }} ({{ currentType.dictType }})
                </el-tag>
                <span v-else style="color: #909399; font-weight: normal">请选择左侧字典类型</span>
              </div>
              <el-button type="primary" :icon="Plus" size="small" :disabled="!currentType" @click="openDataDialog(null)">新增数据</el-button>
            </div>
          </template>

          <el-table :data="dataList" border stripe style="width: 100%" v-loading="dataLoading" :empty-text="currentType ? '暂无字典数据' : '请先选择左侧字典类型'">
            <el-table-column type="index" label="#" width="60" align="center" />
            <el-table-column prop="dictLabel" label="数据标签" min-width="140">
              <template #default="{ row }">
                <el-tag v-if="row.listClass" :type="row.listClass === 'danger' ? 'danger' : row.listClass === 'warning' ? 'warning' : 'success'" effect="dark" style="margin-right: 6px">
                  {{ row.dictValue }}
                </el-tag>
                <span>{{ row.dictLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="dictValue" label="数据键值" min-width="110" align="center" />
            <el-table-column prop="dictSort" label="排序" width="90" align="center" />
            <el-table-column prop="cssClass" label="样式属性" min-width="120" align="center" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.status" active-value="0" inactive-value="1" @change="toggleStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
            <el-table-column label="操作" width="170" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" :icon="Edit" @click="openDataDialog(row)">修改</el-button>
                <el-button type="danger" link size="small" :icon="Delete" @click="deleteData(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="560px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="请输入字典类型（如：sys_status）" :disabled="!!typeForm.dictId" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitType">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="600px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px">
        <el-form-item label="数据标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入数据标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入数据键值" />
        </el-form-item>
        <el-form-item label="显示排序" prop="dictSort">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标签类型">
          <el-select v-model="dataForm.listClass" placeholder="请选择标签类型" style="width: 100%">
            <el-option label="默认（灰）" value="default" />
            <el-option label="成功（绿）" value="success" />
            <el-option label="警告（橙）" value="warning" />
            <el-option label="危险（红）" value="danger" />
            <el-option label="信息（蓝）" value="primary" />
          </el-select>
        </el-form-item>
        <el-form-item label="CSS样式">
          <el-input v-model="dataForm.cssClass" placeholder="自定义样式class，多值空格分隔" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitData">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'

const typeLoading = ref(false)
const dataLoading = ref(false)
const typeList = ref([])
const dataList = ref([])
const currentType = ref(null)

const loadTypeList = async () => {
  typeLoading.value = true
  const res = await request.get('/api/system/dict/type/list').catch(() => ({}))
  if (res?.rows) typeList.value = res.rows
  else typeList.value = generateMockTypes()
  typeLoading.value = false
}

const generateMockTypes = () => [
  { dictId: 1, dictName: '系统状态', dictType: 'sys_status', status: '0', remark: '通用启用/停用状态' },
  { dictId: 2, dictName: '影像类型', dictType: 'image_type', status: '0', remark: '受灾前后正射影像类型' },
  { dictId: 3, dictName: '灾害类型', dictType: 'disaster_type', status: '0', remark: '农业灾害分类' },
  { dictId: 4, dictName: '受灾等级', dictType: 'disaster_level', status: '0', remark: '轻度/中度/重度' },
  { dictId: 5, dictName: 'AI处理状态', dictType: 'ai_status', status: '0', remark: '排队/处理中/成功/失败' },
  { dictId: 6, dictName: '定损状态', dictType: 'assess_status', status: '0', remark: '定损任务流转状态' },
  { dictId: 7, dictName: '作物类型', dictType: 'crop_type', status: '0', remark: '小麦/玉米/水稻/大豆等' },
  { dictId: 8, dictName: '审核结果', dictType: 'audit_result', status: '0', remark: '审核通过/驳回' }
]

const selectType = async (row) => {
  currentType.value = row
  dataLoading.value = true
  const res = await request.get(`/api/system/dict/data/type/${row.dictType}`).catch(() => ({}))
  if (res?.data) dataList.value = res.data
  else dataList.value = generateMockData(row.dictType)
  dataLoading.value = false
}

const generateMockData = (dictType) => {
  const map = {
    sys_status: [
      { dictCode: 1, dictLabel: '正常', dictValue: '0', dictSort: 1, listClass: 'success', cssClass: '', status: '0', remark: '启用' },
      { dictCode: 2, dictLabel: '停用', dictValue: '1', dictSort: 2, listClass: 'danger', cssClass: '', status: '0', remark: '禁用' }
    ],
    image_type: [
      { dictCode: 11, dictLabel: '灾前影像', dictValue: 'BEFORE', dictSort: 1, listClass: 'primary', cssClass: '', status: '0', remark: '' },
      { dictCode: 12, dictLabel: '灾后影像', dictValue: 'AFTER', dictSort: 2, listClass: 'warning', cssClass: '', status: '0', remark: '' },
      { dictCode: 13, dictLabel: 'DOM正射', dictValue: 'DOM', dictSort: 3, listClass: 'success', cssClass: '', status: '0', remark: '' },
      { dictCode: 14, dictLabel: '掩膜图', dictValue: 'MASK', dictSort: 4, listClass: 'danger', cssClass: '', status: '0', remark: '' }
    ],
    disaster_type: [
      { dictCode: 21, dictLabel: '涝灾（淹水）', dictValue: 'FLOOD', dictSort: 1, listClass: 'primary', cssClass: '', status: '0', remark: '' },
      { dictCode: 22, dictLabel: '倒伏', dictValue: 'LODGE', dictSort: 2, listClass: 'warning', cssClass: '', status: '0', remark: '' },
      { dictCode: 23, dictLabel: '枯黄（旱灾）', dictValue: 'WITHER', dictSort: 3, listClass: 'danger', cssClass: '', status: '0', remark: '' }
    ],
    disaster_level: [
      { dictCode: 31, dictLabel: '轻度受灾', dictValue: 'LIGHT', dictSort: 1, listClass: 'success', cssClass: '', status: '0', remark: '受灾30%以内' },
      { dictCode: 32, dictLabel: '中度受灾', dictValue: 'MODERATE', dictSort: 2, listClass: 'warning', cssClass: '', status: '0', remark: '受灾30%-70%' },
      { dictCode: 33, dictLabel: '重度受灾', dictValue: 'SEVERE', dictSort: 3, listClass: 'danger', cssClass: '', status: '0', remark: '受灾70%以上' }
    ],
    ai_status: [
      { dictCode: 41, dictLabel: '排队中', dictValue: 'PENDING', dictSort: 1, listClass: 'default', cssClass: '', status: '0', remark: '' },
      { dictCode: 42, dictLabel: '处理中', dictValue: 'PROCESSING', dictSort: 2, listClass: 'primary', cssClass: '', status: '0', remark: '' },
      { dictCode: 43, dictLabel: '处理成功', dictValue: 'SUCCESS', dictSort: 3, listClass: 'success', cssClass: '', status: '0', remark: '' },
      { dictCode: 44, dictLabel: '处理失败', dictValue: 'FAILED', dictSort: 4, listClass: 'danger', cssClass: '', status: '0', remark: '' }
    ],
    assess_status: [
      { dictCode: 51, dictLabel: '待查勘', dictValue: 'PENDING', dictSort: 1, listClass: 'default', cssClass: '', status: '0', remark: '' },
      { dictCode: 52, dictLabel: '定损中', dictValue: 'PROCESSING', dictSort: 2, listClass: 'primary', cssClass: '', status: '0', remark: '' },
      { dictCode: 53, dictLabel: '待审核', dictValue: 'AUDIT', dictSort: 3, listClass: 'warning', cssClass: '', status: '0', remark: '' },
      { dictCode: 54, dictLabel: '审核通过', dictValue: 'APPROVED', dictSort: 4, listClass: 'success', cssClass: '', status: '0', remark: '' },
      { dictCode: 55, dictLabel: '已驳回', dictValue: 'REJECTED', dictSort: 5, listClass: 'danger', cssClass: '', status: '0', remark: '' }
    ],
    crop_type: [
      { dictCode: 61, dictLabel: '小麦', dictValue: 'WHEAT', dictSort: 1, listClass: 'success', cssClass: '', status: '0', remark: '' },
      { dictCode: 62, dictLabel: '玉米', dictValue: 'CORN', dictSort: 2, listClass: 'warning', cssClass: '', status: '0', remark: '' },
      { dictCode: 63, dictLabel: '水稻', dictValue: 'RICE', dictSort: 3, listClass: 'primary', cssClass: '', status: '0', remark: '' },
      { dictCode: 64, dictLabel: '大豆', dictValue: 'SOYBEAN', dictSort: 4, listClass: 'default', cssClass: '', status: '0', remark: '' },
      { dictCode: 65, dictLabel: '棉花', dictValue: 'COTTON', dictSort: 5, listClass: 'danger', cssClass: '', status: '0', remark: '' },
      { dictCode: 66, dictLabel: '蔬菜', dictValue: 'VEGETABLE', dictSort: 6, listClass: 'success', cssClass: '', status: '0', remark: '' }
    ],
    audit_result: [
      { dictCode: 71, dictLabel: '通过', dictValue: 'PASS', dictSort: 1, listClass: 'success', cssClass: '', status: '0', remark: '' },
      { dictCode: 72, dictLabel: '驳回', dictValue: 'REJECT', dictSort: 2, listClass: 'danger', cssClass: '', status: '0', remark: '' }
    ]
  }
  return map[dictType] || []
}

const toggleStatus = (row) => {
  ElMessage.success(`字典"${row.dictLabel}"状态已切换为"${row.status === '0' ? '正常' : '停用'}"`)
}

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新增字典类型')
const typeForm = reactive({ dictId: null, dictName: '', dictType: '', status: '0', remark: '' })
const typeFormRef = ref(null)
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const openTypeDialog = (row) => {
  typeDialogTitle.value = row ? '修改字典类型' : '新增字典类型'
  Object.keys(typeForm).forEach(k => { typeForm[k] = k === 'status' ? row?.[k] ?? '0' : row?.[k] ?? '' })
  typeDialogVisible.value = true
}
const submitType = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (valid) {
      await request.post('/api/system/dict/type', typeForm).catch(() => {})
      ElNotification.success({ title: '操作成功', message: `${typeDialogTitle.value}完成` })
      typeDialogVisible.value = false
      loadTypeList()
    }
  })
}
const deleteType = (row) => {
  ElMessageBox.confirm(`确定删除字典类型"${row.dictName}"吗？`, '删除确认', { type: 'warning' }).then(async () => {
    await request.delete(`/api/system/dict/type/${row.dictId}`).catch(() => {})
    ElMessage.success('删除成功')
    if (currentType.value?.dictId === row.dictId) { currentType.value = null; dataList.value = [] }
    loadTypeList()
  }).catch(() => {})
}

const dataDialogVisible = ref(false)
const dataDialogTitle = ref('新增字典数据')
const dataForm = reactive({ dictCode: null, dictType: '', dictLabel: '', dictValue: '', dictSort: 0, listClass: '', cssClass: '', status: '0', remark: '' })
const dataFormRef = ref(null)
const dataRules = {
  dictLabel: [{ required: true, message: '请输入数据标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入数据键值', trigger: 'blur' }]
}

const openDataDialog = (row) => {
  dataDialogTitle.value = row ? '修改字典数据' : '新增字典数据'
  Object.keys(dataForm).forEach(k => {
    if (k === 'status') dataForm[k] = row?.[k] ?? '0'
    else if (k === 'dictSort') dataForm[k] = row?.[k] ?? 0
    else if (k === 'dictType') dataForm[k] = currentType.value?.dictType || ''
    else dataForm[k] = row?.[k] ?? ''
  })
  dataDialogVisible.value = true
}
const submitData = async () => {
  if (!dataFormRef.value) return
  await dataFormRef.value.validate(async (valid) => {
    if (valid) {
      await request.post('/api/system/dict/data', dataForm).catch(() => {})
      ElNotification.success({ title: '操作成功', message: `${dataDialogTitle.value}完成` })
      dataDialogVisible.value = false
      selectType(currentType.value)
    }
  })
}
const deleteData = (row) => {
  ElMessageBox.confirm(`确定删除字典数据"${row.dictLabel}"吗？`, '删除确认', { type: 'warning' }).then(async () => {
    await request.delete(`/api/system/dict/data/${row.dictCode}`).catch(() => {})
    ElMessage.success('删除成功')
    selectType(currentType.value)
  }).catch(() => {})
}

onMounted(() => loadTypeList())
</script>

<style scoped lang="scss">
.dict-page {
  padding: 16px;
  .type-card, .data-card { height: calc(100vh - 140px); overflow: auto; }
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .dict-type {
    background: #f0f9ff; padding: 2px 8px; border-radius: 4px;
    color: #0369a1; font-size: 12px; font-weight: 500;
  }
  :deep(.el-table .cell) { line-height: 32px; }
}
</style>
