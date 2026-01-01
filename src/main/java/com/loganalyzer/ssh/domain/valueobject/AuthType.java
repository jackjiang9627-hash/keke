package com.loganalyzer.ssh.domain.valueobject;

/**
 * 认证方式
 */
public enum AuthType {
    PASSWORD("密码认证"),
    KEY("密钥认证");
    
    private final String description;
    
    AuthType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
