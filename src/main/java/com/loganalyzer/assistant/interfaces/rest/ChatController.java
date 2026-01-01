package com.loganalyzer.assistant.interfaces.rest;

import com.loganalyzer.assistant.application.service.ChatApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智能问答REST控制器
 * 
 * 支持的后端：
 * - qwen: 阿里云千问（需配置DASHSCOPE_API_KEY）
 * - ollama: 本地Ollama模型
 * - mock: 降级Mock回复
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final ChatApplicationService chatService;
    
    public ChatController(ChatApplicationService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * 发送消息并获取回复
     * 
     * @param request 请求体，包含：
     *   - message: 用户消息（必填）
     *   - sessionId: 会话ID（可选，用于维护对话历史）
     *   - backend: 后端名称（可选，qwen/ollama/mock）
     *   - model: 模型名称（可选）
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String backend = request.get("backend");
        String model = request.get("model");
        
        log.info("收到问答请求: message={}, backend={}", message, backend);
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "消息不能为空"
            ));
        }
        
        ChatApplicationService.ChatResponse response = chatService.chat(
            sessionId, message, backend, model);
        
        return ResponseEntity.ok(response.toMap());
    }
    
    /**
     * 获取可用后端列表
     */
    @GetMapping("/backends")
    public ResponseEntity<List<Map<String, Object>>> getBackends() {
        return ResponseEntity.ok(chatService.getBackends());
    }
    
    /**
     * 获取可用模型列表
     * 
     * @param backend 后端名称（可选）
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, List<String>>> getModels(
            @RequestParam(required = false) String backend) {
        return ResponseEntity.ok(chatService.getModels(backend));
    }
    
    /**
     * 设置默认后端
     */
    @PostMapping("/backends/default")
    public ResponseEntity<Map<String, Object>> setDefaultBackend(
            @RequestBody Map<String, String> request) {
        String backend = request.get("backend");
        
        if (backend == null || backend.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "后端名称不能为空"
            ));
        }
        
        boolean success = chatService.setDefaultBackend(backend);
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    /**
     * 清空会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Map<String, String>>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }
    
    /**
     * 检查LLM服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "available", chatService.isLlmAvailable(),
            "backends", chatService.getBackends()
        ));
    }
}
