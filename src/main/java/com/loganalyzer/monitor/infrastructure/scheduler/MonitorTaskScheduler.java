package com.loganalyzer.monitor.infrastructure.scheduler;

import com.loganalyzer.monitor.application.service.MonitorApplicationService;
import com.loganalyzer.monitor.domain.entity.MonitorTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控任务调度器
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class MonitorTaskScheduler {
    
    private final MonitorApplicationService monitorApplicationService;
    
    /**
     * 每5秒检查一次是否有需要执行的周期任务
     */
    @Scheduled(fixedRate = 5000)
    public void checkAndExecutePeriodicTasks() {
        List<MonitorTask> runningTasks = monitorApplicationService.getRunningPeriodicTasks();
        
        LocalDateTime now = LocalDateTime.now();
        for (MonitorTask task : runningTasks) {
            if (task.getNextExecuteTime() != null && task.getNextExecuteTime().isBefore(now)) {
                log.debug("执行周期监控任务: {}", task.getName());
                try {
                    monitorApplicationService.executePeriodicTaskMonitoring(task);
                } catch (Exception e) {
                    log.error("周期任务执行失败: {}", task.getName(), e);
                }
            }
        }
    }
}
