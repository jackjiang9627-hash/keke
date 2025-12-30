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

from gateway import PluginBase


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
            'compute_similarity'
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
