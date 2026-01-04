package com.keke.assistant.infrastructure.config;

import com.keke.assistant.domain.repository.CaseRepository;
import com.keke.assistant.domain.service.CaseDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作助手领域服务配置类
 * 
 * DDD概念：配置（Configuration）
 * - 在基础设施层配置领域服务的依赖
 */
@Configuration
public class DomainServiceConfig {

    /**
     * 配置案例领域服务
     */
    @Bean
    public CaseDomainService caseDomainService(CaseRepository caseRepository) {
        return new CaseDomainService(caseRepository);
    }
}
