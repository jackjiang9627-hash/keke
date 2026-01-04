package com.keke.assistant.domain.exception;

/**
 * 案例不存在异常
 */
public class CaseNotFoundException extends CaseException {
    
    public CaseNotFoundException(String caseId) {
        super("案例不存在: " + caseId);
    }
    
    public CaseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
