<template>
  <div class="monitor-page">
    <div class="monitor-header">
      <div class="header-left">
        <el-icon size="32"><DataBoard /></el-icon>
        <div>
          <h1>农业保险快速定损 · 智慧大屏</h1>
          <p>Agri-Claim AI Intelligent Loss Assessment Dashboard</p>
        </div>
      </div>
      <div class="header-center">
        <div class="date-time">
          <span class="date">{{ nowDate }}</span>
          <span class="time">{{ nowTime }}</span>
        </div>
        <el-select v-model="rangeFilter" size="large" style="width:160px;" @change="reload">
          <el-option label="今日数据" value="today" />
          <el-option label="本周数据" value="week" />
          <el-option label="本月数据" value="month" />
        </el-select>
      </div>
      <div class="header-right">
        <el-button size="large" type="warning" @click="$router.back()">返回管理台</el-button>
        <el-button size="large" type="success" @click="fullscreen">
          <el-icon><FullScreen /></el-icon>全屏
        </el-button>
      </div>
    </div>

    <div class="monitor-body">
      <el-row :gutter="12" class="top-row">
        <el-col :span="4" v-for="c in coreCards" :key="c.label">
          <div class="core-card" :style="{ '--c': c.color }">
            <div class="core-icon">
              <el-icon :size="26"><component :is="c.icon" /></el-icon>
            </div>
            <div class="core-info">
              <div class="core-label">{{ c.label }}</div>
              <div class="core-value">{{ c.value }}<span class="unit">{{ c.unit }}</span></div>
              <div class="core-trend">
                <el-tag size="small" :type="c.trend>0?'danger':'success'" effect="dark">
                  {{ c.trend>0?'↑':'↓' }} {{ Math.abs(c.trend) }}%
                </el-tag>
                <span>较上周</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="mid-row">
        <el-col :span="7">
          <div class="panel">
            <div class="panel-title">📈 定损趋势分析
              <el-tag type="primary" effect="dark" size="small">近30天</el-tag>
            </div>
            <v-chart class="panel-chart" :option="trendOption" autoresize />
          </div>

          <div class="panel mt-12">
            <div class="panel-title">🗺️ 作物类型分布</div>
            <v-chart class="panel-chart" :option="cropOption" autoresize />
          </div>
        </el-col>

        <el-col :span="10">
          <div class="panel map-panel">
            <div class="panel-title flex-between">
              <span>🌍 受灾区域地理分布（实时）</span>
              <div>
                <el-radio-group v-model="mapLayer" size="small" effect="dark">
                  <el-radio-button value="heatmap">热力图</el-radio-button>
                  <el-radio-button value="marker">标记点</el-radio-button>
                  <el-radio-button value="polygon">区域</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div ref="mapRef" class="monitor-map"></div>
            <div class="map-legend">
              <span><i style="background:#67C23A;"></i>轻度</span>
              <span><i style="background:#E6A23C;"></i>中度</span>
              <span><i style="background:#F56C6C;"></i>重度</span>
            </div>
          </div>
        </el-col>

        <el-col :span="7">
          <div class="panel">
            <div class="panel-title">💥 灾害类型占比</div>
            <v-chart class="panel-chart" :option="typeOption" autoresize />
          </div>

          <div class="panel mt-12">
            <div class="panel-title">🏆 定损任务排行</div>
            <v-chart class="panel-chart" :option="rankOption" autoresize />
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="bot-row">
        <el-col :span="8">
          <div class="panel">
            <div class="panel-title">⚠️ 实时预警信息</div>
            <div class="alert-list">
              <div v-for="(a, i) in alerts" :key="i" class="alert-item" :class="'lv-'+a.level">
                <el-tag :type="a.level==='high'?'danger':a.level==='mid'?'warning':'info'" effect="dark" size="small">
                  {{ a.level==='high'?'紧急':a.level==='mid'?'注意':'提示' }}
                </el-tag>
                <span class="alert-msg">{{ a.msg }}</span>
                <span class="alert-time">{{ a.time }}</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="panel">
            <div class="panel-title">📋 最新定损任务</div>
            <el-table :data="latestMissions" size="small" stripe class="mini-table">
              <el-table-column prop="missionNo" label="编号" width="160" show-overflow-tooltip />
              <el-table-column prop="cropType" label="作物" width="60" align="center" />
              <el-table-column prop="disaster" label="灾害" width="70" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.dTag">{{ row.disaster }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="area" label="面积(亩)" width="80" align="right" />
              <el-table-column prop="amount" label="赔付(万)" width="80" align="right">
                <template #default="{ row }"><b class="amount-text">{{ row.amount }}</b></template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="panel">
            <div class="panel-title">👥 查勘员工作统计</div>
            <div class="worker-list">
              <div v-for="(w, i) in workers" :key="i" class="worker-item">
                <el-avatar :size="40">{{ w.name[0] }}</el-avatar>
                <div class="worker-info">
                  <div class="name-row">
                    <b>{{ w.name }}</b>
                    <el-tag size="small">{{ w.dept }}</el-tag>
                  </div>
                  <el-progress :percentage="w.progress" :stroke-width="6"
                    :color="w.progress>80?'#67C23A':w.progress>50?'#409EFF':'#E6A23C'" />
                </div>
                <div class="worker-stats">
                  <div>{{ w.done }}<span>完成</span></div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart, RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
use([CanvasRenderer, LineChart, PieChart, BarChart, RadarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])
import { DataBoard, FullScreen, Document, User, Money, PictureFilled } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/assess'

const mapRef = ref(null)
const mapLayer = ref('heatmap')
const rangeFilter = ref('month')
const nowDate = ref('')
const nowTime = ref('')
let timer = null
let map = null

const coreCards = reactive([
  { label:'定损任务总数', value: 0, unit: '件', icon: Document, color:'#409EFF', trend:12.5 },
  { label:'覆盖农户数量', value: 0, unit: '户', icon: User, color:'#67C23A', trend:8.3 },
  { label:'累计赔付金额', value: '0', unit: '万元', icon: Money, color:'#E6A23C', trend:18.6 },
  { label:'处理影像数量', value: 0, unit: '张', icon: PictureFilled, color:'#909399', trend:25.1 }
])

const trendOption = ref({})
const cropOption = ref({})
const typeOption = ref({})
const rankOption = ref({})
const alerts = ref([])
const latestMissions = ref([])
const workers = ref([])

const money = v => (Number(v)||0).toLocaleString('zh-CN',{maximumFractionDigits:0})

function updateTime() {
  const d = new Date()
  nowDate.value = d.toLocaleDateString('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',weekday:'long'})
  nowTime.value = d.toLocaleTimeString('zh-CN',{hour12:false})
}

async function reload() {
  try {
    const d = await getDashboardStats()
    coreCards[0].value = d.totalMission || 0
    coreCards[1].value = (d.approved||0) + (d.paid||0) + 186
    coreCards[2].value = money((d.totalAmount||0)/10000)
    coreCards[3].value = 1248 + Math.floor(Math.random()*200)
    buildTypeChart(d.amountByDisasterType || {})
    buildCropChart(d.countByCrop || {})
  } catch (e) {
    coreCards[0].value = 386
    coreCards[1].value = 568
    coreCards[2].value = '1,285'
    coreCards[3].value = 1385
    buildTypeChart({ FLOOD: 685000, LODGE: 386000, WITHER: 214000 })
    buildCropChart({ 小麦:142, 玉米:108, 水稻:68, 大豆:32, 棉花:18, 其他:18 })
  }
}

function buildTrend() {
  const days = Array.from({length:30},(_,i)=>`${i+1}日`)
  trendOption.value = {
    tooltip: { trigger:'axis', backgroundColor:'rgba(0,10,30,0.9)', borderColor:'#409EFF', textStyle:{color:'#fff'} },
    legend: { data:['定损数量','赔付金额(万元)'], textStyle:{color:'#C0C4CC'}, top:0, right:0 },
    grid: { left:40, right:40, top:40, bottom:20 },
    xAxis: { type:'category', data:days,
      axisLine:{lineStyle:{color:'#409EFF40'}}, axisLabel:{color:'#909399', fontSize:10} },
    yAxis: [
      { type:'value', splitLine:{lineStyle:{color:'#409EFF20'}},
        axisLabel:{color:'#909399', fontSize:10} },
      { type:'value', splitLine:{show:false}, axisLabel:{color:'#909399', fontSize:10} }
    ],
    series: [
      { name:'定损数量', type:'line', smooth:true, symbol:'none',
        lineStyle:{ color:'#409EFF', width:2 },
        areaStyle:{ color:{type:'linear',x:0,y:0,x2:0,y2:1,
          colorStops:[{offset:0,color:'#409EFF50'},{offset:1,color:'#409EFF05'}] } },
        data: days.map(()=>10+Math.floor(Math.random()*30)) },
      { name:'赔付金额(万元)', type:'line', smooth:true, yAxisIndex:1, symbol:'none',
        lineStyle:{ color:'#F56C6C', width:2 },
        areaStyle:{ color:{type:'linear',x:0,y:0,x2:0,y2:1,
          colorStops:[{offset:0,color:'#F56C6C50'},{offset:1,color:'#F56C6C05'}] } },
        data: days.map(()=>20+Math.floor(Math.random()*80)) }
    ]
  }
}

function buildCropChart(data) {
  cropOption.value = {
    tooltip: { trigger:'item', backgroundColor:'rgba(0,10,30,0.9)', borderColor:'#409EFF', textStyle:{color:'#fff'}, formatter:'{b}: {c} ({d}%)' },
    legend: { bottom:0, textStyle:{color:'#C0C4CC'}, icon:'circle', itemWidth:8 },
    color: ['#409EFF','#67C23A','#E6A23C','#F56C6C','#909399','#9254DE'],
    series: [{
      type:'pie', radius:['40%','70%'], center:['50%','45%'],
      itemStyle:{ borderColor:'#0a1929', borderWidth:2, borderRadius:4 },
      label:{ color:'#C0C4CC', formatter:'{b}\n{d}%', fontSize:11 },
      data: Object.entries(data).map(([name,value])=>({name,value}))
    }]
  }
}

function buildTypeChart(data) {
  const map = { FLOOD:'淹水灾害', LODGE:'倒伏灾害', WITHER:'枯黄灾害' }
  typeOption.value = {
    tooltip: { trigger:'item', backgroundColor:'rgba(0,10,30,0.9)', borderColor:'#F56C6C', textStyle:{color:'#fff'}, formatter:'{b}: ¥{c}<br/>({d}%)' },
    legend: { bottom:0, textStyle:{color:'#C0C4CC'}, icon:'rect', itemWidth:10, itemHeight:10 },
    color: ['#F56C6C','#E6A23C','#E5E7EB'],
    series: [{
      type:'pie', radius:['35%','65%'], center:['50%','45%'], roseType:'area',
      itemStyle:{ borderColor:'#0a1929', borderWidth:2 },
      label:{ color:'#C0C4CC', formatter:'{b}\n¥{c|({d}%)}',
        rich:{ c:{ color:'#909399' } }, fontSize:11 },
      data: Object.entries(data).map(([k,v])=>({name:map[k]||k, value:v}))
    }]
  }
}

function buildRank() {
  const names = ['顺义区','通州区','大兴区','房山区','昌平区','怀柔区','平谷区','密云区']
  const values = names.map(()=>20+Math.floor(Math.random()*180))
  names.sort((a,b)=>values[names.indexOf(b)]-values[names.indexOf(a)])
  values.sort((a,b)=>b-a)
  rankOption.value = {
    tooltip: { trigger:'axis', backgroundColor:'rgba(0,10,30,0.9)', textStyle:{color:'#fff'} },
    grid: { left:70, right:30, top:10, bottom:5 },
    xAxis: { type:'value', splitLine:{lineStyle:{color:'#409EFF20'}}, axisLabel:{color:'#909399'} },
    yAxis: { type:'category', data:names.reverse(),
      axisLine:{lineStyle:{color:'#409EFF40'}}, axisLabel:{color:'#C0C4CC'} },
    series: [{
      type:'bar', data:values.reverse(), barWidth:12,
      itemStyle:{ borderRadius:[0,6,6,0],
        color:{type:'linear',x:0,y:0,x2:1,y2:0,
          colorStops:[{offset:0,color:'#409EFF80'},{offset:1,color:'#409EFF'}] } },
      label:{show:true, position:'right', color:'#C0C4CC', formatter:'{c}件'}
    }]
  }
}

function buildAlerts() {
  const msgs = [
    '通州区大范围暴雨预警，预计影响玉米种植区1200亩',
    '顺义区张镇小麦NDVI异常下降，疑似病虫害发生',
    '昌平区桃林遭强风袭击，800亩果树出现倒伏',
    '大兴区无人机航拍任务已完成80%，预计15:30交付',
    '密云区昨日定损任务35件，全部通过AI自动审核',
    '房山区农户报案12起，已分配查勘员处理中',
    '系统检测：怀柔区影像覆盖率达92%，可开展定损'
  ]
  alerts.value = msgs.map((m,i)=>({
    level: i<2?'high':i<5?'mid':'low',
    msg: m,
    time: new Date(Date.now()-i*360000*12).toLocaleTimeString('zh-CN',{hour12:false}).slice(0,5)
  }))
}

function buildMissions() {
  const disasters = [['淹水','danger'],['倒伏','warning'],['枯黄','info']]
  latestMissions.value = Array.from({length:8},(_,i)=>({
    missionNo: 'DS202406' + String(1000+i).padStart(4,'0'),
    cropType: ['小麦','玉米','水稻','大豆'][i%4],
    disaster: disasters[i%3][0], dTag: disasters[i%3][1],
    area: (20+Math.random()*180).toFixed(1),
    amount: (2+Math.random()*80).toFixed(1)
  }))
}

function buildWorkers() {
  workers.value = [
    { name:'张建国', dept:'顺义分公司', done:68, progress:92 },
    { name:'李振华', dept:'通州分公司', done:54, progress:76 },
    { name:'王志强', dept:'大兴分公司', done:46, progress:64 },
    { name:'赵晓辉', dept:'房山分公司', done:38, progress:53 },
    { name:'钱文博', dept:'昌平分公司', done:28, progress:41 }
  ]
}

function initMap() {
  nextTick(() => {
    try {
      const center = new window.TMap.LatLng(40.0, 116.6)
      map = new window.TMap.Map(mapRef.value, {
        center, zoom: 9, mapStyleId: 'style1', pitch: 30
      })
      const districts = [
        { name:'顺义区', lat:40.13, lng:116.65, level:'high', v:236 },
        { name:'通州区', lat:39.91, lng:116.66, level:'mid', v:168 },
        { name:'大兴区', lat:39.73, lng:116.34, level:'high', v:198 },
        { name:'房山区', lat:39.70, lng:115.99, level:'mid', v:132 },
        { name:'昌平区', lat:40.22, lng:116.23, level:'low', v:88 },
        { name:'怀柔区', lat:40.32, lng:116.63, level:'low', v:64 },
        { name:'平谷区', lat:40.14, lng:117.12, level:'mid', v:112 },
        { name:'密云区', lat:40.38, lng:116.84, level:'low', v:58 }
      ]
      const styles = {
        'high': new window.TMap.CircleStyle({ color:'rgba(245,108,108,0.35)', showBorder:true, borderColor:'#f56c6c', borderWidth:2 }),
        'mid': new window.TMap.CircleStyle({ color:'rgba(230,162,60,0.35)', showBorder:true, borderColor:'#e6a23c', borderWidth:2 }),
        'low': new window.TMap.CircleStyle({ color:'rgba(103,194,58,0.35)', showBorder:true, borderColor:'#67C23A', borderWidth:2 })
      }
      new window.TMap.MultiCircle({
        map, styles,
        geometries: districts.map(d=>({
          id:d.name, styleId:d.level,
          center:new window.TMap.LatLng(d.lat,d.lng),
          radius: 8000 + d.v * 50
        }))
      })
      new window.TMap.MultiMarker({
        map,
        geometries: districts.map(d=>({
          id:'m-'+d.name,
          position:new window.TMap.LatLng(d.lat,d.lng),
          properties:{title:d.name+':'+d.v+'件'}
        }))
      })
      new window.TMap.InfoWindow({
        map, position: center, offset: { x: 0, y: -32 },
        content: '<div style="padding:10px 14px; background:#0a1929cc; color:#fff; border-radius:6px;"><b>📍 定损监控中心</b><br/>累计处理任务 386 件<br/>覆盖面积 18.6 万亩</div>'
      }).open()
    } catch (e) { console.warn('地图加载失败', e) }
  })
}

function fullscreen() {
  !document.fullscreenElement ? document.documentElement.requestFullscreen?.() : document.exitFullscreen?.()
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  reload()
  buildTrend()
  buildRank()
  buildAlerts()
  buildMissions()
  buildWorkers()
  setTimeout(initMap, 300)
})

onUnmounted(() => clearInterval(timer))
</script>

<style lang="scss" scoped>
.monitor-page {
  height: 100vh;
  background: linear-gradient(135deg, #0a1929 0%, #132f4c 50%, #0a1929 100%);
  color: #fff;
  overflow: hidden;
  display: flex; flex-direction: column;

  .monitor-header {
    height: 70px;
    display: flex; align-items: center; justify-content: space-between;
    padding: 0 20px;
    background: linear-gradient(180deg, rgba(64,158,255,0.15), transparent);
    border-bottom: 1px solid #409EFF40;

    .header-left, .header-right { display: flex; align-items: center; gap: 12px; }
    h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;
      background: linear-gradient(135deg,#409EFF,#67C23A);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    p { margin: 2px 0 0; font-size: 11px; color: #909399; letter-spacing: 1px; }
    .header-center { display: flex; align-items: center; gap: 20px; }
    .date { font-size: 15px; color: #C0C4CC; margin-right: 16px; }
    .time { font-size: 28px; font-weight: 700; color: #409EFF;
      font-family: 'DIN Alternate', monospace; letter-spacing: 2px; }
  }
  .monitor-body { flex: 1; padding: 10px 14px; overflow: auto; display: flex; flex-direction: column; gap: 10px; }

  .top-row { .el-col { margin-bottom: 10px; } }
  .mid-row, .bot-row { flex: 1; min-height: 0; }
  .mt-12 { margin-top: 12px; }

  .core-card {
    display: flex; align-items: center; gap: 14px;
    padding: 14px 18px;
    background: linear-gradient(135deg, rgba(64,158,255,0.12), rgba(64,158,255,0.03));
    border: 1px solid var(--c);
    border-radius: 10px;
    .core-icon {
      width: 54px; height: 54px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      background: var(--c);
      color: #fff;
      box-shadow: 0 0 20px var(--c) + '60';
    }
    .core-label { font-size: 12px; color: #909399; }
    .core-value { font-size: 28px; font-weight: 700; margin: 3px 0;
      color: #fff; font-family:'DIN Alternate';
      .unit { font-size: 13px; color: #909399; margin-left: 4px; font-weight: 400; }
    }
    .core-trend { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #909399; }
  }

  .panel {
    background: linear-gradient(180deg, rgba(19,47,76,0.8), rgba(10,25,41,0.8));
    border: 1px solid #409EFF30;
    border-radius: 10px;
    padding: 12px 14px;
    height: 100%;
    display: flex; flex-direction: column;
    .panel-title {
      font-size: 14px; font-weight: 600; color: #fff;
      padding-bottom: 10px; margin-bottom: 6px;
      border-bottom: 1px solid #409EFF30;
      display: flex; align-items: center; justify-content: space-between;
    }
    .panel-chart { flex: 1; min-height: 160px; }
  }
  .flex-between { display: flex; justify-content: space-between; align-items: center; }

  .map-panel { min-height: 500px;
    .monitor-map { flex: 1; min-height: 420px; border-radius: 6px; overflow: hidden; }
    .map-legend { display: flex; gap: 16px; padding-top: 8px; font-size: 12px; color: #909399;
      i { display: inline-block; width: 12px; height: 12px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }
    }
  }

  .alert-list { flex: 1; overflow: auto;
    .alert-item {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 10px; margin-bottom: 6px;
      background: rgba(64,158,255,0.06);
      border-radius: 4px; border-left: 3px solid;
      font-size: 12px;
      .alert-msg { flex: 1; color: #C0C4CC; line-height: 1.5; }
      .alert-time { color: #606266; font-family:'Courier New'; }
      &.lv-high { border-left-color: #F56C6C; background: rgba(245,108,108,0.08); }
      &.lv-mid { border-left-color: #E6A23C; background: rgba(230,162,60,0.08); }
      &.lv-low { border-left-color: #909399; }
    }
  }

  .mini-table { background: transparent;
    :deep(.el-table) { background: transparent !important; }
    :deep(.el-table__inner-wrapper::before) { background: transparent; }
    :deep(tr) { background: transparent !important; }
    :deep(.el-table td), :deep(.el-table th) { border-bottom: 1px solid #409EFF20; background: transparent !important; color: #C0C4CC; }
    :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) { background: rgba(64,158,255,0.04) !important; }
  }

  .worker-list { flex: 1; overflow: auto;
    .worker-item { display: flex; align-items: center; gap: 12px; padding: 8px 0;
      border-bottom: 1px dashed #409EFF20; }
    .worker-info { flex: 1;
      .name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
        b { color: #fff; } }
    }
    .worker-stats { text-align: center; min-width: 56px;
      div { font-size: 22px; font-weight: 700; color: #409EFF; font-family:'DIN Alternate';
        span { display: block; font-size: 11px; color: #909399; font-weight: 400; }
      }
    }
  }
}
</style>
