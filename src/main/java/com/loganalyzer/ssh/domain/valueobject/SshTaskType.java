package com.loganalyzer.ssh.domain.valueobject;

/**
 * SSH任务类型
 */
public enum SshTaskType {
    COMMAND("命令执行"),
    UPLOAD("文件上传"),
    DOWNLOAD("文件下载");
    
    private final String description;
    
    SshTaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
