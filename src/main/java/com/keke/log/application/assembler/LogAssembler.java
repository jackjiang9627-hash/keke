package com.keke.log.application.assembler;

import com.keke.log.application.dto.LogOutputDTO;
import com.keke.log.application.dto.LogStatisticsDTO;
import com.keke.log.domain.entity.LogEntry;
import com.keke.log.domain.service.LogAnalysisService;
import com.keke.log.domain.valueobject.LogSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志DTO组装器
 * 
 * DDD概念：组装器（Assembler）
 * - 负责领域对象和DTO之间的转换
 * - 隔离领域层和应用层
 * - 可以根据不同场景组装不同的DTO
 */
public class LogAssembler {

    /**
     * 将领域实体转换为输出DTO
     */
    public static LogOutputDTO toOutputDTO(LogEntry logEntry) {
        if (logEntry == null) {
            return null;
        }

        LogSource source = logEntry.getSource();
        
        // message 优先使用清洗后的内容，否则使用原始内容
        String message = logEntry.getCleanedContent() != null 
                ? logEntry.getCleanedContent() 
                : logEntry.getRawContent();
        
        return LogOutputDTO.builder()
                .id(logEntry.getId().getValue())
                .rawContent(logEntry.getRawContent())
                .cleanedContent(logEntry.getCleanedContent())
                .message(message)
                .level(logEntry.getLevel().name())
                .levelDescription(logEntry.getLevel().getDescription())
                .application(source != null ? source.getApplication() : null)
                .host(source != null ? source.getHost() : null)
                .environment(source != null ? source.getEnvironment() : null)
                .timestamp(logEntry.getTimestamp())
                .cleaned(logEntry.isCleaned())
                .metadata(logEntry.getMetadata())
                .build();
    }

    /**
     * 批量转换为输出DTO
     */
    public static List<LogOutputDTO> toOutputDTOList(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return List.of();
        }
        return logEntries.stream()
                .map(LogAssembler::toOutputDTO)
                .toList();
    }

    /**
     * 将统计结果转换为DTO
     */
    public static LogStatisticsDTO toStatisticsDTO(LogAnalysisService.LogStatistics statistics) {
        if (statistics == null) {
            return new LogStatisticsDTO();
        }

        // 将LogLevel枚举转换为字符串键
        Map<String, Long> countByLevel = new HashMap<>();
        statistics.getCountByLevel().forEach((level, count) -> 
            countByLevel.put(level.name(), count)
        );

        return new LogStatisticsDTO(
                statistics.getTotalCount(),
                countByLevel,
                statistics.getCountByApplication(),
                statistics.getErrorCount(),
                statistics.getWarningCount(),
                statistics.getErrorRate()
        );
    }
}
