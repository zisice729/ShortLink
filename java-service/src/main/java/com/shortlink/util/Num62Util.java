package com.shortlink.util;

/**
 * =====================================================
 * 62进制转换工具类
 * =====================================================
 * 
 * 功能说明：
 *   将数字ID转换为62进制字符串（短码），支持动态长度
 *   字符集：0-9(10个) + a-z(26个) + A-Z(26个) = 62个字符
 * 
 * 短码长度说明：
 *   - 6位短码最大容量：62^6 ≈ 568亿
 *   - 7位短码最大容量：62^7 ≈ 3.5万亿
 *   - 8位短码最大容量：62^8 ≈ 218万亿
 * 
 * 使用场景：
 *   - 数字ID转短码：Num62Util.numTo62(123456) → "w7E"
 *   - 短码转数字ID：Num62Util.codeToNum("w7E") → 123456
 * 
 * 算法原理：
 *   十进制转62进制：不断除以62，取余数，最后反转
 *   62进制转十进制：从左到右，每位乘以62的幂次方累加
 * 
 * @author 短链接系统团队
 */
public class Num62Util {

    /**
     * 62进制字符集
     * 包含数字0-9、小写字母a-z、大写字母A-Z
     * 共62个字符，可作为62进制的每一位
     */
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /** 进制基数，62进制 */
    private static final int SCALE = 62;
    
    /** 6位短码能表示的最大数字（62^6），用于判断是否需要扩展位数 */
    private static final long MAX_6BIT = 56800235584L;

    /** 私有构造函数，防止实例化 */
    private Num62Util() {}

    /**
     * 数字转62进制短码（动态长度）
     * 
     * 算法步骤：
     *   1. 将数字不断除以62，取余数得到各位字符
     *   2. 将余数反转得到正常的62进制表示
     *   3. 长度根据数字大小自动扩展
     * 
     * 示例：
     *   - numTo62(1)    → "1"
     *   - numTo62(62)   → "10"
     *   - numTo62(123)  → "1V"
     * 
     * @param num 要转换的数字，必须大于0
     * @return 62进制字符串，如果输入<=0则返回空字符串
     */
    public static String numTo62(long num) {
        if (num <= 0) return "";
        
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            // 取余数，得到当前位的字符
            sb.append(CHARS.charAt((int) (num % SCALE)));
            // 除以62，继续处理下一位
            num = num / SCALE;
        }
        // 反转字符串得到正常的62进制表示
        return sb.reverse().toString();
    }

    /**
     * 62进制短码转回数字
     * 
     * 算法步骤：
     *   从左到右遍历字符，将每位的值乘以62的幂次方后累加
     * 
     * 示例：
     *   - codeToNum("1")   → 1
     *   - codeToNum("10")  → 62
     *   - codeToNum("1V")  → 123
     * 
     * @param code 62进制短码字符串
     * @return 对应的十进制数字
     */
    public static long codeToNum(String code) {
        long res = 0;
        for (char c : code.toCharArray()) {
            // 累加：res = res * 62 + 当前位的值
            res = res * SCALE + CHARS.indexOf(c);
        }
        return res;
    }

    /**
     * 判断数字是否超过6位短码能表示的范围
     * 
     * 用途：
     *   用于判断生成的ID是否需要扩展为7位及以上
     *   6位短码最大容量约568亿，7位约3.5万亿
     * 
     * @param num 要检查的数字
     * @return true表示超过6位范围，需要扩展位数；false表示在6位范围内
     */
    public static boolean isOver6Bit(long num) {
        return num > MAX_6BIT;
    }
}
