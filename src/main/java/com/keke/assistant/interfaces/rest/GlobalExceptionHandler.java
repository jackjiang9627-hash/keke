package com.keke.assistant.interfaces.rest;

import com.keke.assistant.domain.exception.CaseDuplicateException;
import com.keke.assistant.domain.exception.CaseException;
import com.keke.assistant.domain.exception.CaseNotFoundException;
import com.keke.monitor.domain.exception.MonitorException;
import com.keke.monitor.domain.exception.MonitorTaskNotFoundException;
import com.keke.shared.interfaces.rest.ErrorResponse;
import com.keke.ssh.domain.exception.SshException;
import com.keke.ssh.domain.exception.SshTaskNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * DDD概念：接口层的异常处理
 * - 统一处理异常
 * - 返回标准化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理案例不存在异常
     */
    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCaseNotFoundException(CaseNotFoundException ex) {
        log.warn("案例不存在: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.notFound(ex.getMessage()));
    }
    
    /**
     * 处理案例重复异常
     */
    @ExceptionHandler(CaseDuplicateException.class)
    public ResponseEntity<ErrorResponse> handleCaseDuplicateException(CaseDuplicateException ex) {
        log.warn("案例重复: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(ex.getMessage()));
    }
    
    /**
     * 处理其他案例业务异常
     */
    @ExceptionHandler(CaseException.class)
    public ResponseEntity<ErrorResponse> handleCaseException(CaseException ex) {
        log.warn("案例业务异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }
    
    // === SSH模块异常处理 ===
    
    /**
     * 处理SSH任务不存在异常
     */
    @ExceptionHandler(SshTaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSshTaskNotFoundException(SshTaskNotFoundException ex) {
        log.warn("SSH任务不存在: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.notFound(ex.getMessage()));
    }
    
    /**
     * 处理其他SSH业务异常
     */
    @ExceptionHandler(SshException.class)
    public ResponseEntity<ErrorResponse> handleSshException(SshException ex) {
        log.warn("SSH业务异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }
    
    // === 监控模块异常处理 ===
    
    /**
     * 处理监控任务不存在异常
     */
    @ExceptionHandler(MonitorTaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMonitorTaskNotFoundException(MonitorTaskNotFoundException ex) {
        log.warn("监控任务不存在: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.notFound(ex.getMessage()));
    }
    
    /**
     * 处理其他监控业务异常
     */
    @ExceptionHandler(MonitorException.class)
    public ResponseEntity<ErrorResponse> handleMonitorException(MonitorException ex) {
        log.warn("监控业务异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "请求参数验证失败"
        );

        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        response.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("未知异常: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.internalError("服务器内部错误：" + ex.getMessage()));
    }
}
