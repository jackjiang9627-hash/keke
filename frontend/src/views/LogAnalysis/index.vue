<template>
  <div class="log-page">
    <div class="page-header">
      <h2>日志分析</h2>
      <div class="header-actions">
        <el-upload
          :show-file-list="false"
          accept=".log,.txt"
          :before-upload="handleUpload"
        >
          <el-button type="primary" :icon="Upload" :loading="uploading">上传日志</el-button>
        </el-upload>
        <el-button @click="loadStatistics" :icon="Refresh">刷新</el-button>
      </div>
    </div>
    
    <div class="page-content">
      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card">
          <el-icon size="24"><Document /></el-icon>
          <div class="stat-info">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">总日志数</span>
          </div>
        </div>
        <div class="stat-card error">
          <el-icon size="24"><CircleCloseFilled /></el-icon>
          <div class="stat-info">
            <span class="stat-value">{{ stats.error }}</span>
            <span class="stat-label">ERROR</span>
          </div>
        </div>
        <div class="stat-card warning">
          <el-icon size="24"><WarningFilled /></el-icon>
          <div class="stat-info">
            <span class="stat-value">{{ stats.warn }}</span>
            <span class="stat-label">WARN</span>
          </div>
        </div>
        <div class="stat-card info">
          <el-icon size="24"><InfoFilled /></el-icon>
          <div class="stat-info">
            <span class="stat-value">{{ stats.info }}</span>
            <span class="stat-label">INFO</span>
          </div>
        </div>
      </div>
      
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="filter.level" placeholder="日志级别" clearable style="width: 120px">
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARN" value="WARN" />
          <el-option label="INFO" value="INFO" />
          <el-option label="DEBUG" value="DEBUG" />
        </el-select>
        <el-input 
          v-model="filter.keyword" 
          placeholder="搜索关键词..." 
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @keyup.enter="loadLogs"
        />
        <el-button @click="loadLogs" type="primary">查询</el-button>
      </div>
      
      <!-- 日志列表 -->
      <div class="log-list">
        <el-empty v-if="logs.length === 0" description="暂无日志记录" />
        <div v-else>
          <div 
            v-for="log in logs" 
            :key="log.id" 
            class="log-item"
            :class="log.level?.toLowerCase()"
          >
            <span class="log-level">{{ log.level }}</span>
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { 
  Upload, Refresh, Search, Document, 
  CircleCloseFilled, WarningFilled, InfoFilled 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import logApi from '@/api/log'

const uploading = ref(false)
const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)

const filter = ref({
  level: '',
  keyword: ''
})

const stats = ref({
  total: 0,
  error: 0,
  warn: 0,
  info: 0
})

const handleUpload = async (file) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await logApi.upload(formData)
    ElMessage.success('上传成功')
    loadLogs()
    loadStatistics()
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

const loadLogs = async () => {
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    // 添加筛选条件
    if (filter.value.level) {
      params.level = filter.value.level
    }
    if (filter.value.keyword) {
      params.keyword = filter.value.keyword
    }
    
    const res = await logApi.list(params)
    // 后端返回 PageDTO 格式: { content, totalElements, page, size, ... }
    logs.value = res.content || []
    total.value = res.totalElements || 0
  } catch (e) {
    console.error('加载日志失败', e)
    logs.value = []
    total.value = 0
  }
}

const loadStatistics = async () => {
  try {
    const res = await logApi.statistics()
    // 后端返回字段: totalCount, errorCount, warningCount, countByLevel
    stats.value = {
      total: res.totalCount || 0,
      error: res.errorCount || 0,
      warn: res.warningCount || 0,
      info: res.countByLevel?.INFO || res.countByLevel?.info || 0
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
  loadStatistics()
})
</script>

<style scoped>
.log-page {
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

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f5f7fa;
  border-radius: 12px;
  color: #667eea;
}

.stat-card.error { background: #fef0f0; color: #f56c6c; }
.stat-card.warning { background: #fdf6ec; color: #e6a23c; }
.stat-card.info { background: #f0f9eb; color: #67c23a; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.log-list {
  flex: 1;
  overflow-y: auto;
  background: #fafafa;
  border-radius: 12px;
  padding: 8px;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 4px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
}

.log-level {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  background: #e8e8e8;
  color: #666;
}

.log-item.error .log-level { background: #fef0f0; color: #f56c6c; }
.log-item.warn .log-level { background: #fdf6ec; color: #e6a23c; }
.log-item.info .log-level { background: #f0f9eb; color: #67c23a; }
.log-item.debug .log-level { background: #f4f4f5; color: #909399; }

.log-time {
  flex-shrink: 0;
  color: #999;
  font-size: 12px;
}

.log-message {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.pagination {
  padding: 16px 0 0;
  display: flex;
  justify-content: center;
}
</style>
