package com.keke.monitor.application.service;

import com.keke.monitor.application.assembler.MonitorAssembler;
import com.keke.monitor.application.dto.CreateTaskRequest;
import com.keke.monitor.application.dto.MonitorSnapshotDTO;
import com.keke.monitor.application.dto.MonitorTaskDTO;
import com.keke.monitor.domain.entity.MonitorSnapshot;
import com.keke.monitor.domain.entity.MonitorTask;
import com.keke.monitor.domain.exception.InvalidMonitorTaskException;
import com.keke.monitor.domain.exception.MonitorTaskNotFoundException;
import com.keke.monitor.domain.service.MonitorDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 监控应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorApplicationService {
    
    /** 默认Top N进程数量 */
    private static final int DEFAULT_TOP_PROCESSES = 20;
    
    /** 最小周期任务间隔（秒） */
    private static final int MIN_PERIODIC_INTERVAL_SECONDS = 5;
    
    /** 最大快照历史记录数 */
    private static final int MAX_SNAPSHOT_HISTORY_SIZE = 100;
    
    private final MonitorDomainService monitorDomainService;
    private final MonitorAssembler monitorAssembler;
    
    /** 任务存储(内存存储，生产环境应使用数据库) */
    private final Map<Long, MonitorTask> taskStore = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(1);
    
    /** 快照历史(内存存储) */
    private final List<MonitorSnapshot> snapshotHistory = new ArrayList<>();
    
    /**
     * 执行单次监控检测
     */
    public MonitorSnapshotDTO executeOnceMonitor() {
        log.info("执行单次系统监控检测");
        MonitorSnapshot snapshot = monitorDomainService.executeMonitoring(DEFAULT_TOP_PROCESSES);
        addToHistory(snapshot);
        
        // 检查是否需要告警
        if (monitorDomainService.needAlert(snapshot)) {
            String alertMsg = monitorDomainService.generateAlertMessage(snapshot);
            log.warn("系统监控告警: {}", alertMsg);
        }
        
        return monitorAssembler.toDTO(snapshot);
    }
    
    /**
     * 获取当前系统快照
     */
    public MonitorSnapshotDTO getCurrentSnapshot() {
        MonitorSnapshot snapshot = monitorDomainService.executeMonitoring(DEFAULT_TOP_PROCESSES);
        return monitorAssembler.toDTO(snapshot);
    }
    
    /**
     * 创建监控任务
     */
    public MonitorTaskDTO createTask(CreateTaskRequest request) {
        log.info("创建监控任务: {}, 类型: {}", request.getName(), request.getType());
        
        MonitorTask task;
        if ("PERIODIC".equalsIgnoreCase(request.getType())) {
            if (request.getIntervalSeconds() == null || request.getIntervalSeconds() < MIN_PERIODIC_INTERVAL_SECONDS) {
                throw new InvalidMonitorTaskException(
                    String.format("周期任务必须指定间隔时间，且不能小于%d秒", MIN_PERIODIC_INTERVAL_SECONDS));
            }
            int maxCount = request.getMaxExecuteCount() != null ? request.getMaxExecuteCount() : 0;
            task = MonitorTask.createPeriodicTask(request.getName(), request.getIntervalSeconds(), maxCount);
        } else {
            task = MonitorTask.createOnceTask(request.getName());
        }
        
        task.setId(taskIdGenerator.getAndIncrement());
        task.setRemark(request.getRemark());
        taskStore.put(task.getId(), task);
        
        return monitorAssembler.toTaskDTO(task);
    }
    
    /**
     * 启动任务
     */
    public MonitorTaskDTO startTask(Long taskId) {
        MonitorTask task = getTaskById(taskId);
        task.start();
        
        // 如果是单次任务，立即执行
        if (task.getType() == MonitorTask.TaskType.ONCE) {
            MonitorSnapshot snapshot = monitorDomainService.executeTaskMonitoring(task, DEFAULT_TOP_PROCESSES);
            addToHistory(snapshot);
        }
        
        return monitorAssembler.toTaskDTO(task);
    }
    
    /**
     * 暂停任务
     */
    public MonitorTaskDTO pauseTask(Long taskId) {
        MonitorTask task = getTaskById(taskId);
        task.pause();
        return monitorAssembler.toTaskDTO(task);
    }
    
    /**
     * 恢复任务
     */
    public MonitorTaskDTO resumeTask(Long taskId) {
        MonitorTask task = getTaskById(taskId);
        task.resume();
        return monitorAssembler.toTaskDTO(task);
    }
    
    /**
     * 停止任务
     */
    public MonitorTaskDTO stopTask(Long taskId) {
        MonitorTask task = getTaskById(taskId);
        task.stop();
        return monitorAssembler.toTaskDTO(task);
    }
    
    /**
     * 删除任务
     */
    public void deleteTask(Long taskId) {
        if (!taskStore.containsKey(taskId)) {
            throw new MonitorTaskNotFoundException(taskId);
        }
        taskStore.remove(taskId);
        log.info("删除监控任务: {}", taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<MonitorTaskDTO> getAllTasks() {
        return taskStore.values().stream()
                .map(monitorAssembler::toTaskDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务详情
     */
    public MonitorTaskDTO getTask(Long taskId) {
        return monitorAssembler.toTaskDTO(getTaskById(taskId));
    }
    
    /**
     * 获取快照历史
     */
    public List<MonitorSnapshotDTO> getSnapshotHistory(int limit) {
        int size = Math.min(limit, snapshotHistory.size());
        return snapshotHistory.subList(snapshotHistory.size() - size, snapshotHistory.size())
                .stream()
                .map(monitorAssembler::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定任务的快照历史
     * @param taskId 任务ID
     * @return 该任务的所有监控快照
     */
    public List<MonitorSnapshotDTO> getTaskSnapshotHistory(Long taskId) {
        return snapshotHistory.stream()
                .filter(s -> taskId.equals(s.getTaskId()))
                .map(monitorAssembler::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取运行中的周期任务
     */
    public List<MonitorTask> getRunningPeriodicTasks() {
        return taskStore.values().stream()
                .filter(t -> t.getType() == MonitorTask.TaskType.PERIODIC)
                .filter(t -> t.getStatus() == MonitorTask.TaskStatus.RUNNING)
                .collect(Collectors.toList());
    }
    
    /**
     * 执行周期任务的监控
     */
    public void executePeriodicTaskMonitoring(MonitorTask task) {
        try {
            MonitorSnapshot snapshot = monitorDomainService.executeTaskMonitoring(task, DEFAULT_TOP_PROCESSES);
            addToHistory(snapshot);
            log.info("周期任务[{}]执行完成，执行次数: {}", task.getName(), task.getExecuteCount());
        } catch (Exception e) {
            log.error("周期任务执行失败: {}", task.getName(), e);
            task.fail(e.getMessage());
        }
    }
    
    private MonitorTask getTaskById(Long taskId) {
        MonitorTask task = taskStore.get(taskId);
        if (task == null) {
            throw new MonitorTaskNotFoundException(taskId);
        }
        return task;
    }
    
    private synchronized void addToHistory(MonitorSnapshot snapshot) {
        if (snapshotHistory.size() >= MAX_SNAPSHOT_HISTORY_SIZE) {
            snapshotHistory.remove(0);
        }
        snapshotHistory.add(snapshot);
    }
}
