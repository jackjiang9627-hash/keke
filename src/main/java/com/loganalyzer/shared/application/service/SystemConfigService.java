package com.loganalyzer.shared.application.service;

import com.loganalyzer.shared.domain.entity.SystemConfig;
import com.loganalyzer.shared.domain.repository.SystemConfigRepository;
import com.loganalyzer.shared.infrastructure.crypto.AesEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 系统配置应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {
    
    private final SystemConfigRepository configRepository;
    private final AesEncryptionService encryptionService;
    
    // 需要加密的配置键
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            SystemConfig.KEY_QWEN_API_KEY
    );
    
    /**
     * 获取配置值
     */
    public Optional<String> getValue(String key) {
        return configRepository.findByKey(key)
                .map(config -> {
                    if (config.isEncrypted()) {
                        return encryptionService.decrypt(config.getConfigValue());
                    }
                    return config.getConfigValue();
                });
    }
    
    /**
     * 获取配置（返回脱敏值用于展示）
     */
    public Optional<Map<String, Object>> getConfig(String key) {
        return configRepository.findByKey(key)
                .map(config -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("key", config.getConfigKey());
                    result.put("description", config.getDescription());
                    result.put("encrypted", config.isEncrypted());
                    
                    // 敏感配置只返回掩码
                    if (config.isEncrypted() && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                        result.put("value", maskValue(encryptionService.decrypt(config.getConfigValue())));
                        result.put("hasValue", true);
                    } else {
                        result.put("value", config.getConfigValue());
                        result.put("hasValue", config.getConfigValue() != null && !config.getConfigValue().isEmpty());
                    }
                    
                    return result;
                });
    }
    
    /**
     * 获取所有 LLM 相关配置（脱敏）
     */
    public List<Map<String, Object>> getLlmConfigs() {
        List<Map<String, Object>> configs = new ArrayList<>();
        
        // 千问 API Key
        configs.add(getOrCreateDefaultConfig(
                SystemConfig.KEY_QWEN_API_KEY,
                "阿里云千问 API Key",
                true
        ));
        
        // Ollama 地址
        configs.add(getOrCreateDefaultConfig(
                SystemConfig.KEY_OLLAMA_BASE_URL,
                "Ollama 服务地址",
                false
        ));
        
        // 默认后端
        configs.add(getOrCreateDefaultConfig(
                SystemConfig.KEY_LLM_DEFAULT_BACKEND,
                "默认 LLM 后端",
                false
        ));
        
        return configs;
    }
    
    private Map<String, Object> getOrCreateDefaultConfig(String key, String description, boolean encrypted) {
        return getConfig(key).orElseGet(() -> {
            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("description", description);
            result.put("encrypted", encrypted);
            result.put("value", "");
            result.put("hasValue", false);
            return result;
        });
    }
    
    /**
     * 保存配置值
     */
    @Transactional
    public void saveValue(String key, String value, String description) {
        boolean shouldEncrypt = SENSITIVE_KEYS.contains(key);
        
        String storedValue = value;
        if (shouldEncrypt && value != null && !value.isEmpty()) {
            storedValue = encryptionService.encrypt(value);
        }
        
        SystemConfig config = new SystemConfig(key, storedValue, shouldEncrypt, description);
        configRepository.save(config);
        
        log.info("保存配置: key={}, encrypted={}", key, shouldEncrypt);
    }
    
    /**
     * 批量保存配置
     */
    @Transactional
    public void saveConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 如果值为空或者是掩码，跳过
            if (value == null || value.isEmpty() || value.contains("*")) {
                continue;
            }
            
            String description = getDescriptionForKey(key);
            saveValue(key, value, description);
        }
    }
    
    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String key) {
        configRepository.deleteByKey(key);
        log.info("删除配置: key={}", key);
    }
    
    /**
     * 获取解密后的 API Key（供内部使用）
     */
    public String getApiKey(String key) {
        return getValue(key).orElse(null);
    }
    
    /**
     * 掩码敏感值
     */
    private String maskValue(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
    
    private String getDescriptionForKey(String key) {
        return switch (key) {
            case SystemConfig.KEY_QWEN_API_KEY -> "阿里云千问 API Key";
            case SystemConfig.KEY_OLLAMA_BASE_URL -> "Ollama 服务地址";
            case SystemConfig.KEY_LLM_DEFAULT_BACKEND -> "默认 LLM 后端";
            default -> "";
        };
    }
}
