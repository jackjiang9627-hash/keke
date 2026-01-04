package com.keke.ssh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInputDTO {
    
    private Long id;
    
    /** 设备名称 */
    private String name;
    
    /** IP地址 */
    private String host;
    
    /** SSH端口 */
    private Integer port;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 私钥内容 */
    private String privateKey;
    
    /** 认证方式: PASSWORD, KEY */
    private String authType;
    
    /** 设备分组 */
    private String groupName;
    
    /** 备注 */
    private String remark;
}
