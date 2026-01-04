package com.keke.assistant.infrastructure.adapter;

import com.keke.assistant.domain.entity.CaseEntry;
import com.keke.assistant.domain.port.SemanticSearchPort;
import com.keke.shared.infrastructure.python.PythonBridge;
import com.keke.shared.infrastructure.python.PythonBridge.SemanticMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Python的语义搜索适配器
 * 
 * 所有语义计算和相似度计算均在Python端完成，Java端不参与计算
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
        
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用降级方案");
            return fallbackAdapter.semanticSearch(query, candidates, topK);
        }
        
        try {
            // 调用Python端进行语义搜索
            List<SemanticMatch> matches = callPythonSemanticSearch(query, candidates, 0.0, topK);
            
            // 根据ID查找对应的CaseEntry
            Map<String, CaseEntry> caseMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getId().value(), c -> c));
            
            return matches.stream()
                .map(m -> caseMap.get(m.id()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Python语义搜索失败，使用降级方案", e);
            return fallbackAdapter.semanticSearch(query, candidates, topK);
        }
    }
    
    @Override
    public List<CaseWithScore> semanticSearchWithThreshold(String query, List<CaseEntry> candidates, 
                                                            double threshold, int topK) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("开始语义匹配: query={}, 候选数={}, 阈值={}", query, candidates.size(), threshold);
        
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用降级方案");
            return fallbackAdapter.semanticSearchWithThreshold(query, candidates, threshold, topK);
        }
        
        try {
            // 调用Python端进行语义搜索（所有计算在Python完成）
            List<SemanticMatch> matches = callPythonSemanticSearch(query, candidates, threshold, topK);
            
            // 根据ID查找对应的CaseEntry并返回带分数的结果
            Map<String, CaseEntry> caseMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getId().value(), c -> c));
            
            List<CaseWithScore> result = matches.stream()
                .map(m -> {
                    CaseEntry entry = caseMap.get(m.id());
                    return entry != null ? new CaseWithScore(entry, m.score()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            log.info("语义匹配完成: 返回结果数={}", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("Python语义搜索失败，使用降级方案", e);
            return fallbackAdapter.semanticSearchWithThreshold(query, candidates, threshold, topK);
        }
    }
    
    /**
     * 调用Python端的语义搜索
     */
    private List<SemanticMatch> callPythonSemanticSearch(String query, List<CaseEntry> candidates, 
                                                          double threshold, int topK) {
        // 构建候选列表：传递ID、文本和已计算的向量
        List<Map<String, Object>> candidateList = new ArrayList<>();
        for (CaseEntry caseEntry : candidates) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", caseEntry.getId().value());
            item.put("text", caseEntry.getSearchableText());
            // 如果有预计算的向量，也传递给Python
            if (caseEntry.getEmbedding() != null && caseEntry.getEmbedding().length > 0) {
                item.put("embedding", toFloatList(caseEntry.getEmbedding()));
            }
            candidateList.add(item);
        }
        
        return pythonBridge.semanticSearch(query, candidateList, threshold, topK);
    }
    
    /**
     * float[]转List<Float>
     */
    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }
    
    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        // 这个方法保留但不应该被调用，所有计算应在Python端完成
        log.warn("不应在Java端调用cosineSimilarity，请使用Python端进行计算");
        return fallbackAdapter.cosineSimilarity(vec1, vec2);
    }
    
    @Override
    public boolean isAvailable() {
        return pythonBridge.isAvailable();
    }
}
