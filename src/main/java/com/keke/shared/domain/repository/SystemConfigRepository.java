package com.keke.shared.domain.repository;

import com.keke.shared.domain.entity.SystemConfig;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓库接口
 */
public interface SystemConfigRepository {
    
    Optional<SystemConfig> findByKey(String configKey);
    
    List<SystemConfig> findAll();
    
    List<SystemConfig> findByKeyPrefix(String prefix);
    
    SystemConfig save(SystemConfig config);
    
    void deleteByKey(String configKey);
}
