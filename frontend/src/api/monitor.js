import request from './request'

export default {
  // 获取当前系统快照
  getSnapshot() {
    return request.get('/api/monitor/snapshot')
  },
  
  // 执行单次监控检测
  detect() {
    return request.post('/api/monitor/detect')
  },
  
  // 获取快照历史
  getHistory(limit = 20) {
    return request.get('/api/monitor/history', { params: { limit } })
  },
  
  // 创建监控任务
  createTask(data) {
    return request.post('/api/monitor/tasks', data)
  },
  
  // 获取所有任务
  getTasks() {
    return request.get('/api/monitor/tasks')
  },
  
  // 获取任务详情
  getTask(taskId) {
    return request.get(`/api/monitor/tasks/${taskId}`)
  },
  
  // 启动任务
  startTask(taskId) {
    return request.post(`/api/monitor/tasks/${taskId}/start`)
  },
  
  // 暂停任务
  pauseTask(taskId) {
    return request.post(`/api/monitor/tasks/${taskId}/pause`)
  },
  
  // 恢复任务
  resumeTask(taskId) {
    return request.post(`/api/monitor/tasks/${taskId}/resume`)
  },
  
  // 停止任务
  stopTask(taskId) {
    return request.post(`/api/monitor/tasks/${taskId}/stop`)
  },
  
  // 删除任务
  deleteTask(taskId) {
    return request.delete(`/api/monitor/tasks/${taskId}`)
  },
  
  // 导出当前监控报告
  exportReport() {
    return request.get('/api/monitor/export', { responseType: 'blob' })
  },
  
  // 导出历史报告
  exportHistoryReport(limit = 50) {
    return request.get('/api/monitor/export/history', { 
      params: { limit },
      responseType: 'blob' 
    })
  }
}
