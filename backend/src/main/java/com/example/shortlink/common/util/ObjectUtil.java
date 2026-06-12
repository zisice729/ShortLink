
package com.example.shortlink.common.util;

import java.util.Collection;
import java.util.Map;

/**
 * 对象工具类 - 判空判等操作
 * 统一封装判空判等逻辑，避免直接使用 == !=
 */
public final class ObjectUtil {

    private ObjectUtil() {}

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }

    public static boolean isEmpty(String str) {
        return isNull(str) || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        return isNull(str) || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(Object[] array) {
        return isNull(array) || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return isNull(collection) || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return isNull(map) || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) return true;
        if (isNull(obj1) || isNull(obj2)) return false;
        return obj1.equals(obj2);
    }

    public static boolean notEquals(Object obj1, Object obj2) {
        return !equals(obj1, obj2);
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == str2) return true;
        if (isNull(str1) || isNull(str2)) return false;
        return str1.equalsIgnoreCase(str2);
    }

    public static boolean notEqualsIgnoreCase(String str1, String str2) {
        return !equalsIgnoreCase(str1, str2);
    }

    public static boolean isGreaterThanZero(Number num) {
        return isNotNull(num) && num.doubleValue() > 0;
    }

    public static boolean isGreaterThanOrEqualZero(Number num) {
        return isNotNull(num) && num.doubleValue() >= 0;
    }

    public static boolean isLessThanZero(Number num) {
        return isNotNull(num) && num.doubleValue() < 0;
    }

    public static boolean isLessThanOrEqualZero(Number num) {
        return isNotNull(num) && num.doubleValue() <= 0;
    }

    public static boolean equals(Number num1, Number num2) {
        if (num1 == num2) return true;
        if (isNull(num1) || isNull(num2)) return false;
        return num1.doubleValue() == num2.doubleValue();
    }

    public static boolean notEquals(Number num1, Number num2) {
        return !equals(num1, num2);
    }

    public static <T> T getOrDefault(T obj, T defaultValue) {
        return isNotNull(obj) ? obj : defaultValue;
    }

    public static String getOrDefault(String str, String defaultValue) {
        return isNotEmpty(str) ? str : defaultValue;
    }
}
