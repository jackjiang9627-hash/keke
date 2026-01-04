package com.keke.assistant.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话历史管理器
 * 
 * DDD概念：领域服务（Domain Service）
 * 职责：管理会话的对话历史
 * 
 * 注意：当前使用内存存储，生产环境应使用Redis等分布式缓存
 */
@Slf4j
@Service
public class ConversationHistoryManager {
    
    /** 默认最大历史消息数 */
    private static final int DEFAULT_MAX_HISTORY_SIZE = 20;
    
    /** 对话历史缓存 - 使用线程安全的ConcurrentHashMap */
    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    
    /** 最大历史消息数（可配置） */
    private int maxHistorySize = DEFAULT_MAX_HISTORY_SIZE;
    
    /**
     * 添加用户消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     */
    public void addUserMessage(String sessionId, String content) {
        List<ChatMessage> history = getOrCreateHistory(sessionId);
        history.add(ChatMessage.user(content));
        trimHistory(history);
        log.debug("添加用户消息: sessionId={}, historySize={}", sessionId, history.size());
    }
    
    /**
     * 添加助手消息
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     */
    public void addAssistantMessage(String sessionId, String content) {
        List<ChatMessage> history = getOrCreateHistory(sessionId);
        history.add(ChatMessage.assistant(content));
        trimHistory(history);
        log.debug("添加助手消息: sessionId={}, historySize={}", sessionId, history.size());
    }
    
    /**
     * 移除最后一条消息（用于失败回滚）
     * 
     * @param sessionId 会话ID
     */
    public void removeLastMessage(String sessionId) {
        List<ChatMessage> history = conversationHistory.get(sessionId);
        if (history != null && !history.isEmpty()) {
            history.remove(history.size() - 1);
            log.debug("移除最后一条消息: sessionId={}", sessionId);
        }
    }
    
    /**
     * 获取会话历史
     * 
     * @param sessionId 会话ID
     * @return 历史消息列表（不可变副本）
     */
    public List<ChatMessage> getHistory(String sessionId) {
        return new ArrayList<>(conversationHistory.getOrDefault(sessionId, new ArrayList<>()));
    }
    
    /**
     * 获取会话历史（Map格式，兼容LLM接口）
     * 
     * @param sessionId 会话ID
     * @return 历史消息Map列表
     */
    public List<Map<String, String>> getHistoryAsMap(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        List<Map<String, String>> result = new ArrayList<>();
        for (ChatMessage msg : history) {
            Map<String, String> map = new HashMap<>();
            map.put("role", msg.getRole());
            map.put("content", msg.getContent());
            result.add(map);
        }
        return result;
    }
    
    /**
     * 清空会话历史
     * 
     * @param sessionId 会话ID
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("已清空会话历史: sessionId={}", sessionId);
    }
    
    /**
     * 检查会话是否存在
     * 
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSession(String sessionId) {
        return conversationHistory.containsKey(sessionId);
    }
    
    /**
     * 获取会话数量
     * 
     * @return 会话数量
     */
    public int getSessionCount() {
        return conversationHistory.size();
    }
    
    /**
     * 设置最大历史消息数
     * 
     * @param maxSize 最大消息数
     */
    public void setMaxHistorySize(int maxSize) {
        if (maxSize > 0) {
            this.maxHistorySize = maxSize;
            log.info("设置最大历史消息数: {}", maxSize);
        }
    }
    
    // ==================== 私有方法 ====================
    
    private List<ChatMessage> getOrCreateHistory(String sessionId) {
        return conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }
    
    private void trimHistory(List<ChatMessage> history) {
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }
    
    // ==================== 内部消息类 ====================
    
    /**
     * 聊天消息值对象
     */
    public static class ChatMessage {
        private final String role;
        private final String content;
        private final long timestamp;
        
        private ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }
        
        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
        
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
