package com.keke.ssh.interfaces.rest;

import com.keke.ssh.application.dto.SshTaskInputDTO;
import com.keke.ssh.application.dto.SshTaskOutputDTO;
import com.keke.ssh.application.service.SshTaskApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SSH任务REST API
 */
@RestController
@RequestMapping("/api/ssh/tasks")
@RequiredArgsConstructor
public class SshTaskController {
    
    private final SshTaskApplicationService taskService;
    
    /**
     * 执行命令
     */
    @PostMapping("/command")
    public ResponseEntity<SshTaskOutputDTO> executeCommand(@RequestBody SshTaskInputDTO input) {
        input.setType("COMMAND");
        return ResponseEntity.ok(taskService.executeCommand(input));
    }
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<SshTaskOutputDTO> uploadFile(@RequestBody SshTaskInputDTO input) {
        input.setType("UPLOAD");
        return ResponseEntity.ok(taskService.executeUpload(input));
    }
    
    /**
     * 下载文件
     */
    @PostMapping("/download")
    public ResponseEntity<SshTaskOutputDTO> downloadFile(@RequestBody SshTaskInputDTO input) {
        input.setType("DOWNLOAD");
        return ResponseEntity.ok(taskService.executeDownload(input));
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<SshTaskOutputDTO> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }
    
    /**
     * 获取最近任务列表
     */
    @GetMapping
    public ResponseEntity<List<SshTaskOutputDTO>> getRecentTasks(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(taskService.getRecentTasks(limit));
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }
}
