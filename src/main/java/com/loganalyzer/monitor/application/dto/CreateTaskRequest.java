package com.loganalyzer.monitor.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建监控任务请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    
    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    private String name;
    
    /** 任务类型: ONCE/PERIODIC */
    @NotNull(message = "任务类型不能为空")
    private String type;
    
    /** 检测间隔(秒) - 周期任务必填 */
    @Min(value = 5, message = "检测间隔不能小于5秒")
    private Integer intervalSeconds;
    
    /** 最大执行次数 - 0表示无限制 */
    @Min(value = 0, message = "执行次数不能为负数")
    private Integer maxExecuteCount;
    
    /** 备注 */
    private String remark;
}
