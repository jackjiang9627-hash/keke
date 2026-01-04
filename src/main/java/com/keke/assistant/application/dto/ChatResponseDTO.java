package com.keke.assistant.application.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天响应DTO
 * 
 * DDD概念：数据传输对象（DTO）
 * 职责：封装聊天接口的响应数据
 */
public class ChatResponseDTO {
    
    /** 是否成功 */
    private final boolean success;
    
    /** 回复内容（成功时有值） */
    private final String reply;
    
    /** 错误信息（失败时有值） */
    private final String error;
    
    /** 响应来源：case(案例库) 或 llm(大模型) */
    private final String source;
    
    /** 时间戳 */
    private final long timestamp;
    
    private ChatResponseDTO(boolean success, String reply, String error, String source) {
        this.success = success;
        this.reply = reply;
        this.error = error;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ==================== 工厂方法 ====================
    
    /**
     * 创建成功响应
     * 
     * @param reply 回复内容
     * @return 成功响应
     */
    public static ChatResponseDTO success(String reply) {
        return new ChatResponseDTO(true, reply, null, "llm");
    }
    
    /**
     * 创建成功响应（指定来源）
     * 
     * @param reply 回复内容
     * @param source 响应来源
     * @return 成功响应
     */
    public static ChatResponseDTO success(String reply, String source) {
        return new ChatResponseDTO(true, reply, null, source);
    }
    
    /**
     * 创建案例库匹配的成功响应
     * 
     * @param reply 回复内容
     * @return 成功响应
     */
    public static ChatResponseDTO successFromCase(String reply) {
        return new ChatResponseDTO(true, reply, null, "case");
    }
    
    /**
     * 创建大模型的成功响应
     * 
     * @param reply 回复内容
     * @return 成功响应
     */
    public static ChatResponseDTO successFromLlm(String reply) {
        return new ChatResponseDTO(true, reply, null, "llm");
    }
    
    /**
     * 创建错误响应
     * 
     * @param error 错误信息
     * @return 错误响应
     */
    public static ChatResponseDTO error(String error) {
        return new ChatResponseDTO(false, null, error, null);
    }
    
    // ==================== Getter方法 ====================
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getReply() {
        return reply;
    }
    
    public String getError() {
        return error;
    }
    
    public String getSource() {
        return source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 判断响应是否来自案例库
     */
    public boolean isFromCase() {
        return "case".equals(source);
    }
    
    /**
     * 判断响应是否来自大模型
     */
    public boolean isFromLlm() {
        return "llm".equals(source);
    }
    
    /**
     * 转换为Map格式（兼容旧接口）
     * 
     * @return Map格式的响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("timestamp", timestamp);
        
        if (success) {
            map.put("reply", reply);
            map.put("source", source);
        } else {
            map.put("error", error);
        }
        
        return map;
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("ChatResponseDTO{success=true, source='%s', replyLength=%d}", 
                source, reply != null ? reply.length() : 0);
        } else {
            return String.format("ChatResponseDTO{success=false, error='%s'}", error);
        }
    }
}
