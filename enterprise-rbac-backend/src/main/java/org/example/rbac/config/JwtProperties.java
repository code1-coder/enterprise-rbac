
package org.example.rbac.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定 application.yml 的 jwt 配置，供 JwtTokenProvider、RedisAuthStore 和 JwtAuthenticationFilter 使用。
 * secret 不写进代码；expiration 是毫秒，登录响应里的 expiresIn 要再换成秒。
 * header、prefix 决定过滤器从哪个请求头取出 Bearer 令牌。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 密钥，至少 32 字节。 */
    private String secret;

    /** 令牌有效期，单位毫秒。 */
    private long expiration;

    /** 携带令牌的请求头，默认 Authorization。 */
    private String header = "Authorization";

    /** 令牌前缀，默认 Bearer，后面还有一个空格。 */
    private String prefix = "Bearer";
}
