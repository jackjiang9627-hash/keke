package com.keke.assistant.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keke.assistant.domain.port.LlmPort;
import com.keke.shared.application.service.SystemConfigService;
import com.keke.shared.domain.entity.SystemConfig;
import com.keke.shared.infrastructure.python.PythonBridge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python LLM 适配器
 * 
 * DDD概念：适配器（Adapter）
 * - 实现领域层定义的端口接口
 * - 通过Py4J调用Python LLM插件
 */
@Slf4j
@Component
public class PythonLlmAdapter implements LlmPort {
    
    /** LLM模块名 */
    private static final String MODULE_LLM = "llm";
    
    /** 超时时间（秒）- LLM调用可能较慢 */
    private static final long LLM_TIMEOUT_SECONDS = 120;
    
    private final PythonBridge pythonBridge;
    private final ObjectMapper objectMapper;
    private final SystemConfigService configService;
    
    public PythonLlmAdapter(PythonBridge pythonBridge, ObjectMapper objectMapper, 
                            SystemConfigService configService) {
        this.pythonBridge = pythonBridge;
        this.objectMapper = objectMapper;
        this.configService = configService;
    }
    
    @Override
    public String chat(List<Map<String, String>> messages, String backend, String model) {
        log.info("调用LLM对话: messages={}, backend={}, model={}", 
            messages.size(), backend, model);
        
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用内置降级回复");
            return generateFallbackReply(messages);
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("messages", messages);
            if (backend != null && !backend.isEmpty()) {
                params.put("backend", backend);
            }
            if (model != null && !model.isEmpty()) {
                params.put("model", model);
            }
            
            // 从数据库获取 API Key
            String apiKey = configService.getApiKey(SystemConfig.KEY_QWEN_API_KEY);
            if (apiKey != null && !apiKey.isEmpty()) {
                params.put("api_key", apiKey);
            }
            
            String result = pythonBridge.execute(MODULE_LLM, "chat", params, LLM_TIMEOUT_SECONDS);
            
            // 解析结果
            Map<String, Object> response = objectMapper.readValue(result, 
                new TypeReference<Map<String, Object>>() {});
            
            String reply = (String) response.get("reply");
            String usedBackend = (String) response.get("backend");
            String usedModel = (String) response.get("model");
            
            log.info("LLM回复完成: backend={}, model={}", usedBackend, usedModel);
            return reply;
            
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            return generateFallbackReply(messages);
        }
    }
    
    @Override
    public String chat(String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
        
        return chat(messages, null, null);
    }
    
    @Override
    public List<Map<String, Object>> getBackends() {
        if (!pythonBridge.isAvailable()) {
            // 返回默认的Mock后端
            List<Map<String, Object>> backends = new ArrayList<>();
            Map<String, Object> mockBackend = new HashMap<>();
            mockBackend.put("name", "mock");
            mockBackend.put("available", true);
            mockBackend.put("default", true);
            backends.add(mockBackend);
            return backends;
        }
        
        try {
            String result = pythonBridge.execute(MODULE_LLM, "get_backends", new HashMap<>());
            Map<String, Object> response = objectMapper.readValue(result, 
                new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> backends = (List<Map<String, Object>>) response.get("backends");
            return backends != null ? backends : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取后端列表失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, List<String>> getModels(String backend) {
        if (!pythonBridge.isAvailable()) {
            Map<String, List<String>> models = new HashMap<>();
            models.put("mock", Arrays.asList("mock"));
            return models;
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            if (backend != null && !backend.isEmpty()) {
                params.put("backend", backend);
            }
            
            String result = pythonBridge.execute(MODULE_LLM, "list_models", params);
            Map<String, Object> response = objectMapper.readValue(result, 
                new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> models = (Map<String, List<String>>) response.get("models");
            return models != null ? models : new HashMap<>();
            
        } catch (Exception e) {
            log.error("获取模型列表失败", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public boolean setDefaultBackend(String backend) {
        if (!pythonBridge.isAvailable()) {
            return false;
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("backend", backend);
            
            String result = pythonBridge.execute(MODULE_LLM, "set_default_backend", params);
            Map<String, Object> response = objectMapper.readValue(result, 
                new TypeReference<Map<String, Object>>() {});
            
            return Boolean.TRUE.equals(response.get("success"));
            
        } catch (Exception e) {
            log.error("设置默认后端失败", e);
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return pythonBridge.isAvailable();
    }
    
    @Override
    public void chatStream(List<Map<String, String>> messages, String backend, String model, StreamCallback callback) {
        log.info("调用LLM流式对话: messages={}, backend={}, model={}", 
            messages.size(), backend, model);
        
        if (!pythonBridge.isAvailable()) {
            log.warn("Python服务不可用，使用内置降级回复");
            String reply = generateFallbackReply(messages);
            simulateStreamOutput(reply, callback);
            return;
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("messages", messages);
            params.put("stream", true);  // 请求流式输出
            if (backend != null && !backend.isEmpty()) {
                params.put("backend", backend);
            }
            if (model != null && !model.isEmpty()) {
                params.put("model", model);
            }
            
            // 从数据库获取 API Key
            String apiKey = configService.getApiKey(SystemConfig.KEY_QWEN_API_KEY);
            if (apiKey != null && !apiKey.isEmpty()) {
                params.put("api_key", apiKey);
            }
            
            String result = pythonBridge.execute(MODULE_LLM, "chat_stream", params, LLM_TIMEOUT_SECONDS);
            
            // 解析结果 - Python 返回的是包含所有 chunk 的数组
            Map<String, Object> response = objectMapper.readValue(result, 
                new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chunks = (List<Map<String, Object>>) response.get("chunks");
            
            if (chunks != null && !chunks.isEmpty()) {
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> chunk = chunks.get(i);
                    String content = (String) chunk.getOrDefault("content", "");
                    boolean done = i == chunks.size() - 1;
                    callback.onContent(content, done);
                    
                    // 添加延迟以实现流式效果
                    if (!done) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } else {
                // 如果没有 chunks，尝试使用普通回复
                String reply = (String) response.get("reply");
                if (reply != null) {
                    simulateStreamOutput(reply, callback);
                } else {
                    callback.onContent("抱歉，我暂时无法回答这个问题。", true);
                }
            }
            
        } catch (Exception e) {
            log.error("LLM流式调用失败", e);
            String reply = generateFallbackReply(messages);
            simulateStreamOutput(reply, callback);
        }
    }
    
    /**
     * 模拟流式输出（将完整回复分块发送）
     */
    private void simulateStreamOutput(String reply, StreamCallback callback) {
        if (reply == null || reply.isEmpty()) {
            callback.onContent("", true);
            return;
        }
        
        // 按句子或段落分块发送
        String[] sentences = reply.split("(?<=[\u3002\uff0c\uff1f\uff01\u3001\n])|(?<=\\. )|(?<=\\? )|(?<=! )");
        for (int i = 0; i < sentences.length; i++) {
            boolean done = i == sentences.length - 1;
            callback.onContent(sentences[i], done);
            
            // 稍微延迟以模拟打字效果
            if (!done) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * 生成降级回复（当Python服务不可用时）
     */
    private String generateFallbackReply(List<Map<String, String>> messages) {
        // 获取最后一条用户消息
        String lastMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> msg = messages.get(i);
            if ("user".equals(msg.get("role"))) {
                lastMessage = msg.get("content");
                break;
            }
        }
        
        String lowerMessage = lastMessage.toLowerCase();
        
        if (lowerMessage.contains("待办") || lowerMessage.contains("todo")) {
            return "您可以在左侧导航栏点击**待办事项**模块来管理您的待办。\\n\\n需要我帮您添加新的待办吗？";
        }
        
        if (lowerMessage.contains("案例") || lowerMessage.contains("case")) {
            return "您可以在**案例库**中搜索和管理案例。\\n\\n支持的操作：\\n- 新增案例\\n- 搜索案例\\n- 导入/导出Excel";
        }
        
        if (lowerMessage.contains("日志") || lowerMessage.contains("log")) {
            return "**日志分析**模块支持：\\n- 上传日志文件解析\\n- 日志级别统计\\n- 错误日志筛选\\n\\n请上传日志文件开始分析。";
        }
        
        if (lowerMessage.contains("监控") || lowerMessage.contains("monitor")) {
            return "**系统监控**功能包括：\\n- CPU/内存/磁盘使用率\\n- 网络IO监控\\n- 进程监控\\n\\n请到监控页面查看详情。";
        }
        
        if (lowerMessage.contains("ssh") || lowerMessage.contains("服务器") || lowerMessage.contains("设备")) {
            return "**SSH批量运维**支持：\\n- 设备管理\\n- 批量命令执行\\n- 文件传输\\n\\n请先添加设备后使用。";
        }
        
        if (lowerMessage.contains("你好") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "您好！我是智能助手，可以帮您：\\n\\n1. 管理待办事项\\n2. 搜索案例库\\n3. 分析日志文件\\n4. 查看系统监控\\n5. SSH批量运维\\n\\n请问有什么可以帮您的？";
        }
        
        return "感谢您的提问！作为智能助手，我可以帮您处理以下任务：\\n\\n- 待办事项管理\\n- 案例库搜索\\n- 日志分析\\n- 系统监控\\n- SSH运维\\n\\n请告诉我您需要什么帮助？";
    }
}
