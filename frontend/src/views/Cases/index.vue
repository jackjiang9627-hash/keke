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
      <!-- 模块筛选 -->
      <div class="filter-bar">
        <el-tag
          v-for="module in modules"
          :key="module"
          :type="activeModule === module ? '' : 'info'"
          :effect="activeModule === module ? 'dark' : 'plain'"
          @click="filterByModule(module)"
          class="filter-tag"
        >
          {{ module }}
        </el-tag>
      </div>
      
      <!-- 案例列表 -->
      <div class="cases-list">
        <el-empty v-if="filteredCases.length === 0" description="暂无案例" />
        <div v-else class="case-grid">
          <div 
            v-for="item in filteredCases" 
            :key="item.id" 
            class="case-card"
            @click="showDetail(item)"
          >
            <div class="case-header">
              <el-tag size="small" :type="getTagType(item.moduleName)">{{ item.moduleName }}</el-tag>
              <el-dropdown trigger="click" @command="handleCommand($event, item)">
                <el-icon class="more-btn"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit" :icon="Edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" :icon="Delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <h3 class="case-title">{{ item.title }}</h3>
            <p class="case-desc">{{ item.truncatedSummary || item.summary }}</p>
            <div class="case-footer">
              <span class="case-time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑案例' : '新增案例'"
      width="600"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="所属模块" required>
          <el-input v-model="form.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input 
            v-model="form.summary" 
            type="textarea" 
            :rows="3"
            placeholder="请输入案例摘要"
          />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="4"
            placeholder="请输入详细内容"
          />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.hyperlink" placeholder="请输入相关链接（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" :title="currentCase?.title" width="650">
      <div v-if="currentCase" class="case-detail">
        <el-tag :type="getTagType(currentCase.moduleName)">{{ currentCase.moduleName }}</el-tag>
        <h4>摘要</h4>
        <p>{{ currentCase.summary || '无' }}</p>
        <h4>详细内容</h4>
        <p>{{ currentCase.content || '无' }}</p>
        <div v-if="currentCase.hyperlink" class="detail-link">
          <h4>相关链接</h4>
          <a :href="currentCase.hyperlink" target="_blank">{{ currentCase.hyperlink }}</a>
        </div>
        <div class="detail-meta">
          创建时间：{{ formatDate(currentCase.createdAt) }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, Upload, Download, Edit, Delete, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import caseApi from '@/api/case'

const searchText = ref('')
const activeModule = ref('全部')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentCase = ref(null)
const cases = ref([])
const modules = ref(['全部'])

const form = ref({
  id: null,
  title: '',
  moduleName: '',
  summary: '',
  content: '',
  hyperlink: ''
})

const filteredCases = computed(() => {
  let result = cases.value
  if (activeModule.value !== '全部') {
    result = result.filter(c => c.moduleName === activeModule.value)
  }
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(c => 
      c.title?.toLowerCase().includes(keyword) || 
      c.summary?.toLowerCase().includes(keyword)
    )
  }
  return result
})

const getTagType = (module) => {
  const types = { '用户管理': '', '订单模块': 'success', '支付模块': 'warning' }
  return types[module] || 'info'
}

const filterByModule = (module) => {
  activeModule.value = module
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, title: '', moduleName: '', summary: '', content: '', hyperlink: '' }
  dialogVisible.value = true
}

const showDetail = (item) => {
  currentCase.value = item
  detailVisible.value = true
}

const handleCommand = (cmd, item) => {
  if (cmd === 'edit') {
    isEdit.value = true
    form.value = { 
      id: item.id,
      title: item.title,
      moduleName: item.moduleName,
      summary: item.summary,
      content: item.content,
      hyperlink: item.hyperlink
    }
    dialogVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除该案例吗？', '提示', { type: 'warning' })
      .then(() => deleteCase(item.id))
      .catch(() => {})
  }
}

const handleSubmit = async () => {
  if (!form.value.title || !form.value.moduleName) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (isEdit.value) {
      await caseApi.update(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await caseApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadCases()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

const deleteCase = async (id) => {
  try {
    await caseApi.delete(id)
    ElMessage.success('删除成功')
    loadCases()
  } catch (e) {
    ElMessage.error('删除失败')
  }
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
    // 创建下载链接
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
  try {
    const res = await caseApi.getModules()
    const data = res.data || res || []
    modules.value = ['全部', ...data]
  } catch (e) {
    console.error('加载模块失败', e)
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadCases()
  loadModules()
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

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-tag {
  cursor: pointer;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.case-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.case-card:hover {
  background: #f0f2f5;
  transform: translateY(-2px);
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.more-btn {
  cursor: pointer;
  color: #999;
}

.more-btn:hover {
  color: #666;
}

.case-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-desc {
  font-size: 13px;
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-footer {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
}

.case-detail h4 {
  font-size: 14px;
  color: #303133;
  margin: 16px 0 8px;
}

.case-detail p {
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
}

.detail-link a {
  color: #667eea;
  text-decoration: none;
}

.detail-link a:hover {
  text-decoration: underline;
}

.detail-meta {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}
</style>
