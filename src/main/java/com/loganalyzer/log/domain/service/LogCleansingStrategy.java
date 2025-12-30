package com.loganalyzer.log.domain.service;

import com.loganalyzer.log.domain.valueobject.CleansingResult;

/**
 * 日志清洗策略接口
 * 
 * DDD概念：领域服务（Domain Service）
 * - 当业务逻辑不适合放在实体或值对象中时，使用领域服务
 * - 策略模式：支持多种清洗策略
 */
public interface LogCleansingStrategy {

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 判断是否支持该格式的日志
     */
    boolean supports(String rawContent);

    /**
     * 执行清洗
     */
    CleansingResult cleanse(String rawContent);
}
