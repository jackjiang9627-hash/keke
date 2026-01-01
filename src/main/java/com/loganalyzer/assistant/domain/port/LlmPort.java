package com.loganalyzer.assistant.domain.port;

import java.util.List;
import java.util.Map;

/**
 * LLM端口接口
 * 
 * DDD概念：端口（Port）
 * - 定义在领域层
 * - 由基础设施层实现
 * - 用于与大语言模型进行对话
 */
public interface LlmPort {
    
    /**
     * 与大模型对话
     * 
     * @param messages 对话历史
     * @param backend 后端名称（qwen/ollama/mock，可选）
     * @param model 模型名称（可选）
     * @return 回复内容
     */
    String chat(List<Map<String, String>> messages, String backend, String model);
    
    /**
     * 简单对话（使用默认后端和模型）
     * 
     * @param message 用户消息
     * @return 回复内容
     */
    String chat(String message);
    
    /**
     * 获取可用的后端列表
     * 
     * @return 后端信息列表
     */
    List<Map<String, Object>> getBackends();
    
    /**
     * 获取指定后端的可用模型
     * 
     * @param backend 后端名称（可选，为空则返回所有）
     * @return 模型列表
     */
    Map<String, List<String>> getModels(String backend);
    
    /**
     * 设置默认后端
     * 
     * @param backend 后端名称
     * @return 是否设置成功
     */
    boolean setDefaultBackend(String backend);
    
    /**
     * 检查LLM服务是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}
