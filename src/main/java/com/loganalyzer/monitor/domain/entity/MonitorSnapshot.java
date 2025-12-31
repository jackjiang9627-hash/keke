package com.loganalyzer.monitor.domain.entity;

import com.loganalyzer.monitor.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统监控快照实体 - 聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorSnapshot {
    
    /** 快照ID */
    private Long id;
    
    /** 采集时间 */
    private LocalDateTime collectTime;
    
    /** 系统基础信息 */
    private SystemInfo systemInfo;
    
    /** CPU信息 */
    private CpuInfo cpuInfo;
    
    /** 内存信息 */
    private MemoryInfo memoryInfo;
    
    /** 磁盘信息列表 */
    private List<DiskInfo> diskInfoList;
    
    /** 网络信息列表 */
    private List<NetworkInfo> networkInfoList;
    
    /** 进程信息列表(Top N) */
    private List<ProcessInfo> processInfoList;
    
    /** 任务ID(如果是任务触发) */
    private Long taskId;
    
    /**
     * 创建新快照
     */
    public static MonitorSnapshot create() {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 获取总磁盘使用率
     */
    public double getTotalDiskUsage() {
        if (diskInfoList == null || diskInfoList.isEmpty()) {
            return 0;
        }
        long totalSpace = diskInfoList.stream().mapToLong(DiskInfo::getTotal).sum();
        long usedSpace = diskInfoList.stream().mapToLong(DiskInfo::getUsed).sum();
        return totalSpace > 0 ? (usedSpace * 100.0 / totalSpace) : 0;
    }
    
    /**
     * 获取健康状态
     */
    public HealthStatus getHealthStatus() {
        double cpuUsage = cpuInfo != null ? cpuInfo.getUsagePercent() : 0;
        double memUsage = memoryInfo != null ? memoryInfo.getUsagePercent() : 0;
        double diskUsage = getTotalDiskUsage();
        
        if (cpuUsage > 90 || memUsage > 90 || diskUsage > 95) {
            return HealthStatus.CRITICAL;
        } else if (cpuUsage > 70 || memUsage > 80 || diskUsage > 85) {
            return HealthStatus.WARNING;
        }
        return HealthStatus.HEALTHY;
    }
    
    public enum HealthStatus {
        HEALTHY("健康"),
        WARNING("警告"),
        CRITICAL("严重");
        
        private final String description;
        
        HealthStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
