<template>
  <div class="assess-list">
    <el-card shadow="hover" class="card-container mb-15">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="任务编号">
          <el-input v-model="query.missionNo" placeholder="模糊查询" clearable class="w-160" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.assessStatus" clearable class="w-140">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="待审核" value="AUDIT" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已赔付" value="PAID" />
          </el-select>
        </el-form-item>
        <el-form-item label="灾害类型">
          <el-select v-model="query.disasterType" clearable class="w-120">
            <el-option label="淹水" value="FLOOD" />
            <el-option label="倒伏" value="LODGE" />
            <el-option label="枯黄" value="WITHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="任务/被保险人/地点" clearable class="w-180" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
          <el-button type="success" @click="$router.push('/assess/create')"><el-icon><DocumentAdd /></el-icon>新建定损</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="card-container">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="missionNo" label="任务编号" width="190" fixed>
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/assess/detail/' + row.id)">
              {{ row.missionNo }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="missionName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="policyHolderName" label="被保险人" width="100" align="center" />
        <el-table-column prop="cropType" label="作物" width="80" align="center" />
        <el-table-column prop="disasterType" label="灾害" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="disasterTag(row.disasterType)" size="small">{{ disasterText(row.disasterType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="disasterLevel" label="等级" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.disasterLevel)" size="small" effect="dark">{{ levelText(row.disasterLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="disasterArea" label="受灾面积(亩)" width="110" align="right">
          <template #default="{ row }">{{ row.disasterArea?.toFixed?.(2) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="disasterRatio" label="受灾比例(%)" width="100" align="right">
          <template #default="{ row }">{{ row.disasterRatio?.toFixed?.(2) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="finalAmount" label="赔付金额(元)" width="130" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥{{ money(row.finalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="surveyorName" label="查勘员" width="90" align="center" />
        <el-table-column prop="assessStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.assessStatus)" size="small">{{ statusText(row.assessStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push('/assess/detail/' + row.id)">详情</el-button>
            <el-button link type="success" size="small" @click="audit(row,1)" v-if="['AUDIT'].includes(row.assessStatus)">审核</el-button>
            <el-button link type="warning" size="small" @click="download(row)">下载报告</el-button>
            <el-button link type="danger" size="small" @click="audit(row,0)" v-if="['AUDIT'].includes(row.assessStatus)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-15"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10,20,50,100]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="search"
        @current-change="search"
      />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, DocumentAdd } from '@element-plus/icons-vue'
import { listMission, auditMission, downloadReport } from '@/api/assess'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1, pageSize: 10, missionNo: '', assessStatus: '', disasterType: '', keyword: ''
})

const money = v => v?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
const disasterText = t => ({FLOOD:'淹水',LODGE:'倒伏',WITHER:'枯黄'}[t]||t||'-')
const disasterTag = t => ({FLOOD:'danger',LODGE:'warning',WITHER:'info'}[t]||'')
const levelText = l => ({LIGHT:'轻度',MODERATE:'中度',SEVERE:'重度'}[l]||l||'-')
const levelTag = l => ({LIGHT:'success',MODERATE:'warning',SEVERE:'danger'}[l]||'info')
const statusText = s => ({PENDING:'待处理',PROCESSING:'处理中',AUDIT:'待审核',APPROVED:'已通过',REJECTED:'已驳回',PAID:'已赔付'}[s]||s)
const statusTag = s => ({PENDING:'info',PROCESSING:'warning',AUDIT:'',APPROVED:'success',REJECTED:'danger',PAID:'success'}[s]||'info')

async function search() {
  loading.value = true
  try {
    const d = await listMission({ ...query })
    list.value = d.list || []
    total.value = d.total || 0
  } catch (e) { mock() }
  finally { loading.value = false }
}

function mock() {
  list.value = Array.from({ length: query.pageSize }, (_, i) => {
    const idx = (query.pageNum - 1) * query.pageSize + i
    const type = ['FLOOD','LODGE','WITHER'][idx % 3]
    const level = ['LIGHT','MODERATE','SEVERE'][idx % 3]
    const status = ['PROCESSING','AUDIT','APPROVED','AUDIT','PAID','REJECTED'][idx % 6]
    return {
      id: 100 + idx,
      missionNo: 'DS202406' + (1000 + idx),
      missionName: ['顺义区小麦淹水定损','通州区玉米倒伏','大兴区水稻枯黄评估','房山区大豆受灾'][idx % 4] + '#' + idx,
      policyHolderName: ['张三','李四','王五','赵六','钱七'][idx % 5],
      cropType: ['小麦','玉米','水稻','大豆','棉花'][idx % 5],
      disasterType: type, disasterLevel: level,
      disasterArea: 20 + Math.random() * 180,
      disasterRatio: 10 + Math.random() * 80,
      finalAmount: 10000 + Math.floor(Math.random() * 200000),
      surveyorName: ['张查勘','李查勘','王查勘'][idx % 3],
      assessStatus: status
    }
  })
  total.value = 86
}

function resetQuery() {
  Object.assign(query, { pageNum:1, missionNo:'', assessStatus:'', disasterType:'', keyword:'' })
  search()
}

async function audit(row, passed) {
  try {
    const msg = passed === 1 ? '确认审核通过此定损任务？' : '确认驳回此定损任务？'
    await ElMessageBox.confirm(msg, '提示', { type: passed === 1 ? 'success' : 'warning' })
    const remark = passed === 1 ? '审核通过，同意赔付' : '需要补充影像资料'
    await auditMission(row.id, passed, remark)
    ElMessage.success(passed === 1 ? '审核通过' : '已驳回')
    search()
  } catch (e) {}
}

async function download(row) {
  try {
    const blob = await downloadReport(row.id)
    const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = (row.reportNo || row.missionNo || '定损报告') + '.pdf'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('下载中')
  } catch (e) {}
}

onMounted(search)
</script>

<style lang="scss" scoped>
.assess-list { .w-120 { width: 120px; } .w-140 { width: 140px; } .w-160 { width: 160px; } .w-180 { width: 180px; } .mt-15 { margin-top: 15px; } .mb-15 { margin-bottom: 15px; } }
</style>
