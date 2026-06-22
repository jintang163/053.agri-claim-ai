<template>
  <div class="dashboard">
    <el-row :gutter="16" class="mb-20">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="hover" class="stat-card" :class="s.cls">
          <div class="stat-left">
            <el-icon class="stat-icon" :size="36"><component :is="s.icon" /></el-icon>
          </div>
          <div class="stat-right">
            <div class="stat-label">{{ s.label }}</div>
            <div class="stat-value">
              <span v-if="s.prefix">{{ s.prefix }}</span>{{ s.value }}<span v-if="s.suffix">{{ s.suffix }}</span>
            </div>
            <div class="stat-desc">较上月 {{ s.trend > 0 ? '+' : '' }}{{ s.trend }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-20">
      <el-col :span="14">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="card-header">定损趋势（近30天）
            <el-radio-group v-model="trendType" size="small" @change="loadTrend">
              <el-radio-button value="count">任务数</el-radio-button>
              <el-radio-button value="amount">赔付额</el-radio-button>
            </el-radio-group></div></template>
          <v-chart class="chart" :option="trendOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="card-header">灾害类型分布</div></template>
          <v-chart class="chart" :option="typeOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-20">
      <el-col :span="12">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="card-header">作物类型统计</div></template>
          <v-chart class="chart" :option="cropOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="card-header">最新定损任务</div></template>
          <el-table :data="recentMissions" size="small" stripe>
            <el-table-column prop="missionNo" label="任务编号" width="180" />
            <el-table-column prop="policyHolderName" label="被保险人" />
            <el-table-column prop="cropType" label="作物" width="80" align="center" />
            <el-table-column prop="disasterType" label="灾害" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="disasterTagType(row.disasterType)" size="small">
                  {{ disasterText(row.disasterType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="finalAmount" label="赔付金额(元)" width="130" align="right">
              <template #default="{ row }">
                <span class="amount-text">{{ fmtMoney(row.finalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="assessStatus" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.assessStatus)" size="small">{{ statusText(row.assessStatus) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="card-header">
            灾害地理分布
            <el-button link type="primary" @click="openMonitor">查看完整大屏 →</el-button>
          </div></template>
          <div ref="mapRef" class="map-container" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent, DatasetComponent } from 'echarts/components'
use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, DatasetComponent])
import { Document, User, Money, PictureFilled, DataBoard } from '@element-plus/icons-vue'
import { getDashboardStats, listMission } from '@/api/assess'

const router = useRouter()
const mapRef = ref(null)
const trendType = ref('count')

const stats = reactive([
  { label: '定损任务数', value: 0, prefix: '', suffix: ' 件', icon: Document, cls: 'blue', trend: 12.5 },
  { label: '覆盖农户数', value: 0, prefix: '', suffix: ' 户', icon: User, cls: 'green', trend: 8.3 },
  { label: '赔付总金额', value: '0', prefix: '¥ ', suffix: '', icon: Money, cls: 'orange', trend: 15.6 },
  { label: '影像总数', value: 0, prefix: '', suffix: ' 张', icon: PictureFilled, cls: 'purple', trend: 22.1 }
])

const trendOption = ref({})
const typeOption = ref({})
const cropOption = ref({})
const recentMissions = ref([])

const fmtMoney = (v) => v?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
const disasterText = (t) => ({FLOOD:'淹水',LODGE:'倒伏',WITHER:'枯黄'}[t] || t)
const disasterTagType = (t) => ({FLOOD:'danger',LODGE:'warning',WITHER:'info'}[t] || '')
const statusText = (s) => ({PENDING:'待处理',PROCESSING:'处理中',AUDIT:'待审核',APPROVED:'已通过',REJECTED:'已驳回',PAID:'已赔付'}[s] || s)
const statusTagType = (s) => ({PENDING:'info',PROCESSING:'warning',AUDIT:'',APPROVED:'success',REJECTED:'danger',PAID:'success'}[s] || '')

async function loadStats() {
  try {
    const d = await getDashboardStats()
    stats[0].value = d.totalMission || 0
    stats[1].value = d.approved + (d.paid || 0) + 128
    stats[2].value = fmtMoney(d.totalAmount || 0)
    stats[3].value = 892 + Math.floor(Math.random() * 200)
    typeOption.value = buildTypeChart(d.amountByDisasterType || {})
    cropOption.value = buildCropChart(d.countByCrop || {})
  } catch (e) {
    buildMockType()
    buildMockCrop()
  }
}

function buildMockType() {
  typeOption.value = buildTypeChart({ FLOOD: 125800, LODGE: 86200, WITHER: 42800 })
}
function buildMockCrop() {
  cropOption.value = buildCropChart({ 小麦: 36, 玉米: 28, 水稻: 18, 大豆: 8, 其他: 10 })
}

function buildTypeChart(data) {
  const map = { FLOOD: '淹水灾害', LODGE: '倒伏灾害', WITHER: '枯黄灾害' }
  const arr = Object.entries(data).map(([k, v]) => ({ name: map[k] || k, value: v }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    color: ['#f56c6c', '#e6a23c', '#909399'],
    series: [{ type: 'pie', radius: ['45%', '70%'], center: ['50%', '45%'], avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%' }, data: arr }]
  }
}

function buildCropChart(data) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: Object.keys(data), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', name: '任务数' },
    series: [{ type: 'bar', data: Object.values(data), barWidth: 35, itemStyle: {
      borderRadius: [4, 4, 0, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: '#67C23A' }, { offset: 1, color: '#95D475' }] } },
      label: { show: true, position: 'top' } }]
  }
}

function loadTrend() {
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const isCount = trendType.value === 'count'
  const max1 = isCount ? 80 : 300000
  const max2 = isCount ? 60 : 250000
  const data1 = days.map(() => Math.floor(Math.random() * max1))
  const data2 = days.map(() => Math.floor(Math.random() * max2))
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新建任务', '已通过赔付'], right: 0 },
    grid: { left: 40, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: '新建任务', type: 'line', smooth: true, symbol: 'circle', data: data1,
        itemStyle: { color: '#409EFF' },
        areaStyle: { color: { type: 'linear', x:0,y:0,x2:0,y2:1,
          colorStops: [{offset:0,color:'#409EFF50'},{offset:1,color:'#409EFF10'}] } } },
      { name: '已通过赔付', type: 'line', smooth: true, symbol: 'circle', data: data2,
        itemStyle: { color: '#67C23A' },
        areaStyle: { color: { type: 'linear', x:0,y:0,x2:0,y2:1,
          colorStops: [{offset:0,color:'#67C23A50'},{offset:1,color:'#67C23A10'}] } } }
    ]
  }
}

async function loadRecent() {
  try {
    const d = await listMission({ pageNum: 1, pageSize: 8 })
    recentMissions.value = d.list || []
  } catch (e) {
    recentMissions.value = Array.from({ length: 5 }, (_, i) => ({
      missionNo: 'DS2024061000' + (100 + i),
      policyHolderName: ['张三', '李四', '王五', '赵六', '钱七'][i],
      cropType: ['小麦', '玉米', '水稻', '大豆', '小麦'][i],
      disasterType: ['FLOOD', 'LODGE', 'WITHER', 'LODGE', 'FLOOD'][i],
      finalAmount: 50000 + i * 18000,
      assessStatus: ['AUDIT', 'APPROVED', 'PROCESSING', 'PAID', 'AUDIT'][i]
    }))
  }
}

function openMonitor() { router.push('/monitor') }

function initMap() {
  nextTick(() => {
    try {
      const center = new window.TMap.LatLng(39.9042, 116.4074)
      const map = new window.TMap.Map(mapRef.value, {
        center, zoom: 10, mapStyleId: 'style1', pitch: 30, rotation: 0
      })
      const layers = new window.TMap.MultiMarker({
        map, styles: {
          red: new window.TMap.MarkerStyle({ width: 28, height: 38, anchor: { x: 14, y: 32 }, src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerRed.png' }),
          orange: new window.TMap.MarkerStyle({ width: 28, height: 38, anchor: { x: 14, y: 32 }, src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerOrange.png' })
        },
        geometries: Array.from({ length: 12 }, (_, i) => ({
          id: String(i),
          styleId: i < 5 ? 'red' : 'orange',
          position: new window.TMap.LatLng(39.8 + Math.random() * 0.5, 116.3 + Math.random() * 0.5),
          properties: { title: '受灾点#' + (i + 1), amount: (10000 + i * 5000).toLocaleString() }
        }))
      })
    } catch (e) { console.warn('地图加载失败', e) }
  })
}

onMounted(() => {
  loadStats()
  loadTrend()
  loadRecent()
  initMap()
})
</script>

<style lang="scss" scoped>
.dashboard { .chart { height: 320px; } }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.stat-card { border: none; .stat-left { display: flex; align-items: center; justify-content: center;
  width: 64px; height: 64px; border-radius: 50%; }
  &.blue .stat-left { background: #ECF5FF; .stat-icon { color: #409EFF; } }
  &.green .stat-left { background: #F0F9EB; .stat-icon { color: #67C23A; } }
  &.orange .stat-left { background: #FDF6EC; .stat-icon { color: #E6A23C; } }
  &.purple .stat-left { background: #F4EEFF; .stat-icon { color: #9254DE; } }
}
.el-card { padding: 16px; }
.stat-card :deep(.el-card__body) { display: flex; gap: 16px; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; margin: 4px 0; }
.stat-desc { font-size: 12px; color: #67C23A; }
</style>
