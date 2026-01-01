package com.loganalyzer.ssh.domain.repository;

import com.loganalyzer.ssh.domain.entity.SshTask;
import com.loganalyzer.ssh.domain.entity.SshTaskResult;

import java.util.List;
import java.util.Optional;

/**
 * SSH任务仓储接口
 */
public interface SshTaskRepository {
    
    SshTask save(SshTask task);
    
    Optional<SshTask> findById(Long id);
    
    List<SshTask> findAll();
    
    List<SshTask> findRecent(int limit);
    
    void deleteById(Long id);
    
    // 任务结果相关
    SshTaskResult saveResult(SshTaskResult result);
    
    List<SshTaskResult> findResultsByTaskId(Long taskId);
}
