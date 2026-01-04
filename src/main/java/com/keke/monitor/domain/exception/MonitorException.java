package com.keke.monitor.domain.exception;

/**
 * 监控业务异常基类
 * 
 * DDD概念：领域异常
 */
public abstract class MonitorException extends RuntimeException {
    
    protected MonitorException(String message) {
        super(message);
    }
    
    protected MonitorException(String message, Throwable cause) {
        super(message, cause);
    }
}
