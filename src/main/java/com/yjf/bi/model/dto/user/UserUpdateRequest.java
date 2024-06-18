package com.yjf.bi.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求


 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;
    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    private String userPassword;

//    /**
//     * 简介
//     */
//    private String userProfile;

    /**
     * 剩余使用次数
     */
    private Integer leftNum;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}