package com.loganalyzer.assistant.application.dto;

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
}
