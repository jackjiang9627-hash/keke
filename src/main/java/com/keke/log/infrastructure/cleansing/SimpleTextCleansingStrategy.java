package com.keke.log.infrastructure.cleansing;

import com.keke.log.domain.service.LogCleansingStrategy;
import com.keke.log.domain.valueobject.CleansingResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单文本日志清洗策略 - 兜底策略
 * 
 * 当其他策略都不匹配时，使用此策略进行基本清洗
 * 尝试从纯文本中提取可能的日志级别
 */
@Component
@Order(100) // 最低优先级，作为兜底
public class SimpleTextCleansingStrategy implements LogCleansingStrategy {

    // 尝试匹配日志级别关键词
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "\\b(TRACE|DEBUG|INFO|WARN(?:ING)?|ERROR|FATAL|SEVERE)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getStrategyName() {
        return "SimpleTextCleansing";
    }

    @Override
    public boolean supports(String rawContent) {
        // 作为兜底策略，接受所有非空内容
        return rawContent != null && !rawContent.isBlank();
    }

    @Override
    public CleansingResult cleanse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return CleansingResult.failure(rawContent, "Empty content");
        }

        String trimmed = rawContent.trim();
        Map<String, String> extractedFields = new HashMap<>();

        // 尝试提取日志级别
        Matcher levelMatcher = LEVEL_PATTERN.matcher(trimmed);
        if (levelMatcher.find()) {
            String level = normalizeLevel(levelMatcher.group(1));
            extractedFields.put("level", level);
        } else {
            extractedFields.put("level", "INFO"); // 默认级别
        }

        // 清理内容：移除多余空白，标准化换行
        String cleanedContent = cleanText(trimmed);
        extractedFields.put("message", cleanedContent);

        return CleansingResult.success(rawContent, cleanedContent, extractedFields);
    }

    private String normalizeLevel(String level) {
        String upper = level.toUpperCase();
        if (upper.equals("WARNING")) {
            return "WARN";
        }
        if (upper.equals("SEVERE")) {
            return "ERROR";
        }
        return upper;
    }

    private String cleanText(String text) {
        // 移除多余的空白字符
        String cleaned = text.replaceAll("\\s+", " ");
        // 移除控制字符
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");
        return cleaned.trim();
    }
}
