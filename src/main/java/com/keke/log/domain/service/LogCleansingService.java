package com.keke.log.domain.service;

import com.keke.log.domain.entity.LogEntry;
import com.keke.log.domain.valueobject.CleansingResult;

import java.util.List;

/**
 * 日志清洗领域服务
 * 
 * DDD概念：领域服务（Domain Service）
 * - 封装跨聚合的业务逻辑
 * - 协调多个清洗策略
 * - 无状态
 */
public class LogCleansingService {

    private final List<LogCleansingStrategy> strategies;

    public LogCleansingService(List<LogCleansingStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 清洗单条日志
     */
    public void cleanse(LogEntry logEntry) {
        if (logEntry == null || logEntry.isCleaned()) {
            return;
        }

        String rawContent = logEntry.getRawContent();
        CleansingResult result = findAndApplyStrategy(rawContent);
        logEntry.applyCleansing(result);
    }

    /**
     * 批量清洗日志
     */
    public void cleanseAll(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }
        logEntries.forEach(this::cleanse);
    }

    /**
     * 查找合适的策略并执行清洗
     */
    private CleansingResult findAndApplyStrategy(String rawContent) {
        // 遍历策略，找到第一个支持的策略
        for (LogCleansingStrategy strategy : strategies) {
            if (strategy.supports(rawContent)) {
                return strategy.cleanse(rawContent);
            }
        }
        
        // 没有找到合适的策略，返回失败结果
        return CleansingResult.failure(rawContent, "No suitable cleansing strategy found");
    }

    /**
     * 获取所有可用的策略名称
     */
    public List<String> getAvailableStrategies() {
        return strategies.stream()
                .map(LogCleansingStrategy::getStrategyName)
                .toList();
    }
}
