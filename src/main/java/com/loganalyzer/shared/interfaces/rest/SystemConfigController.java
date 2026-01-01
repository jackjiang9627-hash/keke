package com.loganalyzer.shared.interfaces.rest;

import com.loganalyzer.shared.application.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SystemConfigController {
    
    private final SystemConfigService configService;
    
    /**
     * 获取 LLM 相关配置
     */
    @GetMapping("/llm")
    public ResponseEntity<List<Map<String, Object>>> getLlmConfigs() {
        List<Map<String, Object>> configs = configService.getLlmConfigs();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 保存 LLM 配置
     */
    @PostMapping("/llm")
    public ResponseEntity<Map<String, Object>> saveLlmConfigs(@RequestBody Map<String, String> configs) {
        log.info("保存 LLM 配置: keys={}", configs.keySet());
        configService.saveConfigs(configs);
        return ResponseEntity.ok(Map.of("success", true, "message", "配置保存成功"));
    }
    
    /**
     * 获取单个配置
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String key) {
        return configService.getConfig(key.replace("_", "."))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 保存单个配置
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String realKey = key.replace("_", ".");
        String value = body.get("value");
        String description = body.getOrDefault("description", "");
        
        configService.saveValue(realKey, value, description);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key.replace("_", "."));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
