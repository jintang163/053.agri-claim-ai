import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import { getToken, clearAuth, refreshToken as refreshTokenApi } from './auth'

let isRefreshing = false
let refreshSubscribers = []

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb)
}

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

const service = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    if (config.download) {
      config.responseType = 'blob'
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    if (response.config.download) {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401 || res.code === 1002 || res.code === 1003 || res.code === 1004) {
      const config = response.config
      if (!config._retry) {
        if (!isRefreshing) {
          isRefreshing = true
          config._retry = true
          return refreshTokenApi()
            .then(newToken => {
              onRefreshed(newToken.token)
              config.headers['Authorization'] = 'Bearer ' + newToken.token
              return service(config)
            })
            .catch(() => {
              handleAuthExpire()
              return Promise.reject(new Error('刷新令牌失败'))
            })
            .finally(() => {
              isRefreshing = false
            })
        } else {
          return new Promise(resolve => {
            subscribeTokenRefresh((token) => {
              config.headers['Authorization'] = 'Bearer ' + token
              resolve(service(config))
            })
          })
        }
      } else {
        handleAuthExpire()
        return Promise.reject(new Error(res.msg || '登录已过期'))
      }
    } else {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  (error) => {
    console.error('请求错误：', error)
    if (error.response?.status === 401) {
      handleAuthExpire()
    } else {
      const msg = error?.response?.data?.msg || error.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function handleAuthExpire() {
  ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    clearAuth()
    router.push('/login')
  }).catch(() => {})
}

service.download = (url, params, filename) => {
  return service.get(url, { params, download: true }).then(res => {
    const blob = new Blob([res.data])
    const disposition = res.headers['content-disposition']
    let finalName = filename
    if (disposition && !finalName) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match) finalName = decodeURIComponent(match[1].replace(/['"]/g, ''))
    }
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = finalName || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
  })
}

export default service
