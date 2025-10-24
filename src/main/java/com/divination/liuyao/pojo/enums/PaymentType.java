package com.divination.liuyao.pojo.enums;

/**
 * 支付类型枚举
 * 0：免费额度支付
 * 1：余额支付
 * 2：用户自定义API支付
 */
public enum PaymentType {

    FREE_QUOTA_PAYMENT(0, "免费额度支付"),
    BALANCE_PAYMENT(1, "余额支付"),
    USER_DEFINED_API_PAYMENT(2, "用户自定义API支付");

    private final int code;
    private final String description;

    PaymentType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static PaymentType fromCode(int code) {
        for (PaymentType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的支付类型 code: " + code);
    }
}
