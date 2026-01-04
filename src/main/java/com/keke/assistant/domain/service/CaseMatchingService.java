package com.keke.assistant.domain.service;

import com.keke.assistant.domain.entity.CaseEntry;
import com.keke.assistant.domain.port.SemanticSearchPort;
import com.keke.assistant.domain.port.SemanticSearchPort.CaseWithScore;
import com.keke.assistant.domain.repository.CaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 案例匹配服务
 * 
 * DDD概念：领域服务（Domain Service）
 * 职责：负责从案例库中语义匹配相关案例并构建回复
 * 
 * 符合单一职责原则(SRP)：只负责案例匹配相关的业务逻辑
 */
@Slf4j
@Service
public class CaseMatchingService {
    
    /** 默认语义匹配阈值 */
    private static final double DEFAULT_THRESHOLD = 0.70;
    
    /** 默认最大返回案例数 */
    private static final int DEFAULT_MAX_MATCHES = 3;
    
    /** 案例获取最大数量 */
    private static final int MAX_CASE_FETCH_SIZE = 500;
    
    private final SemanticSearchPort semanticSearchPort;
    private final CaseRepository caseRepository;
    
    public CaseMatchingService(SemanticSearchPort semanticSearchPort,
                               CaseRepository caseRepository) {
        this.semanticSearchPort = semanticSearchPort;
        this.caseRepository = caseRepository;
    }
    
    /**
     * 尝试从案例库匹配答案
     * 
     * @param query 用户问题
     * @param threshold 相似度阈值 (0.0 - 1.0)
     * @return 匹配到的案例回复，或Optional.empty()如果没有匹配
     */
    public Optional<MatchResult> matchCases(String query, double threshold) {
        return matchCases(query, threshold, DEFAULT_MAX_MATCHES);
    }
    
    /**
     * 尝试从案例库匹配答案
     * 
     * @param query 用户问题
     * @param threshold 相似度阈值 (0.0 - 1.0)
     * @param maxMatches 最大返回数量
     * @return 匹配到的案例回复，或Optional.empty()如果没有匹配
     */
    public Optional<MatchResult> matchCases(String query, double threshold, int maxMatches) {
        try {
            // 验证参数
            if (query == null || query.isBlank()) {
                log.warn("查询为空，跳过案例匹配");
                return Optional.empty();
            }
            
            // 规范化阈值
            double normalizedThreshold = normalizeThreshold(threshold);
            
            // 获取所有案例
            List<CaseEntry> allCases = caseRepository.findAll(0, MAX_CASE_FETCH_SIZE);
            if (allCases.isEmpty()) {
                log.debug("案例库为空，跳过语义匹配");
                return Optional.empty();
            }
            
            // 语义搜索
            List<CaseWithScore> matches = semanticSearchPort.semanticSearchWithThreshold(
                query, allCases, normalizedThreshold, maxMatches);
            
            if (matches.isEmpty()) {
                log.info("未找到相似度>={}%的案例", (int)(normalizedThreshold * 100));
                return Optional.empty();
            }
            
            log.info("匹配到{}个相似案例", matches.size());
            
            // 构建回复
            String reply = buildCaseReply(matches);
            return Optional.of(new MatchResult(matches, reply));
            
        } catch (Exception e) {
            log.warn("案例匹配异常: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 使用默认阈值匹配案例
     * 
     * @param query 用户问题
     * @return 匹配结果
     */
    public Optional<MatchResult> matchCasesWithDefaultThreshold(String query) {
        return matchCases(query, DEFAULT_THRESHOLD);
    }
    
    /**
     * 检查是否有匹配的案例
     * 
     * @param query 用户问题
     * @param threshold 相似度阈值
     * @return 是否有匹配
     */
    public boolean hasMatchingCases(String query, double threshold) {
        return matchCases(query, threshold, 1).isPresent();
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 规范化阈值到合理范围
     */
    private double normalizeThreshold(double threshold) {
        if (threshold < 0.0) {
            log.warn("阈值{}小于0，使用最小值0.0", threshold);
            return 0.0;
        }
        if (threshold > 1.0) {
            log.warn("阈值{}大于1，使用最大值1.0", threshold);
            return 1.0;
        }
        return threshold;
    }
    
    /**
     * 构建案例匹配的回复
     * 
     * 采用建造者模式思想，分步构建回复内容
     */
    private String buildCaseReply(List<CaseWithScore> matches) {
        StringBuilder sb = new StringBuilder();
        
        // 头部
        sb.append("📚 **找到以下相关案例：**\n\n");
        
        // 案例列表
        for (int i = 0; i < matches.size(); i++) {
            appendCaseContent(sb, matches.get(i), i + 1);
            
            // 分隔线（最后一个不加）
            if (i < matches.size() - 1) {
                sb.append("---\n\n");
            }
        }
        
        // 尾部
        sb.append("\n---\n_以上内容来自本地案例库，如需更多帮助请继续提问_");
        
        return sb.toString();
    }
    
    /**
     * 追加单个案例内容
     */
    private void appendCaseContent(StringBuilder sb, CaseWithScore match, int index) {
        CaseEntry caseEntry = match.caseEntry();
        int similarityPercent = (int) (match.score() * 100);
        
        // 标题和相似度
        sb.append(String.format("### %d. %s\n", index, caseEntry.getTitle()));
        sb.append(String.format("相似度：**%d%%** | 模块：%s\n\n", 
            similarityPercent, 
            caseEntry.getModuleName() != null ? caseEntry.getModuleName() : "未分类"));
        
        // 摘要
        String summary = caseEntry.getSummary();
        if (summary != null && !summary.isBlank()) {
            sb.append("**摘要：**").append(summary).append("\n\n");
        }
        
        // 详细内容
        String content = caseEntry.getContent();
        if (content != null && !content.isBlank()) {
            sb.append("**详细内容：**\n").append(content).append("\n\n");
        }
        
        // 链接
        String hyperlink = caseEntry.getHyperlink();
        if (hyperlink != null && !hyperlink.isBlank()) {
            sb.append("相关链接：").append(hyperlink).append("\n\n");
        }
    }
    
    // ==================== 结果类 ====================
    
    /**
     * 案例匹配结果
     */
    public static class MatchResult {
        private final List<CaseWithScore> matches;
        private final String formattedReply;
        
        public MatchResult(List<CaseWithScore> matches, String formattedReply) {
            this.matches = matches;
            this.formattedReply = formattedReply;
        }
        
        public List<CaseWithScore> getMatches() {
            return matches;
        }
        
        public String getFormattedReply() {
            return formattedReply;
        }
        
        public int getMatchCount() {
            return matches.size();
        }
        
        public double getTopScore() {
            return matches.isEmpty() ? 0.0 : matches.get(0).score();
        }
    }
}
