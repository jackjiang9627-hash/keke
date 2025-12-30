package com.loganalyzer.assistant.infrastructure.adapter;

import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.port.SemanticSearchPort;
import com.loganalyzer.shared.infrastructure.python.PythonBridge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于Python的语义搜索适配器
 * 
 * 使用Py4J调用Python端的语义模型（如sentence-transformers）
 * 计算真正的语义向量
 */
@Slf4j
@Primary
@Component
public class PythonSemanticSearchAdapter implements SemanticSearchPort {
    
    private final PythonBridge pythonBridge;
    private final SimpleSemanticSearchAdapter fallbackAdapter;
    
    public PythonSemanticSearchAdapter(PythonBridge pythonBridge, 
                                        SimpleSemanticSearchAdapter fallbackAdapter) {
        this.pythonBridge = pythonBridge;
        this.fallbackAdapter = fallbackAdapter;
    }
    
    @Override
    public float[] computeEmbedding(String text) {
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用降级方案");
            return fallbackAdapter.computeEmbedding(text);
        }
        
        try {
            return pythonBridge.computeEmbedding(text);
        } catch (Exception e) {
            log.error("Python计算语义向量失败，使用降级方案", e);
            return fallbackAdapter.computeEmbedding(text);
        }
    }
    
    @Override
    public List<float[]> computeBatchEmbeddings(List<String> texts) {
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用降级方案");
            return fallbackAdapter.computeBatchEmbeddings(texts);
        }
        
        try {
            return pythonBridge.computeBatchEmbeddings(texts);
        } catch (Exception e) {
            log.error("Python批量计算语义向量失败，使用降级方案", e);
            return fallbackAdapter.computeBatchEmbeddings(texts);
        }
    }
    
    @Override
    public List<CaseEntry> semanticSearch(String query, List<CaseEntry> candidates, int topK) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 计算查询向量
        float[] queryEmbedding = computeEmbedding(query);
        
        // 计算所有候选案例的相似度
        List<Map.Entry<CaseEntry, Double>> scoredCases = new ArrayList<>();
        
        for (CaseEntry caseEntry : candidates) {
            // 获取案例的语义向量（从embedding字段或实时计算）
            float[] caseEmbedding = getCaseEmbedding(caseEntry);
            double similarity = cosineSimilarity(queryEmbedding, caseEmbedding);
            scoredCases.add(new AbstractMap.SimpleEntry<>(caseEntry, similarity));
        }
        
        // 按相似度降序排序
        scoredCases.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 返回前topK个
        List<CaseEntry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scoredCases.size()); i++) {
            result.add(scoredCases.get(i).getKey());
        }
        
        return result;
    }
    
    /**
     * 获取案例的语义向量
     * 优先使用已存储的向量，否则实时计算
     */
    private float[] getCaseEmbedding(CaseEntry caseEntry) {
        // 如果案例已有向量，直接使用
        if (caseEntry.getEmbedding() != null && caseEntry.getEmbedding().length > 0) {
            return caseEntry.getEmbedding();
        }
        
        // 否则实时计算
        String text = buildCaseText(caseEntry);
        return computeEmbedding(text);
    }
    
    /**
     * 构建案例的文本表示
     */
    private String buildCaseText(CaseEntry caseEntry) {
        StringBuilder sb = new StringBuilder();
        if (caseEntry.getTitle() != null) {
            sb.append(caseEntry.getTitle()).append(" ");
        }
        if (caseEntry.getSummary() != null) {
            sb.append(caseEntry.getSummary()).append(" ");
        }
        if (caseEntry.getContent() != null) {
            sb.append(caseEntry.getContent());
        }
        return sb.toString().trim();
    }
    
    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
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
        return pythonBridge.isAvailable();
    }
}
