package com.keke.ssh.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSH任务持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ssh_task")
public class SshTaskEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 20)
    private String type;
    
    @Column(columnDefinition = "TEXT")
    private String command;
    
    @Column(length = 500)
    private String localPath;
    
    @Column(length = 500)
    private String remotePath;
    
    /** 以逗号分隔的设备ID列表 */
    @Column(columnDefinition = "TEXT")
    private String deviceIds;
    
    @Column(length = 20)
    private String status;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime createTime;
    
    @Column(length = 50)
    private String createdBy;
}
