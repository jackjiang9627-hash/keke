<template>
  <div class="cases-page">
    <div class="page-header">
      <h2>案例库</h2>
      <div class="header-actions">
        <el-input
          v-model="searchText"
          placeholder="搜索案例..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button 
          v-if="selectedCases.length > 0" 
          type="danger" 
          :icon="Delete"
          @click="handleBatchDelete"
        >
          删除已选 ({{ selectedCases.length }})
        </el-button>
        <el-button type="primary" :icon="Plus" @click="showAddDialog">新增案例</el-button>
        <el-upload
          :show-file-list="false"
          accept=".xlsx,.xls"
          :before-upload="handleImport"
        >
          <el-button :icon="Upload">导入</el-button>
        </el-upload>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
      </div>
    </div>
    
    <div class="page-content">
      <!-- 案例表格 -->
      <div class="table-container">
        <el-table 
          :data="filteredCases" 
          style="width: 100%"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
          stripe
          :header-cell-style="{ background: '#fafafa', color: '#303133', fontWeight: '600' }"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip sortable>
            <template #default="{ row }">
              <div class="title-cell">
                <span class="title-text">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="summary-text">{{ row.summary || '无摘要' }}</span>
            </template>
          </el-table-column>
          <el-table-column 
            prop="moduleName" 
            label="模块分类" 
            width="140" 
            sortable
            :filters="moduleFilters"
            :filter-method="filterModule"
            filter-placement="bottom-end"
          >
            <template #default="{ row }">
              <el-tag size="small" :type="getTagType(row.moduleName)">
                {{ row.moduleName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160" sortable>
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="修改时间" width="160" sortable>
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                link 
                size="small" 
                @click.stop="showEditDialog(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small" 
                @click.stop="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="filteredCases.length === 0" description="暂无案例" />
      </div>
    </div>
    
    <!-- 新增对话框 -->
    <el-dialog 
      v-model="addDialogVisible" 
      title="新增案例"
      width="700px"
      destroy-on-close
    >
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="addForm.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="所属模块" required>
          <el-input v-model="addForm.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input 
            v-model="addForm.summary" 
            type="textarea" 
            :rows="3"
            placeholder="请输入案例摘要"
          />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input 
            v-model="addForm.content" 
            type="textarea" 
            :rows="5"
            placeholder="请输入详细内容"
          />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="addForm.hyperlink" placeholder="请输入相关链接（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑对话框 -->
    <el-dialog 
      v-model="editDialogVisible" 
      title="编辑案例"
      width="700px"
      destroy-on-close
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="所属模块" required>
          <el-input v-model="editForm.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input 
            v-model="editForm.summary" 
            type="textarea" 
            :rows="3"
            placeholder="请输入案例摘要"
          />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input 
            v-model="editForm.content" 
            type="textarea" 
            :rows="5"
            placeholder="请输入详细内容"
          />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="editForm.hyperlink" placeholder="请输入相关链接（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer-actions">
          <el-button type="danger" :icon="Delete" @click="handleDeleteCurrent">删除</el-button>
          <div>
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleUpdate">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
    
    <!-- 复习提醒弹窗 -->
    <el-dialog 
      v-model="reviewDialogVisible" 
      title="📚 今日复习提醒"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="review-dialog-content">
        <p class="review-hint">您有 <strong>{{ todayReviewCases.length }}</strong> 个案例需要复习：</p>
        
        <div class="review-list">
          <div v-for="item in todayReviewCases" :key="item.id" class="review-item">
            <div class="review-item-header">
              <span class="review-item-title">{{ item.title }}</span>
              <div class="mastery-stars">
                <el-icon v-for="n in 5" :key="n" :class="{ active: n <= (item.masteryLevel || 1) }">
                  <Star />
                </el-icon>
              </div>
            </div>
            <div class="review-item-tags" v-if="item.tags">
              <el-tag v-for="tag in item.tags.split(',')" :key="tag" size="small" type="info">{{ tag.trim() }}</el-tag>
            </div>
            <div class="review-item-actions">
              <el-button size="small" @click="viewCaseDetail(item)">查看详情</el-button>
              <el-button size="small" type="success" @click="markReviewed(item, true)">已掌握</el-button>
              <el-button size="small" type="warning" @click="markReviewed(item, false)">还需复习</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="postponeAllReviews">全部延后到明天</el-button>
        <el-button type="primary" @click="reviewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, Upload, Download, Delete, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import caseApi from '@/api/case'

const searchText = ref('')
const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const cases = ref([])
const selectedCases = ref([])
const todayReviewCases = ref([])

const addForm = ref({
  title: '',
  moduleName: '',
  summary: '',
  content: '',
  hyperlink: ''
})

const editForm = ref({
  id: null,
  title: '',
  moduleName: '',
  summary: '',
  content: '',
  hyperlink: ''
})

const filteredCases = computed(() => {
  let result = cases.value
  // 只使用搜索过滤，移除模块过滤
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(c => 
      c.title?.toLowerCase().includes(keyword) || 
      c.summary?.toLowerCase().includes(keyword)
    )
  }
  return result
})

// 动态生成模块筛选器选项
const moduleFilters = computed(() => {
  const uniqueModules = [...new Set(cases.value.map(c => c.moduleName).filter(Boolean))]
  return uniqueModules.map(module => ({ text: module, value: module }))
})

// 模块筛选方法
const filterModule = (value, row) => {
  return row.moduleName === value
}

const getTagType = (module) => {
  const types = { '用户管理': '', '订单模块': 'success', '支付模块': 'warning' }
  return types[module] || 'info'
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedCases.value = selection.map(item => item.id)
}

// 表格行点击
const handleRowClick = (row) => {
  showEditDialog(row)
}

const showAddDialog = () => {
  addForm.value = { 
    title: '', 
    moduleName: '', 
    summary: '', 
    content: '', 
    hyperlink: '' 
  }
  addDialogVisible.value = true
}

const showEditDialog = (item) => {
  editForm.value = { 
    id: item.id,
    title: item.title,
    moduleName: item.moduleName,
    summary: item.summary,
    content: item.content,
    hyperlink: item.hyperlink,
    tags: item.tags || ''
  }
  editDialogVisible.value = true
}

const handleAdd = async () => {
  if (!addForm.value.title || !addForm.value.moduleName) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await caseApi.create(addForm.value)
    ElMessage.success('创建成功')
    addDialogVisible.value = false
    loadCases()
    loadModules()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  }
}

const handleUpdate = async () => {
  if (!editForm.value.title || !editForm.value.moduleName) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await caseApi.update(editForm.value.id, editForm.value)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadCases()
    loadModules()
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

const handleDelete = async (row) => {
  ElMessageBox.confirm('确定删除该案例吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await caseApi.delete(row.id)
        ElMessage.success('删除成功')
        loadCases()
        loadModules()
      } catch (e) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const handleDeleteCurrent = async () => {
  ElMessageBox.confirm('确定删除该案例吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await caseApi.delete(editForm.value.id)
        ElMessage.success('删除成功')
        editDialogVisible.value = false
        loadCases()
        loadModules()
      } catch (e) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const handleBatchDelete = async () => {
  if (selectedCases.value.length === 0) {
    ElMessage.warning('请选择要删除的案例')
    return
  }
  
  ElMessageBox.confirm(`确定删除已选择的 ${selectedCases.value.length} 个案例吗？`, '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        // 逐个删除
        for (const id of selectedCases.value) {
          await caseApi.delete(id)
        }
        ElMessage.success(`已删除 ${selectedCases.value.length} 个案例`)
        selectedCases.value = []
        loadCases()
        loadModules()
      } catch (e) {
        ElMessage.error('批量删除失败')
      }
    })
    .catch(() => {})
}

const handleSearch = async () => {
  if (searchText.value) {
    try {
      const res = await caseApi.search(searchText.value)
      cases.value = res.data || res || []
    } catch (e) {
      console.error('搜索失败', e)
    }
  } else {
    loadCases()
  }
}

const handleImport = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  caseApi.import(formData).then(() => {
    ElMessage.success('导入成功')
    loadCases()
    loadModules()
  }).catch(() => {
    ElMessage.error('导入失败')
  })
  return false
}

const handleExport = async () => {
  try {
    const res = await caseApi.export()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 10)
    link.download = `案例库_${timestamp}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
    console.error('导出失败', e)
  }
}

const loadCases = async () => {
  try {
    const res = await caseApi.list()
    const data = res.data || res
    cases.value = data.content || data || []
  } catch (e) {
    console.error('加载案例失败', e)
  }
}

const loadModules = async () => {
  // 模块信息现在通过表格筛选器动态生成，不再需要单独加载
  // 保留该函数以保持 API 调用兼容性
  try {
    await caseApi.getModules()
  } catch (e) {
    console.error('加载模块失败', e)
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDateTime = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// === 复习相关方法 ===

const loadTodayReviews = async () => {
  try {
    const res = await caseApi.getTodayReviews()
    const data = res.data || res || []
    todayReviewCases.value = data
    if (data.length > 0) {
      reviewDialogVisible.value = true
    }
  } catch (e) {
    console.error('加载待复习案例失败', e)
  }
}

const markReviewed = async (item, mastered) => {
  try {
    await caseApi.markReviewed(item.id, mastered)
    ElMessage.success(mastered ? '已标记为掌握' : '已标记为还需复习')
    // 从待复习列表中移除
    todayReviewCases.value = todayReviewCases.value.filter(c => c.id !== item.id)
    if (todayReviewCases.value.length === 0) {
      reviewDialogVisible.value = false
    }
    loadCases()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const postponeAllReviews = async () => {
  try {
    for (const item of todayReviewCases.value) {
      await caseApi.postponeReview(item.id)
    }
    ElMessage.success('已全部延后到明天')
    todayReviewCases.value = []
    reviewDialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const viewCaseDetail = (item) => {
  reviewDialogVisible.value = false
  showEditDialog(item)
}

onMounted(() => {
  loadCases()
  loadModules()
  // 加载今日待复习案例
  loadTodayReviews()
})
</script>

<style scoped>
.cases-page {
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

.header-actions {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 240px;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 表格样式 */
.table-container {
  background: #fff;
  border-radius: 8px;
}

.table-container :deep(.el-table) {
  font-size: 14px;
}

.table-container :deep(.el-table__row) {
  cursor: pointer;
  transition: background-color 0.2s;
}

.table-container :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.title-cell {
  display: flex;
  align-items: center;
}

.title-text {
  font-weight: 500;
  color: #303133;
}

.summary-text {
  color: #606266;
}

/* 对话框样式 */
.dialog-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}

/* 复习弹窗样式 */
.review-dialog-content {
  max-height: 400px;
  overflow-y: auto;
}

.review-hint {
  margin-bottom: 16px;
  color: #606266;
}

.review-hint strong {
  color: #409eff;
  font-size: 18px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.review-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-item-title {
  font-weight: 500;
  color: #303133;
  font-size: 15px;
}

.mastery-stars {
  display: flex;
  gap: 2px;
}

.mastery-stars .el-icon {
  color: #dcdfe6;
  font-size: 16px;
}

.mastery-stars .el-icon.active {
  color: #f7ba2a;
}

.review-item-tags {
  margin-bottom: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.review-item-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
