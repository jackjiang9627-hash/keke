package com.loganalyzer.ssh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSH任务结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshTaskResultDTO {
    
    private Long id;
    
    /** 设备ID */
    private Long deviceId;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备地址 */
    private String deviceHost;
    
    /** 执行状态 */
    private String status;
    
    /** 执行输出 */
    private String output;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 退出码 */
    private Integer exitCode;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 耗时（毫秒） */
    private Long durationMs;
}
