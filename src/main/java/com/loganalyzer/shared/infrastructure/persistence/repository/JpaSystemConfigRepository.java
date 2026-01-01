package com.loganalyzer.shared.infrastructure.persistence.repository;

import com.loganalyzer.shared.infrastructure.persistence.entity.SystemConfigPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置 JPA 仓库
 */
@Repository
public interface JpaSystemConfigRepository extends JpaRepository<SystemConfigPO, Long> {
    
    Optional<SystemConfigPO> findByConfigKey(String configKey);
    
    List<SystemConfigPO> findByConfigKeyStartingWith(String prefix);
    
    void deleteByConfigKey(String configKey);
}
