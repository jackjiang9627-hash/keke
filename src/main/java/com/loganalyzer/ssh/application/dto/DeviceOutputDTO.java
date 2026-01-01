package com.loganalyzer.ssh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备输出DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOutputDTO {
    
    private Long id;
    
    /** 设备名称 */
    private String name;
    
    /** IP地址 */
    private String host;
    
    /** SSH端口 */
    private Integer port;
    
    /** 用户名 */
    private String username;
    
    /** 认证方式 */
    private String authType;
    
    /** 设备分组 */
    private String groupName;
    
    /** 设备状态 */
    private String status;
    
    /** 最后连接时间 */
    private LocalDateTime lastConnectTime;
    
    /** 最后连接结果 */
    private String lastConnectResult;
    
    /** 备注 */
    private String remark;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
