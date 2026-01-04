package com.keke.assistant.application.service;

import com.keke.assistant.application.dto.ChatResponseDTO;
import com.keke.assistant.domain.port.LlmPort;
import com.keke.assistant.domain.service.CaseMatchingService;
import com.keke.assistant.domain.service.CaseMatchingService.MatchResult;
import com.keke.assistant.domain.service.ConversationHistoryManager;
import com.keke.shared.application.service.ChatConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 聊天应用服务
 * 
 * DDD概念：应用服务（Application Service）
 * 
 * 职责：编排领域服务，协调各服务完成用例
 * - 对话历史管理 -> 委派给 ConversationHistoryManager
 * - 案例匹配逻辑 -> 委派给 CaseMatchingService
 * - 配置读取     -> 委派给 ChatConfigService
 * - LLM调用       -> 委派给 LlmPort
 * 
 * 重构说明：符合单一职责原则(SRP)，只负责编排和协调
 */
@Slf4j
@Service
public class ChatApplicationService {
    
    private final LlmPort llmPort;
    private final CaseMatchingService caseMatchingService;
    private final ConversationHistoryManager historyManager;
    private final ChatConfigService chatConfigService;
    
    /**
     * 构造函数注入依赖
     * 
     * @param llmPort LLM端口
     * @param caseMatchingService 案例匹配服务
     * @param historyManager 对话历史管理器
     * @param chatConfigService 配置服务
     */
    public ChatApplicationService(LlmPort llmPort, 
                                   CaseMatchingService caseMatchingService,
                                   ConversationHistoryManager historyManager,
                                   ChatConfigService chatConfigService) {
        this.llmPort = llmPort;
        this.caseMatchingService = caseMatchingService;
        this.historyManager = historyManager;
        this.chatConfigService = chatConfigService;
    }
    
    /**
     * 发送消息并获取回复
     * 
     * 匹配流程：
     * 1. 先从本地案例库进行语义匹配
     * 2. 如果找到相似度 >= 阈值 的案例，返回案例内容
     * 3. 如果没有匹配的案例，再调用大模型生成回答
     * 
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param backend 后端名称（可选）
     * @param model 模型名称（可选）
     * @return 回复信息
     */
    public ChatResponseDTO chat(String sessionId, String message, String backend, String model) {
        log.info("处理聊天请求: sessionId={}, message={}", sessionId, message);
        
        // 添加用户消息到历史
        historyManager.addUserMessage(sessionId, message);
        
        try {
            // 1. 优先从案例库进行语义匹配
            double threshold = chatConfigService.getSemanticThreshold();
            int maxMatches = chatConfigService.getMaxCaseMatches();
            Optional<MatchResult> matchResult = caseMatchingService.matchCases(message, threshold, maxMatches);
            
            String reply;
            String source;
            
            if (matchResult.isPresent()) {
                // 案例匹配成功
                reply = matchResult.get().getFormattedReply();
                source = "case";
                log.info("案例匹配成功: 匹配{}个案例, 最高相似度={}%", 
                    matchResult.get().getMatchCount(),
                    (int)(matchResult.get().getTopScore() * 100));
            } else {
                // 2. 如果没有匹配的案例，调用大模型
                log.info("未匹配到案例，调用大模型");
                List<Map<String, String>> history = historyManager.getHistoryAsMap(sessionId);
                reply = llmPort.chat(history, backend, model);
                source = "llm";
            }
            
            // 添加助手回复到历史
            historyManager.addAssistantMessage(sessionId, reply);
            
            log.info("聊天回复完成: sessionId={}, source={}", sessionId, source);
            
            return ChatResponseDTO.success(reply, source);
            
        } catch (Exception e) {
            log.error("聊天失败", e);
            // 移除失败的用户消息
            historyManager.removeLastMessage(sessionId);
            return ChatResponseDTO.error("AI服务暂时不可用，请稍后重试");
        }
    }
    
    /**
     * 获取可用后端列表
     * 
     * @return 后端列表
     */
    public List<Map<String, Object>> getBackends() {
        return llmPort.getBackends();
    }
    
    /**
     * 获取可用模型列表
     * 
     * @param backend 后端名称
     * @return 模型列表
     */
    public Map<String, List<String>> getModels(String backend) {
        return llmPort.getModels(backend);
    }
    
    /**
     * 设置默认后端
     * 
     * @param backend 后端名称
     * @return 是否成功
     */
    public boolean setDefaultBackend(String backend) {
        return llmPort.setDefaultBackend(backend);
    }
    
    /**
     * 清空会话历史
     * 
     * @param sessionId 会话ID
     */
    public void clearHistory(String sessionId) {
        historyManager.clearHistory(sessionId);
    }
    
    /**
     * 获取会话历史
     * 
     * @param sessionId 会话ID
     * @return 历史消息列表
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        return historyManager.getHistoryAsMap(sessionId);
    }
    
    /**
     * 检查LLM服务是否可用
     * 
     * @return 是否可用
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
    public void chatStream(String sessionId, String message, String backend, String model, 
                          LlmPort.StreamCallback callback) {
        log.info("处理流式聊天请求: sessionId={}, message={}", sessionId, message);
        
        // 添加用户消息到历史
        historyManager.addUserMessage(sessionId, message);
        
        try {
            // 调用LLM流式接口
            StringBuilder fullReply = new StringBuilder();
            List<Map<String, String>> history = historyManager.getHistoryAsMap(sessionId);
            
            llmPort.chatStream(history, backend, model, (content, done) -> {
                fullReply.append(content);
                callback.onContent(content, done);
            });
            
            // 添加助手回复到历史
            historyManager.addAssistantMessage(sessionId, fullReply.toString());
            
            log.info("流式聊天回复完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            log.error("流式聊天失败", e);
            // 移除失败的用户消息
            historyManager.removeLastMessage(sessionId);
            callback.onContent("AI服务暂时不可用，请稍后重试", true);
        }
    }
    
    // ==================== 兼容旧接口的内部类 ====================
    
    /**
     * 聊天响应（已废弃，请使用ChatResponseDTO）
     * 
     * @deprecated 使用 ChatResponseDTO 替代
     */
    @Deprecated
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
    }
}
