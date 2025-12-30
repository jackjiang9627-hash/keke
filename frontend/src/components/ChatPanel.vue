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
    
    <!-- 滑出面板 -->
    <transition name="slide">
      <div v-if="visible" class="chat-panel" :style="{ width: panelWidth + 'px' }">
        <!-- 拖动调整宽度手柄 -->
        <div class="resize-handle" @mousedown="startResize"></div>
        <div class="panel-header">
          <h3>
            <el-icon><ChatDotRound /></el-icon>
            智能助手
          </h3>
          <el-icon class="close-btn" @click="closePanel"><Close /></el-icon>
        </div>
        
        <div class="panel-body" ref="messagesRef">
          <!-- 欢迎信息 -->
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-icon">
              <el-icon size="32"><ChatDotRound /></el-icon>
            </div>
            <p>有什么可以帮您的？</p>
            <div class="quick-actions">
              <div class="quick-item" @click="sendQuickMessage('帮我查看待办事项')">
                <el-icon><Finished /></el-icon> 查看待办
              </div>
              <div class="quick-item" @click="sendQuickMessage('搜索相关案例')">
                <el-icon><Folder /></el-icon> 搜索案例
              </div>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
            <div class="message-avatar">
              <el-icon v-if="msg.role === 'assistant'" size="16"><ChatDotRound /></el-icon>
              <el-icon v-else size="16"><User /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
            </div>
          </div>
          
          <!-- 加载中 -->
          <div v-if="loading" class="message assistant">
            <div class="message-avatar">
              <el-icon size="16"><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="panel-footer">
          <el-input
            v-model="inputMessage"
            placeholder="输入您的问题..."
            @keydown.enter.exact.prevent="sendMessage"
            :disabled="loading"
          >
            <template #append>
              <el-button 
                :icon="Promotion" 
                @click="sendMessage"
                :disabled="!inputMessage.trim() || loading"
              />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ChatDotRound, Close, User, Promotion, Finished, Folder } from '@element-plus/icons-vue'
import chatApi from '@/api/chat'

const visible = ref(false)
const messagesRef = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const messages = ref([])

// 面板宽度控制
const panelWidth = ref(Math.floor(window.innerWidth / 2)) // 默认50%宽度
const minWidth = 320
const maxWidth = window.innerWidth * 0.8
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

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return
  
  messages.value.push({
    role: 'user',
    content,
    time: new Date()
  })
  inputMessage.value = ''
  
  await scrollToBottom()
  loading.value = true
  
  try {
    const response = await chatApi.send(content)
    messages.value.push({
      role: 'assistant',
      content: response.reply || response.data?.reply || '抱歉，我暂时无法回答这个问题。',
      time: new Date()
    })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '网络错误，请稍后重试。',
      time: new Date()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const sendQuickMessage = (msg) => {
  inputMessage.value = msg
  sendMessage()
}

const formatMessage = (content) => {
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
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
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  z-index: 2001;
}

.chat-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.chat-fab.active {
  transform: rotate(90deg);
}

/* 遮罩层 */
.chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 2000;
}

/* 滑出面板 */
.chat-panel {
  position: fixed;
  top: 0;
  right: 0;
  min-width: 320px;
  max-width: 80vw;
  height: 100vh;
  background: #fff;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 2002;
}

/* 拖动调整宽度手柄 */
.resize-handle {
  position: absolute;
  left: 0;
  top: 0;
  width: 6px;
  height: 100%;
  cursor: ew-resize;
  background: transparent;
  transition: background 0.2s;
}

.resize-handle:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header h3 .el-icon {
  color: #667eea;
}

.close-btn {
  cursor: pointer;
  color: #999;
  font-size: 18px;
}

.close-btn:hover {
  color: #333;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

/* 欢迎区域 */
.welcome {
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.welcome p {
  color: #666;
  margin-bottom: 20px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: #303133;
  transition: all 0.2s;
}

.quick-item:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* 消息样式 */
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.message.user .message-avatar {
  background: #e8e8e8;
  color: #666;
}

.message-content {
  max-width: 75%;
}

.message-text {
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.5;
  font-size: 13px;
}

.message.assistant .message-text {
  background: #f5f7fa;
  border-radius: 14px 14px 14px 4px;
}

.message.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 14px 14px 4px 14px;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 14px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #999;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

/* 输入区域 */
.panel-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
