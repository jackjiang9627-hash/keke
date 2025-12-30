package com.loganalyzer.shared.infrastructure.python;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python桥接服务 - 通用Python调用引擎
 * 
 * 提供高级别的API用于：
 * - 通用的Python模块/方法调用
 * - 提交任务并同步等待结果
 * - 语义处理的便捷方法
 */
@Slf4j
@Service
public class PythonBridge {
    
    /** 默认超时时间（秒） */
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    
    /** 语义处理模块名 */
    public static final String MODULE_SEMANTIC = "semantic";
    
    private final PythonTaskQueue taskQueue;
    private final PythonProcessManager processManager;
    private final ObjectMapper objectMapper;
    
    public PythonBridge(PythonTaskQueue taskQueue, 
                        PythonProcessManager processManager,
                        ObjectMapper objectMapper) {
        this.taskQueue = taskQueue;
        this.processManager = processManager;
        this.objectMapper = objectMapper;
    }
    
    // ==================== 通用执行API ====================
    
    /**
     * 执行Python模块的方法
     * 
     * @param module Python模块名（如 semantic, nlp, ml 等）
     * @param method 方法名
     * @param params 参数（会被序列化为JSON）
     * @return 执行结果（JSON字符串）
     */
    public String execute(String module, String method, Object params) {
        return execute(module, method, params, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * 执行Python模块的方法
     * 
     * @param module Python模块名
     * @param method 方法名
     * @param params 参数（会被序列化为JSON）
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果（JSON字符串）
     */
    public String execute(String module, String method, Object params, long timeoutSeconds) {
        try {
            // 序列化参数
            String paramsJson = objectMapper.writeValueAsString(params);
            
            // 创建任务
            PythonTask task = PythonTask.create(module, method, paramsJson);
            
            log.debug("提交Python任务: module={}, method={}, taskId={}", module, method, task.getTaskId());
            
            // 提交任务
            taskQueue.submit(task);
            
            // 等待结果
            boolean completed = task.await(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                throw new PythonExecutionException(
                    String.format("Python任务超时: module=%s, method=%s, timeout=%ds", 
                        module, method, timeoutSeconds));
            }
            
            if (!task.isSuccess()) {
                throw new PythonExecutionException(
                    String.format("Python任务失败: module=%s, method=%s, error=%s", 
                        module, method, task.getErrorMessage()));
            }
            
            log.debug("Python任务完成: taskId={}, 耗时={}ms", task.getTaskId(), task.getExecutionTime());
            
            return task.getResult();
            
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("参数序列化失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonExecutionException("等待任务被中断", e);
        }
    }
    
    /**
     * 执行Python模块的方法并解析结果
     * 
     * @param module Python模块名
     * @param method 方法名
     * @param params 参数
     * @param resultType 结果类型
     * @return 解析后的结果
     */
    public <T> T execute(String module, String method, Object params, Class<T> resultType) {
        String resultJson = execute(module, method, params);
        try {
            return objectMapper.readValue(resultJson, resultType);
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("结果解析失败: " + resultJson, e);
        }
    }
    
    /**
     * 执行Python模块的方法并获取JSON节点
     */
    public JsonNode executeForJson(String module, String method, Object params) {
        String resultJson = execute(module, method, params);
        try {
            return objectMapper.readTree(resultJson);
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("结果解析失败: " + resultJson, e);
        }
    }
    
    // ==================== 语义处理便捷方法 ====================
    
    /**
     * 计算单个文本的语义向量
     * 
     * @param text 文本
     * @return 语义向量（float数组）
     */
    public float[] computeEmbedding(String text) {
        return computeEmbedding(text, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * 计算单个文本的语义向量
     * 
     * @param text 文本
     * @param timeoutSeconds 超时时间（秒）
     * @return 语义向量（float数组）
     */
    public float[] computeEmbedding(String text, long timeoutSeconds) {
        String resultJson = execute(MODULE_SEMANTIC, "compute_embedding", Map.of("text", text), timeoutSeconds);
        try {
            EmbeddingResult result = objectMapper.readValue(resultJson, EmbeddingResult.class);
            return result.embedding;
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("解析语义向量结果失败", e);
        }
    }
    
    /**
     * 批量计算文本的语义向量
     * 
     * @param texts 文本列表
     * @return 语义向量列表
     */
    public List<float[]> computeBatchEmbeddings(List<String> texts) {
        return computeBatchEmbeddings(texts, DEFAULT_TIMEOUT_SECONDS * texts.size() / 10 + 10);
    }
    
    /**
     * 批量计算文本的语义向量
     * 
     * @param texts 文本列表
     * @param timeoutSeconds 超时时间（秒）
     * @return 语义向量列表
     */
    public List<float[]> computeBatchEmbeddings(List<String> texts, long timeoutSeconds) {
        String resultJson = execute(MODULE_SEMANTIC, "compute_batch_embeddings", Map.of("texts", texts), timeoutSeconds);
        try {
            BatchEmbeddingResult result = objectMapper.readValue(resultJson, BatchEmbeddingResult.class);
            return result.embeddings;
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("解析批量语义向量结果失败", e);
        }
    }
    
    /**
     * 计算两个文本的相似度
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度（0-1之间）
     */
    public double computeSimilarity(String text1, String text2) {
        String resultJson = execute(MODULE_SEMANTIC, "compute_similarity", Map.of("text1", text1, "text2", text2));
        try {
            SimilarityResult result = objectMapper.readValue(resultJson, SimilarityResult.class);
            return result.similarity;
        } catch (JsonProcessingException e) {
            throw new PythonExecutionException("解析相似度结果失败", e);
        }
    }
    
    // ==================== 状态查询 ====================
    
    /**
     * 检查Python服务是否可用
     */
    public boolean isAvailable() {
        return processManager.isStarted() && processManager.getActiveProcessCount() > 0;
    }
    
    /**
     * 获取当前活跃的Python进程数
     */
    public int getActiveProcessCount() {
        return processManager.getActiveProcessCount();
    }
    
    /**
     * 获取待处理任务数
     */
    public int getPendingTaskCount() {
        return taskQueue.getPendingCount();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * Python执行异常
     */
    public static class PythonExecutionException extends RuntimeException {
        public PythonExecutionException(String message) {
            super(message);
        }
        
        public PythonExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // ========== 语义处理结果结构 ==========
    
    public record EmbeddingResult(float[] embedding) {}
    
    public record BatchEmbeddingResult(List<float[]> embeddings) {}
    
    public record SimilarityResult(double similarity) {}
}
