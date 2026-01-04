<template>
  <div class="ssh-container">
    <!-- Tab导航 -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 设备管理 -->
      <el-tab-pane label="设备管理" name="devices">
        <div class="tab-header">
          <el-button type="primary" @click="showDeviceDialog()">添加设备</el-button>
          <el-button type="success" @click="showBatchImportDialog">批量导入</el-button>
          <el-button @click="exportDevices" :disabled="devices.length === 0">导出设备</el-button>
          <el-button @click="batchTestDevices" :disabled="selectedDevices.length === 0">
            批量测试连接
          </el-button>
        </div>
        
        <el-table 
          :data="devices" 
          v-loading="loadingDevices"
          @selection-change="handleDeviceSelectionChange"
          style="width: 100%"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="name" label="设备名称" width="150" />
          <el-table-column prop="host" label="IP地址" width="130" />
          <el-table-column prop="port" label="端口" width="80" />
          <el-table-column prop="username" label="用户名" width="100" />
          <el-table-column prop="groupName" label="分组" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastConnectTime" label="最后连接" width="160">
            <template #default="{ row }">
              {{ formatTime(row.lastConnectTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="testDevice(row)">测试</el-button>
              <el-button size="small" @click="showDeviceDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteDevice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <!-- 批量命令 -->
      <el-tab-pane label="批量命令" name="command">
        <div class="command-panel">
          <!-- 命令输入区 -->
          <div class="command-header">
            <div class="command-input-with-history">
              <el-input
                v-model="command"
                type="textarea"
                :rows="2"
                placeholder="输入要执行的命令，如: ls -la /home"
              />
              <el-dropdown v-if="recentCommands.length > 0" trigger="click" @command="selectRecentCommand">
                <el-button type="info" plain size="small">
                  历史命令 <el-icon><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item 
                      v-for="(cmd, idx) in recentCommands" 
                      :key="idx" 
                      :command="cmd"
                    >
                      <span class="history-cmd">{{ truncateCommand(cmd) }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="command-actions">
              <el-button 
                type="primary" 
                @click="executeCommand" 
                :loading="executing"
                :disabled="commandSelectedIds.length === 0 || !command"
              >
                执行命令
              </el-button>
              <el-button 
                type="danger" 
                @click="stopCommand"
                :disabled="!executing"
              >
                停止
              </el-button>
              <el-button 
                @click="exportCommandReport"
                :disabled="commandResults.length === 0"
              >
                导出报告
              </el-button>
            </div>
          </div>
          
          <!-- 设备/任务列表 -->
          <div class="command-results-table">
            <el-table 
              :data="filteredCommandTableData" 
              style="width: 100%" 
              v-loading="executing"
              @selection-change="handleCommandSelectionChange"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="host" label="设备IP" width="130" sortable />
              <el-table-column prop="name" label="设备名称" width="130" sortable />
              <el-table-column prop="deviceStatus" label="状态" width="80" sortable
                :filters="[{ text: '在线', value: 'ONLINE' }, { text: '未知', value: 'UNKNOWN' }]"
                :filter-method="filterDeviceStatus"
              >
                <template #default="{ row }">
                  <el-tag :type="row.deviceStatus === 'ONLINE' ? 'success' : 'info'" size="small">
                    {{ row.deviceStatus === 'ONLINE' ? '在线' : '未知' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="taskStatus" label="任务" width="80" sortable
                :filters="[{ text: '成功', value: 'SUCCESS' }, { text: '失败', value: 'FAILED' }, { text: '执行中', value: 'RUNNING' }, { text: '待执行', value: 'PENDING' }]"
                :filter-method="filterTaskStatus"
              >
                <template #default="{ row }">
                  <el-tag v-if="row.taskStatus" :type="getTaskStatusType(row.taskStatus)" size="small">
                    {{ getTaskStatusText(row.taskStatus) }}
                  </el-tag>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" width="160">
                <template #default="{ row }">
                  {{ row.startTime ? formatTime(row.startTime) : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="durationMs" label="耗时" width="100">
                <template #default="{ row }">
                  {{ formatDuration(row.durationMs) }}
                </template>
              </el-table-column>
              <el-table-column label="执行结果" min-width="200">
                <template #default="{ row }">
                  <el-popover 
                    v-if="row.output || row.errorMessage"
                    placement="left"
                    :width="500"
                    trigger="hover"
                    :show-after="300"
                  >
                    <template #reference>
                      <div class="output-preview">
                        {{ truncateOutput(row.output || row.errorMessage) }}
                      </div>
                    </template>
                    <pre class="output-popover">{{ row.output || row.errorMessage || '无输出' }}</pre>
                  </el-popover>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
      
      <!-- 文件传输 -->
      <el-tab-pane label="文件传输" name="file">
        <div class="file-panel">
          <!-- 文件传输输入区 -->
          <div class="command-header">
            <div class="file-input-row">
              <el-radio-group v-model="fileMode">
                <el-radio-button label="upload">上传</el-radio-button>
                <el-radio-button label="download">下载</el-radio-button>
              </el-radio-group>
              <template v-if="fileMode === 'upload'">
                <el-upload
                  ref="fileUploadRef"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleFileSelect"
                  :on-exceed="handleFileExceed"
                  :limit="1"
                >
                  <el-button type="primary" plain>选择文件</el-button>
                </el-upload>
                <span v-if="selectedFile" class="selected-file">
                  {{ selectedFile.name }}
                  <el-button type="danger" link size="small" @click="clearSelectedFile">×</el-button>
                </span>
              </template>
              <el-input v-else v-model="localPath" placeholder="本地保存路径" style="width: 250px" />
              <el-input v-model="remotePath" placeholder="远程服务器路径" style="width: 250px" />
            </div>
            <div class="command-actions">
              <el-button 
                type="primary" 
                @click="executeFileTransfer" 
                :loading="fileExecuting"
                :disabled="fileSelectedIds.length === 0 || !remotePath || (fileMode === 'upload' ? !selectedFile : !localPath)"
              >
                {{ fileMode === 'upload' ? '上传' : '下载' }}
              </el-button>
              <el-button 
                type="danger" 
                @click="stopFileTransfer"
                :disabled="!fileExecuting"
              >
                停止
              </el-button>
              <el-button 
                @click="exportFileReport"
                :disabled="fileResults.length === 0"
              >
                导出报告
              </el-button>
            </div>
          </div>
          
          <!-- 设备/任务列表 -->
          <div class="command-results-table">
            <el-table 
              :data="filteredFileTableData" 
              style="width: 100%" 
              @selection-change="handleFileSelectionChange"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="host" label="设备IP" width="130" sortable />
              <el-table-column prop="name" label="设备名称" width="130" sortable />
              <el-table-column prop="deviceStatus" label="状态" width="80" sortable
                :filters="[{ text: '在线', value: 'ONLINE' }, { text: '未知', value: 'UNKNOWN' }]"
                :filter-method="filterDeviceStatus"
              >
                <template #default="{ row }">
                  <el-tag :type="row.deviceStatus === 'ONLINE' ? 'success' : 'info'" size="small">
                    {{ row.deviceStatus === 'ONLINE' ? '在线' : '未知' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="taskStatus" label="任务" width="80" sortable
                :filters="[{ text: '成功', value: 'SUCCESS' }, { text: '失败', value: 'FAILED' }, { text: '执行中', value: 'RUNNING' }, { text: '待执行', value: 'PENDING' }, { text: '已完成', value: 'COMPLETED' }, { text: '已取消', value: 'CANCELLED' }]"
                :filter-method="filterTaskStatus"
              >
                <template #default="{ row }">
                  <el-tag v-if="row.taskStatus" :type="getTaskStatusType(row.taskStatus)" size="small">
                    {{ getTaskStatusText(row.taskStatus) }}
                  </el-tag>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" width="160" sortable>
                <template #default="{ row }">
                  {{ row.startTime ? formatTime(row.startTime) : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="传输进度" width="180">
                <template #default="{ row }">
                  <div v-if="row.transferId && row.taskStatus === 'RUNNING'" class="progress-cell">
                    <el-progress 
                      :percentage="row.percentage || 0" 
                      :stroke-width="10"
                      :format="(p) => `${p}%`"
                    />
                    <div class="transfer-info">
                      <span>{{ formatBytes(row.transferredBytes || 0) }} / {{ formatBytes(row.totalBytes || 0) }}</span>
                      <span v-if="row.speed">{{ formatBytes(row.speed) }}/s</span>
                    </div>
                  </div>
                  <el-tag v-else-if="row.taskStatus === 'COMPLETED'" type="success" size="small">已完成</el-tag>
                  <el-tag v-else-if="row.taskStatus === 'PAUSED'" type="warning" size="small">
                    已暂停
                    <el-button type="primary" link size="small" @click="resumeTransfer(row)">续传</el-button>
                  </el-tag>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="durationMs" label="耗时" width="100" sortable>
                <template #default="{ row }">
                  {{ formatDuration(row.durationMs) }}
                </template>
              </el-table-column>
              <el-table-column label="详情" min-width="200">
                <template #default="{ row }">
                  <el-popover 
                    v-if="row.logs && row.logs.length > 0"
                    placement="left"
                    :width="500"
                    trigger="hover"
                    :show-after="300"
                  >
                    <template #reference>
                      <el-button type="primary" link size="small">查看详情 ({{ row.logs.length }})</el-button>
                    </template>
                    <div class="logs-popover">
                      <div v-for="(log, idx) in row.logs" :key="idx" class="log-item">
                        {{ log }}
                      </div>
                    </div>
                  </el-popover>
                  <el-popover 
                    v-else-if="row.errorMessage"
                    placement="left"
                    :width="400"
                    trigger="hover"
                    :show-after="300"
                  >
                    <template #reference>
                      <el-button type="danger" link size="small">查看错误</el-button>
                    </template>
                    <pre class="output-popover error-text">{{ row.errorMessage }}</pre>
                  </el-popover>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
      
      <!-- 任务历史 -->
      <el-tab-pane label="任务历史" name="history">
        <el-table :data="taskHistory" v-loading="loadingHistory" style="width: 100%">
          <el-table-column prop="name" label="任务名称" width="200" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              {{ getTaskTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="deviceCount" label="设备数" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusType(row.status)" size="small">
                {{ getTaskStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="viewTaskDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 设备编辑对话框 -->
    <el-dialog 
      v-model="deviceDialogVisible" 
      :title="editingDevice ? '编辑设备' : '添加设备'"
      width="500px"
    >
      <el-form :model="deviceForm" label-width="100px">
        <el-form-item label="设备名称" required>
          <el-input v-model="deviceForm.name" placeholder="设备名称" />
        </el-form-item>
        <el-form-item label="IP地址" required>
          <el-input v-model="deviceForm.host" placeholder="192.168.1.1" />
        </el-form-item>
        <el-form-item label="SSH端口" required>
          <el-input-number v-model="deviceForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="deviceForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="认证方式">
          <el-radio-group v-model="deviceForm.authType">
            <el-radio label="PASSWORD">密码认证</el-radio>
            <el-radio label="KEY">密钥认证</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="密码" v-if="deviceForm.authType === 'PASSWORD'">
          <el-input v-model="deviceForm.password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-form-item label="私钥" v-if="deviceForm.authType === 'KEY'">
          <el-input v-model="deviceForm.privateKey" type="textarea" :rows="4" placeholder="粘贴私钥内容" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="deviceForm.groupName" placeholder="设备分组（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="deviceForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDevice" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 批量导入对话框 -->
    <el-dialog 
      v-model="batchImportVisible" 
      title="批量导入设备"
      width="700px"
    >
      <el-alert 
        type="info" 
        :closable="false" 
        style="margin-bottom: 15px"
      >
        <template #title>
          <div>
            <p style="margin: 0">请按以下格式输入设备信息，每行一个设备：</p>
            <code style="background: #f5f5f5; padding: 2px 5px; border-radius: 3px; display: block; margin-top: 8px">
              设备名称,IP地址,端口,用户名,密码,分组(可选)
            </code>
            <p style="margin: 8px 0 0 0; color: #909399">示例：web-server-01,192.168.1.100,22,root,password123,生产环境</p>
          </div>
        </template>
      </el-alert>
      
      <el-input
        v-model="batchImportText"
        type="textarea"
        :rows="10"
        placeholder="粘贴或输入设备信息..."
      />
      
      <div style="margin-top: 15px">
        <el-button type="primary" plain size="small" @click="downloadTemplate">下载模板</el-button>
        <span style="margin-left: 10px; color: #909399; font-size: 12px">解析到 {{ parsedDevices.length }} 个设备</span>
      </div>
      
      <template #footer>
        <el-button @click="batchImportVisible = false">取消</el-button>
        <el-button type="primary" @click="doBatchImport" :loading="batchImporting" :disabled="parsedDevices.length === 0">
          导入 ({{ parsedDevices.length }})
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 任务详情对话框 -->
    <el-dialog v-model="taskDetailVisible" title="任务详情" width="800px">
      <template v-if="selectedTask">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名称">{{ selectedTask.name }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ getTaskTypeText(selectedTask.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getTaskStatusType(selectedTask.status)">
              {{ getTaskStatusText(selectedTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备数">{{ selectedTask.deviceCount }}</el-descriptions-item>
          <el-descriptions-item label="命令" v-if="selectedTask.command">
            <code>{{ selectedTask.command }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(selectedTask.createTime) }}</el-descriptions-item>
        </el-descriptions>
        
        <h4 style="margin-top: 20px">执行结果</h4>
        <el-table :data="selectedTask.results" style="width: 100%">
          <el-table-column prop="deviceName" label="设备" width="150" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时" width="100">
            <template #default="{ row }">
              {{ row.durationMs }}ms
            </template>
          </el-table-column>
          <el-table-column label="输出">
            <template #default="{ row }">
              <pre class="output-small">{{ row.output || row.errorMessage }}</pre>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { deviceApi, sshTaskApi, transferApi } from '@/api/ssh'

// Tab状态
const activeTab = ref('devices')

// 设备相关
const devices = ref([])
const loadingDevices = ref(false)
const selectedDevices = ref([])
const deviceDialogVisible = ref(false)
const editingDevice = ref(null)
const saving = ref(false)

// 批量导入相关
const batchImportVisible = ref(false)
const batchImportText = ref('')
const batchImporting = ref(false)
const parsedDevices = ref([])

const deviceForm = ref({
  name: '',
  host: '',
  port: 22,
  username: '',
  password: '',
  privateKey: '',
  authType: 'PASSWORD',
  groupName: '',
  remark: ''
})

// 命令相关
const commandSelectedIds = ref([])
const command = ref('')
const executing = ref(false)
const commandResults = ref([])
const commandTaskMap = ref({})  // 设备ID -> 任务结果映射
const recentCommands = ref([])  // 最近10次命令历史
const RECENT_COMMANDS_KEY = 'ssh_recent_commands'

// 文件传输相关
const fileMode = ref('upload')
const localPath = ref('')
const remotePath = ref('')
const fileResults = ref([])
const fileSelectedIds = ref([])
const fileExecuting = ref(false)
const fileTaskMap = ref({})  // 设备ID -> 任务结果映射
const selectedFile = ref(null)  // 上传文件选择
const fileUploadRef = ref(null)
const transferIds = ref([])  // 当前活跃的传输ID列表
const progressPollingTimer = ref(null)  // 进度轮询定时器

// 筛选器（已移除，改用表格内置筛选）

// 批量命令表格数据（设备列表 + 任务结果）
const commandTableData = computed(() => {
  return devices.value.map(device => {
    const taskResult = commandTaskMap.value[device.id] || {}
    return {
      ...device,
      deviceStatus: device.status,
      taskStatus: taskResult.status || null,
      startTime: taskResult.startTime || null,
      durationMs: taskResult.durationMs || null,
      output: taskResult.output || null,
      errorMessage: taskResult.errorMessage || null
    }
  })
})

// 文件传输表格数据（设备列表 + 任务结果 + 进度信息）
const fileTableData = computed(() => {
  return devices.value.map(device => {
    const taskResult = fileTaskMap.value[device.id] || {}
    return {
      ...device,
      deviceStatus: device.status,
      taskStatus: taskResult.status || null,
      startTime: taskResult.startTime || null,
      durationMs: taskResult.durationMs || null,
      output: taskResult.output || null,
      errorMessage: taskResult.errorMessage || null,
      // 进度信息
      transferId: taskResult.transferId || null,
      percentage: taskResult.percentage || 0,
      transferredBytes: taskResult.transferredBytes || 0,
      totalBytes: taskResult.totalBytes || 0,
      speed: taskResult.speed || 0,
      resumable: taskResult.resumable || false,
      // 执行日志详情
      logs: taskResult.logs || []
    }
  })
})

// 直接使用表格数据（表格内置筛选功能）
const filteredCommandTableData = computed(() => commandTableData.value)
const filteredFileTableData = computed(() => fileTableData.value)

// 筛选方法
const filterDeviceStatus = (value, row) => {
  return row.deviceStatus === value
}

const filterTaskStatus = (value, row) => {
  return row.taskStatus === value
}

// 文件选择处理
const handleFileSelect = (file) => {
  selectedFile.value = file.raw
  localPath.value = file.name
}

// 文件超出限制时替换
const handleFileExceed = (files) => {
  // 清除旧文件，选择新文件
  if (fileUploadRef.value) {
    fileUploadRef.value.clearFiles()
  }
  const file = files[0]
  selectedFile.value = file
  localPath.value = file.name
}

// 清除已选文件
const clearSelectedFile = () => {
  selectedFile.value = null
  localPath.value = ''
  if (fileUploadRef.value) {
    fileUploadRef.value.clearFiles()
  }
}

// 处理批量命令选择变化
const handleCommandSelectionChange = (selection) => {
  commandSelectedIds.value = selection.map(d => d.id)
}

// 处理文件传输选择变化
const handleFileSelectionChange = (selection) => {
  fileSelectedIds.value = selection.map(d => d.id)
}

// 任务历史
const taskHistory = ref([])
const loadingHistory = ref(false)
const taskDetailVisible = ref(false)
const selectedTask = ref(null)

// 加载设备列表
const loadDevices = async () => {
  loadingDevices.value = true
  try {
    const res = await deviceApi.getAll()
    devices.value = res.data || res
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  } finally {
    loadingDevices.value = false
  }
}

// 显示设备编辑对话框
const showDeviceDialog = (device = null) => {
  editingDevice.value = device
  if (device) {
    deviceForm.value = { ...device }
  } else {
    deviceForm.value = {
      name: '',
      host: '',
      port: 22,
      username: '',
      password: '',
      privateKey: '',
      authType: 'PASSWORD',
      groupName: '',
      remark: ''
    }
  }
  deviceDialogVisible.value = true
}

// 保存设备
const saveDevice = async () => {
  saving.value = true
  try {
    if (editingDevice.value) {
      await deviceApi.update(editingDevice.value.id, deviceForm.value)
      ElMessage.success('设备更新成功')
    } else {
      await deviceApi.create(deviceForm.value)
      ElMessage.success('设备添加成功')
    }
    deviceDialogVisible.value = false
    await loadDevices()
  } catch (e) {
    ElMessage.error('保存设备失败')
  } finally {
    saving.value = false
  }
}

// 显示批量导入对话框
const showBatchImportDialog = () => {
  batchImportText.value = ''
  parsedDevices.value = []
  batchImportVisible.value = true
}

// 监听批量导入文本变化，实时解析
watch(batchImportText, (text) => {
  if (!text.trim()) {
    parsedDevices.value = []
    return
  }
  const lines = text.split('\n').filter(line => line.trim())
  parsedDevices.value = lines.map(line => {
    const parts = line.split(',')
    if (parts.length >= 5) {
      return {
        name: parts[0]?.trim() || '',
        host: parts[1]?.trim() || '',
        port: parseInt(parts[2]?.trim()) || 22,
        username: parts[3]?.trim() || '',
        password: parts[4]?.trim() || '',
        groupName: parts[5]?.trim() || '',
        authType: 'PASSWORD'
      }
    }
    return null
  }).filter(d => d && d.name && d.host && d.username)
})

// 执行批量导入
const doBatchImport = async () => {
  if (parsedDevices.value.length === 0) {
    ElMessage.warning('没有可导入的设备')
    return
  }
  batchImporting.value = true
  try {
    const res = await deviceApi.batchCreate(parsedDevices.value)
    const data = res.data || res
    ElMessage.success(`成功导入 ${data.length} 个设备`)
    batchImportVisible.value = false
    await loadDevices()
  } catch (e) {
    ElMessage.error('批量导入失败: ' + (e.response?.data?.message || e.message))
  } finally {
    batchImporting.value = false
  }
}

// 下载模板
const downloadTemplate = () => {
  const template = '设备名称,IP地址,端口,用户名,密码,分组\nweb-server-01,192.168.1.100,22,root,password123,生产环境\ndb-server-01,192.168.1.101,22,root,password456,生产环境'
  const blob = new Blob([template], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'device_import_template.csv'
  link.click()
}

// 导出设备（不含密码）
const exportDevices = async () => {
  if (devices.value.length === 0) {
    ElMessage.warning('没有可导出的设备')
    return
  }
  try {
    const res = await deviceApi.exportDevices()
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `devices_export_${new Date().toISOString().slice(0,10)}.csv`
    link.click()
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 删除设备
const deleteDevice = async (device) => {
  try {
    await ElMessageBox.confirm(`确定删除设备 "${device.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    await deviceApi.delete(device.id)
    ElMessage.success('删除成功')
    loadDevices()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 测试设备连接
const testDevice = async (device) => {
  try {
    const res = await deviceApi.testConnection(device.id)
    const data = res.data || res
    if (data.status === 'ONLINE') {
      ElMessage.success('连接成功')
    } else {
      ElMessage.warning(`连接失败: ${data.lastConnectResult}`)
    }
    await loadDevices()
  } catch (e) {
    ElMessage.error('测试连接失败')
  }
}

// 设备选择变化
const handleDeviceSelectionChange = (selection) => {
  selectedDevices.value = selection
}

// 批量测试设备
const batchTestDevices = async () => {
  try {
    const deviceIds = selectedDevices.value.map(d => d.id)
    await deviceApi.batchTestConnection(deviceIds)
    ElMessage.success('批量测试完成')
    loadDevices()
  } catch (e) {
    ElMessage.error('批量测试失败')
  }
}

// 执行命令
const executeCommand = async () => {
  executing.value = true
  commandResults.value = []
  
  // 保存到历史命令
  saveCommandToHistory(command.value)
  
  // 清空上次结果，并给选中设备设置待执行状态
  const newTaskMap = {}
  commandSelectedIds.value.forEach(id => {
    newTaskMap[id] = { status: 'RUNNING', startTime: new Date().toISOString() }
  })
  commandTaskMap.value = newTaskMap
  
  try {
    const res = await sshTaskApi.executeCommand({
      name: `命令执行_${new Date().toLocaleString()}`,
      command: command.value,
      deviceIds: commandSelectedIds.value
    })
    const data = res.data || res
    commandResults.value = data.results || []
    
    // 更新任务结果映射
    const resultMap = { ...commandTaskMap.value }
    commandResults.value.forEach(r => {
      resultMap[r.deviceId] = {
        status: r.status,
        startTime: r.startTime || newTaskMap[r.deviceId]?.startTime,
        durationMs: r.durationMs,
        output: r.output,
        errorMessage: r.errorMessage
      }
    })
    commandTaskMap.value = resultMap
    
    ElMessage.success('命令执行完成')
    loadTaskHistory()
  } catch (e) {
    ElMessage.error('命令执行失败')
  } finally {
    executing.value = false
  }
}

// 停止命令
const stopCommand = () => {
  executing.value = false
  ElMessage.info('已发送停止请求')
}

// 导出命令执行报告
const exportCommandReport = () => {
  const data = commandTableData.value.filter(d => d.taskStatus)
  if (data.length === 0) {
    ElMessage.warning('没有可导出的执行结果')
    return
  }
  
  // 生成Excel内容
  let csv = '\uFEFF'  // UTF-8 BOM
  csv += '设备IP,设备名称,任务状态,开始时间,耗时(ms),执行结果\n'
  data.forEach(row => {
    csv += `"${row.host}","${row.name}","${getTaskStatusText(row.taskStatus)}","${row.startTime ? formatTime(row.startTime) : ''}","${row.durationMs || ''}","${(row.output || row.errorMessage || '').replace(/"/g, '""')}"\n`
  })
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `命令执行报告_${new Date().toISOString().slice(0,10)}.csv`
  link.click()
  ElMessage.success('报告导出成功')
}

// 执行文件传输（支持进度显示）
const executeFileTransfer = async () => {
  fileExecuting.value = true
  fileResults.value = []
  
  // 清空上次结果，并给选中设备设置待执行状态
  const newTaskMap = {}
  fileSelectedIds.value.forEach(id => {
    newTaskMap[id] = { status: 'PENDING', startTime: new Date().toISOString() }
  })
  fileTaskMap.value = newTaskMap
  
  try {
    let res
    if (fileMode.value === 'upload') {
      // 上传使用新的传输API
      res = await transferApi.upload(selectedFile.value, remotePath.value, fileSelectedIds.value)
    } else {
      // 下载使用新的传输API
      res = await transferApi.download({
        remotePath: remotePath.value,
        localPath: localPath.value,
        deviceIds: fileSelectedIds.value
      })
    }
    
    const data = res.data || res
    transferIds.value = data.transferIds || []
    console.log('[上传开始] transferIds:', transferIds.value)
    
    // 初始化任务结果映射
    const progresses = data.progresses || []
    const resultMap = { ...fileTaskMap.value }
    progresses.forEach(p => {
      resultMap[p.deviceId] = {
        transferId: p.transferId,
        status: p.status,
        startTime: p.startTime,
        percentage: p.percentage,
        transferredBytes: p.transferredBytes,
        totalBytes: p.totalBytes,
        speed: p.speed,
        resumable: p.resumable
      }
    })
    fileTaskMap.value = resultMap
    
    // 启动进度轮询
    startProgressPolling()
    
  } catch (e) {
    ElMessage.error('文件传输失败: ' + (e.message || '未知错误'))
    fileExecuting.value = false
  }
}

// 启动进度轮询
const startProgressPolling = () => {
  stopProgressPolling()
  
  progressPollingTimer.value = setInterval(async () => {
    if (transferIds.value.length === 0) {
      stopProgressPolling()
      return
    }
    
    try {
      const res = await transferApi.getBatchProgress(transferIds.value)
      const progresses = res.data || res || []
      
      console.log('[进度轮询] 获取到进度数据:', progresses)
      
      const resultMap = { ...fileTaskMap.value }
      let allCompleted = true
      
      progresses.forEach(p => {
        resultMap[p.deviceId] = {
          transferId: p.transferId,
          status: p.status,
          startTime: p.startTime,
          percentage: p.percentage,
          transferredBytes: p.transferredBytes,
          totalBytes: p.totalBytes,
          speed: p.speed,
          durationMs: p.status === 'COMPLETED' ? (Date.now() - new Date(p.startTime).getTime()) : null,
          errorMessage: p.errorMessage,
          resumable: p.resumable,
          logs: p.logs || []
        }
        
        // 只有正在运行的任务才认为未完成
        if (p.status === 'RUNNING' || p.status === 'PENDING') {
          allCompleted = false
        }
      })
      
      fileTaskMap.value = resultMap
      console.log('[进度轮询] 更新 fileTaskMap:', fileTaskMap.value)
      console.log('[进度轮询] allCompleted:', allCompleted, 'progresses.length:', progresses.length)
      
      // 所有任务完成时停止轮询（包括成功、失败、取消等终止状态）
      if (allCompleted && progresses.length > 0) {
        console.log('[进度轮询] 所有任务完成，停止轮询')
        stopProgressPolling()
        fileExecuting.value = false
        
        // 检查是否有失败的任务
        const hasFailures = progresses.some(p => p.status === 'FAILED' || p.status === 'CANCELLED')
        if (hasFailures) {
          ElMessage.warning('文件传输完成，但有任务失败')
        } else {
          ElMessage.success('文件传输完成')
        }
        loadTaskHistory()
      }
    } catch (e) {
      console.error('获取进度失败:', e)
    }
  }, 1000)  // 每秒轮询一次
}

// 停止进度轮询
const stopProgressPolling = () => {
  if (progressPollingTimer.value) {
    clearInterval(progressPollingTimer.value)
    progressPollingTimer.value = null
  }
}

// 停止文件传输
const stopFileTransfer = async () => {
  // 取消所有活跃传输
  for (const tid of transferIds.value) {
    try {
      await transferApi.cancel(tid)
    } catch (e) {
      console.error('取消传输失败:', tid, e)
    }
  }
  
  // 更新表格中的状态
  const resultMap = { ...fileTaskMap.value }
  for (const deviceId in resultMap) {
    if (resultMap[deviceId].status === 'RUNNING' || resultMap[deviceId].status === 'PENDING') {
      resultMap[deviceId].status = 'CANCELLED'
      resultMap[deviceId].taskStatus = 'CANCELLED'
    }
  }
  fileTaskMap.value = resultMap
  
  stopProgressPolling()
  transferIds.value = []
  fileExecuting.value = false
  ElMessage.info('已停止文件传输')
}

// 恢复传输（断点续传）
const resumeTransfer = async (row) => {
  try {
    const res = await transferApi.resume({
      deviceId: row.id,
      remotePath: remotePath.value,
      localPath: localPath.value
    })
    const data = res.data || res
    
    // 添加到活跃传输列表
    transferIds.value.push(data.transferId)
    
    // 更新任务状态
    const resultMap = { ...fileTaskMap.value }
    resultMap[row.id] = {
      ...resultMap[row.id],
      ...data.progress,
      status: 'RUNNING'
    }
    fileTaskMap.value = resultMap
    
    // 启动进度轮询
    fileExecuting.value = true
    startProgressPolling()
    
    ElMessage.success('已恢复传输')
  } catch (e) {
    ElMessage.error('恢复传输失败')
  }
}

// 格式化字节数
const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 导出文件传输报告
const exportFileReport = () => {
  const data = fileTableData.value.filter(d => d.taskStatus)
  if (data.length === 0) {
    ElMessage.warning('没有可导出的执行结果')
    return
  }
  
  // 生成Excel内容
  let csv = '\uFEFF'  // UTF-8 BOM
  csv += '设备IP,设备名称,任务状态,开始时间,耗时(ms),执行结果\n'
  data.forEach(row => {
    csv += `"${row.host}","${row.name}","${getTaskStatusText(row.taskStatus)}","${row.startTime ? formatTime(row.startTime) : ''}","${row.durationMs || ''}","${(row.output || row.errorMessage || '').replace(/"/g, '""')}"\n`
  })
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `文件传输报告_${new Date().toISOString().slice(0,10)}.csv`
  link.click()
  ElMessage.success('报告导出成功')
}

// 加载任务历史
const loadTaskHistory = async () => {
  loadingHistory.value = true
  try {
    const res = await sshTaskApi.getRecent(50)
    taskHistory.value = res.data || res || []
  } catch (e) {
    ElMessage.error('加载任务历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 查看任务详情
const viewTaskDetail = (task) => {
  selectedTask.value = task
  taskDetailVisible.value = true
}

// 工具函数
const getStatusType = (status) => {
  switch (status) {
    case 'ONLINE': return 'success'
    case 'OFFLINE': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'ONLINE': return '在线'
    case 'OFFLINE': return '离线'
    default: return '未知'
  }
}

const getTaskTypeText = (type) => {
  switch (type) {
    case 'COMMAND': return '命令执行'
    case 'UPLOAD': return '文件上传'
    case 'DOWNLOAD': return '文件下载'
    default: return type
  }
}

const getTaskStatusType = (status) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'RUNNING': return 'warning'
    case 'PENDING': return 'info'
    case 'CANCELLED': return 'info'
    case 'PAUSED': return 'warning'
    default: return 'info'
  }
}

const getTaskStatusText = (status) => {
  switch (status) {
    case 'SUCCESS': return '成功'
    case 'FAILED': return '失败'
    case 'RUNNING': return '执行中'
    case 'PENDING': return '待执行'
    case 'COMPLETED': return '已完成'
    case 'CANCELLED': return '已取消'
    case 'PAUSED': return '已暂停'
    default: return status || '-'
  }
}

// 格式化耗时为时分秒格式
const formatDuration = (ms) => {
  if (!ms && ms !== 0) return '-'
  if (ms < 1000) return ms + 'ms'
  
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h${minutes % 60}m${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

// 截断输出文本
const truncateOutput = (text) => {
  if (!text) return '无输出'
  const firstLine = text.split('\n')[0]
  if (firstLine.length > 50) {
    return firstLine.substring(0, 50) + '...'
  }
  if (text.includes('\n')) {
    return firstLine + '...'
  }
  return firstLine
}

// 历史命令相关方法
const loadRecentCommands = () => {
  try {
    const saved = localStorage.getItem(RECENT_COMMANDS_KEY)
    if (saved) {
      recentCommands.value = JSON.parse(saved)
    }
  } catch (e) {
    console.error('加载历史命令失败', e)
  }
}

const saveCommandToHistory = (cmd) => {
  if (!cmd || !cmd.trim()) return
  
  // 移除重复的命令
  const filtered = recentCommands.value.filter(c => c !== cmd.trim())
  // 添加到开头
  filtered.unshift(cmd.trim())
  // 只保留最近10条
  recentCommands.value = filtered.slice(0, 10)
  
  // 保存到localStorage
  try {
    localStorage.setItem(RECENT_COMMANDS_KEY, JSON.stringify(recentCommands.value))
  } catch (e) {
    console.error('保存历史命令失败', e)
  }
}

const selectRecentCommand = (cmd) => {
  command.value = cmd
}

const truncateCommand = (cmd) => {
  if (!cmd) return ''
  if (cmd.length > 60) {
    return cmd.substring(0, 60) + '...'
  }
  return cmd
}

onMounted(() => {
  loadDevices()
  loadTaskHistory()
  loadRecentCommands()
})

onUnmounted(() => {
  stopProgressPolling()
})
</script>

<style scoped>
.ssh-container {
  padding: 20px;
}

.tab-header {
  margin-bottom: 15px;
}

.command-panel, .file-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.command-header {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.command-input-with-history {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.command-input-with-history .el-textarea {
  flex: 1;
}

.history-cmd {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #606266;
}

.file-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.command-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.command-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.command-results-table {
  margin-top: 10px;
}

.output-preview {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  cursor: pointer;
}

.output-preview:hover {
  color: #409eff;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-cell .el-progress {
  width: 100%;
}

.transfer-info {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #909399;
}

.output-popover {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 300px;
  overflow: auto;
}

:deep(.el-tooltip__trigger) {
  max-width: 100%;
}

/* 表格标题样式 */
:deep(.el-table th .cell) {
  white-space: nowrap;
}

.selected-file {
  color: #409eff;
  font-size: 13px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-selector {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
}

.device-selector h4 {
  margin: 0 0 10px 0;
  color: #606266;
}

.command-input h4 {
  margin: 0 0 10px 0;
  color: #606266;
}

.command-results h4, .file-results h4 {
  margin: 0 0 10px 0;
  color: #606266;
}

.output {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 300px;
  overflow: auto;
}

.output-small {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 100px;
  overflow: auto;
}

.duration {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

:deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.el-tabs__content) {
  padding: 20px;
}

.logs-popover {
  max-height: 400px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
}

.log-item {
  padding: 4px 8px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item:nth-child(odd) {
  background: #fafafa;
}

.error-text {
  color: #f56c6c;
}
</style>
