package com.loganalyzer.assistant.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.repository.CaseRepository;
import com.loganalyzer.assistant.domain.valueobject.CaseId;
import com.loganalyzer.assistant.infrastructure.persistence.entity.CaseEntryPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 案例仓储实现
 * 
 * DDD概念：仓储实现（Repository Implementation）
 * - 实现领域层定义的接口
 * - 处理领域对象与持久化对象的转换
 */
@Slf4j
@Repository
public class CaseRepositoryImpl implements CaseRepository {
    
    private final CaseEntryJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    
    public CaseRepositoryImpl(CaseEntryJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CaseEntry save(CaseEntry caseEntry) {
        CaseEntryPO po = toPO(caseEntry);
        CaseEntryPO saved = jpaRepository.save(po);
        return toDomain(saved);
    }
    
    @Override
    public Optional<CaseEntry> findById(CaseId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }
    
    @Override
    public List<CaseEntry> findByModuleName(String moduleName, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return jpaRepository.findByModuleName(moduleName, pageRequest).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public long countByModuleName(String moduleName) {
        return jpaRepository.countByModuleName(moduleName);
    }
    
    @Override
    public List<CaseEntry> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return jpaRepository.findAll(pageRequest).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<CaseEntry> searchByKeyword(String keyword) {
        return jpaRepository.searchByKeyword(keyword).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(CaseId id) {
        jpaRepository.deleteById(id.value());
    }
    
    @Override
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public List<String> findAllModuleNames() {
        return jpaRepository.findAllModuleNames();
    }
    
    @Override
    public Optional<CaseEntry> findByTitleAndModuleName(String title, String moduleName) {
        return jpaRepository.findFirstByTitleAndModuleName(title, moduleName)
            .map(this::toDomain);
    }
    
    @Override
    public List<CaseEntry> findTodayReviewCases() {
        LocalDate today = LocalDate.now();
        return jpaRepository.findByReviewEnabledTrueAndNextReviewDateLessThanEqual(today).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    private CaseEntryPO toPO(CaseEntry entry) {
        CaseEntryPO po = new CaseEntryPO();
        po.setId(entry.getId().value());
        po.setTitle(entry.getTitle());
        po.setSummary(entry.getSummary());
        po.setHyperlink(entry.getHyperlink());
        po.setContent(entry.getContent());
        po.setModuleName(entry.getModuleName());
        po.setCreatedAt(entry.getCreatedAt());
        po.setUpdatedAt(entry.getUpdatedAt());
        
        // AI总结相关字段
        po.setTags(entry.getTags());
        po.setSource(entry.getSource());
        po.setOriginalConversation(entry.getOriginalConversation());
        
        // 复习相关字段
        po.setNextReviewDate(entry.getNextReviewDate());
        po.setReviewCount(entry.getReviewCount());
        po.setMasteryLevel(entry.getMasteryLevel());
        po.setLastReviewTime(entry.getLastReviewTime());
        po.setReviewEnabled(entry.getReviewEnabled());
        
        // 序列化嵌入向量
        if (entry.getEmbedding() != null) {
            try {
                po.setEmbedding(objectMapper.writeValueAsString(entry.getEmbedding()));
            } catch (JsonProcessingException e) {
                log.warn("序列化嵌入向量失败", e);
            }
        }
        
        return po;
    }
    
    private CaseEntry toDomain(CaseEntryPO po) {
        float[] embedding = null;
        if (po.getEmbedding() != null && !po.getEmbedding().isEmpty()) {
            try {
                embedding = objectMapper.readValue(po.getEmbedding(), float[].class);
            } catch (JsonProcessingException e) {
                log.warn("反序列化嵌入向量失败", e);
            }
        }
        
        return CaseEntry.reconstitute(
            CaseId.of(po.getId()),
            po.getTitle(),
            po.getSummary(),
            po.getHyperlink(),
            po.getContent(),
            po.getModuleName(),
            po.getCreatedAt(),
            po.getUpdatedAt(),
            embedding,
            po.getTags(),
            po.getSource(),
            po.getOriginalConversation(),
            po.getNextReviewDate(),
            po.getReviewCount(),
            po.getMasteryLevel(),
            po.getLastReviewTime(),
            po.getReviewEnabled()
        );
    }
}
