import request from './request'

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'
const PERMISSIONS_KEY = 'permissions'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setUserInfo(info) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
  if (info?.userId) localStorage.setItem('userId', String(info.userId))
  if (info?.userName) localStorage.setItem('userName', info.userName)
  if (info?.roleKey) localStorage.setItem('roleKey', info.roleKey)
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
  localStorage.removeItem('userId')
  localStorage.removeItem('userName')
  localStorage.removeItem('roleKey')
  localStorage.removeItem(PERMISSIONS_KEY)
}

export function getPermissions() {
  try {
    const raw = localStorage.getItem(PERMISSIONS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

export function setPermissions(list) {
  localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(list || []))
}

export function hasPermission(perm) {
  const perms = getPermissions()
  return perms.includes('*') || perms.includes(perm)
}

export function hasAnyPermission(perms) {
  return perms.some(p => hasPermission(p))
}

export function isAdmin() {
  return localStorage.getItem('roleKey') === 'admin'
}

export function isSurveyor() {
  return localStorage.getItem('roleKey') === 'surveyor'
}

export function isManager() {
  return localStorage.getItem('roleKey') === 'manager'
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function logout() {
  return request.delete('/auth/logout')
}

export function refreshToken() {
  return request.post('/auth/refresh')
}

export function getCurrentUserInfo() {
  return request.get('/auth/info')
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
