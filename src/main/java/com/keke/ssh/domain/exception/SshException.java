package com.keke.ssh.domain.exception;

/**
 * SSH业务异常基类
 * 
 * DDD概念：领域异常
 * - 领域层的异常应当反映业务规则违反
 * - 与技术异常区分开
 */
public abstract class SshException extends RuntimeException {
    
    protected SshException(String message) {
        super(message);
    }
    
    protected SshException(String message, Throwable cause) {
        super(message, cause);
    }
}
