package com.shortlink.util;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * =====================================================
 * 长链接标准化工具类
 * =====================================================
 * 
 * 功能说明：
 *   将原始URL进行标准化处理，保证相同内容但格式不同的URL生成相同的MD5值
 *   从而实现"相同长链接返回相同短码"的需求
 * 
 * 标准化规则：
 *   1. 域名转小写：http://WWW.EXAMPLE.COM → http://www.example.com
 *   2. 参数排序：?b=2&a=1 → ?a=1&b=2
 *   3. 去除末尾斜杠：http://example.com/ → http://example.com
 *   4. URL解码：对参数值进行解码处理
 * 
 * 示例：
 *   - http://Example.COM/test/ → http://example.com/test
 *   - http://example.com/?b=2&a=1 → http://example.com/?a=1&b=2
 * 
 * 重要性：
 *   不进行标准化的话，以下URL会被认为是不同的：
 *   - http://example.com 和 http://EXAMPLE.COM
 *   - http://example.com?a=1&b=2 和 http://example.com?b=2&a=1
 * 
 * @author 短链接系统团队
 */
public class UrlStandardUtil {

    /** 私有构造函数，防止实例化 */
    private UrlStandardUtil() {}

    /**
     * 链接标准化：去除末尾斜杠、参数排序、统一小写域名
     * 
     * 处理流程：
     *   1. 去除首尾空白并转小写
     *   2. 解析URL各部分（协议、域名、路径、参数）
     *   3. 路径末尾的/去除（如果有）
     *   4. 参数按键排序并URL解码
     *   5. 重新组装URL
     * 
     * @param url 原始URL字符串
     * @return 标准化后的URL字符串
     */
    public static String standard(String url) {
        // 空值或空字符串直接返回空字符串
        if (url == null || url.isEmpty()) return "";
        
        try {
            // 去除首尾空白并转小写
            String standardUrl = url.trim().toLowerCase();
            
            // 解析URL各部分
            URL urlObj = new URL(standardUrl);
            
            // 获取协议（http/https）
            String protocol = urlObj.getProtocol();
            // 获取域名（转小写后）
            String host = urlObj.getHost().toLowerCase();
            // 获取路径
            String path = urlObj.getPath();
            
            // 去除路径末尾的斜杠（如果路径长度大于1）
            // 例如：/test/ → /test，但/保持不变
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            
            // 获取查询参数
            String query = urlObj.getQuery();
            // 对参数进行排序和解码
            String sortedQuery = sortQueryParams(query);
            
            // 重新组装URL
            StringBuilder result = new StringBuilder();
            result.append(protocol).append("://").append(host).append(path);
            
            // 如果有查询参数，添加到结果中
            if (sortedQuery != null && !sortedQuery.isEmpty()) {
                result.append("?").append(sortedQuery);
            }
            
            return result.toString();
        } catch (Exception e) {
            // 解析失败时，返回处理后的原始URL
            return url.trim().toLowerCase();
        }
    }

    /**
     * 对URL查询参数进行排序和解码
     * 
     * 处理逻辑：
     *   1. 将参数分割成 key=value 形式
     *   2. 使用TreeMap自动按键排序
     *   3. 对参数值进行URL解码
     *   4. 重新组装成排序后的参数字符串
     * 
     * 示例：
     *   输入："b=2&a=1&c=3"
     *   输出："a=1&b=2&c=3"
     * 
     * @param query URL中的查询参数字符串（不含?）
     * @return 排序解码后的参数字符串
     */
    private static String sortQueryParams(String query) {
        // 空参数直接返回空字符串
        if (query == null || query.isEmpty()) {
            return "";
        }
        
        // TreeMap自动按键排序
        TreeMap<String, String> paramMap = new TreeMap<>();
        
        // 按&分割参数
        String[] params = query.split("&");
        
        for (String param : params) {
            // 查找=的位置
            int idx = param.indexOf("=");
            if (idx > 0) {
                // 分离键和值
                String key = param.substring(0, idx);
                String value = param.substring(idx + 1);
                // 对值进行URL解码
                try {
                    value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    // 解码失败保持原值
                }
                paramMap.put(key, value);
            } else {
                // 没有=的参数，直接作为键，值为空
                paramMap.put(param, "");
            }
        }
        
        // 重新组装排序后的参数字符串
        StringBuilder sb = new StringBuilder();
        paramMap.forEach((key, value) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(key).append("=").append(value);
        });
        
        return sb.toString();
    }
}
