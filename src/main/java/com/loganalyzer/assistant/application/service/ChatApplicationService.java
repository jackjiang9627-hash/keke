package com.loganalyzer.assistant.application.service;

import com.loganalyzer.assistant.domain.port.LlmPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 聊天应用服务
 * 
 * DDD概念：应用服务（Application Service）
 * - 编排领域服务和基础设施服务
 * - 管理对话历史
 * - 提供AI对话能力
 */
@Slf4j
@Service
public class ChatApplicationService {
    
    private final LlmPort llmPort;
    
    /** 对话历史缓存（简单实现，生产环境应使用Redis等） */
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();
    
    /** 最大历史消息数 */
    private static final int MAX_HISTORY_SIZE = 20;
    
    public ChatApplicationService(LlmPort llmPort) {
        this.llmPort = llmPort;
    }
    
    /**
     * 发送消息并获取回复
     * 
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param backend 后端名称（可选）
     * @param model 模型名称（可选）
     * @return 回复信息
     */
    public ChatResponse chat(String sessionId, String message, String backend, String model) {
        log.info("处理聊天请求: sessionId={}, message={}", sessionId, message);
        
        // 获取或创建会话历史
        List<Map<String, String>> history = conversationHistory.computeIfAbsent(
            sessionId, k -> new ArrayList<>());
        
        // 添加用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        history.add(userMessage);
        
        // 限制历史长度
        trimHistory(history);
        
        try {
            // 调用LLM
            String reply = llmPort.chat(history, backend, model);
            
            // 添加助手回复到历史
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", reply);
            history.add(assistantMessage);
            
            log.info("聊天回复完成: sessionId={}", sessionId);
            
            return ChatResponse.success(reply);
            
        } catch (Exception e) {
            log.error("聊天失败", e);
            // 移除失败的用户消息
            history.remove(history.size() - 1);
            return ChatResponse.error("AI服务暂时不可用，请稍后重试");
        }
    }
    
    /**
     * 获取可用后端列表
     */
    public List<Map<String, Object>> getBackends() {
        return llmPort.getBackends();
    }
    
    /**
     * 获取可用模型列表
     */
    public Map<String, List<String>> getModels(String backend) {
        return llmPort.getModels(backend);
    }
    
    /**
     * 设置默认后端
     */
    public boolean setDefaultBackend(String backend) {
        return llmPort.setDefaultBackend(backend);
    }
    
    /**
     * 清空会话历史
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("已清空会话历史: sessionId={}", sessionId);
    }
    
    /**
     * 获取会话历史
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 检查LLM服务是否可用
     */
    public boolean isLlmAvailable() {
        return llmPort.isAvailable();
    }
    
    /**
     * 限制历史长度
     */
    private void trimHistory(List<Map<String, String>> history) {
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }
    
    /**
     * 聊天响应
     */
    public static class ChatResponse {
        private final boolean success;
        private final String reply;
        private final String error;
        private final long timestamp;
        
        private ChatResponse(boolean success, String reply, String error) {
            this.success = success;
            this.reply = reply;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public static ChatResponse success(String reply) {
            return new ChatResponse(true, reply, null);
        }
        
        public static ChatResponse error(String error) {
            return new ChatResponse(false, null, error);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getReply() {
            return reply;
        }
        
        public String getError() {
            return error;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("success", success);
            if (success) {
                map.put("reply", reply);
            } else {
                map.put("error", error);
            }
            map.put("timestamp", timestamp);
            return map;
        }
    }
}
