package com.loganalyzer.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CPU信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuInfo {
    
    /** CPU型号 */
    private String model;
    
    /** 物理核心数 */
    private int physicalCores;
    
    /** 逻辑核心数 */
    private int logicalCores;
    
    /** CPU使用率(%) */
    private double usagePercent;
    
    /** 系统使用率(%) */
    private double systemPercent;
    
    /** 用户使用率(%) */
    private double userPercent;
    
    /** 空闲率(%) */
    private double idlePercent;
    
    /** CPU温度(°C) */
    private double temperature;
    
    /** CPU频率(MHz) */
    private long frequency;
}
