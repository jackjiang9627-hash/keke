package com.loganalyzer.assistant.infrastructure.adapter;

import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.port.SemanticSearchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单语义搜索适配器（降级方案）
 * 
 * DDD概念：适配器（Adapter）- 六边形架构
 * - 实现领域层定义的端口接口
 * - 使用基于关键词的简单语义匹配（TF-IDF变体）
 * 
 * TODO: 这是当Python服务不可用时的降级方案，需要优化：
 *  - 改进中文分词算法（当前为简单字符级分词）
 *  - 增加更多停用词
 *  - 实现更准确的语义相似度计算
 *  - 考虑使用轻量级本地模型（如fastText）
 * 
 * 注：主要方案应使用PythonSemanticSearchAdapter
 */
@Slf4j
@Component
public class SimpleSemanticSearchAdapter implements SemanticSearchPort {
    
    // 向量维度
    private static final int VECTOR_SIZE = 128;
    
    // 停用词列表（简化版）
    private static final Set<String> STOP_WORDS = Set.of(
        "的", "是", "在", "了", "和", "与", "或", "等", "如", "这", "那", "有", "为",
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "at", "by", "for", "with", "about", "against", "between", "into", "through",
        "to", "from", "up", "down", "in", "out", "on", "off", "over", "under"
    );
    
    @Override
    public float[] computeEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[VECTOR_SIZE];
        }
        
        // 提取关键词并计算词频向量
        Map<String, Integer> wordFreq = tokenize(text);
        
        // 转换为固定长度的向量（使用哈希技巧）
        float[] vector = new float[VECTOR_SIZE];
        
        for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
            int hash = Math.abs(entry.getKey().hashCode()) % VECTOR_SIZE;
            vector[hash] += entry.getValue();
        }
        
        // 归一化
        normalize(vector);
        
        return vector;
    }
    
    @Override
    public List<float[]> computeBatchEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(computeEmbedding(text));
        }
        return results;
    }
    
    @Override
    public List<CaseEntry> semanticSearch(String query, List<CaseEntry> candidates, int topK) {
        if (query == null || query.isBlank() || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.debug("语义搜索(降级): query={}, candidates={}", query, candidates.size());
        
        // 计算查询向量
        float[] queryVector = computeEmbedding(query);
        
        // 计算每个候选的相似度
        List<ScoredCase> scoredCases = new ArrayList<>();
        for (CaseEntry candidate : candidates) {
            float[] candidateVector = candidate.getEmbedding();
            if (candidateVector == null || candidateVector.length == 0) {
                candidateVector = computeEmbedding(candidate.getSearchableText());
            }
            
            double similarity = cosineSimilarity(queryVector, candidateVector);
            
            // 额外加分：如果标题包含查询词
            if (candidate.getTitle() != null && 
                candidate.getTitle().toLowerCase().contains(query.toLowerCase())) {
                similarity += 0.3;
            }
            
            scoredCases.add(new ScoredCase(candidate, similarity));
        }
        
        // 按相似度排序并返回Top K
        return scoredCases.stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(topK)
            .filter(sc -> sc.score > 0.1)  // 过滤低相似度
            .map(sc -> sc.caseEntry)
            .collect(Collectors.toList());
    }
    
    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0) {
            return 0.0;
        }
        
        int len = Math.min(vec1.length, vec2.length);
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < len; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    @Override
    public boolean isAvailable() {
        // 降级方案始终可用
        return true;
    }
    
    /**
     * 分词并计算词频
     * 
     * TODO: 改进分词算法
     *  - 集成专业的中文分词库（如jieba、HanLP）
     *  - 支持词性标注和关键词提取
     *  - 增强英文词干提取
     */
    private Map<String, Integer> tokenize(String text) {
        Map<String, Integer> wordFreq = new HashMap<>();
        
        // 简单分词：按空格和标点分割
        String[] tokens = text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ")
            .split("\\s+");
        
        for (String token : tokens) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            wordFreq.merge(token, 1, Integer::sum);
        }
        
        // 对于中文，进行简单的字符级分词
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                String charStr = String.valueOf(c);
                wordFreq.merge(charStr, 1, Integer::sum);
            }
        }
        
        return wordFreq;
    }
    
    /**
     * L2归一化
     */
    private void normalize(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0) {
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
    
    private record ScoredCase(CaseEntry caseEntry, double score) {}
}
