package com.loganalyzer.assistant.domain.repository;

import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.valueobject.CaseId;

import java.util.List;
import java.util.Optional;

/**
 * 案例仓储接口
 * 
 * DDD概念：仓储（Repository）
 * - 定义在领域层
 * - 由基础设施层实现
 */
public interface CaseRepository {
    
    /**
     * 保存案例
     */
    CaseEntry save(CaseEntry caseEntry);
    
    /**
     * 根据ID查找
     */
    Optional<CaseEntry> findById(CaseId id);
    
    /**
     * 根据模块名称查找案例（分页）
     */
    List<CaseEntry> findByModuleName(String moduleName, int page, int size);
    
    /**
     * 根据模块名称统计数量
     */
    long countByModuleName(String moduleName);
    
    /**
     * 查找所有案例（分页）
     */
    List<CaseEntry> findAll(int page, int size);
    
    /**
     * 精确搜索（标题或摘要包含关键词）
     */
    List<CaseEntry> searchByKeyword(String keyword);
    
    /**
     * 根据ID删除
     */
    void deleteById(CaseId id);
    
    /**
     * 统计案例数量
     */
    long count();
    
    /**
     * 获取所有模块名称（去重）
     */
    List<String> findAllModuleNames();
    
    /**
     * 根据标题和模块名称查找案例（用于去重检查）
     */
    Optional<CaseEntry> findByTitleAndModuleName(String title, String moduleName);
    
    /**
     * 查找今日待复习的案例
     */
    List<CaseEntry> findTodayReviewCases();
}
