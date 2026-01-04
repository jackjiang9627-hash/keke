package com.keke.ssh.infrastructure.persistence.repository;

import com.keke.ssh.infrastructure.persistence.entity.SshTaskResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SshTaskResultJpaRepository extends JpaRepository<SshTaskResultEntity, Long> {
    
    List<SshTaskResultEntity> findByTaskIdOrderByIdAsc(Long taskId);
}
