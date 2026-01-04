package com.keke.assistant.application.service;

import com.keke.assistant.application.assembler.CaseAssembler;
import com.keke.assistant.application.dto.CaseInputDTO;
import com.keke.assistant.application.dto.CaseOutputDTO;
import com.keke.shared.application.dto.PageDTO;
import com.keke.assistant.domain.entity.CaseEntry;
import com.keke.assistant.domain.exception.CaseDuplicateException;
import com.keke.assistant.domain.exception.CaseNotFoundException;
import com.keke.assistant.domain.port.SemanticSearchPort;
import com.keke.assistant.domain.repository.CaseRepository;
import com.keke.assistant.domain.service.CaseDomainService;
import com.keke.assistant.domain.valueobject.CaseId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 案例应用服务
 * 
 * DDD概念：应用服务（Application Service）
 * - 编排领域对象完成用例
 * - 不包含业务逻辑
 */
@Slf4j
@Service
@Transactional
public class CaseApplicationService {
    
    private final CaseRepository caseRepository;
    private final SemanticSearchPort semanticSearchPort;
    private final CaseDomainService caseDomainService;
    
    public CaseApplicationService(CaseRepository caseRepository, 
                                  SemanticSearchPort semanticSearchPort,
                                  CaseDomainService caseDomainService) {
        this.caseRepository = caseRepository;
        this.semanticSearchPort = semanticSearchPort;
        this.caseDomainService = caseDomainService;
    }
    
    /**
     * 添加案例（带去重检查）
     */
    public CaseOutputDTO addCase(CaseInputDTO input) {
        log.info("添加案例: title={}, module={}", input.getTitle(), input.getModuleName());
        
        // 使用领域服务检查去重
        if (caseDomainService.isDuplicate(input.getTitle(), input.getModuleName())) {
            log.warn("案例已存在: title={}, module={}", input.getTitle(), input.getModuleName());
            throw new CaseDuplicateException(input.getTitle(), input.getModuleName());
        }
        
        CaseEntry caseEntry = CaseEntry.create(
            input.getTitle(),
            input.getSummary(),
            input.getHyperlink(),
            input.getContent(),
            input.getModuleName(),
            input.getTags(),
            input.getSource(),
            input.getOriginalConversation()
        );
        
        // 设置复习状态
        if (input.getReviewEnabled() != null) {
            caseEntry.setReviewEnabled(input.getReviewEnabled());
        }
        
        // 计算语义向量
        float[] embedding = semanticSearchPort.computeEmbedding(caseEntry.getSearchableText());
        caseEntry.setEmbedding(embedding);
        
        CaseEntry saved = caseRepository.save(caseEntry);
        log.info("案例添加成功: id={}", saved.getId().value());
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
    /**
     * 添加案例（不检查去重，用于导入时已确认不重复）
     */
    public CaseOutputDTO addCaseWithoutDuplicateCheck(CaseInputDTO input) {
        log.info("添加案例(无去重): title={}, module={}", input.getTitle(), input.getModuleName());
        
        CaseEntry caseEntry = CaseEntry.create(
            input.getTitle(),
            input.getSummary(),
            input.getHyperlink(),
            input.getContent(),
            input.getModuleName(),
            input.getTags(),
            input.getSource(),
            input.getOriginalConversation()
        );
        
        // 计算语义向量
        float[] embedding = semanticSearchPort.computeEmbedding(caseEntry.getSearchableText());
        caseEntry.setEmbedding(embedding);
        
        CaseEntry saved = caseRepository.save(caseEntry);
        log.info("案例添加成功: id={}", saved.getId().value());
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
    /**
     * 更新案例（带去重检查）
     */
    public CaseOutputDTO updateCase(String id, CaseInputDTO input) {
        log.info("更新案例: id={}", id);
        
        CaseEntry caseEntry = caseRepository.findById(CaseId.of(id))
            .orElseThrow(() -> new CaseNotFoundException(id));
        
        // 使用领域服务检查去重（排除当前ID）
        if (caseDomainService.isDuplicateExcludingId(input.getTitle(), input.getModuleName(), id)) {
            log.warn("案例已存在: title={}, module={}", input.getTitle(), input.getModuleName());
            throw new CaseDuplicateException(input.getTitle(), input.getModuleName());
        }
        
        caseEntry.update(
            input.getTitle(),
            input.getSummary(),
            input.getHyperlink(),
            input.getContent(),
            input.getModuleName()
        );
        
        // 重新计算语义向量
        float[] embedding = semanticSearchPort.computeEmbedding(caseEntry.getSearchableText());
        caseEntry.setEmbedding(embedding);
        
        CaseEntry saved = caseRepository.save(caseEntry);
        log.info("案例更新成功: id={}", saved.getId().value());
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
    /**
     * 删除案例
     */
    public void deleteCase(String id) {
        log.info("删除案例: id={}", id);
        caseRepository.deleteById(CaseId.of(id));
    }
    
    /**
     * 根据ID获取案例
     */
    @Transactional(readOnly = true)
    public CaseOutputDTO getCase(String id) {
        return caseRepository.findById(CaseId.of(id))
            .map(CaseAssembler::toOutputDTO)
            .orElse(null);
    }
    
    /**
     * 获取指定模块的案例（分页）
     */
    @Transactional(readOnly = true)
    public PageDTO<CaseOutputDTO> getCasesByModule(String moduleName, int page, int size) {
        List<CaseOutputDTO> content;
        long total;
        
        if (moduleName == null || moduleName.isEmpty()) {
            content = CaseAssembler.toOutputDTOList(caseRepository.findAll(page, size));
            total = caseRepository.count();
        } else {
            content = CaseAssembler.toOutputDTOList(caseRepository.findByModuleName(moduleName, page, size));
            total = caseRepository.countByModuleName(moduleName);
        }
        
        return PageDTO.of(content, page, size, total);
    }
    
    /**
     * 获取所有案例（分页）
     */
    @Transactional(readOnly = true)
    public PageDTO<CaseOutputDTO> getAllCases(int page, int size) {
        List<CaseOutputDTO> content = CaseAssembler.toOutputDTOList(caseRepository.findAll(page, size));
        long total = caseRepository.count();
        return PageDTO.of(content, page, size, total);
    }
    
    /**
     * 搜索案例（精确匹配 + 语义匹配）
     */
    @Transactional(readOnly = true)
    public List<CaseOutputDTO> searchCases(String query) {
        log.info("搜索案例: query={}", query);
        
        // 1. 精确匹配
        List<CaseEntry> exactMatches = caseRepository.searchByKeyword(query);
        
        // 2. 语义匹配（Top 3）
        List<CaseEntry> allCases = caseRepository.findAll(0, 100);
        List<CaseEntry> semanticMatches = semanticSearchPort.semanticSearch(query, allCases, 3);
        
        // 3. 合并结果（去重）
        List<CaseEntry> results = new ArrayList<>(exactMatches);
        for (CaseEntry semantic : semanticMatches) {
            boolean exists = results.stream()
                .anyMatch(r -> r.getId().value().equals(semantic.getId().value()));
            if (!exists) {
                results.add(semantic);
            }
        }
        
        log.info("搜索结果: 精确匹配={}, 语义匹配={}, 总计={}", 
            exactMatches.size(), semanticMatches.size(), results.size());
        
        return CaseAssembler.toOutputDTOList(results);
    }
    
    /**
     * 获取所有模块名称（动态从数据库获取）
     */
    @Transactional(readOnly = true)
    public List<String> getAllModuleNames() {
        List<String> modules = caseRepository.findAllModuleNames();
        // 如果没有模块，返回默认模块
        if (modules.isEmpty()) {
            modules = new ArrayList<>();
            modules.add("集群测试");
        }
        return modules;
    }
    
    // === 复习相关方法 ===
    
    /**
     * 获取今日待复习的案例
     */
    @Transactional(readOnly = true)
    public List<CaseOutputDTO> getTodayReviewCases() {
        log.info("获取今日待复习案例");
        List<CaseEntry> cases = caseRepository.findTodayReviewCases();
        log.info("今日待复习案例数量: {}", cases.size());
        return CaseAssembler.toOutputDTOList(cases);
    }
    
    /**
     * 标记案例已复习
     * @param id 案例ID
     * @param mastered 是否已掌握
     */
    public CaseOutputDTO markCaseReviewed(String id, boolean mastered) {
        log.info("标记案例已复习: id={}, mastered={}", id, mastered);
        
        CaseEntry caseEntry = caseRepository.findById(CaseId.of(id))
            .orElseThrow(() -> new CaseNotFoundException(id));
        
        caseEntry.markAsReviewed(mastered);
        CaseEntry saved = caseRepository.save(caseEntry);
        
        log.info("案例复习状态已更新: id={}, nextReviewDate={}, masteryLevel={}", 
            id, saved.getNextReviewDate(), saved.getMasteryLevel());
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
    /**
     * 延后案例复习到明天
     */
    public CaseOutputDTO postponeCaseReview(String id) {
        log.info("延后案例复习: id={}", id);
        
        CaseEntry caseEntry = caseRepository.findById(CaseId.of(id))
            .orElseThrow(() -> new CaseNotFoundException(id));
        
        caseEntry.postponeReview();
        CaseEntry saved = caseRepository.save(caseEntry);
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
    /**
     * 切换案例复习启用状态
     */
    public CaseOutputDTO toggleCaseReview(String id, boolean enabled) {
        log.info("切换案例复习状态: id={}, enabled={}", id, enabled);
        
        CaseEntry caseEntry = caseRepository.findById(CaseId.of(id))
            .orElseThrow(() -> new CaseNotFoundException(id));
        
        caseEntry.setReviewEnabled(enabled);
        CaseEntry saved = caseRepository.save(caseEntry);
        
        return CaseAssembler.toOutputDTO(saved);
    }
    
}
