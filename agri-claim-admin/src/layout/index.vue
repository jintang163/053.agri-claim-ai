<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <img src="/favicon.svg" alt="logo" class="logo-img" />
        <span v-if="!isCollapse" class="logo-text">农业保险定损</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        background-color="#001529"
        text-color="#b8c7ce"
        active-text-color="#409EFF"
        class="el-menu-vertical"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-sub-menu index="image">
          <template #title>
            <el-icon><PictureFilled /></el-icon>
            <span>影像管理</span>
          </template>
          <el-menu-item index="/image/upload">影像上传</el-menu-item>
          <el-menu-item index="/image/list">影像库</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="assess">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>定损管理</span>
          </template>
          <el-menu-item index="/assess/list">定损任务</el-menu-item>
          <el-menu-item index="/assess/create">新建定损</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/monitor">
          <el-icon><DataBoard /></el-icon>
          <template #title>大屏监控</template>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/dict">字典管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" size="20" @click="toggleCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta?.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="全屏">
            <el-icon size="18" class="icon-btn" @click="fullscreen"><FullScreen /></el-icon>
          </el-tooltip>
          <el-tooltip content="大屏">
            <el-icon size="18" class="icon-btn" @click="$router.push('/monitor')"><DataBoard /></el-icon>
          </el-tooltip>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatar">
                {{ userStore.nickName?.charAt(0) || 'A' }}
              </el-avatar>
              <span class="user-name">{{ userStore.nickName || userStore.userName }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="monitor">大屏监控</el-dropdown-item>
                <el-dropdown-item divided command="logout" @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-main>

      <el-footer class="footer">
        © 2024 Agri-Claim AI · 农业保险快速定损系统 v1.0.0
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}
function fullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}
function handleCommand(c) {
  if (c === 'logout') handleLogout()
  if (c === 'monitor') router.push('/monitor')
}
async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await userStore.Logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.layout-container { height: 100vh; }
.aside {
  background: #001529;
  transition: width 0.3s;
  overflow: hidden;
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #fff;
    background: #002140;
    .logo-img { width: 32px; height: 32px; }
    .logo-text { font-size: 16px; font-weight: 600; white-space: nowrap; }
  }
  :deep(.el-menu-vertical) {
    border-right: none;
  }
}
.header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 16px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-right { display: flex; align-items: center; gap: 16px; }
  .collapse-btn { cursor: pointer; padding: 6px; border-radius: 4px; &:hover { background: #f0f0f0; } }
  .icon-btn { cursor: pointer; padding: 6px; border-radius: 4px; color: #606266;
    &:hover { color: #409EFF; background: #ecf5ff; } }
  .user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 0 8px;
    border-radius: 4px; &:hover { background: #f5f7fa; } }
  .user-name { font-size: 14px; color: #303133; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.main { padding: 16px; background: #f0f2f5; overflow: auto; }
.footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}
.fade-transform-enter-active, .fade-transform-leave-active { transition: all 0.3s; }
.fade-transform-enter-from { opacity: 0; transform: translateX(-10px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(10px); }
</style>
