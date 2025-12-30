import request from './request'

export default {
  list(params) {
    return request.get('/api/v1/logs', { params })
  },
  
  upload(formData) {
    return request.post('/api/v1/logs/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  statistics() {
    return request.get('/api/v1/logs/statistics')
  },
  
  delete(id) {
    return request.delete(`/api/v1/logs/${id}`)
  },
  
  search(keyword) {
    return request.get('/api/v1/logs/search', { params: { keyword } })
  }
}
