#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
语义分析插件

提供语义处理能力：
- compute_embedding: 计算单个文本的语义向量
- compute_batch_embeddings: 批量计算语义向量
- compute_similarity: 计算两个文本的相似度
"""

import logging
import numpy as np
from typing import List, Any

# 添加父目录到路径以导入PluginBase
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from plugin_base import PluginBase


class SemanticPlugin(PluginBase):
    """语义分析插件"""
    
    def __init__(self):
        self.logger = logging.getLogger('SemanticPlugin')
        self.model = None
        self.model_name = 'paraphrase-multilingual-MiniLM-L12-v2'
    
    def get_name(self) -> str:
        return 'semantic'
    
    def get_methods(self) -> list:
        return [
            'compute_embedding',
            'compute_batch_embeddings',
            'compute_similarity',
            'semantic_search'  # 语义搜索：返回匹配的案例ID和相似度
        ]
    
    def initialize(self):
        """初始化：加载语义模型"""
        try:
            from sentence_transformers import SentenceTransformer
            
            self.logger.info(f"加载语义模型: {self.model_name}")
            self.model = SentenceTransformer(self.model_name)
            self.logger.info("语义模型加载完成")
            
        except ImportError:
            self.logger.warning("sentence-transformers未安装，使用简化的TF-IDF方案")
            self.model = None
        except Exception as e:
            self.logger.error(f"加载语义模型失败: {e}")
            self.model = None
    
    def execute(self, method: str, params: dict) -> Any:
        """执行方法"""
        if method == 'compute_embedding':
            return self._compute_embedding(params)
        elif method == 'compute_batch_embeddings':
            return self._compute_batch_embeddings(params)
        elif method == 'compute_similarity':
            return self._compute_similarity(params)
        elif method == 'semantic_search':
            return self._semantic_search(params)
        else:
            raise ValueError(f"未知方法: {method}, 支持的方法: {self.get_methods()}")
    
    def _compute_embedding(self, params: dict) -> dict:
        """
        计算单个文本的语义向量
        
        Args:
            params: {"text": "文本内容"}
            
        Returns:
            {"embedding": [0.1, 0.2, ...]}
        """
        text = params.get('text', '')
        
        if not text:
            raise ValueError("text参数不能为空")
        
        if self.model is not None:
            embedding = self.model.encode(text, convert_to_numpy=True)
            return {'embedding': embedding.tolist()}
        else:
            # 降级方案：使用简化的hash向量
            embedding = self._simple_hash_embedding(text)
            return {'embedding': embedding}
    
    def _compute_batch_embeddings(self, params: dict) -> dict:
        """
        批量计算语义向量
        
        Args:
            params: {"texts": ["文本1", "文本2", ...]}
            
        Returns:
            {"embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...], ...]}
        """
        texts = params.get('texts', [])
        
        if not texts:
            raise ValueError("texts参数不能为空")
        
        if self.model is not None:
            embeddings = self.model.encode(texts, convert_to_numpy=True)
            return {'embeddings': embeddings.tolist()}
        else:
            # 降级方案
            embeddings = [self._simple_hash_embedding(t) for t in texts]
            return {'embeddings': embeddings}
    
    def _compute_similarity(self, params: dict) -> dict:
        """
        计算两个文本的相似度
        
        Args:
            params: {"text1": "文本1", "text2": "文本2"}
            
        Returns:
            {"similarity": 0.85}
        """
        text1 = params.get('text1', '')
        text2 = params.get('text2', '')
        
        if not text1 or not text2:
            raise ValueError("text1和text2参数不能为空")
        
        if self.model is not None:
            embeddings = self.model.encode([text1, text2], convert_to_numpy=True)
            similarity = self._cosine_similarity(embeddings[0], embeddings[1])
        else:
            # 降级方案
            emb1 = self._simple_hash_embedding(text1)
            emb2 = self._simple_hash_embedding(text2)
            similarity = self._cosine_similarity(
                np.array(emb1), 
                np.array(emb2)
            )
        
        return {'similarity': float(similarity)}
    
    def _semantic_search(self, params: dict) -> dict:
        """
        语义搜索：计算查询与候选案例的相似度，返回超过阈值的结果
        
        所有计算在Python端完成，Java端不参与计算
        
        Args:
            params: {
                "query": "查询文本",
                "candidates": [
                    {"id": "case1", "text": "案例文本1", "embedding": [...]},
                    {"id": "case2", "text": "案例文本2", "embedding": [...]},
                    ...
                ],
                "threshold": 0.7,  # 相似度阈值
                "top_k": 3         # 最多返回数量
            }
            
        Returns:
            {
                "matches": [
                    {"id": "case1", "score": 0.85},
                    {"id": "case2", "score": 0.72}
                ]
            }
        """
        query = params.get('query', '')
        candidates = params.get('candidates', [])
        threshold = params.get('threshold', 0.7)
        top_k = params.get('top_k', 3)
        
        if not query:
            raise ValueError("query参数不能为空")
        
        if not candidates:
            return {'matches': []}
        
        self.logger.info(f"语义搜索: query={query[:50]}..., 候选数={len(candidates)}, 阈值={threshold}")
        
        # 计算查询向量
        if self.model is not None:
            query_embedding = self.model.encode(query, convert_to_numpy=True)
        else:
            query_embedding = np.array(self._simple_hash_embedding(query))
        
        query_dim = len(query_embedding)
        self.logger.debug(f"查询向量维度: {query_dim}")
        
        # 计算每个候选的相似度
        results = []
        query_lower = query.strip().lower()
        
        for candidate in candidates:
            case_id = candidate.get('id', '')
            case_text = candidate.get('text', '')
            case_embedding = candidate.get('embedding')
            
            # 精确匹配优先：如果查询完全包含在案例文本中，或完全相等，给高分
            if case_text:
                case_text_lower = case_text.strip().lower()
                # 完全匹配
                if query_lower == case_text_lower:
                    results.append({'id': case_id, 'score': 1.0})
                    self.logger.info(f"精确匹配: id={case_id}, text='{case_text}'")
                    continue
                # 查询是案例文本的子串（标题包含查询）
                if query_lower in case_text_lower:
                    results.append({'id': case_id, 'score': 0.95})
                    self.logger.info(f"包含匹配: id={case_id}, text='{case_text}'")
                    continue
                # 案例文本是查询的子串
                if case_text_lower in query_lower:
                    results.append({'id': case_id, 'score': 0.90})
                    self.logger.info(f"反向包含匹配: id={case_id}, text='{case_text}'")
                    continue
            
            # 语义匹配
            # 如果有预计算的向量且维度匹配，则直接使用；否则实时计算
            if case_embedding and len(case_embedding) == query_dim:
                case_vec = np.array(case_embedding)
            elif case_text:
                # 维度不匹配或无向量，实时计算
                if self.model is not None:
                    case_vec = self.model.encode(case_text, convert_to_numpy=True)
                else:
                    case_vec = np.array(self._simple_hash_embedding(case_text))
            else:
                continue
            
            # 计算相似度
            similarity = self._cosine_similarity(query_embedding, case_vec)
            
            if similarity >= threshold:
                results.append({'id': case_id, 'score': float(similarity)})
        
        # 按相似度降序排序
        results.sort(key=lambda x: x['score'], reverse=True)
        
        # 取Top K
        matches = results[:top_k]
        
        self.logger.info(f"语义搜索完成: 匹配数={len(matches)}")
        
        return {'matches': matches}
    
    def _cosine_similarity(self, vec1: np.ndarray, vec2: np.ndarray) -> float:
        """计算余弦相似度"""
        norm1 = np.linalg.norm(vec1)
        norm2 = np.linalg.norm(vec2)
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        return float(np.dot(vec1, vec2) / (norm1 * norm2))
    
    def _simple_hash_embedding(self, text: str, dim: int = 384) -> List[float]:
        """
        简化的hash embedding（降级方案）
        
        使用字符hash生成固定维度的向量
        """
        embedding = [0.0] * dim
        
        # 基于字符位置和字符值生成向量
        for i, char in enumerate(text):
            idx = (hash(char) + i) % dim
            embedding[idx] += 1.0
        
        # 归一化
        total = sum(embedding)
        if total > 0:
            embedding = [v / total for v in embedding]
        
        return embedding
    
    def shutdown(self):
        """关闭插件"""
        self.model = None
        self.logger.info("语义插件已关闭")
