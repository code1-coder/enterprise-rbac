package org.example.rbac.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * GET /api/auth/user-info 的出参。roles 来自 sys_role，permissions 来自 sys_menu.permission，不含密码。
 */
@Data
public class CurrentUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private List<String> roles;
    private List<String> roleNames;
    private List<String> permissions;
}
