import request from './request'

// 设备管理
export const deviceApi = {
  // 获取所有设备
  getAll: (groupName) => {
    const params = groupName ? { groupName } : {}
    return request.get('/api/ssh/devices', { params })
  },
  
  // 获取设备详情
  getById: (id) => request.get(`/api/ssh/devices/${id}`),
  
  // 创建设备
  create: (data) => request.post('/api/ssh/devices', data),
  
  // 批量创建设备
  batchCreate: (devices) => request.post('/api/ssh/devices/batch', devices),
  
  // 更新设备
  update: (id, data) => request.put(`/api/ssh/devices/${id}`, data),
  
  // 删除设备
  delete: (id) => request.delete(`/api/ssh/devices/${id}`),
  
  // 获取所有分组
  getGroups: () => request.get('/api/ssh/devices/groups'),
  
  // 测试连接
  testConnection: (id) => request.post(`/api/ssh/devices/${id}/test`),
  
  // 批量测试连接
  batchTestConnection: (deviceIds) => request.post('/api/ssh/devices/batch-test', { deviceIds }),
  
  // 导出设备（不含密码）
  exportDevices: () => request.get('/api/ssh/devices/export', { responseType: 'blob' })
}

// SSH任务
export const sshTaskApi = {
  // 执行命令
  executeCommand: (data) => request.post('/api/ssh/tasks/command', data),
  
  // 上传文件
  uploadFile: (data) => request.post('/api/ssh/tasks/upload', data),
  
  // 下载文件
  downloadFile: (data) => request.post('/api/ssh/tasks/download', data),
  
  // 获取任务详情
  getById: (id) => request.get(`/api/ssh/tasks/${id}`),
  
  // 获取最近任务列表
  getRecent: (limit = 50) => request.get('/api/ssh/tasks', { params: { limit } }),
  
  // 删除任务
  delete: (id) => request.delete(`/api/ssh/tasks/${id}`)
}

// 文件传输（支持进度和断点续传）
export const transferApi = {
  // 上传文件（使用FormData）
  upload: (file, remotePath, deviceIds) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('remotePath', remotePath)
    deviceIds.forEach(id => formData.append('deviceIds', id))
    return request.post('/api/ssh/transfer/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 下载文件
  download: (data) => request.post('/api/ssh/transfer/download', data),
  
  // 获取单个传输进度
  getProgress: (transferId) => request.get(`/api/ssh/transfer/progress/${transferId}`),
  
  // 批量获取传输进度
  getBatchProgress: (transferIds) => request.post('/api/ssh/transfer/progress/batch', transferIds),
  
  // 获取所有活跃传输
  getActive: () => request.get('/api/ssh/transfer/active'),
  
  // 取消/暂停传输
  cancel: (transferId) => request.post(`/api/ssh/transfer/cancel/${transferId}`),
  
  // 恢复下载（断点续传）
  resume: (data) => request.post('/api/ssh/transfer/resume', data)
}
