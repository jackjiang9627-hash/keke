package com.loganalyzer.monitor.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监控快照输出DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorSnapshotDTO {
    
    /** 采集时间 */
    private LocalDateTime collectTime;
    
    /** 健康状态 */
    private String healthStatus;
    
    /** 系统信息 */
    private SystemInfoDTO systemInfo;
    
    /** CPU信息 */
    private CpuInfoDTO cpuInfo;
    
    /** 内存信息 */
    private MemoryInfoDTO memoryInfo;
    
    /** 磁盘信息列表 */
    private List<DiskInfoDTO> diskInfoList;
    
    /** 网络信息列表 */
    private List<NetworkInfoDTO> networkInfoList;
    
    /** 进程信息列表 */
    private List<ProcessInfoDTO> processInfoList;
    
    /** 概览数据 */
    private OverviewDTO overview;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfoDTO {
        private String hostname;
        private String osName;
        private String osVersion;
        private String osArch;
        private String upTime;
        private String currentUser;
        private String javaVersion;
        private String jvmMemory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuInfoDTO {
        private String model;
        private int physicalCores;
        private int logicalCores;
        private double usagePercent;
        private double systemPercent;
        private double userPercent;
        private double idlePercent;
        private double temperature;
        private long frequency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfoDTO {
        private String total;
        private String used;
        private String available;
        private double usagePercent;
        private String swapTotal;
        private String swapUsed;
        private double swapUsagePercent;
        private long totalBytes;
        private long usedBytes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfoDTO {
        private String name;
        private String type;
        private String total;
        private String used;
        private String available;
        private double usagePercent;
        private String readSpeed;
        private String writeSpeed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkInfoDTO {
        private String name;
        private String displayName;
        private String macAddress;
        private String ipv4Address;
        private String bytesReceived;
        private String bytesSent;
        private String receiveSpeed;
        private String sendSpeed;
        private String speed;
        private boolean connected;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessInfoDTO {
        private int pid;
        private String name;
        private String state;
        private double cpuPercent;
        private double memoryPercent;
        private String memory;
        private int threadCount;
        private String user;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewDTO {
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private int processCount;
        private int networkInterfaceCount;
    }
}
