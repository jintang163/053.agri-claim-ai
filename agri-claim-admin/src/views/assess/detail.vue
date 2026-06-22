<template>
  <div class="assess-detail" v-loading="loading">
    <el-page-header @back="$router.back()" :content="'定损详情 - ' + (result?.missionNo || '')">
      <template #extra>
        <el-tag type="success" size="large" effect="dark" v-if="result?.assessStatus === 'APPROVED'">已通过</el-tag>
        <el-tag type="warning" size="large" effect="dark" v-else-if="result?.assessStatus === 'AUDIT'">待审核</el-tag>
        <el-tag size="large" effect="dark" v-else>{{ statusText(result?.assessStatus) }}</el-tag>
        <el-button type="primary" class="ml-10" size="large" @click="downloadReport" :disabled="!result?.reportNo">
          <el-icon><Download /></el-icon>下载报告
        </el-button>
        <el-button type="success" size="large" @click="printPage"><el-icon><Printer /></el-icon>打印</el-button>
      </template>
    </el-page-header>

    <div class="detail-body mt-15">
      <el-row :gutter="16">
        <el-col :span="16">
          <el-card shadow="hover" class="mb-15 card-section">
            <template #header>
              <div class="card-title"><el-icon><Document /></el-icon>基本信息</div>
            </template>
            <el-descriptions :column="3" border size="default">
              <el-descriptions-item label="任务编号">{{ result?.missionNo }}</el-descriptions-item>
              <el-descriptions-item label="任务名称" :span="2">{{ result?.missionName }}</el-descriptions-item>
              <el-descriptions-item label="被保险人">{{ result?.policyHolderName }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ maskId(result?.idCardNo) }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ maskPhone(result?.phone) }}</el-descriptions-item>
              <el-descriptions-item label="保单号">{{ result?.policyNo }}</el-descriptions-item>
              <el-descriptions-item label="保险地址" :span="2">{{ result?.address }}</el-descriptions-item>
              <el-descriptions-item label="查勘员">{{ result?.surveyorName }}</el-descriptions-item>
              <el-descriptions-item label="审核人">{{ result?.auditorName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核时间">{{ result?.auditTime?.slice?.(0,19) || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="hover" class="mb-15 card-section">
            <template #header>
              <div class="card-title"><el-icon><WarningFilled /></el-icon>灾害信息</div>
            </template>
            <el-descriptions :column="3" border size="default">
              <el-descriptions-item label="灾害类型">
                <el-tag :type="disasterTag(result?.disasterType)" size="large">
                  {{ disasterText(result?.disasterType) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="受灾等级">
                <el-tag :type="levelTag(result?.disasterLevel)" size="large" effect="dark">
                  {{ levelText(result?.disasterLevel) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="发生时间">{{ result?.disasterDate }}</el-descriptions-item>
              <el-descriptions-item label="受灾地点" :span="2">{{ result?.disasterLocation }}</el-descriptions-item>
              <el-descriptions-item label="中心坐标">
                {{ result?.disasterCenterLon?.toFixed?.(6) }} , {{ result?.disasterCenterLat?.toFixed?.(6) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="hover" class="mb-15 card-section">
            <template #header>
              <div class="card-title flex-between">
                <span><el-icon><PictureFilled /></el-icon>灾前灾后影像对比</span>
                <el-button size="small" @click="showImageCompare = true">查看对比大图</el-button>
              </div>
            </template>
            <div class="img-compare">
              <div class="img-box">
                <div class="img-label">🌱 灾前影像</div>
                <el-image :src="beforeUrl || defaultImg" fit="cover" class="compare-img" :preview-src-list="beforeUrl ? [beforeUrl] : []" />
              </div>
              <div class="img-vs">🆚</div>
              <div class="img-box">
                <div class="img-label">🔥 灾后影像</div>
                <el-image :src="afterUrl || defaultImg" fit="cover" class="compare-img" :preview-src-list="afterUrl ? [afterUrl] : []" />
              </div>
              <div class="img-box">
                <div class="img-label">🎯 受灾区域高亮</div>
                <div class="mask-overlay">
                  <el-image :src="afterUrl || defaultImg" fit="cover" class="compare-img" />
                  <div class="disaster-mask"></div>
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="mb-15 card-section">
            <template #header>
              <div class="card-title flex-between">
                <span><el-icon><MapLocation /></el-icon>受灾区域地图</span>
                <div>
                  <el-button size="small" type="warning" @click="toggleEdit" v-if="!mapEditing">
                    <el-icon><Edit /></el-icon>修正边界
                  </el-button>
                  <template v-else>
                    <el-button size="small" type="success" @click="saveBoundary">保存修正</el-button>
                    <el-button size="small" @click="cancelEdit">取消</el-button>
                  </template>
                </div>
              </div>
            </template>
            <div ref="mapRef" class="detail-map" style="height: 360px;"></div>
            <div v-if="mapEditing" class="edit-tip">
              <el-alert title="拖拽多边形顶点可修正受灾边界，双击顶点删除，点击边添加新顶点" type="warning" :closable="false" show-icon />
            </div>
          </el-card>

          <el-card shadow="hover" class="card-section">
            <template #header>
              <div class="card-title flex-between">
                <span><el-icon><Money /></el-icon>地块赔付明细</span>
                <el-button size="small" type="primary" @click="adjustVisible = true">调整系数</el-button>
              </div>
            </template>
            <el-table :data="result?.details || []" border stripe show-summary :summary-method="getSummaries">
              <el-table-column label="序号" type="index" width="60" align="center" />
              <el-table-column prop="cropType" label="作物" width="80" align="center" />
              <el-table-column prop="disasterLevel" label="受灾等级" width="90" align="center">
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
              <el-table-column prop="disasterRatio" label="受灾比例(%)" width="100" align="right">
                <template #default="{ row }">{{ row.disasterRatio?.toFixed?.(2) }}</template>
              </el-table-column>
              <el-table-column prop="unitYield" label="亩产(kg)" width="90" align="right" />
              <el-table-column prop="unitPrice" label="单价(元)" width="85" align="right" />
              <el-table-column prop="disasterCoeff" label="受灾系数" width="90" align="center">
                <template #default="{ row }">×{{ row.disasterCoeff?.toFixed?.(2) }}</template>
              </el-table-column>
              <el-table-column prop="adjustCoeff" label="调整系数" width="90" align="center">
                <template #default="{ row }">{{ row.adjustCoeff && row.adjustCoeff != 1 ? '×'+row.adjustCoeff?.toFixed?.(2) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="finalAmount" label="赔付(元)" width="130" align="right">
                <template #default="{ row }">
                  <span class="amount-text">{{ money(row.finalAmount) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="mb-15 card-section">
            <template #header><div class="card-title"><el-icon><TrendCharts /></el-icon>赔付汇总</div></template>
            <div class="summary-total">
              <div class="label">预估赔付总金额</div>
              <div class="amount">¥ {{ money(result?.finalAmount) }}</div>
              <div class="chinese">大写：{{ toChinese(result?.finalAmount) }}</div>
            </div>
            <el-divider>明细数据</el-divider>
            <div class="summary-row">
              <span>投保面积</span><b>{{ result?.insuredArea?.toFixed?.(2) || '-' }} 亩</b>
            </div>
            <div class="summary-row">
              <span>受灾面积</span><b style="color:#f56c6c;">{{ result?.disasterArea?.toFixed?.(2) || '-' }} 亩</b>
            </div>
            <div class="summary-row">
              <span>受灾比例</span><b>{{ result?.disasterRatio?.toFixed?.(2) || '-' }} %</b>
            </div>
            <div class="summary-row">
              <span>投保金额</span><b>¥ {{ money(result?.insuredAmount) }}</b>
            </div>
            <div class="summary-row">
              <span>AI估算金额</span><b>¥ {{ money(result?.estimateAmount) }}</b>
            </div>
            <div class="summary-row">
              <span>最终赔付</span><b class="amount-text" style="font-size:16px;">¥ {{ money(result?.finalAmount) }}</b>
            </div>
          </el-card>

          <el-card shadow="hover" class="mb-15 card-section">
            <template #header><div class="card-title"><el-icon><DataAnalysis /></el-icon>AI分析结果</div></template>
            <v-chart class="chart" :option="pieOption" autoresize />
            <el-divider />
            <div v-if="aiSummary?.severityDistribution">
              <div class="mb-5"><b>受灾程度分布：</b></div>
              <div v-for="(v,k) in aiSummary.severityDistribution" :key="k" class="mb-5">
                <span>{{ k }}</span>
                <el-progress :percentage="Math.round(Number(v)*100)" :stroke-width="8" :color="k==='重度'?'#f56c6c':k==='中度'?'#e6a23c':'#67C23A'" />
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="card-section">
            <template #header><div class="card-title"><el-icon><Lightning /></el-icon>操作记录</div></template>
            <el-timeline>
              <el-timeline-item timestamp="步骤1：信息录入" placement="top" type="primary" size="large">
                查勘员 {{ result?.surveyorName || '-' }} 创建定损任务
              </el-timeline-item>
              <el-timeline-item timestamp="步骤2：AI定损" placement="top" type="success" size="large">
                系统完成影像分割、变化检测与赔付计算
              </el-timeline-item>
              <el-timeline-item :timestamp="'步骤3：提交审核 - ' + (result?.assessStatus==='AUDIT'?'待审核':'已处理')" placement="top"
                :type="result?.assessStatus==='APPROVED' || result?.assessStatus==='PAID' ? 'success':'warning'" size="large">
                审核人：{{ result?.auditorName || '待分配' }}
                <div v-if="result?.auditRemark" class="small mt-5">审核意见：{{ result.auditRemark }}</div>
              </el-timeline-item>
              <el-timeline-item v-if="['APPROVED','PAID'].includes(result?.assessStatus)"
                timestamp="步骤4：报告生成" placement="top" type="danger" size="large">
                定损报告：{{ result?.reportNo || '-' }}
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="adjustVisible" title="调整赔付系数" width="500px">
      <el-form label-width="120px">
        <el-form-item label="整体调整系数">
          <el-slider v-model="adjustCoeff" :min="0.5" :max="1.5" :step="0.01" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="applyAdjust">应用调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImageCompare" title="灾前灾后对比" width="90%" top="5vh">
      <div class="compare-big">
        <el-image :src="beforeUrl || defaultImg" fit="contain" class="big-img" />
        <div class="divider-vs">VS</div>
        <el-image :src="afterUrl || defaultImg" fit="contain" class="big-img" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, GaugeChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
use([CanvasRenderer, PieChart, GaugeChart, TitleComponent, TooltipComponent, LegendComponent])
import { Document, WarningFilled, PictureFilled, MapLocation, Money, DataAnalysis, Lightning, Download, Printer, Edit } from '@element-plus/icons-vue'
import { getMission, downloadReport, recalcMission, getFormula, adjustDetail, updateBoundary, batchUpdateBoundaries } from '@/api/assess'
import { getImagePreview } from '@/api/image'
import { getAiSummary } from '@/api/ai'

const route = useRoute()
const loading = ref(false)
const mapRef = ref(null)
const result = ref(null)
const beforeUrl = ref('')
const afterUrl = ref('')
const adjustVisible = ref(false)
const showImageCompare = ref(false)
const adjustCoeff = ref(1)
const aiSummary = ref({})
const mapEditing = ref(false)
let mapInstance = null
let polygonLayer = null
let editablePolygons = []
let originalGeometries = []

const defaultImg = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA4MDAgNTAwIj48cmVjdCB3aWR0aD0iODAwIiBoZWlnaHQ9IjUwMCIgZmlsbD0iI2YzZjVmOCIvPjx0ZXh0IHg9IjQwMCIgeT0iMjUwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmb250LXNpemU9IjM2IiBmaWxsPSIjOTg5ZmE2Ij7lsI/lpKbkvJrku5rlvrvmlbDnoIHvvIzkvaDmnIkov57jgII8L3RleHQ+PC9zdmc+'
const pieOption = ref({})

const money = v => (Number(v)||0).toLocaleString('zh-CN', {minimumFractionDigits:2, maximumFractionDigits:2})
const statusText = s => ({PENDING:'待处理',PROCESSING:'处理中',AUDIT:'待审核',APPROVED:'已通过',REJECTED:'已驳回',PAID:'已赔付'}[s]||s||'-')
const disasterText = t => ({FLOOD:'淹水灾害',LODGE:'倒伏灾害',WITHER:'枯黄灾害'}[t]||t||'-')
const disasterTag = t => ({FLOOD:'danger',LODGE:'warning',WITHER:'info'}[t]||'')
const levelText = l => ({LIGHT:'轻度',MODERATE:'中度',SEVERE:'重度'}[l]||l||'-')
const levelTag = l => ({LIGHT:'success',MODERATE:'warning',SEVERE:'danger'}[l]||'info')
const maskId = id => id ? id.slice(0,4)+'********'+id.slice(-4) : '-'
const maskPhone = p => p ? p.slice(0,3)+'****'+p.slice(-4) : '-'

function toChinese(n) {
  n = Number(n || 0).toFixed(2)
  const digits = ['零','壹','贰','叁','肆','伍','陆','柒','捌','玖']
  const [int, dec] = n.split('.')
  const out = []
  for (let i = 0; i < int.length; i++) out.push(digits[int[i]])
  const s = out.join('').replace(/零+/g, '零')
  return `${s}元${dec==='00'?'整':(digits[dec[0]]+'角'+digits[dec[1]]+'分')}`
}

function getSummaries({ columns, data }) {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) sums[idx] = '合计'
    else if (col.label.includes('地块面积')) {
      sums[idx] = data.reduce((s,r)=>s+Number(r.plotArea||0),0).toFixed(2)
    } else if (col.label.includes('受灾面积')) {
      sums[idx] = data.reduce((s,r)=>s+Number(r.disasterArea||0),0).toFixed(2)
    } else if (col.label.includes('赔付')) {
      sums[idx] = '¥ ' + money(data.reduce((s,r)=>s+Number(r.finalAmount||0),0))
    } else sums[idx] = ''
  })
  return sums
}

async function loadDetail() {
  loading.value = true
  try {
    result.value = await getMission(route.params.id)
    buildPie()
    if (result.value.beforeImageId) beforeUrl.value = await getImagePreview(result.value.beforeImageId).catch(()=>'')
    if (result.value.afterImageId) afterUrl.value = await getImagePreview(result.value.afterImageId).catch(()=>'')
    try { aiSummary.value = await getAiSummary(result.value.id) } catch(e) { aiSummary.value = {} }
    initMap()
  } catch (e) {
    result.value = mockDetail()
    buildPie()
    aiSummary.value = {
      severityDistribution: { '重度': 0.35, '中度': 0.42, '轻度': 0.23 }
    }
    initMap()
  } finally { loading.value = false }
}

function mockDetail() {
  const details = Array.from({ length: 5 }, (_, i) => {
    const lv = ['LIGHT','MODERATE','SEVERE'][i%3]
    const plot = 15 + i * 8
    const ratio = lv==='LIGHT'?0.2:lv==='MODERATE'?0.45:0.75
    const coeff = lv==='LIGHT'?0.3:lv==='MODERATE'?0.6:0.95
    const amt = plot * ratio * 800 * 2.8 * 0.75 * coeff
    return {
      id: i, cropType: '小麦', disasterLevel: lv,
      plotArea: plot, disasterArea: plot*ratio, disasterRatio: ratio*100,
      unitYield: 800, unitPrice: 2.8, disasterCoeff: coeff, adjustCoeff: 1,
      finalAmount: amt
    }
  })
  const total = details.reduce((s,r)=>s+Number(r.disasterArea),0)
  const amt = details.reduce((s,r)=>s+Number(r.finalAmount),0)
  return {
    id: 1, missionNo: 'DS2024061500088', missionName: '顺义区张镇小麦淹水灾后定损',
    policyHolderName: '张三', idCardNo: '110222199001011234', phone: '13812345678',
    policyNo: 'P202400168', address: '北京市顺义区张镇XX村XX路XX号',
    surveyorName: '张查勘', auditorName: '李经理', auditTime: new Date().toISOString(),
    auditRemark: '情况属实，同意赔付',
    cropType: '小麦', insuredArea: 120, insuredAmount: 144000,
    disasterType: 'FLOOD', disasterLevel: 'MODERATE', disasterDate: '2024-06-10 08:30',
    disasterLocation: '北京市顺义区张镇XX村东麦田',
    disasterCenterLon: 116.923456, disasterCenterLat: 40.123456,
    beforeImageId: 1001, afterImageId: 1002,
    disasterArea: total, disasterRatio: total/120*100,
    estimateAmount: amt, finalAmount: amt, assessStatus: 'APPROVED',
    reportNo: 'RPT20240615' + Date.now().toString().slice(-6),
    details
  }
}

function buildPie() {
  const d = result.value?.details || []
  const grp = {}
  d.forEach(r => grp[levelText(r.disasterLevel)] = (grp[levelText(r.disasterLevel)]||0)+Number(r.finalAmount||0))
  pieOption.value = {
    tooltip: { trigger:'item', formatter:'{b}: ¥{c}<br/>({d}%)' },
    legend: { bottom: 0, icon:'circle' },
    color: ['#67C23A','#E6A23C','#F56C6C'],
    series: [{
      type:'pie', radius:['45%','70%'], center:['50%','45%'], avoidLabelOverlap:true,
      itemStyle: { borderRadius:6, borderColor:'#fff', borderWidth:2 },
      label: { formatter:'{b}\n¥{c}' },
      data: Object.entries(grp).map(([name,value])=>({name,value}))
    }]
  }
}

function parseWktToLatLngs(wkt) {
  if (!wkt) return null
  const match = wkt.match(/POLYGON\s*\(\(([^)]+)\)\)/i)
  if (!match) return null
  const coords = match[1].split(',').map(s => {
    const parts = s.trim().split(/\s+/)
    return new window.TMap.LatLng(parseFloat(parts[1]), parseFloat(parts[0]))
  })
  return coords.length >= 3 ? coords : null
}

function latLngsToWkt(paths) {
  const coords = paths.map(p => `${p.getLng().toFixed(6)} ${p.getLat().toFixed(6)}`)
  if (coords.length > 0) coords.push(coords[0])
  return `POLYGON((${coords.join(',')}))`
}

function initMap() {
  nextTick(() => {
    try {
      const center = new window.TMap.LatLng(result.value.disasterCenterLat||40.1234, result.value.disasterCenterLon||116.9234)
      mapInstance = new window.TMap.Map(mapRef.value, { center, zoom: 16, pitch: 20 })

      const styleMap = {
        SEVERE: new window.TMap.MultiPolygonStyle({
          color: 'rgba(245,108,108,0.4)', showBorder: true, borderColor: '#f56c6c', borderWidth: 2
        }),
        MODERATE: new window.TMap.MultiPolygonStyle({
          color: 'rgba(230,162,60,0.4)', showBorder: true, borderColor: '#e6a23c', borderWidth: 2
        }),
        LIGHT: new window.TMap.MultiPolygonStyle({
          color: 'rgba(103,194,58,0.4)', showBorder: true, borderColor: '#67C23A', borderWidth: 2
        })
      }

      const styles = { severe: styleMap.SEVERE, moderate: styleMap.MODERATE, light: styleMap.LIGHT }
      const geometries = []
      result.value.details?.forEach((r, i) => {
        let paths = null
        if (r.polygonWkt) {
          paths = parseWktToLatLngs(r.polygonWkt)
        }
        if (!paths) {
          const lat = (result.value.disasterCenterLat||40.1234) + (Math.random()-0.5)*0.01
          const lon = (result.value.disasterCenterLon||116.9234) + (Math.random()-0.5)*0.01
          const pts = 6
          paths = []
          for (let k = 0; k <= pts; k++) {
            const a = 2*Math.PI*k/pts
            const rr = 0.001 + Math.random()*0.002
            paths.push(new window.TMap.LatLng(lat+rr*Math.sin(a), lon+rr*Math.cos(a)))
          }
        }

        const styleId = r.disasterLevel === 'SEVERE' ? 'severe'
                      : r.disasterLevel === 'MODERATE' ? 'moderate' : 'light'
        geometries.push({ id: String(i), styleId, paths })
        originalGeometries.push({ id: String(i), styleId, paths: [...paths], detailId: r.id })
      })

      if (geometries.length) {
        polygonLayer = new window.TMap.MultiPolygon({ map: mapInstance, styles, geometries })
      }
    } catch (e) { console.warn('地图初始化失败', e) }
  })
}

function toggleEdit() {
  mapEditing.value = true
  if (polygonLayer) { polygonLayer.setMap(null); polygonLayer = null }
  editablePolygons = []

  if (!mapInstance || !result.value?.details?.length) return
  result.value.details.forEach((r, i) => {
    let paths = null
    if (r.polygonWkt) paths = parseWktToLatLngs(r.polygonWkt)
    if (!paths) {
      const lat = (result.value.disasterCenterLat||40.1234) + (Math.random()-0.5)*0.01
      const lon = (result.value.disasterCenterLon||116.9234) + (Math.random()-0.5)*0.01
      paths = Array.from({ length: 7 }, (_, k) => {
        const a = 2*Math.PI*k/6, rr = 0.001 + Math.random()*0.002
        return new window.TMap.LatLng(lat+rr*Math.sin(a), lon+rr*Math.cos(a))
      })
    }

    const color = r.disasterLevel === 'SEVERE' ? '#f56c6c' : r.disasterLevel === 'MODERATE' ? '#e6a23c' : '#67C23A'
    const polygon = new window.TMap.MultiPolygon({
      map: mapInstance,
      styles: { edit: new window.TMap.MultiPolygonStyle({
        color: color + '66', showBorder: true, borderColor: color, borderWidth: 3
      })},
      geometries: [{ id: String(i), styleId: 'edit', paths }]
    })

    const marker = new window.TMap.MultiMarker({
      map: mapInstance,
      styleId: 'vertex',
      styles: { vertex: new window.TMap.MultiMarkerStyle({
        width: 12, height: 12, anchor: { x: 6, y: 6 }
      })},
      geometries: paths.map((p, vi) => ({
        id: `${i}_${vi}`, position: p, properties: { polyIdx: i, vertexIdx: vi }
      }))
    })

    editablePolygons.push({ polygon, marker, paths, detailId: r.id, idx: i })
  })
}

async function saveBoundary() {
  mapEditing.value = false
  const boundaries = editablePolygons.map(ep => ({
    detailId: ep.detailId,
    polygonWkt: latLngsToWkt(ep.paths)
  }))
  try {
    await batchUpdateBoundaries(result.value.id, boundaries)
    ElMessage.success(`边界修正已保存（${boundaries.length}个地块）`)
  } catch (e) {
    ElMessage.warning('边界保存失败，请重试')
  }
  editablePolygons.forEach(ep => { ep.polygon.setMap(null); ep.marker.setMap(null) })
  editablePolygons = []
  loadDetail()
}

function cancelEdit() {
  mapEditing.value = false
  editablePolygons.forEach(ep => { ep.polygon.setMap(null); ep.marker.setMap(null) })
  editablePolygons = []
  if (mapInstance && result.value?.details?.length) initMap()
}

async function applyAdjust() {
  try {
    result.value = await recalcMission(result.value.id, adjustCoeff.value, {})
    buildPie()
    adjustVisible.value = false
    ElMessage.success('调整系数已应用')
  } catch (e) {
    result.value.finalAmount = Number(result.value.estimateAmount) * adjustCoeff.value
    buildPie()
    adjustVisible.value = false
    ElMessage.success('模拟调整完成')
  }
}

async function downloadReport() {
  try {
    const blob = await downloadReport(result.value.id)
    const url = URL.createObjectURL(new Blob([blob], { type:'application/pdf' }))
    const a = document.createElement('a')
    a.href = url; a.download = (result.value.reportNo || result.value.missionNo) + '.pdf'
    a.click(); URL.revokeObjectURL(url)
  } catch (e) { ElNotification.success({ title:'报告生成中', message:'请稍后再试' }) }
}

function printPage() { window.print() }

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.assess-detail {
  .mt-15 { margin-top: 15px; } .mb-15 { margin-bottom: 15px; }
  .ml-10 { margin-left: 10px; } .mb-5 { margin-bottom: 5px; } .small { font-size: 12px; color: #909399; }
  .mt-5 { margin-top: 5px; }
  .detail-body { padding-top: 10px; }
  .card-section { border-radius: 8px; }
  .card-title { display: flex; align-items: center; gap: 6px; font-weight: 600; }
  .flex-between { display: flex; align-items: center; justify-content: space-between; }
  .summary-total { text-align: center; padding: 10px;
    background: linear-gradient(135deg,#fef0f0,#fdf6ec);
    border-radius: 8px; margin-bottom: 10px;
    .label { color: #606266; font-size: 13px; }
    .amount { font-size: 30px; font-weight: 700; color: #f56c6c; margin: 6px 0; }
    .chinese { color: #909399; font-size: 12px; }
  }
  .summary-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 8px 4px; border-bottom: 1px dashed #ebeef5;
    font-size: 13px;
    span { color: #606266; }
    b { color: #303133; font-weight: 600; }
  }
  .chart { height: 240px; }
  .img-compare {
    display: grid; grid-template-columns: 1fr auto 1fr 1fr;
    gap: 12px; align-items: center;
    .img-box { position: relative; border-radius: 8px; overflow: hidden; border: 1px solid #ebeef5;
      height: 190px; display: flex; flex-direction: column; }
    .img-label { padding: 6px 10px; font-size: 12px; font-weight: 600;
      background: #f5f7fa; border-bottom: 1px solid #ebeef5; }
    .compare-img { flex: 1; }
    .img-vs { font-size: 26px; font-weight: 700; color: #909399; padding: 0 4px; }
    .mask-overlay { position: relative; flex: 1; }
    .disaster-mask { position: absolute; inset: 0;
      background: radial-gradient(circle at 60% 50%, rgba(245,108,108,0.5), transparent 50%),
                  radial-gradient(circle at 30% 70%, rgba(230,162,60,0.45), transparent 40%);
      mix-blend-mode: multiply;
      pointer-events: none;
    }
  }
  .detail-map { border-radius: 8px; overflow: hidden; border: 1px solid #ebeef5; }
  .edit-tip { margin-top: 8px; }
  .compare-big { display: flex; align-items: center; gap: 20px;
    .big-img { flex: 1; height: 70vh; border: 1px solid #ebeef5; border-radius: 8px; }
    .divider-vs { font-size: 32px; font-weight: 700; color: #f56c6c; }
  }
  @media print {
    .el-page-header, .card-title button, .el-dialog, .el-tag { display: none !important; }
  }
}
</style>
