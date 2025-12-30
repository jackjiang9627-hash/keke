package com.loganalyzer.assistant.domain.port;

import com.loganalyzer.assistant.domain.entity.CaseEntry;

import java.util.List;

/**
 * 语义搜索端口
 * 
 * DDD概念：端口（Port）- 六边形架构
 * - 定义在领域层，由基础设施层实现
 * - 用于语义向量搜索
 */
public interface SemanticSearchPort {
    
    /**
     * 计算文本的嵌入向量
     */
    float[] computeEmbedding(String text);
    
    /**
     * 批量计算文本的嵌入向量
     */
    List<float[]> computeBatchEmbeddings(List<String> texts);
    
    /**
     * 语义搜索，返回最相似的案例
     * 
     * @param query 查询文本
     * @param candidates 候选案例列表
     * @param topK 返回前K个结果
     * @return 按相似度排序的案例列表
     */
    List<CaseEntry> semanticSearch(String query, List<CaseEntry> candidates, int topK);
    
    /**
     * 计算两个向量的余弦相似度
     */
    double cosineSimilarity(float[] vec1, float[] vec2);
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
