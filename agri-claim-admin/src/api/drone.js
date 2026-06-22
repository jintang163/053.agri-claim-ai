import request from '@/utils/request'

export function planRoute(data) {
  return request({ url: '/ai/route/plan', method: 'post', data })
}

export function calculateGsd(data) {
  return request({ url: '/ai/route/calculate-gsd', method: 'post', data })
}

export function avoidObstacles(data) {
  return request({ url: '/ai/route/avoid-obstacles', method: 'post', data })
}

export function listTemplate(params) {
  return request({ url: '/ai/drone/template/list', method: 'get', params })
}

export function pageTemplate(params) {
  return request({ url: '/ai/drone/template/page', method: 'get', params })
}

export function getTemplate(id) {
  return request({ url: '/ai/drone/template/' + id, method: 'get' })
}

export function saveTemplate(data) {
  return request({ url: '/ai/drone/template', method: 'post', data })
}

export function updateTemplate(id, data) {
  return request({ url: '/ai/drone/template/' + id, method: 'put', data })
}

export function deleteTemplate(id) {
  return request({ url: '/ai/drone/template/' + id, method: 'delete' })
}

export function createTask(data) {
  return request({ url: '/ai/drone/task/create', method: 'post', data })
}

export function getTask(id) {
  return request({ url: '/ai/drone/task/' + id, method: 'get' })
}

export function pageTask(params) {
  return request({ url: '/ai/drone/task/page', method: 'get', params })
}

export function startTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/start', method: 'post' })
}

export function pauseTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/pause', method: 'post' })
}

export function resumeTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/resume', method: 'post' })
}

export function returnTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/return', method: 'post' })
}

export function landTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/land', method: 'post' })
}

export function cancelTask(id) {
  return request({ url: '/ai/drone/task/' + id + '/cancel', method: 'post' })
}

export function completeTask(id, data) {
  return request({ url: '/ai/drone/task/' + id + '/complete', method: 'post', data })
}

export function getTaskStatusLatest(id) {
  return request({ url: '/ai/drone/task/' + id + '/status/latest', method: 'get' })
}

export function getTaskStatusHistory(id, params) {
  return request({ url: '/ai/drone/task/' + id + '/status/history', method: 'get', params })
}

export function reportTaskStatus(data) {
  return request({ url: '/ai/drone/task/status/report', method: 'post', data })
}
