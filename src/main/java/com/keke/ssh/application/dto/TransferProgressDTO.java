package com.keke.ssh.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 传输进度DTO
 * 
 * DDD概念：DTO（Data Transfer Object）
 * - 用于接口层与外部通信
 * - 不包含业务逻辑
 */
@Data
@Builder
public class TransferProgressDTO {
    
    private String transferId;
    private Long deviceId;
    private String deviceName;
    private String deviceHost;
    private String fileName;
    private long totalBytes;
    private long transferredBytes;
    private int percentage;
    private String status;
    private String errorMessage;
    private long startTime;
    private List<String> logs;
    private boolean resumable;
}
