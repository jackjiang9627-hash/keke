package com.keke.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统基础信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {
    
    /** 主机名 */
    private String hostname;
    
    /** 操作系统 */
    private String osName;
    
    /** 操作系统版本 */
    private String osVersion;
    
    /** 系统架构 */
    private String osArch;
    
    /** 系统启动时间 */
    private long bootTime;
    
    /** 系统运行时长(ms) */
    private long upTime;
    
    /** 当前用户 */
    private String currentUser;
    
    /** Java版本 */
    private String javaVersion;
    
    /** JVM内存最大值 */
    private long jvmMaxMemory;
    
    /** JVM已使用内存 */
    private long jvmUsedMemory;
    
    /** JVM空闲内存 */
    private long jvmFreeMemory;
    
    public String formatUpTime() {
        long seconds = upTime / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
    }
}
