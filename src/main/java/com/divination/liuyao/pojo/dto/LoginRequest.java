package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.util.ConstantUtil;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String deviceFingerprint = ConstantUtil.DEFAULT_DEVICE_FINGERPRINT;
}