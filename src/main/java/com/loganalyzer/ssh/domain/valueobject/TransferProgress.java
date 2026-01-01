package com.loganalyzer.ssh.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件传输进度值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferProgress {
    
    /** 传输ID */
    private String transferId;
    
    /** 设备ID */
    private Long deviceId;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备IP */
    private String deviceHost;
    
    /** 文件名 */
    private String fileName;
    
    /** 文件总大小（字节） */
    private long totalBytes;
    
    /** 已传输字节数 */
    private long transferredBytes;
    
    /** 传输进度百分比 (0-100) */
    private int percentage;
    
    /** 传输速度（字节/秒） */
    private long speed;
    
    /** 预计剩余时间（秒） */
    private long remainingSeconds;
    
    /** 传输状态: PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED */
    private String status;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 开始时间 */
    private long startTime;
    
    /** 是否支持断点续传 */
    private boolean resumable;
    
    /** 执行日志详情 */
    @Builder.Default
    private List<String> logs = new ArrayList<>();
    
    public void updateProgress(long transferred) {
        this.transferredBytes = transferred;
        if (totalBytes > 0) {
            this.percentage = (int) ((transferred * 100) / totalBytes);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 0) {
            this.speed = (transferred * 1000) / elapsed;
            if (speed > 0) {
                this.remainingSeconds = (totalBytes - transferred) / speed;
            }
        }
    }
    
    public void addLog(String log) {
        if (this.logs == null) {
            this.logs = new ArrayList<>();
        }
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.logs.add("[" + timestamp + "] " + log);
    }
}
