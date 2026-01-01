package com.loganalyzer.ssh.infrastructure.persistence.repository;

import com.loganalyzer.ssh.infrastructure.persistence.entity.SshTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SshTaskJpaRepository extends JpaRepository<SshTaskEntity, Long> {
    
    List<SshTaskEntity> findTop50ByOrderByCreateTimeDesc();
}
