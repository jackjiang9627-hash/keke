package com.loganalyzer.log.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 日志统计DTO - 用于返回统计分析结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogStatisticsDTO {

    private long totalCount;
    private Map<String, Long> countByLevel;
    private Map<String, Long> countByApplication;
    private long errorCount;
    private long warningCount;
    private double errorRate;
}
