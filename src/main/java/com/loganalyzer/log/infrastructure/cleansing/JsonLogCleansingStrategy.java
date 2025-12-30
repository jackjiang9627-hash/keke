package com.loganalyzer.log.infrastructure.cleansing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.log.domain.service.LogCleansingStrategy;
import com.loganalyzer.log.domain.valueobject.CleansingResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON格式日志清洗策略
 * 
 * 支持格式示例：
 * {"timestamp":"2024-01-15T10:30:45","level":"INFO","message":"User logged in","userId":"123"}
 */
@Component
@Order(2)
public class JsonLogCleansingStrategy implements LogCleansingStrategy {

    private final ObjectMapper objectMapper;

    public JsonLogCleansingStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStrategyName() {
        return "JsonLogCleansing";
    }

    @Override
    public boolean supports(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return false;
        }
        String trimmed = rawContent.trim();
        // 简单检查是否是JSON格式
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    @Override
    public CleansingResult cleanse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return CleansingResult.failure(rawContent, "Empty content");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(rawContent.trim());
            Map<String, String> extractedFields = extractFields(jsonNode);
            
            // 生成清洗后的标准化内容
            String cleanedContent = formatCleanedContent(extractedFields);
            
            return CleansingResult.success(rawContent, cleanedContent, extractedFields);
            
        } catch (JsonProcessingException e) {
            return CleansingResult.failure(rawContent, "Invalid JSON format: " + e.getMessage());
        }
    }

    private Map<String, String> extractFields(JsonNode jsonNode) {
        Map<String, String> fields = new HashMap<>();
        
        // 提取常见的日志字段
        extractField(jsonNode, fields, "timestamp", "time", "@timestamp", "ts");
        extractField(jsonNode, fields, "level", "severity", "log_level", "loglevel");
        extractField(jsonNode, fields, "message", "msg", "log", "text");
        extractField(jsonNode, fields, "class", "logger", "logger_name", "source");
        extractField(jsonNode, fields, "thread", "thread_name", "threadName");
        extractField(jsonNode, fields, "exception", "error", "stack_trace", "stackTrace");
        
        // 提取其他所有字段
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            String normalizedName = fieldName.toLowerCase();
            // 跳过已经提取的字段
            if (!fields.containsKey(normalizedName) && 
                !isCommonField(normalizedName)) {
                JsonNode value = jsonNode.get(fieldName);
                if (value != null && !value.isNull()) {
                    fields.put(fieldName, value.asText());
                }
            }
        }
        
        return fields;
    }

    private void extractField(JsonNode jsonNode, Map<String, String> fields, 
                             String targetName, String... sourceNames) {
        for (String sourceName : sourceNames) {
            JsonNode value = jsonNode.get(sourceName);
            if (value != null && !value.isNull()) {
                fields.put(targetName, value.asText());
                return;
            }
        }
        // 尝试目标名称本身
        JsonNode value = jsonNode.get(targetName);
        if (value != null && !value.isNull()) {
            fields.put(targetName, value.asText());
        }
    }

    private boolean isCommonField(String fieldName) {
        return fieldName.equals("timestamp") || fieldName.equals("time") ||
               fieldName.equals("level") || fieldName.equals("severity") ||
               fieldName.equals("message") || fieldName.equals("msg") ||
               fieldName.equals("class") || fieldName.equals("logger") ||
               fieldName.equals("thread") || fieldName.equals("exception");
    }

    private String formatCleanedContent(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        
        if (fields.containsKey("timestamp")) {
            sb.append("[").append(fields.get("timestamp")).append("] ");
        }
        if (fields.containsKey("level")) {
            sb.append("[").append(fields.get("level").toUpperCase()).append("] ");
        }
        if (fields.containsKey("class")) {
            sb.append("[").append(fields.get("class")).append("] ");
        }
        if (fields.containsKey("message")) {
            sb.append(fields.get("message"));
        }
        if (fields.containsKey("exception")) {
            sb.append(" | Exception: ").append(fields.get("exception"));
        }
        
        return sb.toString().trim();
    }
}
