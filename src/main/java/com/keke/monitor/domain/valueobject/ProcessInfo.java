package com.keke.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进程信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInfo {
    
    /** 进程ID */
    private int pid;
    
    /** 进程名称 */
    private String name;
    
    /** 进程路径 */
    private String path;
    
    /** 进程状态 */
    private String state;
    
    /** CPU使用率(%) */
    private double cpuPercent;
    
    /** 内存使用率(%) */
    private double memoryPercent;
    
    /** 虚拟内存(bytes) */
    private long virtualMemory;
    
    /** 物理内存(bytes) */
    private long residentMemory;
    
    /** 线程数 */
    private int threadCount;
    
    /** 启动时间 */
    private long startTime;
    
    /** 运行时长(ms) */
    private long upTime;
    
    /** 用户名 */
    private String user;
    
    /** 优先级 */
    private int priority;
    
    public String formatResidentMemory() {
        return formatBytes(residentMemory);
    }
    
    public String formatVirtualMemory() {
        return formatBytes(virtualMemory);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
