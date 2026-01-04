package com.keke.shared.infrastructure.python;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Python任务队列
 * 
 * 线程安全的任务队列，支持：
 * - 任务提交和获取
 * - 任务状态跟踪
 * - 等待/通知机制
 */
@Slf4j
@Component
public class PythonTaskQueue {
    
    /** 待处理任务队列 */
    private final BlockingQueue<PythonTask> pendingTasks = new LinkedBlockingQueue<>();
    
    /** 任务ID到任务的映射（用于结果回调） */
    private final Map<String, PythonTask> taskMap = new ConcurrentHashMap<>();
    
    /**
     * 提交任务到队列
     * 
     * @param task 任务
     * @return 任务ID
     */
    public String submit(PythonTask task) {
        taskMap.put(task.getTaskId(), task);
        pendingTasks.offer(task);
        log.debug("任务已提交: taskId={}, module={}, method={}", 
                task.getTaskId(), task.getModule(), task.getMethod());
        return task.getTaskId();
    }
    
    /**
     * 获取待处理任务（阻塞）
     * 
     * 供Python worker调用
     * 
     * @param timeout 超时时间（毫秒）
     * @return 任务，如果超时返回null
     */
    public PythonTask poll(long timeout) {
        try {
            PythonTask task = pendingTasks.poll(timeout, TimeUnit.MILLISECONDS);
            if (task != null) {
                task.markProcessing();
                log.debug("任务已取出: taskId={}", task.getTaskId());
            }
            return task;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * 获取任务信息（JSON格式）
     * 
     * 供Python worker通过Py4J调用
     * 
     * @param timeout 超时时间（毫秒）
     * @return 任务信息JSON，格式：{"taskId":"xxx","module":"xxx","method":"xxx","params":...}
     */
    public String pollTaskJson(long timeout) {
        PythonTask task = poll(timeout);
        if (task == null) {
            return null;
        }
        return task.toJson();
    }
    
    /**
     * 提交任务结果
     * 
     * 供Python worker通过Py4J调用
     * 
     * @param taskId 任务ID
     * @param result 结果（JSON格式）
     */
    public void completeTask(String taskId, String result) {
        PythonTask task = taskMap.get(taskId);
        if (task != null) {
            task.complete(result);
            taskMap.remove(taskId);
            log.debug("任务完成: taskId={}, executionTime={}ms", taskId, task.getExecutionTime());
        } else {
            log.warn("任务不存在: taskId={}", taskId);
        }
    }
    
    /**
     * 标记任务失败
     * 
     * 供Python worker通过Py4J调用
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    public void failTask(String taskId, String errorMessage) {
        PythonTask task = taskMap.get(taskId);
        if (task != null) {
            task.fail(errorMessage);
            taskMap.remove(taskId);
            log.error("任务失败: taskId={}, error={}", taskId, errorMessage);
        } else {
            log.warn("任务不存在: taskId={}", taskId);
        }
    }
    
    /**
     * 获取待处理任务数量
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }
    
    /**
     * 获取正在处理的任务数量
     */
    public int getProcessingCount() {
        return (int) taskMap.values().stream()
                .filter(t -> t.getStatus() == PythonTask.Status.PROCESSING)
                .count();
    }
    
    /**
     * 清空所有任务（用于关闭时）
     */
    public void clear() {
        for (PythonTask task : taskMap.values()) {
            if (!task.isDone()) {
                task.fail("Task queue is shutting down");
            }
        }
        pendingTasks.clear();
        taskMap.clear();
    }
}
