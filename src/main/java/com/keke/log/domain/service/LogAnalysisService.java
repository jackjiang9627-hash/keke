package com.keke.log.domain.service;

import com.keke.log.domain.entity.LogEntry;
import com.keke.log.domain.valueobject.LogLevel;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日志分析领域服务
 * 
 * DDD概念：领域服务（Domain Service）
 * - 封装日志分析相关的业务逻辑
 * - 提供统计、聚合、分析能力
 */
public class LogAnalysisService {

    /**
     * 日志统计结果
     */
    @Getter
    public static class LogStatistics {
        private final long totalCount;
        private final Map<LogLevel, Long> countByLevel;
        private final Map<String, Long> countByApplication;
        private final long errorCount;
        private final long warningCount;

        public LogStatistics(long totalCount, Map<LogLevel, Long> countByLevel,
                            Map<String, Long> countByApplication, long errorCount, long warningCount) {
            this.totalCount = totalCount;
            this.countByLevel = countByLevel;
            this.countByApplication = countByApplication;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
        }

        public double getErrorRate() {
            return totalCount > 0 ? (double) errorCount / totalCount * 100 : 0;
        }
    }

    /**
     * 分析日志并生成统计信息
     */
    public LogStatistics analyze(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return new LogStatistics(0, new HashMap<>(), new HashMap<>(), 0, 0);
        }

        long totalCount = logEntries.size();

        // 按级别统计
        Map<LogLevel, Long> countByLevel = logEntries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));

        // 按应用统计
        Map<String, Long> countByApplication = logEntries.stream()
                .filter(log -> log.getSource() != null)
                .collect(Collectors.groupingBy(
                        log -> log.getSource().getApplication(),
                        Collectors.counting()
                ));

        // 错误数量
        long errorCount = logEntries.stream()
                .filter(log -> log.getLevel().isHigherOrEqualThan(LogLevel.ERROR))
                .count();

        // 警告数量
        long warningCount = logEntries.stream()
                .filter(log -> log.getLevel() == LogLevel.WARN)
                .count();

        return new LogStatistics(totalCount, countByLevel, countByApplication, errorCount, warningCount);
    }

    /**
     * 提取错误日志
     */
    public List<LogEntry> extractErrors(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return List.of();
        }
        return logEntries.stream()
                .filter(LogEntry::isError)
                .toList();
    }

    /**
     * 提取警告及以上级别的日志
     */
    public List<LogEntry> extractWarningsAndAbove(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return List.of();
        }
        return logEntries.stream()
                .filter(LogEntry::isWarningOrAbove)
                .toList();
    }

    /**
     * 按关键词搜索日志
     */
    public List<LogEntry> searchByKeyword(List<LogEntry> logEntries, String keyword) {
        if (logEntries == null || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return logEntries.stream()
                .filter(log -> {
                    String content = log.getDisplayContent();
                    return content != null && content.toLowerCase().contains(lowerKeyword);
                })
                .toList();
    }

    /**
     * 按应用分组
     */
    public Map<String, List<LogEntry>> groupByApplication(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return Map.of();
        }
        return logEntries.stream()
                .filter(log -> log.getSource() != null)
                .collect(Collectors.groupingBy(log -> log.getSource().getApplication()));
    }

    /**
     * 按级别分组
     */
    public Map<LogLevel, List<LogEntry>> groupByLevel(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return Map.of();
        }
        return logEntries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLevel));
    }
}
