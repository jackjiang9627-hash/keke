package com.loganalyzer.assistant.application.dto;

import lombok.Builder;
import lombok.Data;

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
}
