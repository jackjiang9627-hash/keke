package com.loganalyzer.ssh.infrastructure.adapter;

import com.loganalyzer.ssh.domain.entity.Device;
import com.loganalyzer.ssh.domain.port.SshExecutor;
import com.loganalyzer.ssh.domain.valueobject.AuthType;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * Apache MINA SSHD实现的SSH执行器
 */
@Slf4j
@Component
public class SshdExecutorAdapter implements SshExecutor {
    
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int AUTH_TIMEOUT_SECONDS = 30;
    private static final int COMMAND_TIMEOUT_SECONDS = 300;
    
    @Override
    public String testConnection(Device device) {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = createSession(client, device)) {
                authenticate(session, device);
                log.info("SSH连接测试成功: {}@{}:{}", device.getUsername(), device.getHost(), device.getPort());
                return null;
            }
        } catch (Exception e) {
            log.error("SSH连接测试失败: {}@{}:{} - {}", device.getUsername(), device.getHost(), device.getPort(), e.getMessage());
            return e.getMessage();
        }
    }
    
    @Override
    public CommandResult executeCommand(Device device, String command) {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = createSession(client, device)) {
                authenticate(session, device);
                
                try (ChannelExec channel = session.createExecChannel(command)) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                    
                    channel.setOut(outputStream);
                    channel.setErr(errorStream);
                    
                    channel.open().verify(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS));
                    
                    // 等待命令执行完成
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 
                            TimeUnit.SECONDS.toMillis(COMMAND_TIMEOUT_SECONDS));
                    
                    Integer exitStatus = channel.getExitStatus();
                    int exitCode = exitStatus != null ? exitStatus : 0;
                    String output = outputStream.toString();
                    String error = errorStream.toString();
                    
                    log.info("命令执行完成: {}@{}:{} - exitCode={}", 
                            device.getUsername(), device.getHost(), device.getPort(), exitCode);
                    
                    if (exitCode == 0 || error.isEmpty()) {
                        return CommandResult.success(output, exitCode);
                    } else {
                        return new CommandResult(false, output, error, exitCode);
                    }
                }
            }
        } catch (Exception e) {
            log.error("命令执行失败: {}@{}:{} - {}", device.getUsername(), device.getHost(), device.getPort(), e.getMessage());
            return CommandResult.failure(e.getMessage());
        }
    }
    
    @Override
    public String uploadFile(Device device, String localPath, String remotePath) {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = createSession(client, device)) {
                authenticate(session, device);
                
                ScpClientCreator creator = ScpClientCreator.instance();
                ScpClient scpClient = creator.createScpClient(session);
                
                scpClient.upload(Path.of(localPath), remotePath, 
                        ScpClient.Option.PreserveAttributes, ScpClient.Option.TargetIsDirectory);
                
                log.info("文件上传成功: {} -> {}@{}:{}{}", 
                        localPath, device.getUsername(), device.getHost(), device.getPort(), remotePath);
                return null;
            }
        } catch (Exception e) {
            log.error("文件上传失败: {} -> {}@{}:{}{} - {}", 
                    localPath, device.getUsername(), device.getHost(), device.getPort(), remotePath, e.getMessage());
            return e.getMessage();
        }
    }
    
    @Override
    public String downloadFile(Device device, String remotePath, String localPath) {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = createSession(client, device)) {
                authenticate(session, device);
                
                ScpClientCreator creator = ScpClientCreator.instance();
                ScpClient scpClient = creator.createScpClient(session);
                
                scpClient.download(remotePath, Path.of(localPath), 
                        ScpClient.Option.PreserveAttributes);
                
                log.info("文件下载成功: {}@{}:{}{} -> {}", 
                        device.getUsername(), device.getHost(), device.getPort(), remotePath, localPath);
                return null;
            }
        } catch (Exception e) {
            log.error("文件下载失败: {}@{}:{}{} -> {} - {}", 
                    device.getUsername(), device.getHost(), device.getPort(), remotePath, localPath, e.getMessage());
            return e.getMessage();
        }
    }
    
    private ClientSession createSession(SshClient client, Device device) throws IOException {
        return client.connect(device.getUsername(), device.getHost(), device.getPort())
                .verify(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .getSession();
    }
    
    private void authenticate(ClientSession session, Device device) throws IOException {
        if (device.getAuthType() == AuthType.KEY && device.getPrivateKey() != null) {
            // 密钥认证
            try {
                KeyPair keyPair = loadKeyPair(device.getPrivateKey());
                session.addPublicKeyIdentity(keyPair);
            } catch (Exception e) {
                log.warn("密钥加载失败，尝试密码认证: {}", e.getMessage());
                if (device.getPassword() != null) {
                    session.addPasswordIdentity(device.getPassword());
                }
            }
        } else {
            // 密码认证
            if (device.getPassword() != null) {
                session.addPasswordIdentity(device.getPassword());
            }
        }
        
        session.auth().verify(Duration.ofSeconds(AUTH_TIMEOUT_SECONDS));
    }
    
    private KeyPair loadKeyPair(String privateKeyContent) throws Exception {
        // 使用SSHD的密钥加载器
        Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(
                null,
                null,
                new java.io.ByteArrayInputStream(privateKeyContent.getBytes()),
                FilePasswordProvider.EMPTY
        );
        
        for (KeyPair kp : keyPairs) {
            return kp;
        }
        throw new IllegalArgumentException("无法解析私钥");
    }
}
