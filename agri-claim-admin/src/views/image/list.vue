<template>
  <div class="image-list">
    <el-card shadow="hover" class="card-container mb-15">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="影像类型">
          <el-select v-model="query.imageType" clearable class="w-140">
            <el-option label="受灾前" value="BEFORE" />
            <el-option label="受灾后" value="AFTER" />
            <el-option label="DOM正射" value="DOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="query.imageStatus" clearable class="w-140">
            <el-option label="已上传" value="UPLOADED" />
            <el-option label="预处理中" value="PREPROCESSING" />
            <el-option label="已完成" value="PREPROCESSED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="灾害类型">
          <el-select v-model="query.disasterType" clearable class="w-140">
            <el-option label="淹水" value="FLOOD" />
            <el-option label="倒伏" value="LODGE" />
            <el-option label="枯黄" value="WITHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="名称/位置/任务" clearable class="w-180" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
          <el-button type="success" @click="$router.push('/image/upload')"><el-icon><UploadFilled /></el-icon>上传影像</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="card-container">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="缩略图" width="110" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.thumbnailUrl || 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2UwZTBlMCIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzkwOTM5OSI+572R56uZPC90ZXh0Pjwvc3ZnPg=='"
              :preview-src-list="row.previewUrl ? [row.previewUrl] : []"
              fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="imageType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.imageType)" size="small">{{ typeText(row.imageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="imageStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.imageStatus)" size="small" effect="plain">
              {{ statusText(row.imageStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地理位置" min-width="160" show-overflow-tooltip />
        <el-table-column prop="coverageArea" label="覆盖面积(亩)" width="110" align="right">
          <template #default="{ row }">{{ row.coverageArea?.toFixed?.(2) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="qualityScore" label="质量评分" width="100" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.qualityScore || 0" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column prop="surveyorName" label="上传人" width="90" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="170">
          <template #default="{ row }">{{ row.uploadTime?.split('T')?.join(' ')?.slice(0,19) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="view(row)">查看</el-button>
            <el-button link type="success" size="small" @click="preprocess(row)" v-if="row.imageStatus !== 'PREPROCESSED'">预处理</el-button>
            <el-button link type="primary" size="small" @click="download(row)">下载</el-button>
            <el-popconfirm title="确定删除?" @confirm="remove(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-15"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="search"
        @current-change="search"
      />
    </el-card>

    <el-dialog v-model="viewVisible" title="影像详情" width="800px">
      <div v-if="current" class="detail-view">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-image :src="current.previewUrl" fit="contain" class="detail-image"
              preview-teleported :preview-src-list="current.previewUrl ? [current.previewUrl] : []" />
          </el-col>
          <el-col :span="10">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="文件名称">{{ current.originalName }}</el-descriptions-item>
              <el-descriptions-item label="影像编号">{{ current.info?.id || '-' }}</el-descriptions-item>
              <el-descriptions-item label="影像类型">{{ typeText(current.info?.imageType) }}</el-descriptions-item>
              <el-descriptions-item label="处理状态">{{ statusText(current.info?.imageStatus) }}</el-descriptions-item>
              <el-descriptions-item label="灾害类型">{{ disasterText(current.info?.disasterType) }}</el-descriptions-item>
              <el-descriptions-item label="文件大小">{{ formatSize(current.info?.fileSize) }}</el-descriptions-item>
              <el-descriptions-item label="分辨率">{{ current.info?.width }} × {{ current.info?.height }}</el-descriptions-item>
              <el-descriptions-item label="覆盖面积">{{ current.info?.coverageArea?.toFixed?.(2) || '-' }} 亩</el-descriptions-item>
              <el-descriptions-item label="地理位置">{{ current.info?.location || '-' }}</el-descriptions-item>
              <el-descriptions-item label="经纬度">
                {{ current.info?.centerLon }} , {{ current.info?.centerLat }}
              </el-descriptions-item>
              <el-descriptions-item label="质量评分">
                <el-progress :percentage="current.info?.qualityScore || 0" />
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, UploadFilled } from '@element-plus/icons-vue'
import { listImage, getImageDetail, deleteImage, preprocessImage, getImagePreview } from '@/api/image'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const viewVisible = ref(false)
const current = ref(null)

const query = reactive({
  pageNum: 1, pageSize: 10, imageType: '', imageStatus: '', disasterType: '', keyword: ''
})

const typeText = t => ({BEFORE:'灾前',AFTER:'灾后',DOM:'DOM',MASK:'掩膜'}[t]||t)
const typeTag = t => ({BEFORE:'success',AFTER:'danger',DOM:'',MASK:'warning'}[t]||'info')
const statusText = s => ({UPLOADED:'已上传',PREPROCESSING:'预处理中',PREPROCESSED:'预处理完成',FAILED:'失败'}[s]||s)
const statusTag = s => ({UPLOADED:'info',PREPROCESSING:'warning',PREPROCESSED:'success',FAILED:'danger'}[s]||'info')
const disasterText = d => ({FLOOD:'淹水',LODGE:'倒伏',WITHER:'枯黄'}[d]||d||'-')
const formatSize = s => !s ? '-' : s < 1048576 ? (s/1024).toFixed(1)+'KB' : (s/1048576).toFixed(2)+'MB'

async function search() {
  loading.value = true
  try {
    const d = await listImage({ ...query })
    list.value = d.list || []
    total.value = d.total || 0
    await loadUrls()
  } catch (e) {
    mockData()
  } finally {
    loading.value = false
  }
}

async function loadUrls() {
  for (const item of list.value) {
    try {
      item.previewUrl = await getImagePreview(item.id)
      if (!item.previewUrl) item.previewUrl = null
    } catch (e) {}
  }
}

function mockData() {
  list.value = Array.from({ length: query.pageSize }, (_, i) => {
    const idx = (query.pageNum - 1) * query.pageSize + i
    return {
      id: 1000 + idx,
      originalName: `DJI_${(1000 + idx).toString().padStart(4,'0')}.JPG`,
      imageType: ['BEFORE','AFTER','DOM'][idx % 3],
      imageStatus: ['UPLOADED','PREPROCESSING','PREPROCESSED','PREPROCESSED','FAILED'][idx % 5],
      disasterType: ['FLOOD','LODGE','WITHER',''][idx % 4] || null,
      location: '北京市顺义区XX镇XX村',
      coverageArea: 30 + Math.random() * 120,
      qualityScore: 70 + Math.floor(Math.random() * 30),
      fileSize: 8000000 + Math.floor(Math.random() * 30000000),
      width: 4000 + Math.floor(Math.random() * 2000),
      height: 3000 + Math.floor(Math.random() * 2000),
      surveyorName: ['张查勘','李查勘','王查勘'][idx % 3],
      uploadTime: new Date(Date.now() - idx * 3600000).toISOString(),
      thumbnailUrl: null, previewUrl: null
    }
  })
  total.value = 128
}

function resetQuery() {
  Object.assign(query, { pageNum:1, imageType:'', imageStatus:'', disasterType:'', keyword:'' })
  search()
}

async function view(row) {
  try {
    const d = await getImageDetail(row.id)
    current.value = d
  } catch (e) {
    current.value = { info: row, previewUrl: row.previewUrl }
  }
  viewVisible.value = true
}

async function preprocess(row) {
  try {
    await preprocessImage(row.id)
    ElMessage.success('预处理任务已提交')
    search()
  } catch (e) {}
}

function download(row) {
  window.open(`/api/image/download/${row.id}`, '_blank')
}

async function remove(row) {
  try {
    await deleteImage(row.id)
    ElMessage.success('删除成功')
    search()
  } catch (e) {}
}

onMounted(search)
</script>

<style lang="scss" scoped>
.image-list {
  .thumb { width: 88px; height: 66px; border-radius: 4px; background: #f5f5f5; cursor: zoom-in; }
  .w-140 { width: 140px; } .w-180 { width: 180px; }
  .mt-15 { margin-top: 15px; } .mb-15 { margin-bottom: 15px; }
  .detail-image { width: 100%; min-height: 360px; max-height: 500px; background: #000; border-radius: 4px; }
}
</style>
