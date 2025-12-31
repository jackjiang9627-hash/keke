package com.loganalyzer.monitor.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监控任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorTask {
    
    /** 任务ID */
    private Long id;
    
    /** 任务名称 */
    private String name;
    
    /** 任务类型 */
    private TaskType type;
    
    /** 任务状态 */
    private TaskStatus status;
    
    /** 检测间隔(秒) - 周期任务使用 */
    private Integer intervalSeconds;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 下次执行时间 */
    private LocalDateTime nextExecuteTime;
    
    /** 已执行次数 */
    private Integer executeCount;
    
    /** 最大执行次数 - 0表示无限制 */
    private Integer maxExecuteCount;
    
    /** 最后一次执行结果 */
    private String lastResult;
    
    /** 备注 */
    private String remark;
    
    /**
     * 任务类型
     */
    public enum TaskType {
        ONCE("单次检测"),
        PERIODIC("周期检测");
        
        private final String description;
        
        TaskType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 任务状态
     */
    public enum TaskStatus {
        PENDING("待执行"),
        RUNNING("运行中"),
        PAUSED("已暂停"),
        COMPLETED("已完成"),
        FAILED("执行失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 创建单次检测任务
     */
    public static MonitorTask createOnceTask(String name) {
        return MonitorTask.builder()
                .name(name)
                .type(TaskType.ONCE)
                .status(TaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .executeCount(0)
                .build();
    }
    
    /**
     * 创建周期检测任务
     */
    public static MonitorTask createPeriodicTask(String name, int intervalSeconds) {
        return createPeriodicTask(name, intervalSeconds, 0);
    }
    
    /**
     * 创建周期检测任务(带最大执行次数)
     */
    public static MonitorTask createPeriodicTask(String name, int intervalSeconds, int maxExecuteCount) {
        return MonitorTask.builder()
                .name(name)
                .type(TaskType.PERIODIC)
                .status(TaskStatus.PENDING)
                .intervalSeconds(intervalSeconds)
                .maxExecuteCount(maxExecuteCount)
                .createTime(LocalDateTime.now())
                .executeCount(0)
                .build();
    }
    
    /**
     * 开始任务
     */
    public void start() {
        this.status = TaskStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        if (type == TaskType.PERIODIC) {
            this.nextExecuteTime = LocalDateTime.now().plusSeconds(intervalSeconds);
        }
    }
    
    /**
     * 暂停任务
     */
    public void pause() {
        if (this.status == TaskStatus.RUNNING) {
            this.status = TaskStatus.PAUSED;
        }
    }
    
    /**
     * 恢复任务
     */
    public void resume() {
        if (this.status == TaskStatus.PAUSED) {
            this.status = TaskStatus.RUNNING;
            if (type == TaskType.PERIODIC) {
                this.nextExecuteTime = LocalDateTime.now().plusSeconds(intervalSeconds);
            }
        }
    }
    
    /**
     * 完成任务
     */
    public void complete(String result) {
        this.executeCount++;
        this.lastResult = result;
        if (type == TaskType.ONCE) {
            this.status = TaskStatus.COMPLETED;
            this.endTime = LocalDateTime.now();
        } else {
            // 检查是否达到最大执行次数
            if (maxExecuteCount != null && maxExecuteCount > 0 && executeCount >= maxExecuteCount) {
                this.status = TaskStatus.COMPLETED;
                this.endTime = LocalDateTime.now();
            } else {
                this.nextExecuteTime = LocalDateTime.now().plusSeconds(intervalSeconds);
            }
        }
    }
    
    /**
     * 停止任务
     */
    public void stop() {
        this.status = TaskStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 任务失败
     */
    public void fail(String error) {
        this.status = TaskStatus.FAILED;
        this.lastResult = error;
        this.endTime = LocalDateTime.now();
    }
}
