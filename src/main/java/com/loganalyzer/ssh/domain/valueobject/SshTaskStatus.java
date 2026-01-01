package com.loganalyzer.ssh.domain.valueobject;

/**
 * SSH任务状态
 */
public enum SshTaskStatus {
    PENDING("待执行"),
    RUNNING("执行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELLED("已取消");
    
    private final String description;
    
    SshTaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
