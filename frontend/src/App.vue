<template>
  <el-container class="app-container">
    <!-- 侧边栏 -->
    <el-aside width="64px" class="app-aside">
      <div class="logo">
        <span class="logo-text">K</span>
      </div>
      
      <el-menu
        :default-active="currentRoute"
        router
        class="app-menu"
      >
        <el-tooltip 
          v-for="route in menuRoutes" 
          :key="route.path" 
          :content="route.meta.title" 
          placement="right"
        >
          <el-menu-item :index="route.path">
            <el-icon size="22"><component :is="route.meta.icon" /></el-icon>
          </el-menu-item>
        </el-tooltip>
      </el-menu>
      
      <div class="aside-footer">
        <el-tooltip content="系统设置" placement="right">
          <el-icon 
            size="20" 
            class="setting-icon" 
            @click="$router.push('/settings')"
            :class="{ active: currentRoute === '/settings' }"
          >
            <Setting />
          </el-icon>
        </el-tooltip>
      </div>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" ref="currentView" />
        </transition>
      </router-view>
    </el-main>
    
    <!-- 智能助手滑出面板 -->
    <ChatPanel />
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting, Monitor, Folder, Finished, Document } from '@element-plus/icons-vue'
import ChatPanel from '@/components/ChatPanel.vue'

const route = useRoute()
const router = useRouter()

const currentRoute = computed(() => route.path)

// 模块配置
const moduleSettings = ref({
  cases: true,
  todo: true,
  logs: false,
  monitor: false
})

// 路由与模块的映射关系
const routeModuleMap = {
  '/cases': 'cases',
  '/todo': 'todo',
  '/logs': 'logs',
  '/monitor': 'monitor'
}

// 加载模块配置
const loadModuleSettings = () => {
  const saved = localStorage.getItem('keke_settings')
  if (saved) {
    try {
      const settings = JSON.parse(saved)
      if (settings.modules) {
        moduleSettings.value = { ...moduleSettings.value, ...settings.modules }
      }
    } catch (e) {
      console.error('加载模块配置失败', e)
    }
  }
}

// 监听设置变化事件
const handleSettingsChanged = (event) => {
  if (event.detail?.modules) {
    moduleSettings.value = { ...moduleSettings.value, ...event.detail.modules }
  }
}

// 过滤后的菜单路由
const menuRoutes = computed(() => {
  return router.options.routes.filter(r => {
    if (!r.meta?.title || r.path === '/settings') return false
    
    // 检查模块是否启用
    const moduleName = routeModuleMap[r.path]
    if (moduleName && !moduleSettings.value[moduleName]) {
      return false
    }
    return true
  })
})

onMounted(() => {
  loadModuleSettings()
  window.addEventListener('keke-settings-changed', handleSettingsChanged)
})

onUnmounted(() => {
  window.removeEventListener('keke-settings-changed', handleSettingsChanged)
})
</script>

<style scoped>
.app-container {
  height: 100vh;
  background: #f0f2f5;
}

.app-aside {
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.logo {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.logo-text {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.app-menu {
  flex: 1;
  background: transparent;
  border: none;
  width: 100%;
}

.app-menu .el-menu-item {
  height: 48px;
  display: flex;
  justify-content: center;
  margin: 4px 8px;
  border-radius: 10px;
  color: #6c7293;
}

.app-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.app-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.aside-footer {
  padding: 16px 0;
}

.setting-icon {
  color: #6c7293;
  cursor: pointer;
  transition: color 0.2s;
}

.setting-icon:hover,
.setting-icon.active {
  color: #fff;
}

.app-main {
  padding: 0;
  overflow-y: auto;
  height: 100vh;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
