package com.keke.ssh.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ssh_device")
public class DeviceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    @Column(length = 500)
    private String password;
    
    @Column(columnDefinition = "TEXT")
    private String privateKey;
    
    @Column(length = 20)
    private String authType;
    
    @Column(length = 50)
    private String groupName;
    
    @Column(length = 20)
    private String status;
    
    private LocalDateTime lastConnectTime;
    
    @Column(length = 500)
    private String lastConnectResult;
    
    @Column(length = 500)
    private String remark;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
