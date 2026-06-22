import { defineStore } from 'pinia'
import { login, logout, getCaptcha } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    userName: (s) => s.userInfo?.userName || '',
    nickName: (s) => s.userInfo?.nickName || '',
    avatar: (s) => s.userInfo?.avatar || '',
    roleKey: (s) => s.userInfo?.roleKey || '',
    permissions: (s) => s.userInfo?.permissions || []
  },
  actions: {
    async Login(params) {
      const data = await login(params)
      this.token = data.token
      this.userInfo = data
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data))
      return data
    },
    async Logout() {
      try { await logout() } catch (e) {}
      this.token = ''
      this.userInfo = {}
      localStorage.clear()
    },
    async GetCaptcha() {
      return await getCaptcha()
    },
    hasPerm(perm) {
      if (!perm) return true
      if (this.roleKey === 'admin') return true
      return this.permissions?.includes(perm)
    }
  }
})
