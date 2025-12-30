package com.loganalyzer.shared.infrastructure.python;

import lombok.Data;
import lombok.Builder;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Python任务抽象
 * 
 * 支持通用的Python调用：
 * - 指定要执行的模块（插件）
 * - 指定要调用的方法名
 * - 指定参数（JSON格式）
 * - 使用CountDownLatch实现等待/通知机制
 */
@Data
@Builder
public class PythonTask {
    
    /**
     * 任务状态枚举
     */
    public enum Status {
        PENDING,      // 等待处理
        PROCESSING,   // 处理中
        COMPLETED,    // 完成
        FAILED        // 失败
    }
    
    /** 任务唯一ID */
    private final String taskId;
    
    /** Python模块名（插件名，如 semantic, nlp, ml 等） */
    private final String module;
    
    /** 要调用的方法名 */
    private final String method;
    
    /** 输入参数（JSON格式） */
    private final String params;
    
    /** 任务状态 */
    private volatile Status status;
    
    /** 输出结果（JSON格式） */
    private volatile String result;
    
    /** 错误信息 */
    private volatile String errorMessage;
    
    /** 创建时间 */
    private final long createTime;
    
    /** 完成时间 */
    private volatile long completeTime;
    
    /** 等待锁 */
    private final CountDownLatch latch;
    
    /**
     * 创建新任务
     * 
     * @param module Python模块名（插件名）
     * @param method 方法名
     * @param params 参数（JSON格式）
     */
    public static PythonTask create(String module, String method, String params) {
        return PythonTask.builder()
                .taskId(UUID.randomUUID().toString())
                .module(module)
                .method(method)
                .params(params)
                .status(Status.PENDING)
                .createTime(System.currentTimeMillis())
                .latch(new CountDownLatch(1))
                .build();
    }
    
    /**
     * 标记任务完成
     */
    public void complete(String result) {
        this.result = result;
        this.status = Status.COMPLETED;
        this.completeTime = System.currentTimeMillis();
        this.latch.countDown();
    }
    
    /**
     * 标记任务失败
     */
    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = Status.FAILED;
        this.completeTime = System.currentTimeMillis();
        this.latch.countDown();
    }
    
    /**
     * 标记任务开始处理
     */
    public void markProcessing() {
        this.status = Status.PROCESSING;
    }
    
    /**
     * 等待任务完成
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时前完成
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }
    
    /**
     * 无限等待任务完成
     */
    public void await() throws InterruptedException {
        latch.await();
    }
    
    /**
     * 检查任务是否完成（成功或失败）
     */
    public boolean isDone() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }
    
    /**
     * 检查任务是否成功
     */
    public boolean isSuccess() {
        return status == Status.COMPLETED;
    }
    
    /**
     * 获取任务执行时间（毫秒）
     */
    public long getExecutionTime() {
        if (completeTime > 0) {
            return completeTime - createTime;
        }
        return System.currentTimeMillis() - createTime;
    }
    
    /**
     * 转换为JSON格式（供Python端解析）
     */
    public String toJson() {
        return String.format(
            "{\"taskId\":\"%s\",\"module\":\"%s\",\"method\":\"%s\",\"params\":%s}",
            taskId, module, method, params != null ? params : "null"
        );
    }
}
