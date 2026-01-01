package com.loganalyzer.shared.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置领域实体
 */
@Getter
@NoArgsConstructor
public class SystemConfig {
    
    private Long id;
    private String configKey;
    private String configValue;
    private boolean encrypted;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public SystemConfig(String configKey, String configValue, boolean encrypted, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.encrypted = encrypted;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateValue(String value) {
        this.configValue = value;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
    
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }
    
    // 常用配置键
    public static final String KEY_QWEN_API_KEY = "llm.qwen.api_key";
    public static final String KEY_OLLAMA_BASE_URL = "llm.ollama.base_url";
    public static final String KEY_LLM_DEFAULT_BACKEND = "llm.default_backend";
}
