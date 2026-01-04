import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/cases'
  },
  {
    path: '/cases',
    name: 'Cases',
    component: () => import('../views/Cases/index.vue'),
    meta: { title: '案例库', icon: 'Folder' }
  },
  {
    path: '/todo',
    name: 'Todo',
    component: () => import('../views/Todo/index.vue'),
    meta: { title: '待办事项', icon: 'Finished' }
  },
  {
    path: '/logs',
    name: 'LogAnalysis',
    component: () => import('../views/LogAnalysis/index.vue'),
    meta: { title: '日志分析', icon: 'Document' }
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('../views/Monitor/index.vue'),
    meta: { title: '系统监控', icon: 'Monitor' }
  },
  {
    path: '/ssh',
    name: 'Ssh',
    component: () => import('../views/Ssh/index.vue'),
    meta: { title: 'SSH管理', icon: 'Connection' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings/index.vue'),
    meta: { title: '系统设置', icon: 'Setting' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '首页'} - Keke`
  next()
})

export default router
