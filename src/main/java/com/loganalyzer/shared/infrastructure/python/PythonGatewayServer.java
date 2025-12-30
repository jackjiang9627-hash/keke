package com.loganalyzer.shared.infrastructure.python;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import py4j.GatewayServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Py4J网关服务器
 * 
 * 提供Java端的入口点，供Python调用
 * Python通过这个网关可以：
 * - 获取任务
 * - 提交结果
 */
@Slf4j
@Component
public class PythonGatewayServer {
    
    /** 网关端口 */
    @Value("${python.gateway.port:25333}")
    private int port;
    
    /** 任务队列 */
    private final PythonTaskQueue taskQueue;
    
    /** Py4J网关服务器 */
    private GatewayServer gatewayServer;
    
    /** 是否已启动 */
    private volatile boolean started = false;
    
    public PythonGatewayServer(PythonTaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }
    
    /**
     * 启动网关服务器
     */
    @PostConstruct
    public void start() {
        try {
            // 创建入口点对象
            PythonEntryPoint entryPoint = new PythonEntryPoint(taskQueue);
            
            // 创建并启动Py4J网关服务器
            gatewayServer = new GatewayServer(entryPoint, port);
            gatewayServer.start();
            
            started = true;
            log.info("Py4J网关服务器已启动，端口: {}", port);
            
        } catch (Exception e) {
            log.error("启动Py4J网关服务器失败", e);
        }
    }
    
    /**
     * 停止网关服务器
     */
    @PreDestroy
    public void stop() {
        if (gatewayServer != null) {
            gatewayServer.shutdown();
            log.info("Py4J网关服务器已停止");
        }
        started = false;
    }
    
    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * 获取端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Python入口点
     * 
     * Python通过这个类与Java交互
     */
    public static class PythonEntryPoint {
        
        private final PythonTaskQueue taskQueue;
        
        public PythonEntryPoint(PythonTaskQueue taskQueue) {
            this.taskQueue = taskQueue;
        }
        
        /**
         * 获取任务（供Python调用）
         * 
         * @param timeout 超时时间（毫秒）
         * @return 任务JSON，格式：{"taskId":"xxx","taskType":"xxx","inputData":"xxx"}
         */
        public String pollTask(long timeout) {
            return taskQueue.pollTaskJson(timeout);
        }
        
        /**
         * 提交任务结果（供Python调用）
         * 
         * @param taskId 任务ID
         * @param result 结果JSON
         */
        public void completeTask(String taskId, String result) {
            taskQueue.completeTask(taskId, result);
        }
        
        /**
         * 标记任务失败（供Python调用）
         * 
         * @param taskId 任务ID
         * @param errorMessage 错误信息
         */
        public void failTask(String taskId, String errorMessage) {
            taskQueue.failTask(taskId, errorMessage);
        }
        
        /**
         * 心跳检测（供Python调用）
         * 
         * @return "pong"
         */
        public String ping() {
            return "pong";
        }
        
        /**
         * 获取待处理任务数（供Python调用）
         */
        public int getPendingCount() {
            return taskQueue.getPendingCount();
        }
    }
}
