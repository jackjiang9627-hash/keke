package com.loganalyzer.ssh.infrastructure.persistence.repository;

import com.loganalyzer.ssh.infrastructure.persistence.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceJpaRepository extends JpaRepository<DeviceEntity, Long> {
    
    List<DeviceEntity> findByGroupName(String groupName);
    
    List<DeviceEntity> findByIdIn(List<Long> ids);
    
    boolean existsByHostAndPort(String host, Integer port);
    
    @Query("SELECT DISTINCT d.groupName FROM DeviceEntity d WHERE d.groupName IS NOT NULL")
    List<String> findAllGroupNames();
}
