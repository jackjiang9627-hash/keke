package com.keke.ssh.domain.entity;

import com.keke.ssh.domain.valueobject.SshTaskStatus;
import com.keke.ssh.domain.valueobject.SshTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH任务实体 - 批量执行的SSH任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshTask {
    
    /** 任务ID */
    private Long id;
    
    /** 任务名称 */
    private String name;
    
    /** 任务类型 */
    private SshTaskType type;
    
    /** 命令内容（COMMAND类型使用） */
    private String command;
    
    /** 本地文件路径（UPLOAD/DOWNLOAD使用） */
    private String localPath;
    
    /** 远程文件路径（UPLOAD/DOWNLOAD使用） */
    private String remotePath;
    
    /** 目标设备ID列表 */
    @Builder.Default
    private List<Long> deviceIds = new ArrayList<>();
    
    /** 任务状态 */
    private SshTaskStatus status;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 创建人 */
    private String createdBy;
    
    /**
     * 创建命令执行任务
     */
    public static SshTask createCommandTask(String name, String command, List<Long> deviceIds) {
        return SshTask.builder()
                .name(name)
                .type(SshTaskType.COMMAND)
                .command(command)
                .deviceIds(deviceIds != null ? deviceIds : new ArrayList<>())
                .status(SshTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建文件上传任务
     */
    public static SshTask createUploadTask(String name, String localPath, String remotePath, List<Long> deviceIds) {
        return SshTask.builder()
                .name(name)
                .type(SshTaskType.UPLOAD)
                .localPath(localPath)
                .remotePath(remotePath)
                .deviceIds(deviceIds != null ? deviceIds : new ArrayList<>())
                .status(SshTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建文件下载任务
     */
    public static SshTask createDownloadTask(String name, String remotePath, String localPath, List<Long> deviceIds) {
        return SshTask.builder()
                .name(name)
                .type(SshTaskType.DOWNLOAD)
                .localPath(localPath)
                .remotePath(remotePath)
                .deviceIds(deviceIds != null ? deviceIds : new ArrayList<>())
                .status(SshTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
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
     * 标记完成
     */
    public void complete(boolean success) {
        this.status = success ? SshTaskStatus.SUCCESS : SshTaskStatus.FAILED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 取消任务
     */
    public void cancel() {
        this.status = SshTaskStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
}
