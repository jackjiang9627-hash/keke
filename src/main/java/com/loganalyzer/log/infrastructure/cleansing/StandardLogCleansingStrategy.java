package com.loganalyzer.log.infrastructure.cleansing;

import com.loganalyzer.log.domain.service.LogCleansingStrategy;
import com.loganalyzer.log.domain.valueobject.CleansingResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标准日志格式清洗策略
 * 
 * 支持格式示例：
 * 2024-01-15 10:30:45.123 [INFO] [com.example.MyClass] - This is a log message
 * 2024-01-15 10:30:45 INFO  MyClass - This is a log message
 */
@Component
@Order(1)
public class StandardLogCleansingStrategy implements LogCleansingStrategy {

    // 匹配标准日志格式的正则表达式
    private static final Pattern STANDARD_LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?)\\s+" +  // 时间戳
            "\\[?(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\]?\\s+" +                       // 日志级别
            "(?:\\[([^\\]]+)\\]\\s*)?(?:-\\s*)?(.*)$",                                 // 类名和消息
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getStrategyName() {
        return "StandardLogCleansing";
    }

    @Override
    public boolean supports(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return false;
        }
        return STANDARD_LOG_PATTERN.matcher(rawContent.trim()).matches();
    }

    @Override
    public CleansingResult cleanse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return CleansingResult.failure(rawContent, "Empty content");
        }

        String trimmed = rawContent.trim();
        Matcher matcher = STANDARD_LOG_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            return CleansingResult.failure(rawContent, "Does not match standard log format");
        }

        Map<String, String> extractedFields = new HashMap<>();
        
        // 提取时间戳
        String timestamp = matcher.group(1);
        if (timestamp != null) {
            extractedFields.put("timestamp", timestamp);
        }

        // 提取日志级别
        String level = matcher.group(2);
        if (level != null) {
            extractedFields.put("level", level.toUpperCase());
        }

        // 提取类名
        String className = matcher.group(3);
        if (className != null && !className.isBlank()) {
            extractedFields.put("class", className);
        }

        // 提取消息
        String message = matcher.group(4);
        if (message != null) {
            extractedFields.put("message", message.trim());
        }

        // 生成清洗后的标准化内容
        String cleanedContent = formatCleanedContent(extractedFields);

        return CleansingResult.success(rawContent, cleanedContent, extractedFields);
    }

    private String formatCleanedContent(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        
        if (fields.containsKey("timestamp")) {
            sb.append("[").append(fields.get("timestamp")).append("] ");
        }
        if (fields.containsKey("level")) {
            sb.append("[").append(fields.get("level")).append("] ");
        }
        if (fields.containsKey("class")) {
            sb.append("[").append(fields.get("class")).append("] ");
        }
        if (fields.containsKey("message")) {
            sb.append(fields.get("message"));
        }
        
        return sb.toString().trim();
    }
}
