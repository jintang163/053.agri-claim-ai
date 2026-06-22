<template>
  <div class="upload-page">
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="hover" class="card-container">
          <template #header><div class="page-title">📸 无人机影像上传</div></template>

          <el-form :model="form" label-width="100px" size="default">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="影像类型" required>
                  <el-select v-model="form.imageType" class="w-full">
                    <el-option label="受灾前影像" value="BEFORE" />
                    <el-option label="受灾后影像" value="AFTER" />
                    <el-option label="正射DOM" value="DOM" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="灾害类型">
                  <el-select v-model="form.disasterType" class="w-full" clearable>
                    <el-option label="淹水灾害" value="FLOOD" />
                    <el-option label="倒伏灾害" value="LODGE" />
                    <el-option label="枯黄灾害" value="WITHER" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属任务">
                  <el-select v-model="form.missionId" class="w-full" clearable filterable>
                    <el-option v-for="m in missions" :key="m.id" :label="m.missionName" :value="m.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="拍摄时间">
                  <el-date-picker v-model="form.shootTime" type="datetime" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="中心经度">
                  <el-input-number v-model="form.centerLon" :precision="8" :step="0.001" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="中心纬度">
                  <el-input-number v-model="form.centerLat" :precision="8" :step="0.001" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="地理位置">
                  <el-input v-model="form.location" placeholder="如：北京市顺义区XX镇XX村" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注说明">
                  <el-input v-model="form.remark" type="textarea" :rows="2" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="影像文件" required>
              <el-upload
                drag
                multiple
                :auto-upload="false"
                :limit="50"
                :on-change="handleChange"
                :on-remove="handleRemove"
                :before-upload="beforeUpload"
                accept=".jpg,.jpeg,.png,.tif,.tiff,.bmp,.img,.dem"
              >
                <el-icon class="el-icon--upload" style="font-size: 48px; color: #409EFF;">
                  <upload-filled />
                </el-icon>
                <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 JPG / PNG / TIF / TIFF / BMP / IMG / DEM 格式，单文件最大 500MB，支持批量上传（最多50个）
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item v-if="uploadList.length" label="上传列表">
              <div class="upload-list">
                <div v-for="(u, i) in uploadList" :key="u.uid" class="upload-item">
                  <el-icon><Document /></el-icon>
                  <span class="name text-ellipsis" :title="u.name">{{ u.name }}</span>
                  <span class="size">{{ formatSize(u.size) }}</span>
                  <el-progress v-if="u.status === 'uploading'" :percentage="u.percent || 0" :stroke-width="6" style="width: 140px;" />
                  <el-tag v-if="u.status === 'success'" type="success" size="small">成功</el-tag>
                  <el-tag v-if="u.status === 'error'" type="danger" size="small">失败</el-tag>
                  <el-tag v-if="u.status === 'pending'" type="info" size="small">等待中</el-tag>
                  <el-button link type="danger" @click="removeFile(i)" v-if="u.status !== 'uploading'">移除</el-button>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="uploading" @click="startUpload" :disabled="!uploadList.length">
                <el-icon><UploadFilled /></el-icon>开始上传
              </el-button>
              <el-button size="large" @click="resetForm">重置</el-button>
              <el-button size="large" type="success" @click="goList">
                <el-icon><PictureFilled /></el-icon>前往影像库
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="hover" class="card-container mb-15">
          <template #header><div class="page-title">📋 上传说明</div></template>
          <el-timeline>
            <el-timeline-item timestamp="步骤1" placement="top" type="primary">
              填写影像元信息：类型、时间、位置等
            </el-timeline-item>
            <el-timeline-item timestamp="步骤2" placement="top" type="success">
              选择要上传的无人机影像文件，支持批量
            </el-timeline-item>
            <el-timeline-item timestamp="步骤3" placement="top" type="warning">
              点击"开始上传"，文件将上传至MinIO对象存储
            </el-timeline-item>
            <el-timeline-item timestamp="步骤4" placement="top">
              系统自动执行：缩略图生成 → GDAL几何校正 → 辐射校正 → 正射校正 → DOM生成
            </el-timeline-item>
            <el-timeline-item timestamp="步骤5" placement="top" type="danger">
              预处理完成后，可在"影像库"查看并用于AI智能定损
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="hover" class="card-container">
          <template #header><div class="page-title">🗺️ 地理位置预览</div></template>
          <div ref="mapRef" class="map-preview" style="height: 300px;"></div>
          <div class="mt-10 text-right">
            <el-button size="small" @click="pickLocation">从地图选点</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { UploadFilled, PictureFilled, Document } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/image'
import { listMission } from '@/api/assess'

const router = useRouter()
const mapRef = ref(null)
const uploading = ref(false)
const missions = ref([])

const form = reactive({
  imageType: 'AFTER',
  disasterType: '',
  missionId: null,
  missionName: '',
  shootTime: new Date(),
  location: '',
  centerLon: 116.4074,
  centerLat: 39.9042,
  remark: ''
})

const uploadList = ref([])

function beforeUpload(file) {
  const okSize = file.size / 1024 / 1024 < 500
  if (!okSize) { ElMessage.warning('文件大小不能超过 500MB'); return false }
  return true
}

function handleChange(file) {
  if (file && !uploadList.value.find(u => u.uid === file.uid)) {
    uploadList.value.push({ ...file.raw, uid: file.uid, name: file.name, size: file.size,
      status: 'pending', percent: 0, raw: file.raw })
  }
}

function handleRemove(file) {
  const i = uploadList.value.findIndex(u => u.uid === file.uid)
  if (i >= 0) uploadList.value.splice(i, 1)
}

function removeFile(i) {
  uploadList.value.splice(i, 1)
}

async function startUpload() {
  if (!uploadList.value.length) return
  uploading.value = true
  let success = 0, fail = 0

  for (let i = 0; i < uploadList.value.length; i++) {
    const item = uploadList.value[i]
    item.status = 'uploading'
    try {
      await uploadImage({
        ...form,
        missionName: (missions.value.find(m => m.id === form.missionId)?.missionName) || '',
        file: item.raw,
        shootTime: form.shootTime ? new Date(form.shootTime).toISOString() : null
      }, (evt) => {
        item.percent = Math.floor((evt.loaded / evt.total) * 100)
      })
      item.status = 'success'
      item.percent = 100
      success++
    } catch (e) {
      item.status = 'error'
      fail++
    }
  }

  uploading.value = false
  if (success) ElNotification.success({ title: '上传完成', message: `成功 ${success} 个，失败 ${fail} 个` })
}

function resetForm() {
  uploadList.value = []
}

function goList() {
  router.push('/image/list')
}

function formatSize(s) {
  if (!s) return '0B'
  if (s < 1024) return s + 'B'
  if (s < 1024 * 1024) return (s / 1024).toFixed(1) + 'KB'
  return (s / 1024 / 1024).toFixed(2) + 'MB'
}

async function loadMissions() {
  try {
    const d = await listMission({ pageNum: 1, pageSize: 50 })
    missions.value = d.list || []
  } catch (e) {
    missions.value = Array.from({ length: 5 }, (_, i) => ({
      id: i + 1, missionName: `定损任务示例-${i + 1}`
    }))
  }
}

function pickLocation() {
  ElMessage.info('请在地图上点击选择位置')
}

function initMap() {
  nextTick(() => {
    try {
      const map = new window.TMap.Map(mapRef.value, {
        center: new window.TMap.LatLng(form.centerLat, form.centerLon),
        zoom: 13, pitch: 0
      })
      let marker = new window.TMap.MultiMarker({
        map,
        geometries: [{ id: '1', position: new window.TMap.LatLng(form.centerLat, form.centerLon) }]
      })
      map.on('click', (evt) => {
        const lat = evt.latLng.getLat().toFixed(6)
        const lng = evt.latLng.getLng().toFixed(6)
        form.centerLat = parseFloat(lat)
        form.centerLon = parseFloat(lng)
        marker.setGeometries([{ id: '1', position: new window.TMap.LatLng(lat, lng) }])
      })
    } catch (e) { console.warn('地图初始化失败', e) }
  })
}

onMounted(() => {
  loadMissions()
  initMap()
})
</script>

<style lang="scss" scoped>
.upload-page {
  .upload-list { max-height: 260px; overflow-y: auto; border: 1px solid #ebeef5; border-radius: 4px; }
  .upload-item {
    display: flex; align-items: center; gap: 10px;
    padding: 8px 12px; border-bottom: 1px solid #f5f7fa;
    &:last-child { border-bottom: none; }
    .name { flex: 1; min-width: 0; font-size: 13px; color: #303133; }
    .size { color: #909399; font-size: 12px; min-width: 70px; }
  }
  .mb-15 { margin-bottom: 15px; }
  .mt-10 { margin-top: 10px; }
}
</style>
