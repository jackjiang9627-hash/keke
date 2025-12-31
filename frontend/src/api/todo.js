import request from './request'

export default {
  list() {
    return request.get('/api/todos')
  },
  
  get(id) {
    return request.get(`/api/todos/${id}`)
  },
  
  create(data) {
    return request.post('/api/todos', data)
  },
  
  update(id, data) {
    return request.put(`/api/todos/${id}`, data)
  },
  
  delete(id) {
    return request.delete(`/api/todos/${id}`)
  }
}
