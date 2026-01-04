package com.keke.ssh.domain.exception;

/**
 * 设备不存在异常
 */
public class DeviceNotFoundException extends SshException {
    
    public DeviceNotFoundException(Long deviceId) {
        super("设备不存在: " + deviceId);
    }
}
