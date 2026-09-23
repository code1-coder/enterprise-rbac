
package org.example.rbac.service;

import java.util.List;

/**
 * 用户当前生效的角色和权限，供登录响应、当前用户和令牌签发使用。
 * roleCodes 是 sys_role.role_code，roleNames 是 sys_role.role_name，permissions 是 sys_menu.permission。
 * 三者都只包含未删除且 status=1 的角色，以及这些角色被分配的菜单权限。
 */
public record UserAccess(List<String> roleCodes, List<String> roleNames, List<String> permissions) {
}
