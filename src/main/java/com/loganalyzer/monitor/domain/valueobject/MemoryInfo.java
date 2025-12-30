package com.loganalyzer.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内存信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryInfo {
    
    /** 总内存(bytes) */
    private long total;
    
    /** 已使用内存(bytes) */
    private long used;
    
    /** 可用内存(bytes) */
    private long available;
    
    /** 内存使用率(%) */
    private double usagePercent;
    
    /** 交换区总量(bytes) */
    private long swapTotal;
    
    /** 交换区已使用(bytes) */
    private long swapUsed;
    
    /** 交换区使用率(%) */
    private double swapUsagePercent;
    
    /**
     * 格式化内存大小
     */
    public String formatTotal() {
        return formatBytes(total);
    }
    
    public String formatUsed() {
        return formatBytes(used);
    }
    
    public String formatAvailable() {
        return formatBytes(available);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
