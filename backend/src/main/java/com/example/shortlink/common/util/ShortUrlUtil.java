
package com.example.shortlink.common.util;

/**
 * 短链接工具类 - 62进制转换（打乱字符集规避递增规律）
 */
public final class ShortUrlUtil {

    private static final String CHARS = "m8s2k9pzqrvn5t3g7bjx6y4dwhc1a0ulieofMNXJKLPRSTUVWYZABCGDHQ";
    private static final int SCALE = 62;
    private static final int FIX_SHORT_LEN = 6;

    private ShortUrlUtil() {}

    public static String numToFixedCode(long uid) {
        StringBuilder sb = new StringBuilder();
        long num = uid;
        while (num > 0) {
            sb.append(CHARS.charAt((int) (num % SCALE)));
            num /= SCALE;
        }
        while (sb.length() < FIX_SHORT_LEN) {
            sb.insert(0, CHARS.charAt(0));
        }
        return sb.reverse().toString();
    }

    public static long codeToUid(String code) {
        if (ObjectUtil.isEmpty(code)) {
            return 0;
        }
        long num = 0;
        for (char c : code.toCharArray()) {
            num = num * SCALE + CHARS.indexOf(c);
        }
        return num;
    }
}
