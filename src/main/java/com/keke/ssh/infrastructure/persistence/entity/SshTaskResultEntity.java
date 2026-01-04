package com.keke.ssh.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSH任务结果持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ssh_task_result")
public class SshTaskResultEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long taskId;
    
    @Column(nullable = false)
    private Long deviceId;
    
    @Column(length = 100)
    private String deviceName;
    
    @Column(length = 100)
    private String deviceHost;
    
    @Column(length = 20)
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String output;
    
    @Column(length = 1000)
    private String errorMessage;
    
    private Integer exitCode;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Long durationMs;
}
