package com.divination.liuyao.pojo.entity;

import java.math.BigDecimal;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String userName;  // 账号
    private String passWord;
    private String phoneNumber;
    private String email;      // 邮箱
    private String salt;
    //是否是vip用户 0：否 ，1：是
    private Integer isVip;
    //余额 默认余额10  1元 = 10点
    private BigDecimal balance;

    //欲扣款余额，其实是 balance余额的另一种状态，当在执行订单支付的过程中balance会减去费用，而frozenBalance会增加对应的费用（中间状态）
    //当支付流程完成后frozenBalance会继续置为0，而balance则会根据执行的成功与失败扣减或者恢复对应的余额
    private BigDecimal frozenBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    /**
     * 返回加密的手机号
     * @return
     */
    public String getEncryptedPhoneNumber(){
        if( this.phoneNumber == null || this.phoneNumber.isEmpty()){
            return "------------";
        }
        return phoneNumber.substring(0,3) + "****" + phoneNumber.substring(7,10);
    }
}