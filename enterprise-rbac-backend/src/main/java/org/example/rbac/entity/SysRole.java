package org.example.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表 sys_role。用户经 sys_user_role 绑定本表，菜单经 sys_role_menu 绑定。
 * role_code 是角色标识；接口鉴权看的是菜单 permission，不是这里的编码。
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleCode;
    private Integer status;
    private Integer sort;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}
