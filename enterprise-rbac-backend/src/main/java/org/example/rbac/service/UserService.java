package org.example.rbac.service;

import org.example.rbac.common.result.PageResult;
import org.example.rbac.dto.user.PasswordUpdateDTO;
import org.example.rbac.dto.user.ResetPasswordDTO;
import org.example.rbac.dto.user.UserCreateDTO;
import org.example.rbac.dto.user.UserQueryDTO;
import org.example.rbac.dto.user.UserUpdateDTO;
import org.example.rbac.vo.user.UserVO;

import java.util.List;

/**
 * 用户主数据和角色分配。账号在 sys_user，角色关系在 sys_user_role。
 * 逻辑删除只改 deleted，不会级联清理关联，删除用户时要同时处理这两张表。
 */
public interface UserService {

    PageResult<UserVO> pageList(UserQueryDTO query);

    UserVO getUserById(Long id);

    void createUser(UserCreateDTO dto);

    void updateUser(Long id, UserUpdateDTO dto);

    void deleteUser(Long id);

    void batchDeleteUsers(List<Long> ids);

    void changePassword(Long id, PasswordUpdateDTO dto);

    void resetPassword(Long id, ResetPasswordDTO dto);

    void assignRoles(Long id, List<Long> roleIds);
}
