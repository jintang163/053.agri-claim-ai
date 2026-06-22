import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('@/views/monitor/index.vue'),
    meta: { title: '大屏监控', noAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer' }
      },
      {
        path: 'image/upload',
        name: 'ImageUpload',
        component: () => import('@/views/image/upload.vue'),
        meta: { title: '影像上传', icon: 'UploadFilled' }
      },
      {
        path: 'image/list',
        name: 'ImageList',
        component: () => import('@/views/image/list.vue'),
        meta: { title: '影像库', icon: 'PictureFilled' }
      },
      {
        path: 'assess/list',
        name: 'AssessList',
        component: () => import('@/views/assess/list.vue'),
        meta: { title: '定损任务', icon: 'List' }
      },
      {
        path: 'assess/create',
        name: 'AssessCreate',
        component: () => import('@/views/assess/create.vue'),
        meta: { title: '新建定损', icon: 'DocumentAdd' }
      },
      {
        path: 'assess/detail/:id',
        name: 'AssessDetail',
        component: () => import('@/views/assess/detail.vue'),
        meta: { title: '定损详情', icon: 'Document' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', icon: 'UserFilled' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict.vue'),
        meta: { title: '字典管理', icon: 'Reading' }
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/error/404.vue'),
        meta: { title: '404', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  document.title = to.meta?.title
    ? `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
    : import.meta.env.VITE_APP_TITLE

  if (to.meta.noAuth) {
    next()
  } else {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
