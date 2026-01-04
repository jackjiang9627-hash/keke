package com.keke.assistant.interfaces.rest;

import com.keke.assistant.application.service.ChatApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public ChatController(ChatApplicationService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * 流式输出问答接口 (SSE)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String backend = request.get("backend");
        String model = request.get("model");
        
        log.info("收到流式问答请求: message={}, backend={}, model={}", message, backend, model);
        
        SseEmitter emitter = new SseEmitter(1200000L); // 2分钟超时
        emitter.onCompletion(() -> log.debug("SSE连接完成"));
        emitter.onTimeout(() -> log.warn("SSE连接超时"));
        
        executor.execute(() -> {
            try {
                chatService.chatStream(sessionId, message, backend, model, (content, done) -> {
                    try {
                        String data = String.format("{\"content\":\"%s\",\"done\":%s}", 
                            escapeJson(content), done);
                        // 直接发送数据，不使用 event builder
                        emitter.send(data, MediaType.APPLICATION_JSON);
                        log.debug("发送SSE数据: {}", data.length() > 50 ? data.substring(0, 50) + "..." : data);
                    } catch (Exception e) {
                        log.error("发送SSE消息失败", e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("流式问答失败", e);
                try {
                    emitter.send("{\"content\":\"抱歉，服务异常，请稍后重试。\",\"done\":true}", MediaType.APPLICATION_JSON);
                } catch (Exception ex) {
                    // 忽略
                }
                emitter.complete();
            }
        });
        
        return emitter;
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
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
