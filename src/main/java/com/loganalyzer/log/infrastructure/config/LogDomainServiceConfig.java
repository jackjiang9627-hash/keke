package com.loganalyzer.log.infrastructure.config;

import com.loganalyzer.log.domain.service.LogAnalysisService;
import com.loganalyzer.log.domain.service.LogCleansingService;
import com.loganalyzer.log.domain.service.LogCleansingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 日志分析领域服务配置类
 * 
 * DDD概念：配置（Configuration）
 * - 在基础设施层配置领域服务的依赖
 * - 组装领域服务需要的策略和组件
 */
@Configuration
public class LogDomainServiceConfig {

    /**
     * 配置日志清洗领域服务
     * 
     * Spring会自动注入所有实现了LogCleansingStrategy接口的Bean
     * 按@Order注解排序
     */
    @Bean
    public LogCleansingService logCleansingService(List<LogCleansingStrategy> strategies) {
        return new LogCleansingService(strategies);
    }

    /**
     * 配置日志分析领域服务
     */
    @Bean
    public LogAnalysisService logAnalysisService() {
        return new LogAnalysisService();
    }
}
