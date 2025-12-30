package com.loganalyzer.log.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志输入DTO - 用于接收外部日志数据
 * 
 * DDD概念：DTO（Data Transfer Object）
 * - 用于跨层传输数据
 * - 隔离领域模型和外部接口
 * - 包含输入验证
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogInputDTO {

    @NotBlank(message = "日志内容不能为空")
    private String content;

    private String application;
    private String host;
    private String environment;
}
