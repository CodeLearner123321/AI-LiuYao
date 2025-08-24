package com.divination.liuyao.util;

import com.divination.liuyao.pojo.entity.Prediction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供JSON字符串与Java对象之间的转换功能
 * 
 * @author AI-LiuYao
 * @since 2024-01-01
 */
public class JsonUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // 配置ObjectMapper
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 注册Java8时间模块
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }
    
    /**
     * 获取ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * 将对象转换为JSON字符串
     * 
     * @param obj 要转换的对象
     * @return JSON字符串，转换失败返回null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将对象转换为格式化的JSON字符串
     * 
     * @param obj 要转换的对象
     * @return 格式化的JSON字符串，转换失败返回null
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转格式化JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象
     * 
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || clazz == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.error("JSON转对象失败: {}, 目标类型: {}", e.getMessage(), clazz.getName(), e);
            return null;
        }
    }

    public static void main(String[] args) {
        String json = "{\\n    \\\"description\\\": {\\n        \\\"question\\\": \\\"如己月己日占久病，得到困卦，变为兑卦\\\",\\n        \\\"background\\\": \\\"野鹤说：这两种说法都不成立，殊不知虚一待用，待以后的月日可以填上。断卦说：“世爻寅木化出巳火，寅能刑巳，三刑中缺少‘申’字，须预防申日的危险。”果然死于申日申时。这是少一字而得到后来申日补充，怎么可以说“少一字就不成三刑”呢？\\\"\\n    },\\n    \\\"gua\\\": {\\n        \\\"zhuGua\\\": \\\"困卦\\\",\\n        \\\"bianGua\\\": \\\"兑卦\\\"\\n    },\\n    \\\"time\\\": {\\n        \\\"year\\\": {\\n            \\\"ganzhi\\\": null,\\n            \\\"time\\\": null\\n        },\\n        \\\"month\\\": {\\n            \\\"ganzhi\\\": {\\n                \\\"tiangan\\\": \\\"己\\\",\\n                \\\"dizhi\\\": \\\"巳\\\"\\n            },\\n            \\\"time\\\": null\\n        },\\n        \\\"day\\\": {\\n            \\\"ganzhi\\\": {\\n                \\\"tiangan\\\": \\\"己\\\",\\n                \\\"dizhi\\\": \\\"巳\\\"\\n            },\\n            \\\"time\\\": null\\n        },\\n        \\\"hour\\\": {\\n            \\\"ganzhi\\\": null,\\n            \\\"time\\\": null\\n        }\\n    }\\n}\"";
        Prediction prediction = fromJson(json, Prediction.class);
        System.out.println(prediction.toString());
    }

    /**
     * 将JSON字符串转换为指定类型的对象（支持泛型）
     * 
     * @param json JSON字符串
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty() || typeReference == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            logger.error("JSON转对象失败: {}, 类型引用: {}", e.getMessage(), typeReference.getType(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为List
     * 
     * @param json JSON字符串
     * @param clazz List中元素的类型
     * @param <T> 泛型类型
     * @return 转换后的List，转换失败返回null
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || clazz == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            logger.error("JSON转List失败: {}, 元素类型: {}", e.getMessage(), clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为Map
     * 
     * @param json JSON字符串
     * @return 转换后的Map，转换失败返回null
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            logger.error("JSON转Map失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为指定类型的Map
     * 
     * @param json JSON字符串
     * @param keyClass Map的key类型
     * @param valueClass Map的value类型
     * @param <K> key的泛型类型
     * @param <V> value的泛型类型
     * @return 转换后的Map，转换失败返回null
     */
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.trim().isEmpty() || keyClass == null || valueClass == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (IOException e) {
            logger.error("JSON转Map失败: {}, key类型: {}, value类型: {}", 
                e.getMessage(), keyClass.getName(), valueClass.getName(), e);
            return null;
        }
    }
    
    /**
     * 检查字符串是否为有效的JSON格式
     * 
     * @param json 要检查的字符串
     * @return 如果是有效JSON返回true，否则返回false
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 深度复制对象（通过JSON序列化实现）
     * 
     * @param obj 要复制的对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 复制后的对象，复制失败返回null
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.error("深度复制对象失败: {}, 目标类型: {}", e.getMessage(), clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 将对象转换为字节数组
     * 
     * @param obj 要转换的对象
     * @return 字节数组，转换失败返回null
     */
    public static byte[] toBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转字节数组失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将字节数组转换为对象
     * 
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0 || clazz == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            logger.error("字节数组转对象失败: {}, 目标类型: {}", e.getMessage(), clazz.getName(), e);
            return null;
        }
    }
}
