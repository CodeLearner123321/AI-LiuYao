package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.liuyao.pojo.entity.CardKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 卡密数据访问层接口
 * 继承BaseMapper获得通用的CRUD方法
 */
@Mapper
public interface CardKeyMapper extends BaseMapper<CardKey> {
    
    /**
     * 根据卡密码查询卡密信息
     * @param cardCode 卡密码
     * @return 卡密对象的可选包装
     */
    Optional<CardKey> findByCardCode(@Param("cardCode") String cardCode);
    
    /**
     * 使用卡密
     * 更新卡密状态为已使用，并记录使用者ID和使用时间
     * 使用乐观锁，只有当卡密未使用时才能更新成功
     * 
     * @param cardCode 卡密码
     * @param userId 使用者ID
     * @return 影响的行数，1表示成功，0表示失败（已使用或不存在）
     */
    int useCardKey(@Param("cardCode") String cardCode, @Param("userId") Long userId);
    
    /**
     * 批量插入卡密
     * 
     * @param cardKeyList 卡密列表
     * @return 插入的行数
     */
    int batchInsert(@Param("list") List<CardKey> cardKeyList);
    
    /**
     * 根据状态和金额查询卡密列表
     * 
     * @param status 卡密状态（可选）
     * @param amount 卡密金额（可选）
     * @return 卡密列表
     */
    List<CardKey> queryCardKeys(@Param("status") Integer status, @Param("amount") java.math.BigDecimal amount);
}

