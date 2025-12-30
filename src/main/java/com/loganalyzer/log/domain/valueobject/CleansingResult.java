package com.loganalyzer.log.domain.valueobject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 清洗结果 - 值对象
 * 
 * DDD概念：值对象（Value Object）
 * - 封装日志清洗的结果信息
 * - 不可变
 */
public final class CleansingResult {

    private final boolean success;                    // 清洗是否成功
    private final String originalContent;             // 原始内容
    private final String cleanedContent;              // 清洗后内容
    private final Map<String, String> extractedFields;  // 提取的字段
    private final String errorMessage;                // 错误信息

    private CleansingResult(boolean success, String originalContent, 
                           String cleanedContent, Map<String, String> extractedFields, 
                           String errorMessage) {
        this.success = success;
        this.originalContent = originalContent;
        this.cleanedContent = cleanedContent;
        this.extractedFields = extractedFields != null 
            ? Collections.unmodifiableMap(new HashMap<>(extractedFields)) 
            : Collections.emptyMap();
        this.errorMessage = errorMessage;
    }

    /**
     * 创建成功的清洗结果
     */
    public static CleansingResult success(String originalContent, String cleanedContent, 
                                          Map<String, String> extractedFields) {
        return new CleansingResult(true, originalContent, cleanedContent, extractedFields, null);
    }

    /**
     * 创建失败的清洗结果
     */
    public static CleansingResult failure(String originalContent, String errorMessage) {
        return new CleansingResult(false, originalContent, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getCleanedContent() {
        return cleanedContent;
    }

    public Map<String, String> getExtractedFields() {
        return extractedFields;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CleansingResult that = (CleansingResult) o;
        return success == that.success &&
               Objects.equals(originalContent, that.originalContent) &&
               Objects.equals(cleanedContent, that.cleanedContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, originalContent, cleanedContent);
    }
}
