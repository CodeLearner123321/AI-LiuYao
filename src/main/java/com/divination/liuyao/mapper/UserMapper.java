package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.divination.liuyao.pojo.entity.User;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 用户数据访问层接口
 * 继承BaseMapper获得通用的CRUD方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUserName(String userName);
    
    /**
     * 根据ID查询用户
     */
    Optional<User> findById(Long id);
    
    /**
     * 预扣费操作，使用乐观锁
     * 
     * @param userId 用户ID
     * @param amount 扣费金额
     * @return 影响的行数，1表示成功，0表示失败（余额不足或并发更新）
     */
    int preDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    /**
     * 实际扣费操作，将冻结余额清零
     */
    int confirmDeduct(@Param("userId") Long userId,  @Param("frozenBalance") BigDecimal frozenBalance,
                      @Param("balance") BigDecimal balance);
    
    /**
     * 退款操作，取消预扣费
     * 
     * @param userId 用户ID
     * @param amount 退款金额
     * @return 影响的行数
     */
    int refund(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    /**
     * 充值操作，增加用户余额
     * 
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 影响的行数
     */
    int recharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int update(User user);
} 