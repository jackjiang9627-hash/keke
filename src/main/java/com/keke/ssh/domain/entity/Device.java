package com.keke.ssh.domain.entity;

import com.keke.ssh.domain.valueobject.AuthType;
import com.keke.ssh.domain.valueobject.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备实体 - SSH管理的目标服务器
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    
    /** 设备ID */
    private Long id;
    
    /** 设备名称 */
    private String name;
    
    /** IP地址 */
    private String host;
    
    /** SSH端口 */
    private Integer port;
    
    /** 用户名 */
    private String username;
    
    /** 密码（加密存储） */
    private String password;
    
    /** 私钥内容（加密存储） */
    private String privateKey;
    
    /** 认证方式 */
    private AuthType authType;
    
    /** 设备分组 */
    private String groupName;
    
    /** 设备状态 */
    private DeviceStatus status;
    
    /** 最后连接时间 */
    private LocalDateTime lastConnectTime;
    
    /** 最后连接结果 */
    private String lastConnectResult;
    
    /** 备注 */
    private String remark;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /**
     * 创建新设备
     */
    public static Device create(String name, String host, int port, String username) {
        return Device.builder()
                .name(name)
                .host(host)
                .port(port)
                .username(username)
                .authType(AuthType.PASSWORD)
                .status(DeviceStatus.UNKNOWN)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 更新连接状态
     */
    public void updateConnectStatus(boolean success, String message) {
        this.lastConnectTime = LocalDateTime.now();
        this.lastConnectResult = message;
        this.status = success ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 标记为在线
     */
    public void markOnline() {
        this.status = DeviceStatus.ONLINE;
        this.lastConnectTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 标记为离线
     */
    public void markOffline(String reason) {
        this.status = DeviceStatus.OFFLINE;
        this.lastConnectResult = reason;
        this.updateTime = LocalDateTime.now();
    }
}
