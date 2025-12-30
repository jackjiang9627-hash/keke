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
  }
}
