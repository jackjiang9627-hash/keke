package com.loganalyzer.ssh.domain.valueobject;

/**
 * 设备状态
 */
public enum DeviceStatus {
    ONLINE("在线"),
    OFFLINE("离线"),
    UNKNOWN("未知");
    
    private final String description;
    
    DeviceStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
