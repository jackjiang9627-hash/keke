<template>
  <div class="todo-page">
    <div class="page-header">
      <h2>待办事项</h2>
      <el-button type="primary" :icon="Plus" @click="showAddDialog">新增待办</el-button>
    </div>
    
    <div class="page-content">
      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待完成</div>
        </div>
        <div class="stat-card completed">
          <div class="stat-value">{{ completedCount }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat-card today">
          <div class="stat-value">{{ todayCount }}</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
      
      <!-- 待办列表 -->
      <div class="todo-section">
        <h3>进行中 <span class="count">{{ pendingTodos.length }}</span></h3>
        <el-empty v-if="pendingTodos.length === 0" description="暂无待办事项" :image-size="80" />
        <transition-group name="list" tag="div" class="todo-list">
          <div 
            v-for="item in pendingTodos" 
            :key="item.id"
            class="todo-item"
          >
            <el-checkbox 
              :model-value="item.completed"
              @change="toggleComplete(item)"
              class="todo-check"
            />
            <div class="todo-content" @click="showEditDialog(item)">
              <span class="todo-title">{{ item.content }}</span>
              <span v-if="item.priority >= 3" class="priority high">紧急</span>
              <span v-else-if="item.priority === 2" class="priority medium">重要</span>
            </div>
            <div class="todo-actions">
              <el-icon class="action-btn" @click="showEditDialog(item)"><Edit /></el-icon>
              <el-icon class="action-btn delete" @click="confirmDelete(item)"><Delete /></el-icon>
            </div>
          </div>
        </transition-group>
      </div>
      
      <!-- 已完成 -->
      <div class="todo-section completed-section" v-if="completedTodos.length > 0">
        <h3 @click="showCompleted = !showCompleted" class="clickable">
          已完成 <span class="count">{{ completedTodos.length }}</span>
          <el-icon class="toggle-icon" :class="{ expanded: showCompleted }"><ArrowDown /></el-icon>
        </h3>
        <transition name="collapse">
          <div v-if="showCompleted" class="todo-list">
            <div 
              v-for="item in completedTodos" 
              :key="item.id"
              class="todo-item completed"
            >
              <el-checkbox 
                :model-value="item.completed"
                @change="toggleComplete(item)"
                class="todo-check"
              />
              <div class="todo-content">
                <span class="todo-title">{{ item.content }}</span>
              </div>
              <div class="todo-actions">
                <el-icon class="action-btn delete" @click="confirmDelete(item)"><Delete /></el-icon>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑待办' : '新增待办'"
      width="480"
    >
      <el-form :model="form" label-width="70px">
        <el-form-item label="内容" required>
          <el-input 
            v-model="form.content" 
            placeholder="请输入待办内容"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio :value="1">普通</el-radio>
            <el-radio :value="2">重要</el-radio>
            <el-radio :value="3">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import todoApi from '@/api/todo'

const todos = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const showCompleted = ref(false)

const form = ref({
  id: null,
  content: '',
  priority: 1
})

const pendingTodos = computed(() => todos.value.filter(t => !t.completed))
const completedTodos = computed(() => todos.value.filter(t => t.completed))
const pendingCount = computed(() => pendingTodos.value.length)
const completedCount = computed(() => completedTodos.value.length)
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return todos.value.filter(t => {
    if (!t.createdAt) return false
    return new Date(t.createdAt).toDateString() === today
  }).length
})

const showAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, content: '', priority: 1 }
  dialogVisible.value = true
}

const showEditDialog = (item) => {
  isEdit.value = true
  form.value = { 
    id: item.id, 
    content: item.content, 
    priority: item.priority || 1 
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.content?.trim()) {
    ElMessage.warning('请输入待办内容')
    return
  }
  try {
    if (isEdit.value) {
      await todoApi.update(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await todoApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTodos()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

const toggleComplete = async (item) => {
  try {
    await todoApi.update(item.id, { ...item, completed: !item.completed })
    loadTodos()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const confirmDelete = (item) => {
  ElMessageBox.confirm('确定删除该待办吗？', '提示', { type: 'warning' })
    .then(() => deleteTodo(item.id))
    .catch(() => {})
}

const deleteTodo = async (id) => {
  try {
    await todoApi.delete(id)
    ElMessage.success('删除成功')
    loadTodos()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const loadTodos = async () => {
  try {
    const res = await todoApi.list()
    todos.value = res.data || res || []
  } catch (e) {
    console.error('加载待办失败', e)
  }
}

loadTodos()
</script>

<style scoped>
.todo-page {
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.page-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
}

.stat-card.completed {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-card.today {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}

.todo-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-section h3 .count {
  font-size: 13px;
  color: #999;
  font-weight: normal;
}

.todo-section h3.clickable {
  cursor: pointer;
}

.toggle-icon {
  transition: transform 0.2s;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 10px;
  transition: all 0.2s;
}

.todo-item:hover {
  background: #f0f2f5;
}

.todo-item.completed {
  opacity: 0.6;
}

.todo-item.completed .todo-title {
  text-decoration: line-through;
  color: #999;
}

.todo-check {
  margin-right: 12px;
}

.todo-content {
  flex: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-title {
  font-size: 14px;
  color: #303133;
}

.priority {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.priority.high {
  background: #fef0f0;
  color: #f56c6c;
}

.priority.medium {
  background: #fdf6ec;
  color: #e6a23c;
}

.todo-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.todo-item:hover .todo-actions {
  opacity: 1;
}

.action-btn {
  cursor: pointer;
  color: #999;
  padding: 4px;
}

.action-btn:hover {
  color: #667eea;
}

.action-btn.delete:hover {
  color: #f56c6c;
}

.completed-section {
  margin-top: 24px;
}

/* 列表动画 */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.2s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
}
</style>
