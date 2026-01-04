package com.keke.monitor.domain.exception;

/**
 * 监控任务配置无效异常
 */
public class InvalidMonitorTaskException extends MonitorException {
    
    public InvalidMonitorTaskException(String message) {
        super(message);
    }
}
