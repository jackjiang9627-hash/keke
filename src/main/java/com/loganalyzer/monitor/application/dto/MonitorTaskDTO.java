package com.loganalyzer.monitor.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监控任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorTaskDTO {
    
    /** 任务ID */
    private Long id;
    
    /** 任务名称 */
    private String name;
    
    /** 任务类型: ONCE/PERIODIC */
    private String type;
    
    /** 任务类型描述 */
    private String typeDescription;
    
    /** 任务状态 */
    private String status;
    
    /** 任务状态描述 */
    private String statusDescription;
    
    /** 检测间隔(秒) */
    private Integer intervalSeconds;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 下次执行时间 */
    private LocalDateTime nextExecuteTime;
    
    /** 已执行次数 */
    private Integer executeCount;
    
    /** 最大执行次数 - 0表示无限制 */
    private Integer maxExecuteCount;
    
    /** 最后一次执行结果 */
    private String lastResult;
    
    /** 备注 */
    private String remark;
}
