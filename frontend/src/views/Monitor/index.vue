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
        <el-button type="primary" @click="detectNow" :loading="detecting">
          <el-icon><VideoPlay /></el-icon>立即检测
        </el-button>
        <el-button type="success" @click="showTaskDialog = true">
          <el-icon><Timer /></el-icon>周期任务
        </el-button>
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
    <el-dialog v-model="showTaskDialog" title="周期任务管理" width="1000px">
      <div class="task-dialog-content">
        <!-- 顶部操作栏 -->
        <div class="task-operations">
          <el-form :model="newTask" inline>
            <el-form-item label="任务名称">
              <el-input 
                v-model="newTask.name" 
                placeholder="默认: sysmonitor+时间戳" 
                clearable
                style="width: 180px" 
              />
            </el-form-item>
            <el-form-item label="间隔(秒)">
              <el-input-number v-model="newTask.intervalSeconds" :min="5" :max="3600" style="width: 100px" />
            </el-form-item>
            <el-form-item label="监控次数">
              <el-input-number v-model="newTask.maxExecuteCount" :min="0" :max="9999" style="width: 100px" placeholder="0为无限" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createTask" :loading="creatingTask">
                <el-icon><Plus /></el-icon>创建任务
              </el-button>
            </el-form-item>
            
            <!-- 批量操作按钮 -->
            <el-form-item label="批量操作">
              <el-button-group>
                <el-button size="default" type="success" @click="batchStart" :disabled="selectedTasks.length === 0">
                  <el-icon><VideoPlay /></el-icon>启动
                </el-button>
                <el-button size="default" type="warning" @click="batchPause" :disabled="selectedTasks.length === 0">
                  <el-icon><VideoPause /></el-icon>暂停
                </el-button>
                <el-button size="default" type="info" @click="batchStop" :disabled="selectedTasks.length === 0">
                  <el-icon><SwitchButton /></el-icon>停止
                </el-button>
                <el-button size="default" type="danger" @click="batchDelete" :disabled="selectedTasks.length === 0">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
                <el-button size="default" type="primary" @click="batchExport" :disabled="selectedTasks.length === 0">
                  <el-icon><Download /></el-icon>导出
                </el-button>
              </el-button-group>
            </el-form-item>
            
            <!-- 选中数量提示 -->
            <el-form-item v-if="selectedTasks.length > 0">
              <el-tag type="info" size="large">已选 {{ selectedTasks.length }} 个</el-tag>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 任务表格 -->
        <div class="task-table-container">
          <el-table 
            ref="taskTableRef"
            :data="filteredTasks" 
            size="small" 
            stripe
            row-key="id"
            @selection-change="handleSelectionChange"
            :header-cell-style="{ background: '#fafafa', color: '#303133', fontWeight: '600' }"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="name" label="任务名称" min-width="150" show-overflow-tooltip sortable />
            <el-table-column 
              prop="status" 
              label="状态" 
              width="100"
              sortable
              :filters="statusFilters"
              :filter-method="filterStatus"
            >
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ row.statusDescription }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="intervalSeconds" label="间隔(秒)" width="90" sortable />
            <el-table-column label="执行次数" width="100" sortable>
              <template #default="{ row }">
                {{ row.executeCount }}{{ row.maxExecuteCount > 0 ? '/' + row.maxExecuteCount : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="lastResult" label="最后结果" width="100" show-overflow-tooltip />
            <el-table-column prop="lastExecuteTime" label="最后执行" width="160" sortable>
              <template #default="{ row }">
                {{ formatTime(row.lastExecuteTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
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
                    type="info" 
                    @click="stopTask(row.id)"
                  >停止</el-button>
                  <el-button type="primary" @click="exportSingleTask(row.id)">
                    <el-icon><Download /></el-icon>报告
                  </el-button>
                  <el-button type="danger" @click="deleteTask(row.id)">删除</el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-if="tasks.length === 0" description="暂无任务" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  VideoPlay, Timer, Download, ArrowDown,
  Cpu, Coin, Files, Operation, Monitor, Connection, Plus,
  VideoPause, SwitchButton, Delete
} from '@element-plus/icons-vue'
import monitorApi from '@/api/monitor'

const detecting = ref(false)
const snapshot = ref(null)
const tasks = ref([])
const showTaskDialog = ref(false)
const creatingTask = ref(false)
const taskRefreshTimer = ref(null)
const selectedTasks = ref([])
const selectedTaskIds = ref(new Set()) // 保存选中的任务ID
const taskTableRef = ref(null) // 表格引用
const isRestoringSelection = ref(false) // 是否正在恢复选中状态

const newTask = ref({
  name: '',
  type: 'PERIODIC',
  intervalSeconds: 10,
  maxExecuteCount: 60
})

const healthTagType = computed(() => {
  const status = snapshot.value?.healthStatus
  if (status === '健康') return 'success'
  if (status === '警告') return 'warning'
  if (status === '严重') return 'danger'
  return 'info'
})

// 任务状态筛选器
const statusFilters = computed(() => {
  const uniqueStatuses = [...new Set(tasks.value.map(t => t.statusDescription).filter(Boolean))]
  return uniqueStatuses.map(status => ({ text: status, value: status }))
})

// 筛选后的任务列表
const filteredTasks = computed(() => {
  return tasks.value
})

// 筛选方法
const filterStatus = (value, row) => {
  return row.statusDescription === value
}

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

const detectNow = async () => {
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
    // 保存当前选中的ID（刷新前）
    const previousSelectedIds = new Set(selectedTaskIds.value)
    
    tasks.value = await monitorApi.getTasks()
    
    // 刷新后恢复选中状态
    if (taskTableRef.value && previousSelectedIds.size > 0) {
      isRestoringSelection.value = true
      await nextTick()
      
      // 恢复选中状态
      tasks.value.forEach(task => {
        if (previousSelectedIds.has(task.id)) {
          taskTableRef.value.toggleRowSelection(task, true)
        }
      })
      
      // 恢复选中ID集合
      selectedTaskIds.value = previousSelectedIds
      // 恢复selectedTasks
      selectedTasks.value = tasks.value.filter(t => previousSelectedIds.has(t.id))
      
      isRestoringSelection.value = false
    }
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

// 生成可读的时间格式任务名
const generateTaskName = () => {
  const now = new Date()
  const pad = (n) => n.toString().padStart(2, '0')
  return `监控_${now.getFullYear()}${pad(now.getMonth()+1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

const createTask = async () => {
  creatingTask.value = true
  try {
    const taskData = {
      name: newTask.value.name || generateTaskName(),
      type: 'PERIODIC',
      intervalSeconds: newTask.value.intervalSeconds,
      maxExecuteCount: newTask.value.maxExecuteCount || 0
    }
    await monitorApi.createTask(taskData)
    ElMessage.success('任务创建成功')
    newTask.value = { name: '', type: 'PERIODIC', intervalSeconds: 10, maxExecuteCount: 60 }
    await loadTasks()
  } catch (e) {
    ElMessage.error('创建任务失败')
  } finally {
    creatingTask.value = false
  }
}

// 单个任务操作（调用批量接口）
const startTask = async (taskId) => {
  try {
    await monitorApi.startTasks([taskId])
    ElMessage.success('任务已启动')
    await loadTasks()
  } catch (e) {
    ElMessage.error('启动失败')
  }
}

const pauseTask = async (taskId) => {
  try {
    await monitorApi.pauseTasks([taskId])
    ElMessage.success('任务已暂停')
    await loadTasks()
  } catch (e) {
    ElMessage.error('暂停失败')
  }
}

const stopTask = async (taskId) => {
  try {
    await monitorApi.stopTasks([taskId])
    ElMessage.success('任务已停止')
    await loadTasks()
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const deleteTask = async (taskId) => {
  try {
    await monitorApi.deleteTasks([taskId])
    ElMessage.success('任务已删除')
    await loadTasks()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 选择变化处理
const handleSelectionChange = (selection) => {
  // 如果正在恢复选中状态，忽略此次事件
  if (isRestoringSelection.value) {
    return
  }
  selectedTasks.value = selection
  // 同步更新选中ID集合
  selectedTaskIds.value = new Set(selection.map(t => t.id))
}

// 批量启动
const batchStart = async () => {
  if (selectedTasks.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确认启动 ${selectedTasks.value.length} 个任务？`, '批量启动', {
      type: 'warning'
    })
    
    const taskIds = selectedTasks.value.map(task => task.id)
    await monitorApi.startTasks(taskIds)
    ElMessage.success(`批量启动成功`)
    await loadTasks()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量启动失败')
    }
  }
}

// 批量暂停
const batchPause = async () => {
  if (selectedTasks.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确认暂停 ${selectedTasks.value.length} 个任务？`, '批量暂停', {
      type: 'warning'
    })
    
    const taskIds = selectedTasks.value.map(task => task.id)
    await monitorApi.pauseTasks(taskIds)
    ElMessage.success(`批量暂停成功`)
    await loadTasks()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量暂停失败')
    }
  }
}

// 批量停止
const batchStop = async () => {
  if (selectedTasks.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确认停止 ${selectedTasks.value.length} 个任务？`, '批量停止', {
      type: 'warning'
    })
    
    const taskIds = selectedTasks.value.map(task => task.id)
    await monitorApi.stopTasks(taskIds)
    ElMessage.success(`批量停止成功`)
    await loadTasks()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量停止失败')
    }
  }
}

// 批量删除
const batchDelete = async () => {
  if (selectedTasks.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedTasks.value.length} 个任务？此操作不可恢复！`, 
      '批量删除', 
      {
        type: 'error',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    
    const taskIds = selectedTasks.value.map(task => task.id)
    await monitorApi.deleteTasks(taskIds)
    ElMessage.success(`批量删除成功`)
    // 清空选中状态
    selectedTasks.value = []
    selectedTaskIds.value.clear()
    await loadTasks()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 批量导出报告
const batchExport = async () => {
  if (selectedTasks.value.length === 0) return
  
  try {
    const taskIds = selectedTasks.value.map(t => t.id)
    const blob = await monitorApi.exportTaskReports(taskIds)
    const filename = `任务报告_${selectedTasks.value.length}个_${new Date().toISOString().slice(0,10)}.xlsx`
    
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

// 导出单个任务报告（调用批量接口）
const exportSingleTask = async (taskId) => {
  try {
    const blob = await monitorApi.exportTaskReports([taskId])
    const task = tasks.value.find(t => t.id === taskId)
    const filename = `任务报告_${task?.name || taskId}_${new Date().toISOString().slice(0,10)}.xlsx`
    
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

onMounted(async () => {
  // 初始化加载数据
  try {
    snapshot.value = await monitorApi.getSnapshot()
  } catch (e) {
    console.error('获取监控快照失败', e)
  }
  await loadTasks()
  
  // 每3秒自动刷新任务状态
  taskRefreshTimer.value = setInterval(async () => {
    if (showTaskDialog.value) {
      await loadTasks()
    }
  }, 3000)
})

onUnmounted(() => {
  if (taskRefreshTimer.value) {
    clearInterval(taskRefreshTimer.value)
  }
})
</script>

<style scoped>
.monitor-page {
  padding: 20px;
  padding-bottom: 40px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  overflow-y: auto;
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

.task-operations {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.task-operations :deep(.el-form) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.task-operations :deep(.el-form-item) {
  margin-bottom: 0;
}

.task-table-container {
  margin-top: 0;
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
