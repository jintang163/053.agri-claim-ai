import request from '@/utils/request'

export function createMission(data) {
  return request({ url: '/assess/mission', method: 'post', data })
}

export function getMission(id) {
  return request({ url: '/assess/mission/' + id, method: 'get' })
}

export function listMission(params) {
  return request({ url: '/assess/mission/list', method: 'get', params })
}

export function recalcMission(id, globalAdjust, detailAdjusts) {
  return request({
    url: '/assess/mission/' + id + '/recalc',
    method: 'post',
    params: { globalAdjust },
    data: detailAdjusts || {}
  })
}

export function auditMission(id, passed, remark) {
  return request({
    url: '/assess/mission/' + id + '/audit',
    method: 'post',
    params: { passed, remark }
  })
}

export function downloadReport(id) {
  return request({
    url: '/assess/mission/' + id + '/report',
    method: 'get',
    responseType: 'blob'
  })
}

export function getDetails(missionId) {
  return request({ url: '/assess/mission/' + missionId + '/details', method: 'get' })
}

export function adjustDetail(id, missionId, adjustCoeff) {
  return request({
    url: '/assess/detail/' + id + '/adjust',
    method: 'post',
    params: { missionId, adjustCoeff }
  })
}

export function getDashboardStats() {
  return request({ url: '/assess/dashboard/stats', method: 'get' })
}

export function getFormula() {
  return request({ url: '/assess/formula', method: 'get' })
}
