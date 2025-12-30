import request from './request'

export default {
  send(message) {
    return request.post('/api/chat', { message })
  }
}
