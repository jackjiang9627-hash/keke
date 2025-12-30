<template>
  <div class="monitor-page">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2>系统监控</h2>
        <el-tag :type="healthTagType" size="large">{{ snapshot?.healthStatus || '未知' }}</el-tag>
        <span class="update-time" v-if="snapshot?.collectTime">
          最后更新: {{ formatTime(snapshot.collectTime) }}
        </span>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button type="success" @click="detectOnce" :loading="detecting">
          <el-icon><VideoPlay /></el-icon>单次检测
        </el-button>
        <el-button type="warning" @click="showTaskDialog = true">
          <el-icon><Timer /></el-icon>周期任务
        </el-button>
        <el-dropdown @command="handleExport">
          <el-button>
            <el-icon><Download /></el-icon>导出报告<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="current">导出当前报告</el-dropdown-item>
              <el-dropdown-item command="history">导出历史报告</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-cards">
      <div class="stat-card cpu-card">
        <div class="card-icon"><el-icon><Cpu /></el-icon></div>
        <div class="card-content">
          <div class="card-title">CPU使用率</div>
          <div class="card-value">{{ formatPercent(snapshot?.overview?.cpuUsage) }}</div>
          <el-progress 
            :percentage="snapshot?.overview?.cpuUsage || 0" 
            :stroke-width="8"
            :color="getProgressColor(snapshot?.overview?.cpuUsage)"
          />
        </div>
      </div>
      <div class="stat-card memory-card">
        <div class="card-icon"><el-icon><Coin /></el-icon></div>
        <div class="card-content">
          <div class="card-title">内存使用率</div>
          <div class="card-value">{{ formatPercent(snapshot?.overview?.memoryUsage) }}</div>
          <el-progress 
            :percentage="snapshot?.overview?.memoryUsage || 0" 
            :stroke-width="8"
            :color="getProgressColor(snapshot?.overview?.memoryUsage)"
          />
        </div>
      </div>
      <div class="stat-card disk-card">
        <div class="card-icon"><el-icon><Files /></el-icon></div>
        <div class="card-content">
          <div class="card-title">磁盘使用率</div>
          <div class="card-value">{{ formatPercent(snapshot?.overview?.diskUsage) }}</div>
          <el-progress 
            :percentage="snapshot?.overview?.diskUsage || 0" 
            :stroke-width="8"
            :color="getProgressColor(snapshot?.overview?.diskUsage)"
          />
        </div>
      </div>
      <div class="stat-card process-card">
        <div class="card-icon"><el-icon><Operation /></el-icon></div>
        <div class="card-content">
          <div class="card-title">进程数</div>
          <div class="card-value">{{ snapshot?.overview?.processCount || 0 }}</div>
          <div class="card-desc">网卡: {{ snapshot?.overview?.networkInterfaceCount || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 详细信息面板 -->
    <div class="detail-panels">
      <!-- 左侧面板 -->
      <div class="panel-column">
        <!-- 系统信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Monitor /></el-icon> 系统信息</span>
            </div>
          </template>
          <div class="info-grid" v-if="snapshot?.systemInfo">
            <div class="info-item">
              <span class="label">主机名</span>
              <span class="value">{{ snapshot.systemInfo.hostname }}</span>
            </div>
            <div class="info-item">
              <span class="label">操作系统</span>
              <span class="value">{{ snapshot.systemInfo.osName }} {{ snapshot.systemInfo.osVersion }}</span>
            </div>
            <div class="info-item">
              <span class="label">系统架构</span>
              <span class="value">{{ snapshot.systemInfo.osArch }}</span>
            </div>
            <div class="info-item">
              <span class="label">运行时长</span>
              <span class="value">{{ snapshot.systemInfo.upTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">当前用户</span>
              <span class="value">{{ snapshot.systemInfo.currentUser }}</span>
            </div>
            <div class="info-item">
              <span class="label">Java版本</span>
              <span class="value">{{ snapshot.systemInfo.javaVersion }}</span>
            </div>
            <div class="info-item">
              <span class="label">JVM内存</span>
              <span class="value">{{ snapshot.systemInfo.jvmMemory }}</span>
            </div>
          </div>
        </el-card>

        <!-- CPU信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Cpu /></el-icon> CPU详情</span>
            </div>
          </template>
          <div class="info-grid" v-if="snapshot?.cpuInfo">
            <div class="info-item">
              <span class="label">型号</span>
              <span class="value">{{ snapshot.cpuInfo.model }}</span>
            </div>
            <div class="info-item">
              <span class="label">核心数</span>
              <span class="value">{{ snapshot.cpuInfo.physicalCores }}物理 / {{ snapshot.cpuInfo.logicalCores }}逻辑</span>
            </div>
            <div class="info-item">
              <span class="label">频率</span>
              <span class="value">{{ snapshot.cpuInfo.frequency }} MHz</span>
            </div>
            <div class="info-item">
              <span class="label">用户占用</span>
              <span class="value">{{ formatPercent(snapshot.cpuInfo.userPercent) }}</span>
            </div>
            <div class="info-item">
              <span class="label">系统占用</span>
              <span class="value">{{ formatPercent(snapshot.cpuInfo.systemPercent) }}</span>
            </div>
            <div class="info-item">
              <span class="label">空闲率</span>
              <span class="value">{{ formatPercent(snapshot.cpuInfo.idlePercent) }}</span>
            </div>
          </div>
        </el-card>

        <!-- 内存信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Coin /></el-icon> 内存详情</span>
            </div>
          </template>
          <div class="info-grid" v-if="snapshot?.memoryInfo">
            <div class="info-item">
              <span class="label">总内存</span>
              <span class="value">{{ snapshot.memoryInfo.total }}</span>
            </div>
            <div class="info-item">
              <span class="label">已使用</span>
              <span class="value">{{ snapshot.memoryInfo.used }}</span>
            </div>
            <div class="info-item">
              <span class="label">可用</span>
              <span class="value">{{ snapshot.memoryInfo.available }}</span>
            </div>
            <div class="info-item full-width">
              <el-progress 
                :percentage="snapshot.memoryInfo.usagePercent" 
                :stroke-width="20"
                :format="(p) => p.toFixed(1) + '%'"
                :color="getProgressColor(snapshot.memoryInfo.usagePercent)"
              />
            </div>
            <div class="info-item">
              <span class="label">交换区总量</span>
              <span class="value">{{ snapshot.memoryInfo.swapTotal }}</span>
            </div>
            <div class="info-item">
              <span class="label">交换区已用</span>
              <span class="value">{{ snapshot.memoryInfo.swapUsed }} ({{ formatPercent(snapshot.memoryInfo.swapUsagePercent) }})</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧面板 -->
      <div class="panel-column">
        <!-- 磁盘信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Files /></el-icon> 磁盘信息</span>
            </div>
          </template>
          <el-table :data="snapshot?.diskInfoList || []" size="small" max-height="200">
            <el-table-column prop="name" label="挂载点" width="120" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column prop="total" label="总容量" width="100" />
            <el-table-column prop="used" label="已使用" width="100" />
            <el-table-column label="使用率" width="150">
              <template #default="{ row }">
                <el-progress 
                  :percentage="row.usagePercent" 
                  :stroke-width="10"
                  :format="(p) => p.toFixed(1) + '%'"
                  :color="getProgressColor(row.usagePercent)"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 网络信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Connection /></el-icon> 网络接口</span>
            </div>
          </template>
          <el-table :data="snapshot?.networkInfoList || []" size="small" max-height="200">
            <el-table-column prop="displayName" label="网卡" min-width="150" show-overflow-tooltip />
            <el-table-column prop="ipv4Address" label="IP地址" width="130" />
            <el-table-column label="接收速度" width="100">
              <template #default="{ row }">{{ row.receiveSpeed }}</template>
            </el-table-column>
            <el-table-column label="发送速度" width="100">
              <template #default="{ row }">{{ row.sendSpeed }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.connected ? 'success' : 'info'" size="small">
                  {{ row.connected ? '已连接' : '断开' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 进程信息 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Operation /></el-icon> Top进程 (按CPU排序)</span>
            </div>
          </template>
          <el-table :data="snapshot?.processInfoList || []" size="small" max-height="250">
            <el-table-column prop="pid" label="PID" width="70" />
            <el-table-column prop="name" label="进程名" min-width="150" show-overflow-tooltip />
            <el-table-column prop="cpuPercent" label="CPU%" width="80">
              <template #default="{ row }">{{ row.cpuPercent.toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column prop="memoryPercent" label="内存%" width="80">
              <template #default="{ row }">{{ row.memoryPercent.toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column prop="memory" label="内存占用" width="100" />
            <el-table-column prop="threadCount" label="线程" width="60" />
            <el-table-column prop="user" label="用户" width="100" show-overflow-tooltip />
          </el-table>
        </el-card>
      </div>
    </div>

    <!-- 周期任务管理对话框 -->
    <el-dialog v-model="showTaskDialog" title="监控任务管理" width="700px">
      <div class="task-dialog-content">
        <div class="task-form">
          <el-form :model="newTask" inline>
            <el-form-item label="任务名称">
              <el-input v-model="newTask.name" placeholder="输入任务名称" style="width: 150px" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="newTask.type" style="width: 120px">
                <el-option label="单次检测" value="ONCE" />
                <el-option label="周期检测" value="PERIODIC" />
              </el-select>
            </el-form-item>
            <el-form-item label="间隔(秒)" v-if="newTask.type === 'PERIODIC'">
              <el-input-number v-model="newTask.intervalSeconds" :min="5" :max="3600" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createTask">创建任务</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <el-table :data="tasks" size="small">
          <el-table-column prop="name" label="任务名称" />
          <el-table-column prop="typeDescription" label="类型" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ row.statusDescription }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="intervalSeconds" label="间隔" width="70">
            <template #default="{ row }">
              {{ row.type === 'PERIODIC' ? row.intervalSeconds + 's' : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="executeCount" label="执行次数" width="80" />
          <el-table-column prop="lastResult" label="最后结果" width="90" show-overflow-tooltip />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button-group size="small">
                <el-button 
                  v-if="row.status === 'PENDING' || row.status === 'PAUSED'" 
                  type="success" 
                  @click="startTask(row.id)"
                >启动</el-button>
                <el-button 
                  v-if="row.status === 'RUNNING'" 
                  type="warning" 
                  @click="pauseTask(row.id)"
                >暂停</el-button>
                <el-button 
                  v-if="row.status === 'RUNNING' || row.status === 'PAUSED'" 
                  type="danger" 
                  @click="stopTask(row.id)"
                >停止</el-button>
                <el-button type="danger" @click="deleteTask(row.id)">删除</el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Refresh, VideoPlay, Timer, Download, ArrowDown,
  Cpu, Coin, Files, Operation, Monitor, Connection
} from '@element-plus/icons-vue'
import monitorApi from '@/api/monitor'

const loading = ref(false)
const detecting = ref(false)
const snapshot = ref(null)
const tasks = ref([])
const showTaskDialog = ref(false)
const refreshTimer = ref(null)

const newTask = ref({
  name: '',
  type: 'ONCE',
  intervalSeconds: 30
})

const healthTagType = computed(() => {
  const status = snapshot.value?.healthStatus
  if (status === '健康') return 'success'
  if (status === '警告') return 'warning'
  if (status === '严重') return 'danger'
  return 'info'
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const formatPercent = (value) => {
  if (value === undefined || value === null) return '0%'
  return value.toFixed(1) + '%'
}

const getProgressColor = (percent) => {
  if (percent >= 90) return '#F56C6C'
  if (percent >= 70) return '#E6A23C'
  return '#67C23A'
}

const getStatusType = (status) => {
  const map = {
    'PENDING': 'info',
    'RUNNING': 'success',
    'PAUSED': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

const refreshData = async () => {
  loading.value = true
  try {
    snapshot.value = await monitorApi.getSnapshot()
  } catch (e) {
    ElMessage.error('获取监控数据失败')
  } finally {
    loading.value = false
  }
}

const detectOnce = async () => {
  detecting.value = true
  try {
    snapshot.value = await monitorApi.detect()
    ElMessage.success('检测完成')
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    detecting.value = false
  }
}

const loadTasks = async () => {
  try {
    tasks.value = await monitorApi.getTasks()
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

const createTask = async () => {
  if (!newTask.value.name) {
    ElMessage.warning('请输入任务名称')
    return
  }
  try {
    await monitorApi.createTask(newTask.value)
    ElMessage.success('任务创建成功')
    newTask.value = { name: '', type: 'ONCE', intervalSeconds: 30 }
    loadTasks()
  } catch (e) {
    ElMessage.error('创建任务失败')
  }
}

const startTask = async (taskId) => {
  try {
    await monitorApi.startTask(taskId)
    ElMessage.success('任务已启动')
    loadTasks()
  } catch (e) {
    ElMessage.error('启动失败')
  }
}

const pauseTask = async (taskId) => {
  try {
    await monitorApi.pauseTask(taskId)
    ElMessage.success('任务已暂停')
    loadTasks()
  } catch (e) {
    ElMessage.error('暂停失败')
  }
}

const stopTask = async (taskId) => {
  try {
    await monitorApi.stopTask(taskId)
    ElMessage.success('任务已停止')
    loadTasks()
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const deleteTask = async (taskId) => {
  try {
    await monitorApi.deleteTask(taskId)
    ElMessage.success('任务已删除')
    loadTasks()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleExport = async (command) => {
  try {
    let blob
    let filename
    if (command === 'current') {
      blob = await monitorApi.exportReport()
      filename = `系统监控报告_${new Date().toISOString().slice(0,10)}.xlsx`
    } else {
      blob = await monitorApi.exportHistoryReport(50)
      filename = `监控历史报告_${new Date().toISOString().slice(0,10)}.xlsx`
    }
    
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  refreshData()
  loadTasks()
  // 每30秒自动刷新
  refreshTimer.value = setInterval(refreshData, 30000)
})

onUnmounted(() => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
})
</script>

<style scoped>
.monitor-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.update-time {
  color: #909399;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.cpu-card .card-icon { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.memory-card .card-icon { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.disk-card .card-icon { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.process-card .card-icon { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }

.card-content {
  flex: 1;
}

.card-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.card-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.card-desc {
  font-size: 12px;
  color: #909399;
}

.detail-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  border-radius: 8px;
}

.info-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: #fafafa;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item .label {
  font-size: 12px;
  color: #909399;
}

.info-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.task-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-form {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
}

@media (max-width: 1200px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .detail-panels {
    grid-template-columns: 1fr;
  }
}
</style>
