package com.keke.assistant.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案例输出DTO
 */
@Data
@Builder
public class CaseOutputDTO {
    private String id;
    private String title;
    private String summary;
    private String truncatedSummary;  // 截断的摘要
    private String hyperlink;
    private String content;
    private String moduleName;        // 模块名称
    private boolean hasHyperlink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // AI总结相关
    private String tags;              // 标签
    private String source;            // 来源：MANUAL/AI_CHAT
    private boolean fromAiChat;       // 是否来自AI对话
    
    // 复习相关
    private LocalDate nextReviewDate; // 下次复习日期
    private Integer reviewCount;      // 复习次数
    private Integer masteryLevel;     // 熟练度 1-5
    private LocalDateTime lastReviewTime;  // 上次复习时间
    private Boolean reviewEnabled;    // 是否启用复习
    private boolean needsReviewToday; // 是否需要今天复习
}
