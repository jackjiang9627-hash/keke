import request from './request'

// 案例API
export const caseApi = {
  // 获取案例列表（分页）
  getList(params) {
    return request.get('/cases', { params })
  },
  
  // 按模块获取案例
  getByModule(moduleName, params) {
    return request.get(`/cases/module/${encodeURIComponent(moduleName)}`, { params })
  },
  
  // 搜索案例
  search(query) {
    return request.get('/cases/search', { params: { query } })
  },
  
  // 获取案例详情
  getById(id) {
    return request.get(`/cases/${id}`)
  },
  
  // 创建案例
  create(data) {
    return request.post('/cases', data)
  },
  
  // 更新案例
  update(id, data) {
    return request.put(`/cases/${id}`, data)
  },
  
  // 删除案例
  delete(id) {
    return request.delete(`/cases/${id}`)
  },
  
  // 获取所有模块
  getModules() {
    return request.get('/cases/modules')
  },
  
  // 导入案例
  import(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/cases/import', formData)
  },
  
  // 导出案例URL
  getExportUrl() {
    return '/api/cases/export'
  }
}

// 待办API
export const todoApi = {
  // 获取今日待办
  getToday() {
    return request.get('/todos/today')
  },
  
  // 获取所有待办
  getAll() {
    return request.get('/todos')
  },
  
  // 创建待办
  create(data) {
    return request.post('/todos', data)
  },
  
  // 更新待办
  update(id, data) {
    return request.put(`/todos/${id}`, data)
  },
  
  // 切换完成状态
  toggle(id) {
    return request.patch(`/todos/${id}/toggle`)
  },
  
  // 删除待办
  delete(id) {
    return request.delete(`/todos/${id}`)
  }
}
