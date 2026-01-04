package com.keke.monitor.domain.exception;

/**
 * 监控任务不存在异常
 */
public class MonitorTaskNotFoundException extends MonitorException {
    
    public MonitorTaskNotFoundException(Long taskId) {
        super("监控任务不存在: " + taskId);
    }
}
