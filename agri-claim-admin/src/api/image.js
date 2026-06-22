import request from '@/utils/request'

const CHUNK_SIZE = 5 * 1024 * 1024

export function uploadImage(data, onProgress) {
  const formData = new FormData()
  Object.keys(data).forEach(key => {
    if (data[key] !== undefined && data[key] !== null) {
      if (key === 'file' && Array.isArray(data[key])) {
        data[key].forEach(f => formData.append('files', f))
      } else {
        formData.append(key, data[key])
      }
    }
  })
  return request({
    url: data.file && Array.isArray(data.file) ? '/image/batchUpload' : '/image/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function initChunkUpload(fileName) {
  return request({ url: '/image/chunk/init', method: 'post', params: { fileName } })
}

export function checkUploadedChunks(uploadId) {
  return request({ url: '/image/chunk/check', method: 'get', params: { uploadId } })
}

export function uploadChunk(formData, onProgress) {
  return request({
    url: '/image/chunk/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function mergeChunks(data) {
  return request({ url: '/image/chunk/merge', method: 'post', data })
}

export async function chunkUploadFile(file, extraData, onProgress) {
  if (file.size <= CHUNK_SIZE) {
    const formData = new FormData()
    formData.append('file', file)
    Object.keys(extraData).forEach(k => {
      if (extraData[k] !== undefined && extraData[k] !== null) formData.append(k, extraData[k])
    })
    return uploadImage({ ...extraData, file }, onProgress)
  }

  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  const initRes = await initChunkUpload(file.name)
  const uploadId = initRes.uploadId

  const uploadedChunks = await checkUploadedChunks(uploadId)
  const uploadedSet = new Set(uploadedChunks || [])

  let uploadedBytes = 0
  for (let i = 0; i < totalChunks; i++) {
    if (uploadedSet.has(i)) {
      uploadedBytes += Math.min(CHUNK_SIZE, file.size - i * CHUNK_SIZE)
      continue
    }

    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunkBlob = file.slice(start, end)

    const formData = new FormData()
    formData.append('file', chunkBlob)
    formData.append('uploadId', uploadId)
    formData.append('fileName', file.name)
    formData.append('chunkIndex', i)
    formData.append('totalChunks', totalChunks)
    formData.append('chunkSize', chunkBlob.size)
    formData.append('totalSize', file.size)
    if (extraData.imageType) formData.append('imageType', extraData.imageType)
    if (extraData.disasterType) formData.append('disasterType', extraData.disasterType)
    if (extraData.missionId) formData.append('missionId', extraData.missionId)
    if (extraData.missionName) formData.append('missionName', extraData.missionName)
    if (extraData.location) formData.append('location', extraData.location)
    if (extraData.centerLon) formData.append('centerLon', extraData.centerLon)
    if (extraData.centerLat) formData.append('centerLat', extraData.centerLat)

    await uploadChunk(formData, (evt) => {
      if (onProgress && evt.total) {
        const chunkProgress = (evt.loaded / evt.total) * (end - start)
        onProgress({ loaded: uploadedBytes + chunkProgress, total: file.size })
      }
    })
    uploadedBytes = end
  }

  return mergeChunks({
    uploadId,
    fileName: file.name,
    totalChunks,
    totalSize: file.size,
    ...extraData
  })
}

export function listImage(params) {
  return request({ url: '/image/list', method: 'get', params })
}

export function getImageDetail(id) {
  return request({ url: '/image/' + id, method: 'get' })
}

export function preprocessImage(id) {
  return request({ url: '/image/preprocess/' + id, method: 'post' })
}

export function deleteImage(ids) {
  return request({ url: '/image/' + ids, method: 'delete' })
}

export function getImagePreview(id) {
  return request({ url: '/image/preview/' + id, method: 'get' })
}

export function calcNdvi(params) {
  return request({ url: '/image/ndvi', method: 'get', params })
}

export function getImagesByMission(params) {
  return request({ url: '/image/mission/' + params.missionId, method: 'get', params })
}
