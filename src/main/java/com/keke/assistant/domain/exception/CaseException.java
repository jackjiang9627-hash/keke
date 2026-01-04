package com.keke.assistant.domain.exception;

/**
 * 案例业务异常基类
 * 
 * DDD概念：领域异常
 * - 领域层的异常应当反映业务规则违反
 * - 与技术异常区分开
 */
public abstract class CaseException extends RuntimeException {
    
    protected CaseException(String message) {
        super(message);
    }
    
    protected CaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
