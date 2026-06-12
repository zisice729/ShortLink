
package com.example.shortlink.common.enums;

/**
 * 短链接状态枚举
 */
public enum StatusEnum {

    NORMAL(1, "正常"),
    DISABLED(0, "禁用"),
    DELETED(-1, "已删除");

    private final int code;
    private final String desc;

    StatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static StatusEnum fromCode(int code) {
        for (StatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
}
