<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>
    
    <div class="page-content">
      <div class="settings-section">
        <h3>模块显示</h3>
        <div class="settings-card">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">案例库</span>
              <span class="setting-desc">管理和搜索工作案例</span>
            </div>
            <el-switch v-model="settings.modules.cases" disabled />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">待办事项</span>
              <span class="setting-desc">任务和待办管理</span>
            </div>
            <el-switch v-model="settings.modules.todo" disabled />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">日志分析</span>
              <span class="setting-desc">日志文件解析与分析功能</span>
            </div>
            <el-switch v-model="settings.modules.logs" @change="onModuleChange" />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">系统监控</span>
              <span class="setting-desc">CPU、内存、磁盘、网络实时监控</span>
            </div>
            <el-switch v-model="settings.modules.monitor" @change="onModuleChange" />
          </div>
        </div>
      </div>
      
      <div class="settings-section">
        <h3>基础配置</h3>
        <div class="settings-card">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">最大并发数</span>
              <span class="setting-desc">系统同时处理的最大请求数量</span>
            </div>
            <el-input-number 
              v-model="settings.maxConcurrency" 
              :min="1" 
              :max="100"
              size="default"
            />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">Python最大进程数</span>
              <span class="setting-desc">Python任务执行的最大进程池大小</span>
            </div>
            <el-input-number 
              v-model="settings.pythonMaxProcesses" 
              :min="1" 
              :max="16"
              size="default"
            />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">任务超时时间（秒）</span>
              <span class="setting-desc">单个任务的最大执行时间</span>
            </div>
            <el-input-number 
              v-model="settings.taskTimeout" 
              :min="10" 
              :max="300"
              :step="10"
              size="default"
            />
          </div>
        </div>
      </div>
      
      <div class="settings-section">
        <h3>日志分析配置</h3>
        <div class="settings-card">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">单文件最大行数</span>
              <span class="setting-desc">日志文件解析的最大行数限制</span>
            </div>
            <el-input-number 
              v-model="settings.maxLogLines" 
              :min="1000" 
              :max="100000"
              :step="1000"
              size="default"
            />
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">自动清理周期（天）</span>
              <span class="setting-desc">超过此天数的日志记录将被自动清理</span>
            </div>
            <el-input-number 
              v-model="settings.logRetentionDays" 
              :min="7" 
              :max="365"
              size="default"
            />
          </div>
        </div>
      </div>
      
      <div class="settings-section">
        <h3>智能助手配置</h3>
        <div class="settings-card">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">AI模型</span>
              <span class="setting-desc">选择智能问答使用的AI模型</span>
            </div>
            <el-select v-model="settings.aiModel" style="width: 180px">
              <el-option label="本地模型" value="local" />
              <el-option label="OpenAI GPT" value="openai" disabled />
              <el-option label="通义千问" value="qwen" disabled />
            </el-select>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">语义搜索</span>
              <span class="setting-desc">启用后案例搜索将使用语义匹配</span>
            </div>
            <el-switch v-model="settings.semanticSearch" />
          </div>
        </div>
      </div>
      
      <div class="settings-section">
        <h3>关于</h3>
        <div class="settings-card about-card">
          <div class="about-logo">K</div>
          <div class="about-info">
            <h4>Keke - 个人工作助手</h4>
            <p>版本：1.0.0</p>
            <p>一款集成智能问答、案例管理、待办事项和日志分析的个人效率工具</p>
          </div>
        </div>
      </div>
      
      <div class="action-bar">
        <el-button @click="resetSettings">恢复默认</el-button>
        <el-button type="primary" @click="saveSettings" :loading="saving">保存配置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const saving = ref(false)

const defaultSettings = {
  modules: {
    cases: true,      // 案例库 - 默认开启，不可关闭
    todo: true,       // 待办事项 - 默认开启，不可关闭
    logs: false,      // 日志分析 - 默认关闭
    monitor: false    // 系统监控 - 默认关闭
  },
  maxConcurrency: 10,
  pythonMaxProcesses: 4,
  taskTimeout: 60,
  maxLogLines: 10000,
  logRetentionDays: 30,
  aiModel: 'local',
  semanticSearch: true
}

const settings = ref({ ...defaultSettings })

const loadSettings = () => {
  const saved = localStorage.getItem('keke_settings')
  if (saved) {
    try {
      settings.value = { ...defaultSettings, ...JSON.parse(saved) }
    } catch (e) {
      console.error('加载设置失败', e)
    }
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    localStorage.setItem('keke_settings', JSON.stringify(settings.value))
    // TODO: 调用后端API保存配置
    // await settingsApi.save(settings.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const resetSettings = () => {
  settings.value = { ...defaultSettings, modules: { ...defaultSettings.modules } }
  ElMessage.info('已恢复默认设置')
}

// 模块开关变化时即时保存
const onModuleChange = () => {
  localStorage.setItem('keke_settings', JSON.stringify(settings.value))
  // 触发全局事件通知App.vue更新菜单
  window.dispatchEvent(new CustomEvent('keke-settings-changed', { detail: settings.value }))
}

onMounted(loadSettings)
</script>

<style scoped>
.settings-page {
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.page-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.settings-section {
  margin-bottom: 32px;
}

.settings-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
}

.settings-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.setting-desc {
  font-size: 12px;
  color: #999;
}

.about-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
}

.about-logo {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
}

.about-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.about-info p {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
}
</style>
