package com.divination.liuyao.util;

public class ConstantUtil {
    public final static String USER_REDIS_KEY = "USER_TOKEN";

    //默认设备指纹
    public final static String DEFAULT_DEVICE_FINGERPRINT = "web";

    //短信验证码Redis键前缀
    public final static String SMS_CODE_KEY = "SMS_CODE_KEY:";

    //短信验证码计数器Redis键前缀，用于限制每日发送次数
    public final static String SMS_COUNTER_KEY = "SMS_COUNTER:";

    //注册业务短信验证码
    public final static String SMS_CODE_TYPE_SIGN_IN = "SIGN_IN";

    //密码业务短信验证码
    public final static String SMS_CODE_TYPE_UPDATE = "UPDATE";

    //短信验证码过期时间(秒)
    public final static int SMS_CODE_EXPIRE_TIME = 60;  // 1分钟

    //短信验证码每日最大发送次数
    public final static int SMS_MAX_DAILY_COUNT = 3;

    public final static String AI_ERROR_RESULT = "该问题或背景有误，请明确问题后在进行分析！";

    public final static String AI_ERROR_RESULT_KEY = "分析失败";

    public final static String AI_ERROR_RESULT_CODE = "FALSE";

    public final static String LIU_CHONG = "六冲";

    public final static String LIU_HE = "六合";


    public final static String YI_MA = "驿马";

    public final static String JIAN_XING = "将星";

    public final static String XIAN_CHI = "咸池";

    public final static String HUA_GAI = "华盖";

    public final static String TIAN_XI = "天喜";

    public final static String TIAN_YI = "天医";

    public final static String LU_SHE = "禄神";

    public final static String WEN_CHANG = "文昌";

    public final static String YANG_REN = "羊刃";

    public final static String GUI_REN = "贵人";


}
