import request from './request'

export default {
  // 发送消息
  send(message, backend = null, model = null) {
    const data = { message }
    if (backend) data.backend = backend
    if (model) data.model = model
    return request.post('/api/chat', data)
  },
  
  // 获取可用后端列表
  getBackends() {
    return request.get('/api/chat/backends')
  },
  
  // 获取可用模型列表
  getModels(backend) {
    return request.get('/api/chat/models', { params: { backend } })
  },
  
  // 获取服务状态
  getStatus() {
    return request.get('/api/chat/status')
  }
}
