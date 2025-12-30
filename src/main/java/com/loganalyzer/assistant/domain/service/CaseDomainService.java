package com.loganalyzer.assistant.domain.service;

import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.repository.CaseRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 案例领域服务
 * 
 * DDD概念：领域服务（Domain Service）
 * - 处理跨实体的业务逻辑
 * - 案例去重检查等业务规则
 */
@Slf4j
public class CaseDomainService {
    
    private final CaseRepository caseRepository;
    
    public CaseDomainService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }
    
    /**
     * 检查案例是否已存在（基于标题和模块名）
     * 
     * 业务规则：同一模块下不能有相同标题的案例
     */
    public boolean isDuplicate(String title, String moduleName) {
        if (title == null || title.isBlank()) {
            return false;
        }
        Optional<CaseEntry> existing = caseRepository.findByTitleAndModuleName(title.trim(), moduleName);
        return existing.isPresent();
    }
    
    /**
     * 检查案例是否已存在（排除指定ID，用于更新时检查）
     */
    public boolean isDuplicateExcludingId(String title, String moduleName, String excludeId) {
        if (title == null || title.isBlank()) {
            return false;
        }
        Optional<CaseEntry> existing = caseRepository.findByTitleAndModuleName(title.trim(), moduleName);
        if (existing.isEmpty()) {
            return false;
        }
        // 如果找到的是当前正在编辑的案例，则不算重复
        return !existing.get().getId().value().equals(excludeId);
    }
    
    /**
     * 查找已存在的案例（用于导入时更新）
     */
    public Optional<CaseEntry> findExisting(String title, String moduleName) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return caseRepository.findByTitleAndModuleName(title.trim(), moduleName);
    }
}
