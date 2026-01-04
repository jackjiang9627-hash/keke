package com.keke.monitor.domain.service;

import com.keke.monitor.domain.entity.MonitorSnapshot;
import com.keke.monitor.domain.entity.MonitorTask;
import com.keke.monitor.domain.port.SystemInfoCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 监控领域服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorDomainService {
    
    private final SystemInfoCollector systemInfoCollector;
    
    /**
     * 执行监控采集
     */
    public MonitorSnapshot executeMonitoring(int topProcessCount) {
        log.info("开始执行系统监控采集，进程数量限制: {}", topProcessCount);
        MonitorSnapshot snapshot = systemInfoCollector.collectSnapshot(topProcessCount);
        log.info("系统监控采集完成，健康状态: {}", snapshot.getHealthStatus().getDescription());
        return snapshot;
    }
    
    /**
     * 执行任务触发的监控
     */
    public MonitorSnapshot executeTaskMonitoring(MonitorTask task, int topProcessCount) {
        log.info("执行任务[{}]触发的监控采集", task.getName());
        MonitorSnapshot snapshot = executeMonitoring(topProcessCount);
        snapshot.setTaskId(task.getId());
        
        // 更新任务状态
        task.complete(snapshot.getHealthStatus().getDescription());
        
        return snapshot;
    }
    
    /**
     * 判断快照是否需要告警
     */
    public boolean needAlert(MonitorSnapshot snapshot) {
        MonitorSnapshot.HealthStatus status = snapshot.getHealthStatus();
        return status == MonitorSnapshot.HealthStatus.WARNING || 
               status == MonitorSnapshot.HealthStatus.CRITICAL;
    }
    
    /**
     * 生成告警信息
     */
    public String generateAlertMessage(MonitorSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统告警 - 状态: ").append(snapshot.getHealthStatus().getDescription());
        
        if (snapshot.getCpuInfo() != null) {
            double cpuUsage = snapshot.getCpuInfo().getUsagePercent();
            if (cpuUsage > 70) {
                sb.append("\n- CPU使用率过高: ").append(String.format("%.1f%%", cpuUsage));
            }
        }
        
        if (snapshot.getMemoryInfo() != null) {
            double memUsage = snapshot.getMemoryInfo().getUsagePercent();
            if (memUsage > 80) {
                sb.append("\n- 内存使用率过高: ").append(String.format("%.1f%%", memUsage));
            }
        }
        
        double diskUsage = snapshot.getTotalDiskUsage();
        if (diskUsage > 85) {
            sb.append("\n- 磁盘使用率过高: ").append(String.format("%.1f%%", diskUsage));
        }
        
        return sb.toString();
    }
}
