package com.shortlink.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * =====================================================
 * MD5工具类
 * =====================================================
 * 
 * 功能说明：
 *   计算字符串的MD5值，用于长链接去重和唯一标识
 * 
 * 使用场景：
 *   1. 长链接去重：将长链接的MD5值存储，用于快速判断是否已生成短链
 *   2. 数据标识：生成唯一标识符
 * 
 * MD5特性：
 *   - 任意长度输入 → 固定32位输出
 *   - 不可逆：无法从MD5值反推原始数据
 *   - 碰撞率低：不同数据产生相同MD5的概率极低
 * 
 * 注意事项：
 *   - MD5不是加密算法，不适合加密敏感信息
 *   - 对于安全性要求高的场景，建议使用SHA-256
 * 
 * @author 短链接系统团队
 */
public class Md5Util {

    /** 私有构造函数，防止实例化 */
    private Md5Util() {}

    /**
     * 计算字符串MD5值
     * 
     * 处理流程：
     *   1. 获取MD5算法实例
     *   2. 将字符串转换为字节数组
     *   3. 计算摘要
     *   4. 将字节转换为十六进制字符串
     * 
     * 示例：
     *   - md5("hello") → "5d41402abc4b2a76b9719d911017c592"
     *   - md5("https://example.com") → "1a2b3c4d5e6f..." (具体值)
     * 
     * @param input 输入字符串
     * @return 32位十六进制MD5字符串，空输入返回空字符串
     */
    public static String md5(String input) {
        // 空值或空字符串直接返回空字符串
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            // 获取MD5算法实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // 计算摘要
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 将字节转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                // %02x 表示两位十六进制，不足补0
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5算法总是可用的，如果不可用抛出运行时异常
            throw new RuntimeException("MD5算法不可用", e);
        }
    }
}
