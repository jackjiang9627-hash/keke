package com.keke.shared.infrastructure.python;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Python进程管理器
 * 
 * 负责：
 * - Python进程的启动和停止
 * - 进程池管理（动态扩缩容）
 * - 进程健康检查
 */
@Slf4j
@Component
public class PythonProcessManager {
    
    /** Python脚本路径 */
    @Value("${python.worker.script:python/semantic_worker.py}")
    private String pythonScript;
    
    /** Python解释器路径 */
    @Value("${python.interpreter:python3}")
    private String pythonInterpreter;
    
    /** 最小进程数 */
    @Value("${python.worker.min-processes:1}")
    private int minProcesses;
    
    /** 最大进程数 */
    @Value("${python.worker.max-processes:4}")
    private int maxProcesses;
    
    /** Py4J网关端口 */
    @Value("${python.gateway.port:25333}")
    private int gatewayPort;
    
    /** 工作目录 */
    @Value("${python.worker.workdir:${user.dir}}")
    private String workDir;
    
    /** 进程列表 */
    private final List<Process> processes = new CopyOnWriteArrayList<>();
    
    /** 活跃进程数 */
    private final AtomicInteger activeProcessCount = new AtomicInteger(0);
    
    /** 调度线程池 */
    private ScheduledExecutorService scheduler;
    
    /** 任务队列引用 */
    private final PythonTaskQueue taskQueue;
    
    /** 是否已启动 */
    private volatile boolean started = false;
    
    /** 启动锁 */
    private final Object startLock = new Object();
    
    public PythonProcessManager(PythonTaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }
    
    /**
     * 确保进程管理器已启动（懒加载）
     * 在首次需要使用时才启动Python进程
     */
    public void ensureStarted() {
        if (started) {
            return;
        }
        synchronized (startLock) {
            if (!started) {
                start();
            }
        }
    }
    
    /**
     * 启动进程管理器
     */
    private void start() {
        log.info("启动Python进程管理器...");
        
        // 检查Python脚本是否存在（尝试多个可能的路径）
        File scriptFile = findPythonScript();
        if (scriptFile == null) {
            log.warn("Python脚本不存在，跳过Python进程启动。尝试的路径: {}/{}", workDir, pythonScript);
            return;
        }
        
        // 更新工作目录为脚本所在的项目根目录
        workDir = scriptFile.getParentFile().getParent();
        log.info("使用Python脚本: {}, 工作目录: {}", scriptFile.getAbsolutePath(), workDir);
        
        // 启动最小数量的进程
        for (int i = 0; i < minProcesses; i++) {
            startWorkerProcess();
        }
        
        // 启动监控线程
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::monitorAndScale, 10, 5, TimeUnit.SECONDS);
        
        started = true;
        log.info("Python进程管理器已启动，当前进程数: {}", activeProcessCount.get());
    }
    
    /**
     * 查找Python脚本文件（尝试多个可能的位置）
     */
    private File findPythonScript() {
        // 尝试的路径列表
        List<String> possiblePaths = new ArrayList<>();
        possiblePaths.add(new File(workDir, pythonScript).getAbsolutePath());
        possiblePaths.add(new File(System.getProperty("user.dir"), pythonScript).getAbsolutePath());
        
        // 尝试从 classpath 资源推断项目根目录
        try {
            var resource = getClass().getClassLoader().getResource("application.yml");
            if (resource != null) {
                String path = resource.getPath();
                // file:/path/to/project/target/classes/application.yml -> /path/to/project
                if (path.contains("/target/classes")) {
                    String projectRoot = path.substring(0, path.indexOf("/target/classes"));
                    if (projectRoot.startsWith("file:")) {
                        projectRoot = projectRoot.substring(5);
                    }
                    possiblePaths.add(new File(projectRoot, pythonScript).getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.debug("无法从 classpath 推断项目路径", e);
        }
        
        // 检查每个可能的路径
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * 停止进程管理器
     */
    @PreDestroy
    public void stop() {
        log.info("停止Python进程管理器...");
        started = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        // 停止所有进程
        for (Process process : processes) {
            stopProcess(process);
        }
        processes.clear();
        activeProcessCount.set(0);
        
        log.info("Python进程管理器已停止");
    }
    
    /**
     * 启动一个Worker进程
     */
    private synchronized void startWorkerProcess() {
        if (activeProcessCount.get() >= maxProcesses) {
            log.debug("已达到最大进程数: {}", maxProcesses);
            return;
        }
        
        try {
            File scriptFile = new File(workDir, pythonScript);
            ProcessBuilder pb = new ProcessBuilder(
                    pythonInterpreter,
                    scriptFile.getAbsolutePath(),
                    "--port", String.valueOf(gatewayPort)
            );
            pb.directory(new File(workDir));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            processes.add(process);
            activeProcessCount.incrementAndGet();
            
            // 启动日志读取线程
            startLogReader(process);
            
            log.info("Python Worker进程已启动，PID: {}, 当前进程数: {}", 
                    process.pid(), activeProcessCount.get());
            
        } catch (IOException e) {
            log.error("启动Python Worker进程失败", e);
        }
    }
    
    /**
     * 停止一个进程
     */
    private void stopProcess(Process process) {
        try {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }
            activeProcessCount.decrementAndGet();
            log.debug("Python进程已停止");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
    
    /**
     * 启动日志读取线程
     */
    private void startLogReader(Process process) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[Python] {}", line);
                }
            } catch (IOException e) {
                // 进程已关闭，忽略
            }
        });
        logThread.setDaemon(true);
        logThread.setName("python-log-reader-" + process.pid());
        logThread.start();
    }
    
    /**
     * 监控和动态扩缩容
     */
    private void monitorAndScale() {
        if (!started) return;
        
        // 清理已终止的进程
        List<Process> deadProcesses = new ArrayList<>();
        for (Process process : processes) {
            if (!process.isAlive()) {
                deadProcesses.add(process);
            }
        }
        for (Process dead : deadProcesses) {
            processes.remove(dead);
            activeProcessCount.decrementAndGet();
            log.warn("Python进程已终止，将重启");
        }
        
        // 确保至少有最小进程数
        while (activeProcessCount.get() < minProcesses) {
            startWorkerProcess();
        }
        
        // 根据任务队列动态扩容
        int pendingTasks = taskQueue.getPendingCount();
        int currentProcesses = activeProcessCount.get();
        
        if (pendingTasks > currentProcesses * 2 && currentProcesses < maxProcesses) {
            // 任务堆积，扩容
            int toStart = Math.min(pendingTasks / 2, maxProcesses - currentProcesses);
            for (int i = 0; i < toStart; i++) {
                startWorkerProcess();
            }
            log.info("任务堆积，扩容: {} -> {} 进程", currentProcesses, activeProcessCount.get());
        } else if (pendingTasks == 0 && currentProcesses > minProcesses) {
            // 无任务，缩容（保留最小进程数）
            int toStop = currentProcesses - minProcesses;
            for (int i = 0; i < toStop && processes.size() > minProcesses; i++) {
                Process process = processes.remove(processes.size() - 1);
                stopProcess(process);
            }
            log.info("无任务，缩容: {} -> {} 进程", currentProcesses, activeProcessCount.get());
        }
    }
    
    /**
     * 获取活跃进程数
     */
    public int getActiveProcessCount() {
        return activeProcessCount.get();
    }
    
    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started;
    }
}
