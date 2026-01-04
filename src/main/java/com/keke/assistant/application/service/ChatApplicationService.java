package com.keke.assistant.application.service;

import com.keke.assistant.domain.entity.CaseEntry;
import com.keke.assistant.domain.port.LlmPort;
import com.keke.assistant.domain.port.SemanticSearchPort;
import com.keke.assistant.domain.port.SemanticSearchPort.CaseWithScore;
import com.keke.assistant.domain.repository.CaseRepository;
import com.keke.shared.domain.repository.SystemConfigRepository;
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
 * - 优先语义匹配本地案例库
 */
@Slf4j
@Service
public class ChatApplicationService {
    
    private final LlmPort llmPort;
    private final SemanticSearchPort semanticSearchPort;
    private final CaseRepository caseRepository;
    private final SystemConfigRepository systemConfigRepository;
    
    /** 对话历史缓存（简单实现，生产环境应使用Redis等） */
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();
    
    /** 最大历史消息数 */
    private static final int MAX_HISTORY_SIZE = 20;
    
    /** 配置键：语义匹配阈值 */
    private static final String CONFIG_KEY_SEMANTIC_THRESHOLD = "chat.semantic.threshold";
    
    /** 默认语义匹配阈值（70%） */
    private static final double DEFAULT_SEMANTIC_THRESHOLD = 0.70;
    
    /** 最多返回的匹配案例数 */
    private static final int MAX_CASE_MATCHES = 3;
    
    public ChatApplicationService(LlmPort llmPort, 
                                   SemanticSearchPort semanticSearchPort,
                                   CaseRepository caseRepository,
                                   SystemConfigRepository systemConfigRepository) {
        this.llmPort = llmPort;
        this.semanticSearchPort = semanticSearchPort;
        this.caseRepository = caseRepository;
        this.systemConfigRepository = systemConfigRepository;
    }
    
    /**
     * 获取语义匹配阈值（从系统配置读取）
     */
    private double getSemanticThreshold() {
        return systemConfigRepository.findByKey(CONFIG_KEY_SEMANTIC_THRESHOLD)
            .map(config -> {
                try {
                    double value = Double.parseDouble(config.getConfigValue());
                    // 确保阈值在合理范围内
                    if (value < 0.0 || value > 1.0) {
                        log.warn("语义阈值配置超出范围(0-1): {}, 使用默认值", value);
                        return DEFAULT_SEMANTIC_THRESHOLD;
                    }
                    return value;
                } catch (NumberFormatException e) {
                    log.warn("语义阈值配置无效: {}, 使用默认值", config.getConfigValue());
                    return DEFAULT_SEMANTIC_THRESHOLD;
                }
            })
            .orElse(DEFAULT_SEMANTIC_THRESHOLD);
    }
    
    /**
     * 发送消息并获取回复
     * 
     * 匄配流程：
     * 1. 先从本地案例库进行语义匹配
     * 2. 如果找到相似度 >= 70% 的案例，返回案例内容
     * 3. 如果没有匹配的案例，再调用大模型生成回答
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
            // 1. 优先从案例库进行语义匹配
            String reply = tryMatchCases(message);
            
            // 2. 如果没有匹配的案例，调用大模型
            if (reply == null) {
                log.info("未匹配到案例，调用大模型");
//                reply = llmPort.chat(history, backend, model);
                reply = "none";
            }
            
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
     * 尝试从案例库匹配答案
     * 
     * @param query 用户问题
     * @return 匹配到的案例回复，或null如果没有匹配
     */
    private String tryMatchCases(String query) {
        try {
            // 获取所有案例
            List<CaseEntry> allCases = caseRepository.findAll(0, 500);
            if (allCases.isEmpty()) {
                log.debug("案例库为空，跳过语义匹配");
                return null;
            }
            
            // 语义搜索：使用配置的阈值，最多Top3
            double threshold = getSemanticThreshold();
            List<CaseWithScore> matches = semanticSearchPort.semanticSearchWithThreshold(
                query, allCases, threshold, MAX_CASE_MATCHES);
            
            if (matches.isEmpty()) {
                log.info("未找到相似度>{}%的案例", (int)(threshold * 100));
                return null;
            }
            
            // 构建回复
            log.info("匹配到{}个相似案例", matches.size());
            return buildCaseReply(matches);
            
        } catch (Exception e) {
            log.warn("案例匹配失败，跳过: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 构建案例匹配的回复
     */
    private String buildCaseReply(List<CaseWithScore> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 **找到以下相关案例：**\n\n");
        
        for (int i = 0; i < matches.size(); i++) {
            CaseWithScore match = matches.get(i);
            CaseEntry caseEntry = match.caseEntry();
            int similarityPercent = (int) (match.score() * 100);
            
            sb.append(String.format("### %d. %s\n", i + 1, caseEntry.getTitle()));
            sb.append(String.format("相似度：**%d%%** | 模块：%s\n\n", 
                similarityPercent, caseEntry.getModuleName() != null ? caseEntry.getModuleName() : "未分类"));
            
            // 摘要
            if (caseEntry.getSummary() != null && !caseEntry.getSummary().isBlank()) {
                sb.append("**摘要：**").append(caseEntry.getSummary()).append("\n\n");
            }
            
            // 详细内容
            if (caseEntry.getContent() != null && !caseEntry.getContent().isBlank()) {
                sb.append("**详细内容：**\n").append(caseEntry.getContent()).append("\n\n");
            }
            
            // 链接
            if (caseEntry.getHyperlink() != null && !caseEntry.getHyperlink().isBlank()) {
                sb.append("相关链接：").append(caseEntry.getHyperlink()).append("\n\n");
            }
            
            if (i < matches.size() - 1) {
                sb.append("---\n\n");
            }
        }
        
        sb.append("\n---\n_以上内容来自本地案例库，如需更多帮助请继续提问_");
        
        return sb.toString();
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
     * 流式发送消息并获取回复
     * 
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param backend 后端名称（可选）
     * @param model 模型名称（可选）
     * @param callback 流式回调
     */
    public void chatStream(String sessionId, String message, String backend, String model, LlmPort.StreamCallback callback) {
        log.info("处理流式聊天请求: sessionId={}, message={}", sessionId, message);
        
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
            // 调用LLM流式接口
            StringBuilder fullReply = new StringBuilder();
            llmPort.chatStream(history, backend, model, (content, done) -> {
                fullReply.append(content);
                callback.onContent(content, done);
            });
            
            // 添加助手回复到历史
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", fullReply.toString());
            history.add(assistantMessage);
            
            log.info("流式聊天回复完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            log.error("流式聊天失败", e);
            // 移除失败的用户消息
            history.remove(history.size() - 1);
            callback.onContent("AI服务暂时不可用，请稍后重试", true);
        }
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
