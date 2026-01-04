package com.keke.shared.domain.port;

/**
 * 加密服务端口（领域层定义）
 * 
 * 用于加密敏感配置（如API Key）
 * 基础设施层提供具体实现
 */
public interface EncryptionPort {
    
    /**
     * 加密文本
     *
     * @param plainText 明文
     * @return 加密后的密文（Base64编码）
     */
    String encrypt(String plainText);
    
    /**
     * 解密文本
     *
     * @param encryptedText 密文（Base64编码）
     * @return 解密后的明文
     */
    String decrypt(String encryptedText);
}
