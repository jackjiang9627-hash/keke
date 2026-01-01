package com.loganalyzer.shared.infrastructure.persistence.repository;

import com.loganalyzer.shared.domain.entity.SystemConfig;
import com.loganalyzer.shared.domain.repository.SystemConfigRepository;
import com.loganalyzer.shared.infrastructure.persistence.entity.SystemConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统配置仓库实现
 */
@Repository
@RequiredArgsConstructor
public class SystemConfigRepositoryImpl implements SystemConfigRepository {
    
    private final JpaSystemConfigRepository jpaRepository;
    
    @Override
    public Optional<SystemConfig> findByKey(String configKey) {
        return jpaRepository.findByConfigKey(configKey)
                .map(this::toEntity);
    }
    
    @Override
    public List<SystemConfig> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SystemConfig> findByKeyPrefix(String prefix) {
        return jpaRepository.findByConfigKeyStartingWith(prefix).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public SystemConfig save(SystemConfig config) {
        SystemConfigPO po = toPO(config);
        
        // 检查是否已存在
        Optional<SystemConfigPO> existing = jpaRepository.findByConfigKey(config.getConfigKey());
        if (existing.isPresent()) {
            po.setId(existing.get().getId());
            po.setCreatedAt(existing.get().getCreatedAt());
        }
        
        SystemConfigPO saved = jpaRepository.save(po);
        return toEntity(saved);
    }
    
    @Override
    public void deleteByKey(String configKey) {
        jpaRepository.deleteByConfigKey(configKey);
    }
    
    private SystemConfig toEntity(SystemConfigPO po) {
        SystemConfig entity = new SystemConfig();
        entity.setId(po.getId());
        entity.setConfigKey(po.getConfigKey());
        entity.setConfigValue(po.getConfigValue());
        entity.setEncrypted(po.isEncrypted());
        entity.setDescription(po.getDescription());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
    
    private SystemConfigPO toPO(SystemConfig entity) {
        SystemConfigPO po = new SystemConfigPO();
        po.setId(entity.getId());
        po.setConfigKey(entity.getConfigKey());
        po.setConfigValue(entity.getConfigValue());
        po.setEncrypted(entity.isEncrypted());
        po.setDescription(entity.getDescription());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }
}
