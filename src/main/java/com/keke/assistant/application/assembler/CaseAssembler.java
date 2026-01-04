package com.keke.assistant.application.assembler;

import com.keke.assistant.application.dto.CaseOutputDTO;
import com.keke.assistant.domain.entity.CaseEntry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案例装配器
 * 
 * DDD概念：装配器（Assembler）
 * - 负责领域对象与DTO之间的转换
 * - 保持应用服务的职责单一
 */
public final class CaseAssembler {
    
    private static final int DEFAULT_SUMMARY_MAX_LENGTH = 100;
    
    private CaseAssembler() {
        // 工具类，禁止实例化
    }
    
    /**
     * 领域实体转换为输出DTO
     */
    public static CaseOutputDTO toOutputDTO(CaseEntry entry) {
        return toOutputDTO(entry, DEFAULT_SUMMARY_MAX_LENGTH);
    }
    
    /**
     * 领域实体转换为输出DTO（自定义摘要长度）
     */
    public static CaseOutputDTO toOutputDTO(CaseEntry entry, int summaryMaxLength) {
        if (entry == null) {
            return null;
        }
        
        return CaseOutputDTO.builder()
            .id(entry.getId().value())
            .title(entry.getTitle())
            .summary(entry.getSummary())
            .truncatedSummary(entry.getTruncatedSummary(summaryMaxLength))
            .hyperlink(entry.getHyperlink())
            .content(entry.getContent())
            .moduleName(entry.getModuleName())
            .hasHyperlink(entry.hasHyperlink())
            .createdAt(entry.getCreatedAt())
            .updatedAt(entry.getUpdatedAt())
            // AI总结相关
            .tags(entry.getTags())
            .source(entry.getSource())
            .fromAiChat(entry.isFromAiChat())
            // 复习相关
            .nextReviewDate(entry.getNextReviewDate())
            .reviewCount(entry.getReviewCount())
            .masteryLevel(entry.getMasteryLevel())
            .lastReviewTime(entry.getLastReviewTime())
            .reviewEnabled(entry.getReviewEnabled())
            .needsReviewToday(entry.needsReviewToday())
            .build();
    }
    
    /**
     * 批量转换为输出DTO
     */
    public static List<CaseOutputDTO> toOutputDTOList(List<CaseEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
            .map(CaseAssembler::toOutputDTO)
            .collect(Collectors.toList());
    }
}
