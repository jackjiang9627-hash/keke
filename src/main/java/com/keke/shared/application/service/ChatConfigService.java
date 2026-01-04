package com.keke.shared.application.service;

import com.keke.shared.domain.repository.SystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 聊天配置服务
 * 
 * DDD概念：应用服务（Application Service）
 * 职责：统一管理聊天相关的配置读取
 * 
 * 符合单一职责原则(SRP)：只负责配置的读取和转换
 */
@Slf4j
@Service
public class ChatConfigService {
    
    // ==================== 配置键常量 ====================
    
    /** 语义匹配阈值配置键 */
    public static final String CONFIG_KEY_SEMANTIC_THRESHOLD = "chat.semantic.threshold";
    
    /** LLM默认后端配置键 */
    public static final String CONFIG_KEY_LLM_DEFAULT_BACKEND = "llm.default.backend";
    
    /** LLM默认模型配置键 */
    public static final String CONFIG_KEY_LLM_DEFAULT_MODEL = "llm.default.model";
    
    /** 最大历史消息数配置键 */
    public static final String CONFIG_KEY_MAX_HISTORY_SIZE = "chat.max.history.size";
    
    /** 最大案例匹配数配置键 */
    public static final String CONFIG_KEY_MAX_CASE_MATCHES = "chat.max.case.matches";
    
    // ==================== 默认值常量 ====================
    
    /** 默认语义匹配阈值（70%） */
    public static final double DEFAULT_SEMANTIC_THRESHOLD = 0.70;
    
    /** 默认最大历史消息数 */
    public static final int DEFAULT_MAX_HISTORY_SIZE = 20;
    
    /** 默认最大案例匹配数 */
    public static final int DEFAULT_MAX_CASE_MATCHES = 3;
    
    private final SystemConfigRepository systemConfigRepository;
    
    public ChatConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }
    
    /**
     * 获取语义匹配阈值
     * 
     * @return 阈值 (0.0 - 1.0)
     */
    public double getSemanticThreshold() {
        return systemConfigRepository.findByKey(CONFIG_KEY_SEMANTIC_THRESHOLD)
            .map(config -> parseDoubleWithRange(config.getConfigValue(), 0.0, 1.0, DEFAULT_SEMANTIC_THRESHOLD))
            .orElse(DEFAULT_SEMANTIC_THRESHOLD);
    }
    
    /**
     * 获取最大历史消息数
     * 
     * @return 最大消息数
     */
    public int getMaxHistorySize() {
        return systemConfigRepository.findByKey(CONFIG_KEY_MAX_HISTORY_SIZE)
            .map(config -> parseIntWithMin(config.getConfigValue(), 1, DEFAULT_MAX_HISTORY_SIZE))
            .orElse(DEFAULT_MAX_HISTORY_SIZE);
    }
    
    /**
     * 获取最大案例匹配数
     * 
     * @return 最大匹配数
     */
    public int getMaxCaseMatches() {
        return systemConfigRepository.findByKey(CONFIG_KEY_MAX_CASE_MATCHES)
            .map(config -> parseIntWithMin(config.getConfigValue(), 1, DEFAULT_MAX_CASE_MATCHES))
            .orElse(DEFAULT_MAX_CASE_MATCHES);
    }
    
    /**
     * 获取默认LLM后端
     * 
     * @return 后端名称（可选）
     */
    public Optional<String> getDefaultLlmBackend() {
        return systemConfigRepository.findByKey(CONFIG_KEY_LLM_DEFAULT_BACKEND)
            .map(config -> config.getConfigValue())
            .filter(value -> value != null && !value.isBlank());
    }
    
    /**
     * 获取默认LLM模型
     * 
     * @return 模型名称（可选）
     */
    public Optional<String> getDefaultLlmModel() {
        return systemConfigRepository.findByKey(CONFIG_KEY_LLM_DEFAULT_MODEL)
            .map(config -> config.getConfigValue())
            .filter(value -> value != null && !value.isBlank());
    }
    
    /**
     * 获取配置值（字符串）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfigValue(String key, String defaultValue) {
        return systemConfigRepository.findByKey(key)
            .map(config -> config.getConfigValue())
            .orElse(defaultValue);
    }
    
    /**
     * 获取配置值（整数）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getConfigValueAsInt(String key, int defaultValue) {
        return systemConfigRepository.findByKey(key)
            .map(config -> parseIntSafe(config.getConfigValue(), defaultValue))
            .orElse(defaultValue);
    }
    
    /**
     * 获取配置值（双精度浮点数）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public double getConfigValueAsDouble(String key, double defaultValue) {
        return systemConfigRepository.findByKey(key)
            .map(config -> parseDoubleSafe(config.getConfigValue(), defaultValue))
            .orElse(defaultValue);
    }
    
    /**
     * 获取配置值（布尔）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getConfigValueAsBoolean(String key, boolean defaultValue) {
        return systemConfigRepository.findByKey(key)
            .map(config -> "true".equalsIgnoreCase(config.getConfigValue()))
            .orElse(defaultValue);
    }
    
    // ==================== 私有解析方法 ====================
    
    private double parseDoubleWithRange(String value, double min, double max, double defaultValue) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                log.warn("配置值{}超出范围({}-{})，使用默认值{}", value, min, max, defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("配置值{}无法解析为数字，使用默认值{}", value, defaultValue);
            return defaultValue;
        }
    }
    
    private int parseIntWithMin(String value, int min, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min) {
                log.warn("配置值{}小于最小值{}，使用默认值{}", value, min, defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("配置值{}无法解析为整数，使用默认值{}", value, defaultValue);
            return defaultValue;
        }
    }
    
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
