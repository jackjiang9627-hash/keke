import request from './request'

export default {
  list(params) {
    return request.get('/api/cases', { params })
  },
  
  get(id) {
    return request.get(`/api/cases/${id}`)
  },
  
  create(data) {
    return request.post('/api/cases', data)
  },
  
  update(id, data) {
    return request.put(`/api/cases/${id}`, data)
  },
  
  delete(id) {
    return request.delete(`/api/cases/${id}`)
  },
  
  search(keyword) {
    return request.get('/api/cases/search', { params: { query: keyword } })
  },
  
  getModules() {
    return request.get('/api/cases/modules')
  },
  
  import(formData) {
    return request.post('/api/cases/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  export() {
    return request.get('/api/cases/export', {
      responseType: 'arraybuffer'
    })
  },
  
  // === 复习相关API ===
  
  // 获取今日待复习案例
  getTodayReviews() {
    return request.get('/api/cases/today-reviews')
  },
  
  // 标记案例已复习
  markReviewed(id, mastered) {
    return request.post(`/api/cases/${id}/review`, { mastered })
  },
  
  // 延后复习到明天
  postponeReview(id) {
    return request.post(`/api/cases/${id}/postpone`)
  },
  
  // 切换复习启用状态
  toggleReview(id, enabled) {
    return request.post(`/api/cases/${id}/toggle-review`, { enabled })
  }
}
