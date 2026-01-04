package com.keke.ssh.domain.exception;

/**
 * SSH任务不存在异常
 */
public class SshTaskNotFoundException extends SshException {
    
    public SshTaskNotFoundException(Long taskId) {
        super("SSH任务不存在: " + taskId);
    }
}
