package com.divination.liuyao.util;

public class ConstantUtil {
    public final static Integer USER_FREE_QUOTA = 2;

    public final static String USER_REDIS_KEY = "USER_TOKEN";

    //默认设备指纹
    public final static String DEFAULT_DEVICE_FINGERPRINT = "web";

    //短信验证码Redis键前缀
    public final static String SMS_CODE_KEY = "SMS_CODE_KEY:";

    //邮箱验证码Redis键前缀
    public final static String EMAIL_CODE_KEY = "EMAIL_CODE_KEY:";

    //用户余额Redis键前缀
    public final static String USER_BALANCE_KEY = "user:balance:";
    
    //用户余额分布式锁键前缀
    public final static String USER_BALANCE_LOCK_KEY = "lock:user:balance:";
    
    //用户余额缓存过期时间(秒)
    public final static int USER_BALANCE_EXPIRE_TIME = 60;  // 1分钟

    //短信验证码计数器Redis键前缀，用于限制每日发送次数
    public final static String SMS_COUNTER_KEY = "SMS_COUNTER:";

    //邮箱验证码计数器Redis键前缀，用于限制每日发送次数
    public final static String EMAIL_COUNTER_KEY = "EMAIL_COUNTER:";

    //注册业务短信验证码
    public final static String SMS_CODE_TYPE_SIGN_IN = "SIGN_IN";

    //密码业务短信验证码
    public final static String SMS_CODE_TYPE_UPDATE = "UPDATE";

    //短信验证码过期时间(秒)
    public final static int SMS_CODE_EXPIRE_TIME = 60;  // 1分钟

    //邮箱验证码过期时间(秒)
    public final static int EMAIL_CODE_EXPIRE_TIME = 300;  // 5分钟

    //短信验证码每日最大发送次数
    public final static int SMS_MAX_DAILY_COUNT = 3;

    //邮箱验证码每日最大发送次数
    public final static int EMAIL_MAX_DAILY_COUNT = 5;

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

    //图片处理提示词
    public final static String IMAGE_PROCESSING_PROMPT_WORDS = "请帮我识别这个六爻卦象的内容，并将内容严格按照要求输出。\n" +
            "要求如下：\n" +
            "1、结果按照JSON形式输出，且只用返回JSON形式就行了。\n" +
            "2、返回的形式包含时间（时间格式按照yyyy-MM-dd-HH:mm:ss格式返回）、主卦（只包含名称，不能包含任何内容，例子：乾为天、天风姤等）、变卦（和主卦规则一样）。\n" +
            "举例：{\"time\": 2021-01-12-10:09,\"zhuGua\": \"雷水解\",\"bianGua\": \"雷地豫\"}就是雷水解变雷地豫";

    // todo 待优化
    public final static String IMAGE_PROCESSING_PROMPT_WORDS2 = "你有一个任务：提取图片中的有效信息,并按照Json格式返回(请注意只用返回字符串信息就可以了，除此之外不要返回任何其他的特殊字符,也不要修改原文)。同时你知道你返回的数据将会被转换成一个Java对象，所以你必须严格返回数据。 现在为了方便处理，你可以把你的提取任务分为三个方面。 1、提取问题与背景 2、提取卦象（本卦与变卦，如果没有变卦，则变卦和本卦一样。） 3、提取时间（时间分为 年月日时，年月日时又分为数字时间和干支,如果出现时间没有给出或者没有给全等不明确的情况，千万不要自己造一个数据，没有就是null）";
    public final static String IMAGE_SYSTEM_PROMPT = "你是一个资深的六爻专家，同时也是一名高级后端开发程序员,你需要根据我的任务，返回指定格式的Json数据。Json数据如下：";
}
