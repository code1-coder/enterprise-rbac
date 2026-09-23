package org.example.rbac.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * 登录响应里的用户摘要，嵌在 LoginVO.userInfo。角色和权限与 CurrentUserVO 同源，但不返回实体。
 */
@Data
public class LoginUserVO {

    private Long id;
    private String username;
    private String nickname;
    private List<String> roles;
    private List<String> permissions;
}
