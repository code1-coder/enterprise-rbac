
package org.example.rbac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 应用入口，并扫描 org.example.rbac.mapper，把 Mapper 注册为 Spring Bean。
 * 调用链：JwtAuthenticationFilter 还原登录态，Controller 校验并鉴权，Service 编排用例，Mapper 访问表。
 * 权限数据沿 sys_user、sys_user_role、sys_role、sys_role_menu、sys_menu 关联；
 * hasAuthority 比对的是 sys_menu.permission，不是角色编码。
 * 排除内存用户自动配置，避免启动时生成随机密码；登录改为查询 sys_user。
 * sys_operation_log、sys_login_log 目前只有表，没有对应 Mapper。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("org.example.rbac.mapper")
public class RbacApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacApplication.class, args);
    }
}
