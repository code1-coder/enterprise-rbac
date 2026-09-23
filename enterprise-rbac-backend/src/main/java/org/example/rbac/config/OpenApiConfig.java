package org.example.rbac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 告诉 Knife4j 受保护接口使用 HTTP Bearer。
 * 调试时点 Authorize，只填写登录返回的 LoginVO.token，不要自己再写 Bearer。
 * 文档会把它写成请求头 Authorization: Bearer <token>，JwtAuthenticationFilter 解析后写入 SecurityContext。
 * 登录和注册在 AuthController 上用空的 SecurityRequirements 标明公开，不套用这里的全局要求。
 */
@Configuration
public class OpenApiConfig {

    /** 与 SecurityScheme 名称一致，Knife4j 的 Authorize 按这个名字持久化令牌。 */
    public static final String SECURITY_SCHEME = "Authorization";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("只填登录接口 data.token，不要包含 Bearer 前缀");
        return new OpenAPI()
                .info(new Info()
                        .title("企业权限角色分配系统接口文档")
                        .description("RBAC权限管理系统API")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, bearer));
    }
}
