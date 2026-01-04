package com.keke.log.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志输出DTO - 用于返回日志数据
 * 
 * DDD概念：DTO（Data Transfer Object）
 * - 将领域对象转换为外部可用的数据格式
 * - 可以根据不同场景定制返回字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogOutputDTO {

    private String id;
    private String rawContent;
    private String cleanedContent;
    private String message;  // 日志消息（清洗后的内容或原始内容）
    private String level;
    private String levelDescription;
    private String application;
    private String host;
    private String environment;
    private LocalDateTime timestamp;
    private boolean cleaned;
    private Map<String, String> metadata;
}
