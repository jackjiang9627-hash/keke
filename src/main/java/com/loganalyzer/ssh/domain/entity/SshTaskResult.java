package com.loganalyzer.ssh.domain.entity;

import com.loganalyzer.ssh.domain.valueobject.SshTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSH任务执行结果 - 每个设备的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshTaskResult {
    
    /** 结果ID */
    private Long id;
    
    /** 关联的任务ID */
    private Long taskId;
    
    /** 设备ID */
    private Long deviceId;
    
    /** 设备名称（冗余存储） */
    private String deviceName;
    
    /** 设备地址（冗余存储） */
    private String deviceHost;
    
    /** 执行状态 */
    private SshTaskStatus status;
    
    /** 执行结果/输出 */
    private String output;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 退出码（命令执行用） */
    private Integer exitCode;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 耗时（毫秒） */
    private Long durationMs;
    
    /**
     * 创建执行结果
     */
    public static SshTaskResult create(Long taskId, Long deviceId, String deviceName, String deviceHost) {
        return SshTaskResult.builder()
                .taskId(taskId)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .deviceHost(deviceHost)
                .status(SshTaskStatus.PENDING)
                .build();
    }
    
    /**
     * 开始执行
     */
    public void start() {
        this.status = SshTaskStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    /**
     * 标记成功
     */
    public void success(String output, Integer exitCode) {
        this.status = SshTaskStatus.SUCCESS;
        this.output = output;
        this.exitCode = exitCode;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * 标记失败
     */
    public void fail(String errorMessage) {
        this.status = SshTaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        if (startTime != null) {
            this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
}
