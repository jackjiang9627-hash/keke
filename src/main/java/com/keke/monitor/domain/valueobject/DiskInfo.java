package com.keke.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 磁盘信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskInfo {
    
    /** 磁盘名称/挂载点 */
    private String name;
    
    /** 磁盘类型 */
    private String type;
    
    /** 总容量(bytes) */
    private long total;
    
    /** 已使用(bytes) */
    private long used;
    
    /** 可用(bytes) */
    private long available;
    
    /** 使用率(%) */
    private double usagePercent;
    
    /** 读取速度(bytes/s) */
    private long readSpeed;
    
    /** 写入速度(bytes/s) */
    private long writeSpeed;
    
    /** 读取次数 */
    private long readCount;
    
    /** 写入次数 */
    private long writeCount;
    
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
