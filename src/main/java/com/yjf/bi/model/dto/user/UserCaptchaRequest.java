package com.yjf.bi.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户获取验证码请求
 */
@Data
public class UserCaptchaRequest implements Serializable {
    private String email;

    private static final long serialVersionUID =1L;
}
