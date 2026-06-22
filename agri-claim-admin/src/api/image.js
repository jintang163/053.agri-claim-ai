// 影像相关API
import request from '@/utils/request'

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
