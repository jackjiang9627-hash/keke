package com.keke.ssh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSH任务输出DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshTaskOutputDTO {
    
    private Long id;
    
    /** 任务名称 */
    private String name;
    
    /** 任务类型 */
    private String type;
    
    /** 命令内容 */
    private String command;
    
    /** 本地路径 */
    private String localPath;
    
    /** 远程路径 */
    private String remotePath;
    
    /** 设备数量 */
    private Integer deviceCount;
    
    /** 任务状态 */
    private String status;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 执行结果列表 */
    private List<SshTaskResultDTO> results;
}
