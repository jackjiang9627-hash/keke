package com.loganalyzer.ssh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SSH任务输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshTaskInputDTO {
    
    /** 任务名称 */
    private String name;
    
    /** 任务类型: COMMAND, UPLOAD, DOWNLOAD */
    private String type;
    
    /** 命令内容（COMMAND类型） */
    private String command;
    
    /** 本地文件路径（UPLOAD/DOWNLOAD） */
    private String localPath;
    
    /** 远程文件路径（UPLOAD/DOWNLOAD） */
    private String remotePath;
    
    /** 目标设备ID列表 */
    private List<Long> deviceIds;
}
