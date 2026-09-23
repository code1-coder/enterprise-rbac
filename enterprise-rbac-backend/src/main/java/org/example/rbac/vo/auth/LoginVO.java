package org.example.rbac.vo.auth;

import lombok.Data;

/**
 * 登录和刷新令牌的出参。token 交给前端，userInfo 使用 LoginUserVO，而不是 SysUser。
 */
@Data
public class LoginVO {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private LoginUserVO userInfo;
}
