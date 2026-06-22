import request from '@/utils/request'

export function runSegment(data) {
  return request({ url: '/ai/segment', method: 'post', data })
}

export function runDetect(data) {
  return request({ url: '/ai/detect', method: 'post', data })
}

export function fullProcess(data) {
  return request({ url: '/ai/process', method: 'post', data })
}

export function getSegments(taskId) {
  return request({ url: '/ai/segment/task/' + taskId, method: 'get' })
}

export function getFarmlands(taskId) {
  return request({ url: '/ai/segment/farmland/' + taskId, method: 'get' })
}

export function getDetect(taskId) {
  return request({ url: '/ai/detect/task/' + taskId, method: 'get' })
}

export function getAiSummary(taskId) {
  return request({ url: '/ai/summary/' + taskId, method: 'get' })
}

export function listSegments(params) {
  return request({ url: '/ai/segment/list', method: 'get', params })
}

export function listDetects(params) {
  return request({ url: '/ai/detect/list', method: 'get', params })
}

export function getClassStats(taskId) {
  return request({ url: '/ai/stats/class/' + taskId, method: 'get' })
}
