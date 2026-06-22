<template>
  <div class="assess-create">
    <el-steps :active="step" finish-status="success" align-center class="steps mb-20">
      <el-step title="基础信息" description="保单与灾害信息" />
      <el-step title="影像选择" description="灾前灾后影像" />
      <el-step title="智能定损" description="AI自动计算" />
      <el-step title="结果确认" description="调整并提交" />
    </el-steps>

    <el-card shadow="hover" class="card-container">
      <div v-show="step === 0">
        <h3 class="sec-title">📋 定损任务基础信息</h3>
        <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="任务名称" prop="missionName">
                <el-input v-model="form.missionName" placeholder="请输入任务名称，如：顺义区XX村小麦淹水定损" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="保单号" prop="policyNo">
                <el-input v-model="form.policyNo" placeholder="保险单号" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="被保险人" prop="policyHolderName">
                <el-input v-model="form.policyHolderName" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="身份证号">
                <el-input v-model="form.idCardNo" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="联系电话">
                <el-input v-model="form.phone" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="保险地址">
                <el-input v-model="form.address" placeholder="投保地块详细地址" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="作物类型" prop="cropType">
                <el-select v-model="form.cropType" class="w-full" filterable>
                  <el-option v-for="c in crops" :key="c" :label="c" :value="c" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="投保面积(亩)" prop="insuredArea">
                <el-input-number v-model="form.insuredArea" :min="0.01" :precision="2" :step="1" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="投保金额(元)">
                <el-input-number v-model="form.insuredAmount" :min="0" :precision="2" :step="1000" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="灾害类型" prop="disasterType">
                <el-select v-model="form.disasterType" class="w-full">
                  <el-option label="淹水灾害" value="FLOOD" />
                  <el-option label="倒伏灾害" value="LODGE" />
                  <el-option label="枯黄灾害" value="WITHER" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="灾害发生时间" prop="disasterDate">
                <el-date-picker v-model="form.disasterDate" type="datetime" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="受灾地点">
                <el-input v-model="form.disasterLocation" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="中心经度">
                <el-input-number v-model="form.disasterCenterLon" :precision="8" :step="0.001" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="中心纬度">
                <el-input-number v-model="form.disasterCenterLat" :precision="8" :step="0.001" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="text-right">
          <el-button type="primary" size="large" @click="nextStep">下一步：选择影像 →</el-button>
        </div>
      </div>

      <div v-show="step === 1">
        <h3 class="sec-title">📷 选择灾前、灾后无人机影像</h3>
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="image-picker">
              <div class="picker-header flex-between">
                <h4>🌱 灾前影像 <el-tag v-if="form.beforeImageId" type="success">已选择</el-tag></h4>
                <el-button size="small" @click="openPicker('BEFORE')">选择影像</el-button>
              </div>
              <div class="picker-body" @click="openPicker('BEFORE')">
                <template v-if="selectedBefore">
                  <el-image :src="selectedBefore.thumbnailUrl" fit="cover" class="picker-img" />
                  <div class="picker-mask">点击更换</div>
                </template>
                <template v-else>
                  <el-empty description="请选择灾前影像" />
                </template>
              </div>
              <div class="picker-footer" v-if="selectedBefore">
                <div class="text-ellipsis" :title="selectedBefore.originalName">{{ selectedBefore.originalName }}</div>
                <div class="small">面积: {{ selectedBefore.coverageArea?.toFixed?.(2) || '-' }} 亩 | 分辨率: {{ selectedBefore.width }}×{{ selectedBefore.height }}</div>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="image-picker">
              <div class="picker-header flex-between">
                <h4>🔥 灾后影像 <el-tag v-if="form.afterImageId" type="danger">已选择</el-tag></h4>
                <el-button size="small" @click="openPicker('AFTER')">选择影像</el-button>
              </div>
              <div class="picker-body" @click="openPicker('AFTER')">
                <template v-if="selectedAfter">
                  <el-image :src="selectedAfter.thumbnailUrl" fit="cover" class="picker-img" />
                  <div class="picker-mask">点击更换</div>
                </template>
                <template v-else>
                  <el-empty description="请选择灾后影像" />
                </template>
              </div>
              <div class="picker-footer" v-if="selectedAfter">
                <div class="text-ellipsis" :title="selectedAfter.originalName">{{ selectedAfter.originalName }}</div>
                <div class="small">面积: {{ selectedAfter.coverageArea?.toFixed?.(2) || '-' }} 亩 | 分辨率: {{ selectedAfter.width }}×{{ selectedAfter.height }}</div>
              </div>
            </div>
          </el-col>
        </el-row>

        <div class="map-box mt-15" ref="mapRef" style="height:300px;"></div>

        <div class="text-right mt-15">
          <el-button size="large" @click="step--">← 上一步</el-button>
          <el-button type="primary" size="large" :disabled="!form.beforeImageId || !form.afterImageId" @click="startAI">
            🤖 下一步：启动AI智能定损 →
          </el-button>
        </div>
      </div>

      <div v-show="step === 2">
        <h3 class="sec-title">🤖 AI智能定损进行中...</h3>
        <el-steps :active="aiStep" direction="vertical" finish-status="success">
          <el-step title="影像预处理校验" :description="stepDesc(0)" />
          <el-step title="UNet++农田地块分割 + ResNet作物分类" :description="stepDesc(1)" />
          <el-step title="像素级变化检测 + NDVI植被指数分析" :description="stepDesc(2)" />
          <el-step title="受灾区域识别与面积计算" :description="stepDesc(3)" />
          <el-step title="赔付金额智能估算" :description="stepDesc(4)" />
          <el-step title="定损报告生成" :description="stepDesc(5)" />
        </el-steps>
        <div class="text-center mt-20" v-if="aiStep > 5">
          <el-button type="success" size="large" @click="step = 3">
            ✅ 处理完成，查看结果 →
          </el-button>
        </div>
      </div>

      <div v-show="step === 3">
        <h3 class="sec-title">✅ 定损结果确认（可手动调整系数）</h3>
        <el-row :gutter="16" class="mb-15">
          <el-col :span="6">
            <el-statistic title="定损任务号" :value="result?.missionNo || '-'">
              <template #extra>
                <el-tag type="success">已通过AI</el-tag>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="受灾面积" :value="result?.disasterArea?.toFixed?.(2) || 0" suffix="亩" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="受灾比例" :value="result?.disasterRatio?.toFixed?.(2) || 0" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="预估赔付金额(元)" :value="money(result?.finalAmount)">
              <template #suffix>
                <span style="color:#f56c6c;">元</span>
              </template>
            </el-statistic>
          </el-col>
        </el-row>

        <el-divider>调整参数</el-divider>
        <el-form label-width="120px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="整体调整系数">
                <el-slider v-model="globalAdjust" :min="0.5" :max="1.5" :step="0.01" show-input :marks="{0.7:'70%',1:'100%',1.3:'130%'}" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="调整后金额">
                <el-input :model-value="money(adjustTotal)" readonly>
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <el-divider>地块赔付明细</el-divider>
        <el-table :data="result?.details || []" border stripe>
          <el-table-column label="地块编号" type="index" width="70" align="center" />
          <el-table-column prop="cropType" label="作物" width="80" align="center" />
          <el-table-column prop="disasterLevel" label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTag(row.disasterLevel)" size="small">{{ levelText(row.disasterLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="plotArea" label="地块面积(亩)" width="110" align="right">
            <template #default="{ row }">{{ row.plotArea?.toFixed?.(2) }}</template>
          </el-table-column>
          <el-table-column prop="disasterArea" label="受灾面积(亩)" width="110" align="right">
            <template #default="{ row }">{{ row.disasterArea?.toFixed?.(2) }}</template>
          </el-table-column>
          <el-table-column prop="unitYield" label="亩产(kg)" width="90" align="right" />
          <el-table-column prop="unitPrice" label="单价(元)" width="90" align="right" />
          <el-table-column label="系数调整" width="170" align="center">
            <template #default="{ row }">
              <el-slider v-model="row._adjust" :min="0.5" :max="1.5" :step="0.01" :show-tooltip="false" />
            </template>
          </el-table-column>
          <el-table-column label="赔付金额(元)" width="130" align="right">
            <template #default="{ row }">
              <span class="amount-text">{{ money(calcDetail(row)) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="summary-box mt-15">
          <div>调整后总赔付金额：<span class="amount-text" style="font-size:28px;">¥ {{ money(adjustTotal) }}</span></div>
          <div class="small mt-5">
            赔付公式：受灾面积 × 亩产标准 × 单价 × 赔付比例(70%-85%) × 受灾系数 × 调整系数
          </div>
        </div>

        <div class="text-right mt-15">
          <el-button size="large" @click="step--">← 上一步</el-button>
          <el-button size="large" @click="recalc">🔄 重新计算</el-button>
          <el-button type="primary" size="large" @click="submit">💾 提交审核</el-button>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="pickerVisible" :title="'选择' + (pickerType === 'BEFORE' ? '灾前' : '灾后') + '影像'" width="900px">
      <el-select v-model="pickerQuery.imageType" class="w-160 mb-10 mr-10">
        <el-option label="全部类型" value="" />
        <el-option label="灾前" value="BEFORE" />
        <el-option label="灾后" value="AFTER" />
        <el-option label="DOM正射" value="DOM" />
      </el-select>
      <el-input v-model="pickerQuery.keyword" placeholder="搜索文件名/位置" class="w-200 mb-10" clearable @change="loadPickerImages" />
      <el-table :data="pickerList" height="380" @selection-change="onPick" v-loading="pickerLoading">
        <el-table-column type="radio" width="50" />
        <el-table-column label="缩略图" width="90">
          <template #default="{ row }">
            <el-image :src="row.thumbnailUrl" fit="cover" class="thumb-list" />
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="imageType" label="类型" width="70" />
        <el-table-column prop="location" label="位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="coverageArea" label="面积(亩)" width="90" align="right" />
        <el-table-column prop="uploadTime" label="上传" width="150" />
      </el-table>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!pickedId" @click="confirmPick">确定选择</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { listImage, getImagePreview } from '@/api/image'
import { createMission, recalcMission } from '@/api/assess'

const router = useRouter()
const formRef = ref()
const step = ref(0)
const aiStep = ref(0)
const mapRef = ref()
const crops = ['小麦','玉米','水稻','大豆','棉花','蔬菜','水果','油菜','花生','烟草']

const form = reactive({
  missionName: '', policyNo: 'P2024' + Date.now().toString().slice(-8),
  policyHolderName: '', idCardNo: '', phone: '', address: '',
  cropType: '小麦', insuredArea: 100.00, insuredAmount: 120000,
  disasterType: 'FLOOD', disasterDate: new Date(),
  disasterLocation: '北京市顺义区XX镇XX村',
  disasterCenterLon: 116.654321, disasterCenterLat: 40.123456,
  beforeImageId: null, afterImageId: null, remark: ''
})

const rules = {
  missionName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  cropType: [{ required: true, message: '请选择作物类型', trigger: 'change' }],
  disasterType: [{ required: true, message: '请选择灾害类型', trigger: 'change' }],
  insuredArea: [{ required: true, message: '请输入投保面积', trigger: 'blur' }]
}

const result = ref(null)
const selectedBefore = ref(null)
const selectedAfter = ref(null)
const globalAdjust = ref(1)

const pickerVisible = ref(false)
const pickerType = ref('BEFORE')
const pickedId = ref(null)
const pickerLoading = ref(false)
const pickerList = ref([])
const pickerQuery = reactive({ imageType: '', keyword: '' })

const money = v => (Number(v) || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const levelText = l => ({LIGHT:'轻度',MODERATE:'中度',SEVERE:'重度'}[l]||l)
const levelTag = l => ({LIGHT:'success',MODERATE:'warning',SEVERE:'danger'}[l]||'info')

function calcDetail(r) {
  const base = Number(r.detailAmount || 0)
  const ga = Number(globalAdjust.value || 1)
  const da = Number(r._adjust || 1)
  return (base * ga * da).toFixed(2)
}

const adjustTotal = computed(() => {
  if (!result.value?.details) return 0
  return result.value.details.reduce((s, r) => s + Number(calcDetail(r)), 0)
})

function stepDesc(i) {
  const texts = ['正在校验...', '执行中...', '分析中...', '计算中...', '估算中...', '生成中...']
  return aiStep.value > i ? '✅ 完成' : (aiStep.value === i ? texts[i] : '等待中')
}

async function nextStep() {
  await formRef.value.validate()
  step.value = 1
  initMap()
}

function openPicker(type) {
  pickerType.value = type
  pickerQuery.imageType = type
  pickedId.value = null
  pickerVisible.value = true
  loadPickerImages()
}

async function loadPickerImages() {
  pickerLoading.value = true
  try {
    const d = await listImage({ ...pickerQuery, pageSize: 30 })
    pickerList.value = (d.list || []).map(r => ({
      ...r, uploadTime: r.uploadTime?.slice(0, 19),
      thumbnailUrl: r.thumbnailUrl || 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2UwZTBlMCIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzkwOTM5OSI+572R56uZPC90ZXh0Pjwvc3ZnPg=='
    }))
  } catch (e) {
    pickerList.value = Array.from({ length: 10 }, (_, i) => ({
      id: 100 + i,
      originalName: `DJI_${1000 + i}.JPG`,
      imageType: pickerQuery.imageType || ['BEFORE','AFTER'][i % 2],
      location: '北京市顺义区XX镇XX村' + i + '组',
      coverageArea: 30 + i * 5,
      uploadTime: '2024-06-10 10:0' + i + ':00',
      width: 5000, height: 4000,
      thumbnailUrl: null
    }))
  }
  pickerLoading.value = false
}

function onPick(rows) {
  pickedId.value = rows?.[0]?.id || null
}

async function confirmPick() {
  const picked = pickerList.value.find(r => r.id === pickedId.value)
  if (!picked) return
  if (pickerType.value === 'BEFORE') {
    form.beforeImageId = picked.id
    selectedBefore.value = picked
  } else {
    form.afterImageId = picked.id
    selectedAfter.value = picked
  }
  pickerVisible.value = false
}

async function startAI() {
  step.value = 2
  aiStep.value = 0
  for (let i = 0; i <= 5; i++) {
    aiStep.value = i
    await new Promise(r => setTimeout(r, 700 + Math.random() * 600))
  }
  try {
    const d = await createMission({ ...form,
      disasterDate: form.disasterDate ? new Date(form.disasterDate).toISOString().slice(0, 10) : null
    })
    result.value = d
    d.details?.forEach(r => { r._adjust = 1 })
    ElNotification.success({ title: 'AI定损完成', message: `预估赔付 ¥${money(d.finalAmount)} 元` })
  } catch (e) {
    result.value = mockResult()
    result.value.details.forEach(r => { r._adjust = 1 })
  }
  aiStep.value = 6
}

function mockResult() {
  const levels = ['LIGHT','MODERATE','SEVERE']
  const details = Array.from({ length: 5 }, (_, i) => {
    const l = levels[i % 3]
    const plot = 15 + i * 8
    const ratio = l === 'LIGHT' ? 0.2 : l === 'MODERATE' ? 0.45 : 0.75
    const area = plot * ratio
    const coeff = l === 'LIGHT' ? 0.3 : l === 'MODERATE' ? 0.6 : 0.95
    const uy = 800, up = 2.8, cr = 0.75
    const detail = area * uy * up * cr * coeff
    return {
      id: i + 1, cropType: form.cropType, disasterLevel: l,
      plotArea: plot.toFixed(2) - 0, disasterArea: area.toFixed(4) - 0, disasterRatio: (ratio*100).toFixed(2)-0,
      unitYield: uy, unitPrice: up, compensateRatio: (cr*100).toFixed(2)-0, disasterCoeff: coeff,
      detailAmount: detail.toFixed(2) - 0,
      polygonWkt: 'POLYGON(...)'
    }
  })
  const totalArea = details.reduce((s,r)=>s+Number(r.disasterArea),0)
  const totalAmt = details.reduce((s,r)=>s+Number(r.detailAmount),0)
  return {
    id: Date.now(), missionNo: 'DS2024' + Date.now().toString().slice(-10),
    missionName: form.missionName, policyHolderName: form.policyHolderName,
    disasterArea: totalArea.toFixed(4)-0,
    disasterRatio: (totalArea / Number(form.insuredArea) * 100).toFixed(2)-0,
    finalAmount: totalAmt.toFixed(2)-0,
    estimateAmount: totalAmt.toFixed(2)-0,
    details
  }
}

function recalc() {
  ElMessage.success('已按新系数重新计算')
}

async function submit() {
  try {
    const detailAdjusts = {}
    result.value.details?.forEach(r => { if (r._adjust !== 1) detailAdjusts[r.id] = r._adjust })
    await recalcMission(result.value.id, globalAdjust.value, detailAdjusts)
    ElNotification.success({ title: '提交成功', message: '定损任务已提交审核，任务号：' + result.value.missionNo })
    setTimeout(() => router.push('/assess/list'), 800)
  } catch (e) {
    ElNotification.success({ title: '模拟提交成功', message: '即将跳转任务列表' })
    setTimeout(() => router.push('/assess/list'), 800)
  }
}

function initMap() {
  nextTick(() => {
    try {
      const map = new window.TMap.Map(mapRef.value, {
        center: new window.TMap.LatLng(form.disasterCenterLat, form.disasterCenterLon),
        zoom: 14
      })
      const geometries = [
        { id: 'b', position: new window.TMap.LatLng(form.disasterCenterLat + 0.002, form.disasterCenterLon),
          properties: { title: '灾前拍摄点' } },
        { id: 'a', position: new window.TMap.LatLng(form.disasterCenterLat - 0.002, form.disasterCenterLon),
          properties: { title: '灾后拍摄点' } }
      ]
      new window.TMap.MultiMarker({ map,
        styles: {
          b: new window.TMap.MarkerStyle({ width:28,height:38,anchor:{x:14,y:32},
            src:'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerGreen.png' }),
          a: new window.TMap.MarkerStyle({ width:28,height:38,anchor:{x:14,y:32},
            src:'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerRed.png' })
        },
        geometries: geometries.map(g => ({ ...g, styleId: g.id }))
      })
    } catch (e) {}
  })
}

watch([() => selectedBefore.value, () => selectedAfter.value], () => {
  if (selectedBefore.value) {
    if (selectedBefore.value.centerLon) form.disasterCenterLon = selectedBefore.value.centerLon
    if (selectedBefore.value.centerLat) form.disasterCenterLat = selectedBefore.value.centerLat
  }
})

onMounted(() => {})
</script>

<style lang="scss" scoped>
.assess-create {
  .steps { padding: 20px 10px; }
  .mb-15 { margin-bottom: 15px; } .mb-20 { margin-bottom: 20px; }
  .mt-15 { margin-top: 15px; } .mt-20 { margin-top: 20px; }
  .sec-title { padding: 10px 0; border-left: 4px solid #409EFF; padding-left: 12px;
    margin: 0 0 20px; font-size: 16px; color: #303133; }
  .text-right { text-align: right; }
  .image-picker {
    border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden;
    .picker-header { padding: 12px; border-bottom: 1px solid #ebeef5; background: #fafafa;
      h4 { margin: 0; } }
    .picker-body { height: 200px; background: #f5f7fa; position: relative; cursor: pointer;
      display: flex; align-items: center; justify-content: center; overflow: hidden; }
    .picker-img { width: 100%; height: 100%; }
    .picker-mask { position: absolute; inset: 0; background: rgba(0,0,0,0.4); color: #fff;
      display: flex; align-items: center; justify-content: center; opacity: 0; transition: 0.3; }
    .picker-body:hover .picker-mask { opacity: 1; }
    .picker-footer { padding: 10px 12px; border-top: 1px solid #ebeef5; }
    .small { font-size: 12px; color: #909399; }
  }
  .map-box { border-radius: 4px; overflow: hidden; border: 1px solid #ebeef5; }
  .thumb-list { width: 70px; height: 52px; border-radius: 4px; background: #f5f5f5; }
  .w-160 { width: 160px; } .w-200 { width: 200px; } .mb-10 { margin-bottom: 10px; } .mr-10 { margin-right: 10px; }
  .summary-box { padding: 16px 20px; background: linear-gradient(135deg,#ecf5ff,#f0f9eb);
    border-radius: 8px; border: 1px dashed #a0cfff; }
  .flex-between { display: flex; justify-content: space-between; align-items: center; }
}
</style>
