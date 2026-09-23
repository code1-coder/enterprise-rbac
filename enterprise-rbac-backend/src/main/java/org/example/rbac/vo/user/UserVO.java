package org.example.rbac.vo.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户出参，对应 sys_user，并带上由 sys_user_role 解析出的 roleIds、roles、roleNames；不含 password。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private List<Long> roleIds;
    private List<String> roles;
    private List<String> roleNames;
    private LocalDateTime createTime;
    private String remark;
}
