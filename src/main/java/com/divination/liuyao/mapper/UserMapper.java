package com.divination.liuyao.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.divination.liuyao.pojo.entity.User;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUserName(String userName);
    
    Optional<User> findById(Long id);
    
    int insert(User user);
    
    int update(User user);
    
    int deleteById(Long id);
    
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
     * 
     * @param userId 用户ID
     * @return 影响的行数
     */
    int confirmDeduct(@Param("userId") Long userId);
    
    /**
     * 退款操作，取消预扣费
     * 
     * @param userId 用户ID
     * @param amount 退款金额
     * @return 影响的行数
     */
    int refund(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
} 