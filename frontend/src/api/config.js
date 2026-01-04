import request from './request'

export default {
  // 获取 LLM 配置
  getLlmConfigs() {
    return request.get('/api/config/llm')
  },
  
  // 保存 LLM 配置
  saveLlmConfigs(configs) {
    return request.post('/api/config/llm', configs)
  },
  
  // 获取单个配置
  getConfig(key) {
    return request.get(`/api/config/${key.replace(/\./g, '_')}`)
  },
  
  // 保存单个配置
  saveConfig(key, value, description = '') {
    return request.put(`/api/config/${key.replace(/\./g, '_')}`, { value, description })
  }
}
