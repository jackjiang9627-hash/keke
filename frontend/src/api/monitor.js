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
  
  // 批量启动任务
  startTasks(taskIds) {
    return request.post('/api/monitor/tasks/start', taskIds)
  },
  
  // 批量暂停任务
  pauseTasks(taskIds) {
    return request.post('/api/monitor/tasks/pause', taskIds)
  },
  
  // 批量恢复任务
  resumeTasks(taskIds) {
    return request.post('/api/monitor/tasks/resume', taskIds)
  },
  
  // 批量停止任务
  stopTasks(taskIds) {
    return request.post('/api/monitor/tasks/stop', taskIds)
  },
  
  // 批量删除任务
  deleteTasks(taskIds) {
    return request.post('/api/monitor/tasks/delete', taskIds)
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
  },
  
  // 批量导出任务报告
  exportTaskReports(taskIds) {
    return request.post('/api/monitor/export/tasks', taskIds, {
      responseType: 'blob'
    })
  }
}
