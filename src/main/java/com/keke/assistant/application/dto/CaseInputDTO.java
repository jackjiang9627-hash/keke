package com.keke.assistant.application.dto;

import lombok.Data;

/**
 * 案例输入DTO
 */
@Data
public class CaseInputDTO {
    private String title;
    private String summary;
    private String hyperlink;
    private String content;
    private String moduleName;  // 自定义模块名称
    private String tags;        // 标签（逗号分隔）
    private String source;      // 来源：MANUAL/AI_CHAT
    private String originalConversation;  // 原始对话记录(JSON)
    private Boolean reviewEnabled;  // 是否启用复习
}
