<template>
  <div class="chat-panel-wrapper">
    <!-- 悬浮按钮 -->
    <div class="chat-fab" @click="togglePanel" :class="{ active: visible }">
      <el-icon size="24"><ChatDotRound /></el-icon>
    </div>
    
    <!-- 遮罩层 -->
    <transition name="fade">
      <div v-if="visible" class="chat-overlay" @click="closePanel"></div>
    </transition>
    
    <!-- 对话面板 -->
    <transition name="slide">
      <div v-if="visible" class="chat-panel" :style="{ width: panelWidth + 'px' }">
        <!-- 拖动手柄 -->
        <div class="resize-handle" @mousedown="startResize"></div>
        
        <!-- 头部 -->
        <div class="panel-header">
          <div class="header-left">
            <div class="logo">
              <el-icon size="20"><ChatDotRound /></el-icon>
            </div>
            <span class="title">智能助手</span>
          </div>
          <div class="header-right">
            <!-- 后端选择 -->
            <el-dropdown trigger="click" @command="handleBackendChange">
              <div class="model-btn">
                <span>{{ backendLabels[selectedBackend] || selectedBackend }}</span>
                <el-icon size="12"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item 
                    v-for="b in backendOptions" 
                    :key="b.value" 
                    :command="b.value"
                  >
                    <div class="model-item">
                      <span class="name">{{ b.label }}</span>
                      <span class="desc">{{ b.desc }}</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 模型选择 -->
            <el-dropdown trigger="click" @command="handleModelChange">
              <div class="model-btn">
                <span>{{ selectedModel }}</span>
                <el-icon size="12"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item 
                    v-for="m in currentModels" 
                    :key="m.value" 
                    :command="m.value"
                  >
                    <div class="model-item">
                      <span class="name">{{ m.label }}</span>
                      <span class="desc">{{ m.desc }}</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 总结为案例 -->
            <el-tooltip content="总结为案例" placement="bottom">
              <div class="icon-btn" @click="summarizeToCase" :class="{ disabled: messages.length < 2 }">
                <el-icon size="18"><Memo /></el-icon>
              </div>
            </el-tooltip>
            <!-- 新对话 -->
            <el-tooltip content="新对话" placement="bottom">
              <div class="icon-btn" @click="clearMessages">
                <el-icon size="18"><Plus /></el-icon>
              </div>
            </el-tooltip>
            <!-- 关闭 -->
            <div class="icon-btn close" @click="closePanel">
              <el-icon size="18"><Close /></el-icon>
            </div>
          </div>
        </div>
        
        <!-- 消息区域 -->
        <div class="messages-area" ref="messagesRef">
          <!-- 空状态 -->
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon size="48"><ChatDotRound /></el-icon>
            </div>
            <h3>有什么可以帮您？</h3>
            <div class="suggestions">
              <div class="suggestion-item" @click="sendQuickMessage('帮我分析一下日志')">
                <el-icon><Document /></el-icon>
                <span>分析日志</span>
              </div>
              <div class="suggestion-item" @click="sendQuickMessage('查看待办事项')">
                <el-icon><Finished /></el-icon>
                <span>查看待办</span>
              </div>
              <div class="suggestion-item" @click="sendQuickMessage('搜索相关案例')">
                <el-icon><Search /></el-icon>
                <span>搜索案例</span>
              </div>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div v-else class="messages-list">
            <template v-for="(msg, index) in messages" :key="index">
              <!-- 用户消息 -->
              <div v-if="msg.role === 'user'" class="message user-message">
                <div class="message-content">{{ msg.content }}</div>
              </div>
              
              <!-- AI消息 -->
              <div v-else class="message ai-message">
                <div class="ai-avatar">
                  <el-icon size="16"><ChatDotRound /></el-icon>
                </div>
                <div class="ai-content">
                  <!-- 思考中 -->
                  <div v-if="msg.thinking" class="thinking">
                    <span class="thinking-label">思考中</span>
                    <span class="thinking-time">{{ msg.thinkingTime }}s</span>
                  </div>
                  <!-- 消息内容 -->
                  <div v-else class="content-body" v-html="renderMarkdown(msg.content)"></div>
                  <!-- 操作按钮 -->
                  <div v-if="!msg.thinking" class="message-actions">
                    <el-tooltip content="复制" placement="top">
                      <div class="action-btn" @click="copyMessage(msg.content)">
                        <el-icon size="14"><CopyDocument /></el-icon>
                      </div>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-wrapper">
            <textarea
              ref="inputRef"
              v-model="inputMessage"
              placeholder="输入消息..."
              @keydown="handleKeydown"
              @compositionstart="isComposing = true"
              @compositionend="isComposing = false"
              @input="autoResize"
              :disabled="loading"
              rows="1"
            ></textarea>
            <button 
              class="send-btn" 
              @click="sendMessage"
              :disabled="!inputMessage.trim() || loading"
            >
              <el-icon size="18"><Promotion /></el-icon>
            </button>
          </div>
          <div class="input-hint">Enter 发送，Shift + Enter 换行</div>
        </div>
      </div>
    </transition>
    
    <!-- 案例编辑对话框 -->
    <el-dialog 
      v-model="caseDialogVisible" 
      title="保存为案例"
      width="600px"
      :close-on-click-modal="false"
      draggable
    >
      <div v-if="summarizing" class="summarizing-hint">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在总结对话...</span>
      </div>
      <el-form v-else :model="caseForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="caseForm.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="所属模块">
          <el-input v-model="caseForm.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input 
            v-model="caseForm.summary" 
            type="textarea" 
            :rows="3"
            placeholder="案例摘要"
          />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input 
            v-model="caseForm.content" 
            type="textarea" 
            :rows="8"
            placeholder="详细内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCase" :loading="savingCase" :disabled="summarizing">保存到案例库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch, computed } from 'vue'
import { 
  ChatDotRound, Close, Promotion, Finished, 
  ArrowDown, Plus, Document, Search, CopyDocument, Memo, Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import chatApi from '@/api/chat'
import caseApi from '@/api/case'

const visible = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const messages = ref([])
const isComposing = ref(false)

// 会话 ID（用于保持上下文）
const sessionId = ref(generateSessionId())

function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 后端选项
const backendOptions = [
  { value: 'qwen', label: '千问 AI', desc: '阿里云大模型' },
  { value: 'ollama', label: '本地模型', desc: 'Ollama 本地部署' },
  { value: 'mock', label: 'Mock', desc: '测试模式' },
]

const backendLabels = {
  'qwen': '千问 AI',
  'ollama': '本地模型',
  'mock': 'Mock'
}

// 各后端模型列表
const qwenModels = [
  { value: 'qwen-max', label: 'Qwen Max', desc: '最强效果' },
  { value: 'qwen-plus', label: 'Qwen Plus', desc: '效果均衡' },
  { value: 'qwen-turbo', label: 'Qwen Turbo', desc: '快速响应' },
  { value: 'qwen-coder-plus', label: 'Qwen Coder', desc: '代码专用' },
]

const ollamaModels = [
  { value: 'llama3', label: 'Llama 3', desc: 'Meta 开源模型' },
  { value: 'qwen2', label: 'Qwen2', desc: '通义千问' },
  { value: 'codellama', label: 'CodeLlama', desc: '代码模型' },
]

const mockModels = [
  { value: 'mock', label: 'Mock', desc: '测试模式' },
]

// 当前后端对应的模型列表
const currentModels = computed(() => {
  switch (selectedBackend.value) {
    case 'qwen': return qwenModels
    case 'ollama': return ollamaModels
    case 'mock': return mockModels
    default: return qwenModels
  }
})

const selectedModel = ref('qwen-plus')
const selectedBackend = ref('qwen')

// 面板宽度
const panelWidth = ref(480)
const minWidth = 360
const maxWidth = 800
const isResizing = ref(false)

const startResize = (e) => {
  isResizing.value = true
  document.addEventListener('mousemove', doResize)
  document.addEventListener('mouseup', stopResize)
}

const doResize = (e) => {
  if (!isResizing.value) return
  const newWidth = window.innerWidth - e.clientX
  if (newWidth >= minWidth && newWidth <= maxWidth) {
    panelWidth.value = newWidth
  }
}

const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', doResize)
  document.removeEventListener('mouseup', stopResize)
}

const togglePanel = () => {
  visible.value = !visible.value
}

const closePanel = () => {
  visible.value = false
}

const handleModelChange = (model) => {
  selectedModel.value = model
}

const handleBackendChange = (backend) => {
  selectedBackend.value = backend
  // 切换后端时，选择该后端的默认模型
  const models = backend === 'qwen' ? qwenModels : 
                 backend === 'ollama' ? ollamaModels : mockModels
  selectedModel.value = models[0]?.value || 'default'
}

const clearMessages = () => {
  messages.value = []
  sessionId.value = generateSessionId()  // 重置会话
}

// 自动调整输入框高度
const autoResize = () => {
  const textarea = inputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px'
  }
}

// 键盘事件
const handleKeydown = (e) => {
  if (isComposing.value) return
  
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return
  
  // 检查是否是总结命令
  if (content === '总结' || content === '总结案例') {
    inputMessage.value = ''
    summarizeToCase()
    return
  }
  
  messages.value.push({
    role: 'user',
    content,
    time: new Date()
  })
  inputMessage.value = ''
  
  // 重置输入框高度
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
  
  await scrollToBottom()
  loading.value = true
  
  // 添加思考中消息
  messages.value.push({
    role: 'assistant',
    content: '',
    thinking: true,
    thinkingTime: 0,
    time: new Date()
  })
  const assistantIndex = messages.value.length - 1
  
  // 计时器
  const startTime = Date.now()
  const timer = setInterval(() => {
    const elapsed = Math.floor((Date.now() - startTime) / 1000)
    messages.value[assistantIndex] = {
      ...messages.value[assistantIndex],
      thinkingTime: elapsed
    }
  }, 1000)
  
  try {
    const response = await chatApi.send(content, selectedBackend.value, selectedModel.value, sessionId.value)
    const reply = response.reply || response.data?.reply || '抱歉，我暂时无法回答。'
    
    messages.value[assistantIndex] = {
      role: 'assistant',
      content: reply,
      thinking: false,
      time: new Date()
    }
  } catch (e) {
    console.error('请求失败', e)
    messages.value[assistantIndex] = {
      role: 'assistant',
      content: '网络错误，请稍后重试。',
      thinking: false,
      time: new Date()
    }
  } finally {
    clearInterval(timer)
    loading.value = false
    scrollToBottom()
    // 自动聚焦输入框
    nextTick(() => {
      setTimeout(() => {
        inputRef.value?.focus()
      }, 50)
    })
  }
}

const sendQuickMessage = (msg) => {
  inputMessage.value = msg
  sendMessage()
}

// Markdown 渲染（简化版）
const renderMarkdown = (content) => {
  if (!content) return ''
  
  return content
    // 代码块
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
    // 行内代码
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 换行
    .replace(/\n/g, '<br>')
}

// 复制消息
const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

// 监听面板打开，聚焦输入框
watch(visible, (val) => {
  if (val) {
    nextTick(() => {
      setTimeout(() => {
        inputRef.value?.focus()
      }, 100)
    })
  }
})

// ========== 案例总结功能 ==========
const caseDialogVisible = ref(false)
const summarizing = ref(false)
const savingCase = ref(false)
const caseForm = ref({
  title: '',
  moduleName: '',
  summary: '',
  content: ''
})

// 总结对话为案例
const summarizeToCase = async () => {
  if (messages.value.length < 2) {
    ElMessage.warning('请先进行对话')
    return
  }
  
  // 打开对话框
  caseDialogVisible.value = true
  summarizing.value = true
  
  // 构建对话内容
  const conversation = messages.value
    .filter(m => !m.thinking)
    .map(m => `${m.role === 'user' ? '用户' : 'AI'}: ${m.content}`)
    .join('\n\n')
  
  // 调用 AI 总结
  const summaryPrompt = `请总结以下对话，生成一个案例。请返回 JSON 格式：
{"title": "案例标题", "moduleName": "模块分类", "summary": "简短摘要", "content": "详细内容，包含问题和解决方案"}

对话内容：
${conversation}`
  
  try {
    const response = await chatApi.send(summaryPrompt, selectedBackend.value, selectedModel.value, sessionId.value + '_summary')
    const reply = response.reply || response.data?.reply || ''
    
    // 尝试解析 JSON
    try {
      // 提取 JSON 部分
      const jsonMatch = reply.match(/\{[\s\S]*\}/)
      if (jsonMatch) {
        const parsed = JSON.parse(jsonMatch[0])
        caseForm.value = {
          title: parsed.title || '',
          moduleName: parsed.moduleName || '',
          summary: parsed.summary || '',
          content: parsed.content || ''
        }
      } else {
        // 如果不是 JSON，直接使用回复作为内容
        caseForm.value = {
          title: '',
          moduleName: '',
          summary: '',
          content: reply
        }
      }
    } catch (e) {
      caseForm.value = {
        title: '',
        moduleName: '',
        summary: '',
        content: reply
      }
    }
  } catch (e) {
    console.error('总结失败', e)
    ElMessage.error('总结失败，请重试')
    caseDialogVisible.value = false
  } finally {
    summarizing.value = false
  }
}

// 保存案例
const saveCase = async () => {
  if (!caseForm.value.title) {
    ElMessage.warning('请输入案例标题')
    return
  }
  
  savingCase.value = true
  try {
    await caseApi.create(caseForm.value)
    ElMessage.success('案例保存成功')
    caseDialogVisible.value = false
    // 重置表单
    caseForm.value = { title: '', moduleName: '', summary: '', content: '' }
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    savingCase.value = false
  }
}
</script>

<style scoped>
.chat-panel-wrapper {
  position: fixed;
  z-index: 2000;
}

/* 悬浮按钮 */
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 52px;
  height: 52px;
  background: #1a1a1a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: all 0.2s;
  z-index: 2001;
}

.chat-fab:hover {
  transform: scale(1.05);
  background: #333;
}

.chat-fab.active {
  background: #333;
}

/* 遮罩 */
.chat-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  z-index: 2000;
}

/* 面板 */
.chat-panel {
  position: fixed;
  top: 0;
  right: 0;
  height: 100vh;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  z-index: 2002;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
}

.resize-handle {
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  cursor: ew-resize;
  background: transparent;
}

.resize-handle:hover {
  background: #667eea;
}

/* 头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #1a1a1a;
  color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.title {
  font-size: 15px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.model-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.model-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-item .name {
  font-size: 13px;
  font-weight: 500;
}

.model-item .desc {
  font-size: 11px;
  color: #909399;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.icon-btn.close:hover {
  background: rgba(255, 59, 48, 0.2);
}

/* 消息区域 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f0f0f0;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 24px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-item:hover {
  border-color: #667eea;
  color: #667eea;
}

/* 消息列表 */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  max-width: 100%;
}

/* 用户消息 */
.user-message {
  display: flex;
  justify-content: flex-end;
}

.user-message .message-content {
  max-width: 80%;
  padding: 12px 16px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 16px 16px 4px 16px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* AI 消息 */
.ai-message {
  display: flex;
  gap: 12px;
}

.ai-avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.ai-content {
  flex: 1;
  min-width: 0;
}

.content-body {
  font-size: 14px;
  line-height: 1.7;
  color: #333;
}

.content-body :deep(strong) {
  font-weight: 600;
}

.content-body :deep(.code-block) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 12px 0;
  overflow-x: auto;
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
}

.content-body :deep(.inline-code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
}

/* 思考中 */
.thinking {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #f5f5f5;
  border-radius: 8px;
}

.thinking-label {
  font-size: 13px;
  color: #666;
}

.thinking-time {
  font-size: 12px;
  color: #999;
  font-family: 'SF Mono', Monaco, monospace;
}

/* 消息操作 */
.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.ai-message:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f0f0f0;
  color: #333;
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #eee;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 12px 16px;
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  max-height: 150px;
  outline: none;
  font-family: inherit;
}

.input-wrapper textarea::placeholder {
  color: #999;
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #333;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #999;
  margin-top: 8px;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

/* 禁用状态 */
.icon-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 总结中加载 */
.summarizing-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #666;
  font-size: 14px;
}

.summarizing-hint .is-loading {
  animation: rotating 1.5s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
