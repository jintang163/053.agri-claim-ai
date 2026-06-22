<template>
  <div class="flight-plan-page">
    <el-page-header @back="$router.back()" class="page-header">
      <template #content>
        <span class="page-title">无人机航线规划</span>
        <el-tag type="info" size="small" class="ml-10">之字形全覆盖</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="16">
        <el-card shadow="hover" class="map-card">
          <template #header>
            <div class="card-title flex-between">
              <span><el-icon><Location /></el-icon> 作业地块</span>
              <div>
                <el-button size="small" type="danger" plain @click="clearVertices">
                  <el-icon><Delete /></el-icon>清除顶点
                </el-button>
                <el-button size="small" type="primary" :disabled="vertices.length < 3" @click="generateRoute">
                  <el-icon><Promotion /></el-icon>生成航线
                </el-button>
              </div>
            </div>
          </template>
          <div class="tip">
            <el-alert title="点击地图添加多边形顶点，至少3个顶点可生成航线；拖拽顶点调整边界；长按顶点删除"
              type="info" :closable="false" show-icon />
          </div>
          <div ref="mapRef" class="plan-map" style="height: 580px;"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="param-card mb-15">
          <template #header>
            <div class="card-title"><el-icon><Setting /></el-icon>飞行参数</div>
          </template>
          <el-form label-width="90px" size="default">
            <el-form-item label="飞行高度">
              <el-slider v-model="flightHeight" :min="50" :max="200" :step="10"
                show-stops show-input :input-size="50" />
              <div class="param-unit">{{ flightHeight }} m</div>
            </el-form-item>
            <el-form-item label="航向重叠">
              <el-slider v-model="frontOverlap" :min="60" :max="90" :step="5"
                show-stops show-input :input-size="50" />
              <div class="param-unit">{{ frontOverlap }} %</div>
            </el-form-item>
            <el-form-item label="旁向重叠">
              <el-slider v-model="sideOverlap" :min="50" :max="80" :step="5"
                show-stops show-input :input-size="50" />
              <div class="param-unit">{{ sideOverlap }} %</div>
            </el-form-item>
            <el-form-item label="飞行速度">
              <el-slider v-model="flightSpeed" :min="1" :max="15" :step="1"
                show-stops show-input :input-size="50" />
              <div class="param-unit">{{ flightSpeed }} m/s</div>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" class="param-card mb-15" v-if="gsdResult">
          <template #header>
            <div class="card-title"><el-icon><DataLine /></el-icon>GSD 地面采样距离</div>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="GSD">{{ gsdResult.gsd }} cm/px</el-descriptions-item>
            <el-descriptions-item label="地面幅宽">{{ gsdResult.groundWidth }} m</el-descriptions-item>
            <el-descriptions-item label="航向间距">{{ routePlan?.spacingAlong || '-' }} m</el-descriptions-item>
            <el-descriptions-item label="旁向间距">{{ routePlan?.spacingAcross || '-' }} m</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="hover" class="param-card mb-15">
          <template #header>
            <div class="card-title flex-between">
              <span><el-icon><Warning /></el-icon>障碍物管理</span>
              <el-button size="small" type="primary" plain @click="showAddObstacle = true">
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
          </template>
          <el-table :data="obstacles" size="small" v-if="obstacles.length">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="type" label="类型">
              <template #default="{ row }">
                <el-tag size="small" :type="obstacleTypeTag(row.type).type">
                  {{ obstacleTypeTag(row.type).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="radius" label="半径(m)" width="80" />
            <el-table-column prop="height" label="高度(m)" width="80" />
            <el-table-column label="操作" width="60">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="obstacles.splice($index, 1)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无障碍物" :image-size="60" />
        </el-card>

        <el-card shadow="hover" class="param-card" v-if="routePlan">
          <template #header>
            <div class="card-title flex-between">
              <span><el-icon><CircleCheck /></el-icon>航线规划结果</span>
              <el-button size="small" type="success" @click="saveTemplate">
                <el-icon><Collection /></el-icon>保存模板
              </el-button>
            </div>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="航点数">{{ routePlan.waypointCount }}</el-descriptions-item>
            <el-descriptions-item label="照片数">{{ routePlan.photoCount }}</el-descriptions-item>
            <el-descriptions-item label="航程">{{ routePlan.estimatedDistance }} m</el-descriptions-item>
            <el-descriptions-item label="预计时间">{{ routePlan.estimatedTime }} min</el-descriptions-item>
            <el-descriptions-item label="覆盖面积">{{ routePlan.estimatedArea }} 亩</el-descriptions-item>
            <el-descriptions-item label="预计耗电">{{ routePlan.estimatedBattery }} %</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showAddObstacle" title="添加障碍物" width="500px">
      <el-form :model="obstacleForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="obstacleForm.name" placeholder="如：高压塔1号" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="obstacleForm.type">
            <el-option label="高压线塔" value="POWER_TOWER" />
            <el-option label="树木" value="TREE" />
            <el-option label="建筑物" value="BUILDING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="obstacleForm.longitude" :precision="8" :step="0.000001" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="obstacleForm.latitude" :precision="8" :step="0.000001" />
        </el-form-item>
        <el-form-item label="影响半径">
          <el-input-number v-model="obstacleForm.radius" :min="5" :max="500" />
          <span class="ml-5">米</span>
        </el-form-item>
        <el-form-item label="障碍物高度">
          <el-input-number v-model="obstacleForm.height" :min="5" :max="500" />
          <span class="ml-5">米</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddObstacle = false">取消</el-button>
        <el-button type="primary" @click="addObstacle">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Location, Setting, DataLine, Promotion, Delete, Warning, CircleCheck, Plus, Collection } from '@element-plus/icons-vue'
import { planRoute, calculateGsd, avoidObstacles, saveTemplate as saveTplApi } from '@/api/drone'
import { formatDateTime } from '@/utils'

const mapRef = ref(null)
const vertices = ref([])
const flightHeight = ref(100)
const frontOverlap = ref(80)
const sideOverlap = ref(60)
const flightSpeed = ref(5)
const routePlan = ref(null)
const gsdResult = ref(null)
const obstacles = ref([])
const showAddObstacle = ref(false)
const obstacleForm = reactive({
  name: '', type: 'POWER_TOWER', longitude: 116.4, latitude: 39.9, radius: 20, height: 30
})

let mapInstance = null
let polygonLayer = null
let routeLayer = null
let vertexMarkers = null
let obstacleMarkers = null
let draggingIndex = -1

function obstacleTypeTag(type) {
  const map = {
    POWER_TOWER: { type: 'danger', text: '高压线塔' },
    TREE: { type: 'success', text: '树木' },
    BUILDING: { type: 'warning', text: '建筑物' },
    OTHER: { type: 'info', text: '其他' }
  }
  return map[type] || { type: 'info', text: type }
}

function initMap() {
  nextTick(() => {
    try {
      const center = new window.TMap.LatLng(39.9042, 116.4074)
      mapInstance = new window.TMap.Map(mapRef.value, { center, zoom: 17, pitch: 30 })

      mapInstance.on('click', (evt) => {
        const lat = evt.latLng.getLat().toFixed(8)
        const lon = evt.latLng.getLng().toFixed(8)
        vertices.value.push({ lat: parseFloat(lat), lon: parseFloat(lon) })
        drawPolygon()
      })

      updateGsd()
    } catch (e) {
      console.warn('地图初始化失败', e)
    }
  })
}

function drawPolygon() {
  if (polygonLayer) { polygonLayer.setMap(null); polygonLayer = null }
  if (vertexMarkers) { vertexMarkers.setMap(null); vertexMarkers = null }
  if (obstacleMarkers) { obstacleMarkers.setMap(null); obstacleMarkers = null }

  if (vertices.value.length >= 1) {
    const paths = vertices.value.map(v => new window.TMap.LatLng(v.lat, v.lon))
    if (vertices.value.length >= 3) paths.push(paths[0])

    polygonLayer = new window.TMap.MultiPolygon({
      map: mapInstance,
      styles: {
        'region': new window.TMap.MultiPolygonStyle({
          color: 'rgba(64,158,255,0.15)',
          showBorder: true,
          borderColor: '#409EFF',
          borderWidth: 3
        })
      },
      geometries: [{ id: 'region', styleId: 'region', paths }]
    })

    vertexMarkers = new window.TMap.MultiMarker({
      map: mapInstance,
      styles: {
        'vertex': new window.TMap.MultiMarkerStyle({
          width: 18, height: 18, anchor: { x: 9, y: 9 },
          src: 'data:image/svg+xml;base64,' + btoa(
            '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18">' +
            '<circle cx="9" cy="9" r="7" fill="#409EFF" stroke="white" stroke-width="2"/></svg>'
          )
        })
      },
      geometries: vertices.value.map((v, i) => ({
        id: String(i),
        styleId: 'vertex',
        position: new window.TMap.LatLng(v.lat, v.lon),
        properties: { index: i }
      }))
    })
  }

  if (obstacles.value.length) {
    const obsGeoms = []
    obstacles.value.forEach((obs, i) => {
      const color = obs.type === 'POWER_TOWER' ? '#f56c6c' : obs.type === 'TREE' ? '#67c23a' : '#e6a23c'
      obsGeoms.push({
        id: 'obs_' + i,
        styleId: 'obs',
        paths: Array.from({ length: 17 }, (_, k) => {
          const a = 2 * Math.PI * k / 16
          const r = obs.radius / 111000
          return new window.TMap.LatLng(obs.latitude + r * Math.sin(a), obs.longitude + r * Math.cos(a))
        })
      })
    })

    obstacleMarkers = new window.TMap.MultiPolygon({
      map: mapInstance,
      styles: {
        'obs': new window.TMap.MultiPolygonStyle({
          color: 'rgba(245,108,108,0.1)',
          showBorder: true,
          borderColor: '#f56c6c',
          borderWidth: 2,
          borderDash: [8, 4]
        })
      },
      geometries: obsGeoms
    })
  }
}

function drawRoute() {
  if (routeLayer) { routeLayer.setMap(null); routeLayer = null }
  if (!routePlan.value?.waypoints) return

  const normalPts = routePlan.value.waypoints
    .filter(w => w.index >= 0)
    .map(w => new window.TMap.LatLng(w.latitude, w.longitude))

  const styleMap = {
    '-1': { color: '#67c23a', width: 4, dashArray: [10, 5] },
    '-2': { color: '#e6a23c', width: 4, dashArray: [10, 5] },
    '-3': { color: '#f56c6c', width: 4, dashArray: [10, 5] }
  }

  const geoms = []
  for (let i = 0; i < normalPts.length - 1; i++) {
    geoms.push({
      id: String(i),
      styleId: 'normal',
      paths: [normalPts[i], normalPts[i + 1]]
    })
  }

  routeLayer = new window.TMap.MultiPolyline({
    map: mapInstance,
    styles: {
      'normal': new window.TMap.MultiPolylineStyle({ color: '#409EFF', width: 3 }),
      'takeoff': new window.TMap.MultiPolylineStyle({ color: '#67c23a', width: 4, borderDash: [10, 5] }),
      'land': new window.TMap.MultiPolylineStyle({ color: '#e6a23c', width: 4, borderDash: [10, 5] }),
      'detour': new window.TMap.MultiPolylineStyle({ color: '#f56c6c', width: 4, borderDash: [10, 5] })
    },
    geometries: geoms
  })
}

function clearVertices() {
  vertices.value = []
  routePlan.value = null
  drawPolygon()
  drawRoute()
  ElMessage.info('已清除所有顶点')
}

async function updateGsd() {
  try {
    const res = await calculateGsd({ flightHeight: flightHeight.value })
    gsdResult.value = res
  } catch (e) {
    gsdResult.value = { gsd: 2.75, groundWidth: 15.0, groundHeight: 10.0 }
  }
}

watch([flightHeight], updateGsd)

async function generateRoute() {
  if (vertices.value.length < 3) {
    ElMessage.warning('至少需要3个顶点')
    return
  }
  try {
    const res = await planRoute({
      polygonVertices: vertices.value.map(v => ({ lat: v.lat, lon: v.lon })),
      flightHeight: flightHeight.value,
      frontOverlap: frontOverlap.value,
      sideOverlap: sideOverlap.value,
      flightSpeed: flightSpeed.value,
      obstacles: obstacles.value
    })
    routePlan.value = res
    drawPolygon()
    drawRoute()
    ElNotification.success({
      title: '航线生成成功',
      message: `航点数: ${res.waypointCount} | 航程: ${res.estimatedDistance}m | 时间: ${res.estimatedTime}min`
    })
  } catch (e) {
    ElMessage.error('航线生成失败')
  }
}

function addObstacle() {
  if (!obstacleForm.name) {
    ElMessage.warning('请输入障碍物名称')
    return
  }
  obstacles.value.push({ ...obstacleForm })
  showAddObstacle.value = false
  obstacleForm.name = ''
  drawPolygon()
  ElMessage.success('障碍物已添加')
}

async function saveTemplate() {
  if (!routePlan.value) {
    ElMessage.warning('请先生成航线')
    return
  }
  try {
    const { value } = await ElMessage.prompt('请输入模板名称', '保存为模板', {
      inputValue: `航线模板_${formatDateTime(new Date(), 'MM-DD HH:mm')}`,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '模板名称不能为空'
    })

    await saveTplApi({
      templateName: value,
      locationName: '北京市海淀区',
      centerLon: vertices.value.reduce((s, v) => s + v.lon, 0) / vertices.value.length,
      centerLat: vertices.value.reduce((s, v) => s + v.lat, 0) / vertices.value.length,
      polygonWkt: toWktPolygon(vertices.value),
      flightHeight: flightHeight.value,
      frontOverlap: frontOverlap.value,
      sideOverlap: sideOverlap.value,
      flightSpeed: flightSpeed.value,
      estimatedTime: routePlan.value.estimatedTime,
      estimatedDistance: routePlan.value.estimatedDistance,
      estimatedArea: routePlan.value.estimatedArea,
      waypointCount: routePlan.value.waypointCount,
      photoCount: routePlan.value.photoCount,
      estimatedBattery: routePlan.value.estimatedBattery,
      routePlanJson: JSON.stringify(routePlan.value),
      obstacles: JSON.stringify(obstacles.value)
    })

    ElNotification.success({ title: '保存成功', message: `模板 "${value}" 已保存` })
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('保存失败')
  }
}

function toWktPolygon(verts) {
  const coords = verts.map(v => `${v.lon.toFixed(6)} ${v.lat.toFixed(6)}`)
  coords.push(coords[0])
  return `POLYGON((${coords.join(',')}))`
}

onMounted(initMap)
</script>

<style scoped lang="scss">
.flight-plan-page {
  .page-header { padding: 16px 20px; background: #fff; border-radius: 8px;
    .page-title { font-size: 16px; font-weight: 600; }
  }
  .tip { margin-bottom: 12px; }
  .param-card {
    :deep(.el-descriptions__label), :deep(.el-descriptions__content) {
      padding: 8px 12px; font-size: 13px;
    }
  }
  .param-unit { font-size: 13px; color: #409eff; font-weight: 600; margin-top: -8px; text-align: right; }
  .plan-map { border-radius: 6px; overflow: hidden; border: 1px solid #ebeef5; }
  .ml-5 { margin-left: 5px; }
  .ml-10 { margin-left: 10px; }
  .mt-20 { margin-top: 20px; }
  .mb-15 { margin-bottom: 15px; }
  .card-title { font-weight: 600; }
  .flex-between { display: flex; justify-content: space-between; align-items: center; }
}
</style>
