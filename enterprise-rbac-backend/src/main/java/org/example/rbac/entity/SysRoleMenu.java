package org.example.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-菜单关联 sys_role_menu。给角色分配权限时写入的是 menu_id，没有单独的权限表。
 * 当前用户的菜单和按钮权限，是其角色在本表中的菜单，再取 sys_menu.permission。
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long menuId;
    private LocalDateTime createTime;
}
