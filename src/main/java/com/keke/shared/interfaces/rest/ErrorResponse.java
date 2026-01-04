package com.keke.shared.interfaces.rest;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一错误响应DTO
 * 
 * 用于API统一的错误响应格式
 */
@Getter
public class ErrorResponse {
    
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private Map<String, String> details;
    
    public ErrorResponse(int status, String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    /**
     * 工厂方法：创建400错误响应
     */
    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(400, "Bad Request", message);
    }
    
    /**
     * 工厂方法：创建404错误响应
     */
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "Not Found", message);
    }
    
    /**
     * 工厂方法：创建409错误响应（冲突）
     */
    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "Conflict", message);
    }
    
    /**
     * 工厂方法：创建500错误响应
     */
    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(500, "Internal Server Error", message);
    }
}
