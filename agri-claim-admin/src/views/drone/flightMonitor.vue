<template>
  <div class="flight-monitor-page">
    <el-page-header @back="$router.back()" class="page-header">
      <template #content>
        <span class="page-title">无人机飞行监控</span>
        <el-tag :type="statusTag.type" size="small" class="ml-10">{{ statusTag.text }}</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="16" class="mt-16">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="battery-section">
            <div class="battery-label">电池电量</div>
            <el-progress type="dashboard" :percentage="flightStatus.batteryPercent"
              :color="batteryColor" :width="140" :stroke-width="12">
              <span class="battery-value">{{ flightStatus.batteryPercent }}%</span>
            </el-progress>
            <div class="battery-volts">{{ flightStatus.batteryVoltage }}V / {{ flightStatus.batteryCurrent }}A</div>
          </div>
          <el-divider />
          <div class="info-grid">
            <div class="info-item">
              <div class="info-label">连接状态</div>
              <div class="info-value" :class="{connected: isConnected}">
                <span class="dot"></span>
                {{ isConnected ? '已连接' : '未连接' }}
              </div>
            </div>
            <div class="info-item">
              <div class="info-label">飞行模式</div>
              <div class="info-value mode">{{ flightStatus.flightMode || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">作业任务</div>
              <div class="info-value">{{ currentTask?.taskNo || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">当前航点</div>
              <div class="info-value">
                <span class="highlight">{{ flightStatus.currentWaypointIndex || 0 }}</span>
                / {{ flightStatus.totalWaypoints || 0 }}
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="data-card mt-15">
          <el-row :gutter="8">
            <el-col :span="12">
              <div class="data-item">
                <div class="data-icon alt"><el-icon><Aim /></el-icon></div>
                <div class="data-info">
                  <div class="data-value">{{ flightStatus.aircraftAltitude || 0 }}<span class="unit">m</span></div>
                  <div class="data-label">飞行高度</div>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="data-item">
                <div class="data-icon speed"><el-icon><Cpu /></el-icon></div>
                <div class="data-info">
                  <div class="data-value">{{ flightStatus.groundSpeed || 0 }}<span class="unit">m/s</span></div>
                  <div class="data-label">地速</div>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="data-item">
                <div class="data-icon dist"><el-icon><Position /></el-icon></div>
                <div class="data-info">
                  <div class="data-value">{{ flightStatus.distanceToHome || 0 }}<span class="unit">m</span></div>
                  <div class="data-label">距家距离</div>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="data-item">
                <div class="data-icon time"><el-icon><Timer /></el-icon></div>
                <div class="data-info">
                  <div class="data-value">{{ flightTime }}<span class="unit">min</span></div>
                  <div class="data-label">飞行时长</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="hover" class="control-card mt-15">
          <template #header>
            <div class="card-title">飞行控制</div>
          </template>
          <el-row :gutter="8">
            <el-col :span="12" v-if="taskStatus === 'PENDING' || taskStatus === 'READY'">
              <el-button type="success" class="control-btn" @click="startFlight">
                <el-icon><VideoPlay /></el-icon>开始飞行
              </el-button>
            </el-col>
            <el-col :span="12" v-if="taskStatus === 'FLYING'">
              <el-button type="warning" class="control-btn" @click="pauseFlight">
                <el-icon><VideoPause /></el-icon>暂停
              </el-button>
            </el-col>
            <el-col :span="12" v-if="taskStatus === 'PAUSED'">
              <el-button type="success" class="control-btn" @click="resumeFlight">
                <el-icon><VideoPlay /></el-icon>继续
              </el-button>
            </el-col>
            <el-col :span="12" v-if="taskStatus === 'FLYING' || taskStatus === 'PAUSED'">
              <el-button type="primary" class="control-btn" @click="returnHome">
                <el-icon><House /></el-icon>返航
              </el-button>
            </el-col>
            <el-col :span="12">
              <el-button type="info" class="control-btn" @click="landNow">
                <el-icon><Bottom /></el-icon>降落
              </el-button>
            </el-col>
            <el-col :span="12">
              <el-button type="danger" class="control-btn" @click="emergencyStop">
                <el-icon><WarningFilled /></el-icon>紧急停止
              </el-button>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="hover" class="detail-card mt-15">
          <template #header>
            <div class="card-title flex-between">
              <span>详细参数</span>
              <el-switch v-model="showDetail" size="small" />
            </div>
          </template>
          <el-descriptions v-if="showDetail" :column="2" size="small" border>
            <el-descriptions-item label="航向">{{ flightStatus.heading }}°</el-descriptions-item>
            <el-descriptions-item label="云台俯仰">{{ flightStatus.gimbalPitch }}°</el-descriptions-item>
            <el-descriptions-item label="俯仰">{{ flightStatus.pitch }}°</el-descriptions-item>
            <el-descriptions-item label="横滚">{{ flightStatus.roll }}°</el-descriptions-item>
            <el-descriptions-item label="电池温度">{{ flightStatus.batteryTemperature }}°C</el-descriptions-item>
            <el-descriptions-item label="绝对高度">{{ flightStatus.absoluteAltitude }}m</el-descriptions-item>
            <el-descriptions-item label="垂直速度">{{ flightStatus.speedZ }}m/s</el-descriptions-item>
            <el-descriptions-item label="水平速度">{{ flightStatus.speedX }}/{{ flightStatus.speedY }}m/s</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card shadow="hover" class="map-card">
          <div ref="mapRef" class="monitor-map" style="height: 820px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Cpu, Position, Timer, VideoPlay, VideoPause, House, Bottom, WarningFilled } from '@element-plus/icons-vue'
import { getTask, startTask, pauseTask, resumeTask, returnTask, landTask, cancelTask, getTaskStatusLatest, getTaskStatusHistory } from '@/api/drone'
import ws from '@/utils/websocket'
import { formatDateTime } from '@/utils'

const props = defineProps({ taskId: { type: [String, Number], default: null } })
const router = useRouter()
const route = useRoute()

const mapRef = ref(null)
const showDetail = ref(true)
const currentTask = ref(null)
const taskStatus = ref('PENDING')
const flightStartTime = ref(null)
const flightTime = ref('0')

const flightStatus = reactive({
  batteryPercent: 100,
  batteryVoltage: 22.8,
  batteryCurrent: 5.2,
  batteryTemperature: 28,
  aircraftLon: null,
  aircraftLat: null,
  aircraftAltitude: 0,
  absoluteAltitude: 0,
  groundSpeed: 0,
  speedX: 0,
  speedY: 0,
  speedZ: 0,
  heading: 0,
  pitch: 0,
  roll: 0,
  yaw: 0,
  gimbalPitch: -90,
  gimbalYaw: 0,
  flightMode: 'IDLE',
  currentWaypointIndex: 0,
  totalWaypoints: 0,
  distanceToHome: 0,
  isFlying: false,
  isReturningHome: false,
  isLanding: false,
  isTakingOff: false
})

const isConnected = computed(() => flightStatus.batteryPercent > 0 && flightStatus.aircraftLon != null)
const batteryColor = computed(() => {
  if (flightStatus.batteryPercent > 30) return '#67c23a'
  if (flightStatus.batteryPercent > 15) return '#e6a23c'
  return '#f56c6c'
})
const statusTag = computed(() => {
  const map = {
    PENDING: { type: 'info', text: '待起飞' },
    READY: { type: 'primary', text: '已就绪' },
    FLYING: { type: 'success', text: '飞行中' },
    PAUSED: { type: 'warning', text: '已暂停' },
    RETURNING: { type: 'warning', text: '返航中' },
    LANDING: { type: 'info', text: '降落中' },
    COMPLETED: { type: 'success', text: '已完成' },
    FAILED: { type: 'danger', text: '失败' },
    CANCELED: { type: 'info', text: '已取消' }
  }
  return map[taskStatus.value] || { type: 'info', text: '-' }
})

let mapInstance = null
let routeLayer = null
let droneMarker = null
let homeMarker = null
let waypointMarkers = null
let statusTimer = null
let wsUnsub = null

function initMap() {
  nextTick(() => {
    try {
      const center = new window.TMap.LatLng(39.9042, 116.4074)
      mapInstance = new window.TMap.Map(mapRef.value, { center, zoom: 18, pitch: 45 })

      if (currentTask.value?.routePlan?.waypoints) {
        drawRoute(currentTask.value.routePlan.waypoints)
      }

      setTimeout(() => refreshStatus(), 500)
    } catch (e) {
      console.warn('地图初始化失败', e)
    }
  })
}

function drawRoute(waypoints) {
  if (routeLayer) { routeLayer.setMap(null); routeLayer = null }
  if (waypointMarkers) { waypointMarkers.setMap(null); waypointMarkers = null }
  if (homeMarker) { homeMarker.setMap(null); homeMarker = null }
  if (droneMarker) { droneMarker.setMap(null); droneMarker = null }

  const normalPts = waypoints.filter(w => w.index >= 0)
  if (!normalPts.length) return

  flightStatus.totalWaypoints = normalPts.length

  const paths = normalPts.map(w => new window.TMap.LatLng(w.latitude, w.longitude))
  routeLayer = new window.TMap.MultiPolyline({
    map: mapInstance,
    styles: { 'route': new window.TMap.MultiPolylineStyle({
      color: 'rgba(64,158,255,0.8)', width: 4,
      showArrow: true, arrowSpacing: 50, arrowSize: 8
    })},
    geometries: [{ id: 'route', styleId: 'route', paths }]
  })

  waypointMarkers = new window.TMap.MultiMarker({
    map: mapInstance,
    styles: { 'wp': new window.TMap.MultiMarkerStyle({
      width: 16, height: 16, anchor: { x: 8, y: 8 },
      src: 'data:image/svg+xml;base64,' + btoa(
        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">' +
        '<circle cx="8" cy="8" r="5" fill="#409EFF" stroke="white" stroke-width="2"/></svg>'
      )
    })},
    geometries: normalPts.map((w, i) => ({
      id: String(i),
      styleId: 'wp',
      position: new window.TMap.LatLng(w.latitude, w.longitude),
      properties: { index: i }
    }))
  })

  const firstWp = normalPts[0]
  homeMarker = new window.TMap.MultiMarker({
    map: mapInstance,
    styles: { 'home': new window.TMap.MultiMarkerStyle({
      width: 36, height: 36, anchor: { x: 18, y: 36 },
      src: 'data:image/svg+xml;base64,' + btoa(
        '<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36">' +
        '<path d="M18 2 L32 14 L32 32 L4 32 L4 14 Z" fill="#67c23a" stroke="white" stroke-width="2"/>' +
        '<rect x="14" y="20" width="8" height="12" fill="white"/></svg>'
      )
    })},
    geometries: [{ id: 'home', styleId: 'home',
      position: new window.TMap.LatLng(firstWp.latitude, firstWp.longitude) }]
  })

  mapInstance.panTo(new window.TMap.LatLng(firstWp.latitude, firstWp.longitude))
}

function updateDronePosition() {
  if (!flightStatus.aircraftLat || !flightStatus.aircraftLon) return

  const pos = new window.TMap.LatLng(flightStatus.aircraftLat, flightStatus.aircraftLon)

  if (!droneMarker) {
    droneMarker = new window.TMap.MultiMarker({
      map: mapInstance,
      styles: { 'drone': new window.TMap.MultiMarkerStyle({
        width: 40, height: 40, anchor: { x: 20, y: 20 },
        src: 'data:image/svg+xml;base64,' + btoa(
          '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40">' +
          '<defs><animateTransform id="rot" attributeName="transform" type="rotate" from="0 20 20" to="360 20 20" dur="1s" repeatCount="indefinite"/></defs>' +
          '<g transform="rotate(' + (flightStatus.heading || 0) + ' 20 20)">' +
          '<ellipse cx="20" cy="8" rx="16" ry="3" fill="#409EFF"/>' +
          '<ellipse cx="20" cy="32" rx="16" ry="3" fill="#409EFF"/>' +
          '<ellipse cx="8" cy="20" rx="3" ry="16" fill="#409EFF"/>' +
          '<ellipse cx="32" cy="20" rx="3" ry="16" fill="#409EFF"/>' +
          '<circle cx="20" cy="20" r="6" fill="#f56c6c" stroke="white" stroke-width="2"/></g></svg>'
        )
      })},
      geometries: [{ id: 'drone', styleId: 'drone', position: pos }]
    })
  } else {
    droneMarker.updateGeometries([{ id: 'drone', position: pos }])
  }

  mapInstance.easeTo({ center: pos, duration: 500 })
}

async function loadTask() {
  const id = props.taskId || route.query.id
  if (!id) return
  try {
    currentTask.value = await getTask(id)
    taskStatus.value = currentTask.value.flightStatus
    if (currentTask.value.startTime) {
      flightStartTime.value = new Date(currentTask.value.startTime).getTime()
    }
    initMap()
  } catch (e) {
    ElMessage.error('加载任务失败')
  }
}

async function refreshStatus() {
  const id = props.taskId || route.query.id
  if (!id) return
  try {
    const status = await getTaskStatusLatest(id)
    if (status) updateFlightStatus(status)
  } catch (e) {
  }
}

function updateFlightStatus(status) {
  Object.assign(flightStatus, status)
  updateDronePosition()
  if (status.isFlying && !flightStartTime.value) {
    flightStartTime.value = Date.now()
  }
}

async function startFlight() {
  try {
    await ElMessageBox.confirm('确认开始飞行任务？', '提示', { type: 'warning' })
    const id = props.taskId || route.query.id
    await startTask(id)
    taskStatus.value = 'FLYING'
    flightStatus.isFlying = true
    flightStartTime.value = Date.now()
    ElMessage.success('飞行任务已启动')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('启动失败')
  }
}

async function pauseFlight() {
  try {
    const id = props.taskId || route.query.id
    await pauseTask(id)
    taskStatus.value = 'PAUSED'
    ElMessage.success('已暂停')
  } catch (e) { ElMessage.error('暂停失败') }
}

async function resumeFlight() {
  try {
    const id = props.taskId || route.query.id
    await resumeTask(id)
    taskStatus.value = 'FLYING'
    ElMessage.success('已继续')
  } catch (e) { ElMessage.error('恢复失败') }
}

async function returnHome() {
  try {
    await ElMessageBox.confirm('确认控制无人机返航？', '提示', { type: 'warning' })
    const id = props.taskId || route.query.id
    await returnTask(id)
    taskStatus.value = 'RETURNING'
    flightStatus.isReturningHome = true
    ElMessage.success('已指令返航')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function landNow() {
  try {
    await ElMessageBox.confirm('确认立即降落？', '提示', { type: 'warning' })
    const id = props.taskId || route.query.id
    await landTask(id)
    taskStatus.value = 'LANDING'
    flightStatus.isLanding = true
    ElMessage.success('已指令降落')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function emergencyStop() {
  try {
    await ElMessageBox.confirm('紧急停止将强制降落无人机，确认执行？', '警告', {
      type: 'error', confirmButtonText: '紧急停止', cancelButtonText: '取消'
    })
    const id = props.taskId || route.query.id
    await cancelTask(id)
    taskStatus.value = 'CANCELED'
    ElMessage.warning('已紧急停止')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

function updateFlightTime() {
  if (flightStatus.isFlying && flightStartTime.value) {
    const mins = Math.floor((Date.now() - flightStartTime.value) / 60000)
    const secs = Math.floor(((Date.now() - flightStartTime.value) % 60000) / 1000)
    flightTime.value = `${mins}:${secs.toString().padStart(2, '0')}`
  }
}

function handleWsMessage(msg) {
  if (msg.type === 'DRONE_STATUS' && msg.taskId == (props.taskId || route.query.id)) {
    updateFlightStatus(msg.payload)
  }
  if (msg.type === 'FLIGHT_STATUS' && msg.taskId == (props.taskId || route.query.id)) {
    taskStatus.value = msg.status
    if (msg.status === 'COMPLETED' || msg.status === 'CANCELED') {
      flightStatus.isFlying = false
      if (statusTimer) clearInterval(statusTimer)
    }
  }
}

onMounted(() => {
  loadTask()
  statusTimer = setInterval(() => { refreshStatus(); updateFlightTime() }, 3000)
  wsUnsub = ws.on('message', handleWsMessage)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
  if (wsUnsub) wsUnsub()
})
</script>

<style scoped lang="scss">
.flight-monitor-page {
  .page-header { padding: 16px 20px; background: #fff; border-radius: 8px;
    .page-title { font-size: 16px; font-weight: 600; }
  }
  .status-card {
    .battery-section { text-align: center;
      .battery-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
      .battery-value { font-size: 24px; font-weight: 700; line-height: 1; }
      .battery-volts { font-size: 12px; color: #909399; margin-top: 4px; }
    }
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
      .info-item {
        .info-label { font-size: 12px; color: #909399; }
        .info-value { font-size: 14px; font-weight: 600; margin-top: 4px;
          &.connected { color: #67c23a; }
          &.mode { color: #409eff; }
          .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%;
            background: currentColor; margin-right: 6px;
            animation: blink 2s infinite;
          }
          .highlight { color: #f56c6c; font-size: 18px; }
        }
      }
    }
  }
  .data-card {
    .data-item { display: flex; align-items: center; gap: 10px; padding: 12px;
      border-radius: 8px; background: #f5f7fa;
      .data-icon { width: 44px; height: 44px; border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
        color: white; font-size: 20px;
        &.alt { background: linear-gradient(135deg, #667eea, #764ba2); }
        &.speed { background: linear-gradient(135deg, #f093fb, #f5576c); }
        &.dist { background: linear-gradient(135deg, #4facfe, #00f2fe); }
        &.time { background: linear-gradient(135deg, #43e97b, #38f9d7); }
      }
      .data-info {
        .data-value { font-size: 20px; font-weight: 700; line-height: 1.2;
          .unit { font-size: 12px; color: #909399; font-weight: 400; margin-left: 3px; }
        }
        .data-label { font-size: 12px; color: #909399; margin-top: 2px; }
      }
    }
  }
  .control-card {
    .control-btn { width: 100%; margin-bottom: 8px; }
  }
  .monitor-map { border-radius: 6px; overflow: hidden; border: 1px solid #ebeef5; }
  .ml-10 { margin-left: 10px; }
  .mt-15 { margin-top: 15px; }
  .mt-16 { margin-top: 16px; }
  .card-title { font-weight: 600; }
  .flex-between { display: flex; justify-content: space-between; align-items: center; }

  @keyframes blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.3; }
  }
}
</style>
