package com.atguigu.gulimall.cart.to;

import lombok.Data;

@Data
public class UserInfoTo {
    private Long userId; //登录用户
    private String userKey;//临时用户
    /**
     * 是否临时用户
     */
    private Boolean tempUser = false;
}
