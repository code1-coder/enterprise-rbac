> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。版本以 [pom.xml](../pom.xml) 为准。

# 企业权限角色分配系统 - 依赖配置说明

> 进度（2026-09-25）：后端接口和 Vue 3 管理端已有代码；前端类型检查及完整联调尚未通过验证。详见 [项目总览](../../README.md)。

## 📋 版本总览

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| Java | 17 | JDK版本 |
| MyBatis-Plus | 3.5.7 | 持久层框架 |
| MySQL Driver | 8.x | 数据库驱动 |
| Druid | 1.2.23 | 数据库连接池 |
| Spring Security | 6.2.x | 由 Spring Boot 3.2.5 管理，不要单独写版本 |
| JWT (jjwt) | 0.12.6 | Token认证 |
| Redis | 本机 6.x 或 7.x | 缓存，不是 Maven 依赖版本 |
| Lombok | 自动管理 | 代码简化工具 |
| Hutool | 5.8.29 | 工具类库 |
| Knife4j | 4.5.0 | API文档 |
| Fastjson2 | 2.0.52 | JSON处理 |

Spring Boot 3 要使用 Jakarta 版 starter：`mybatis-plus-spring-boot3-starter` 和 `druid-spring-boot-3-starter`。不要用 Boot 2 的 `mybatis-plus-boot-starter`、`druid-spring-boot-starter`。

## 🎯 依赖分类详解

### 1️⃣ Web层依赖

#### spring-boot-starter-web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**作用**：
- 提供 Spring MVC 框架
- 内置 Tomcat 服务器
- 支持 RESTful API 开发
- 自动配置 JSON 序列化

---

### 2️⃣ 安全认证依赖

#### Spring Security
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
**作用**：
- 实现用户认证和授权
- 提供密码加密（BCrypt）
- 支持权限注解 `@PreAuthorize`
- 当前 `SecurityConfig` 关闭了 CSRF 和表单登录，因为设计是无 Session 的 JWT。只加这个依赖不会自动消除 XSS

#### JWT (jjwt-api, jjwt-impl, jjwt-jackson)
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```
**作用**：
- 生成和验证 JWT Token
- 无状态认证方案
- 支持签名和加密

**为什么需要三个包？**
- `jjwt-api`：接口定义
- `jjwt-impl`：具体实现
- `jjwt-jackson`：JSON解析支持

---

### 3️⃣ 数据库依赖

#### MyBatis-Plus
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```
**作用**：
- 增强版 MyBatis
- 通用 CRUD 方法
- 支持分页查询
- 逻辑删除支持。代码生成器是另一个构件 `mybatis-plus-generator`，本项目没引入

**为什么不用原生 MyBatis？**
- MyBatis-Plus 功能更强大
- 减少重复的 CRUD 代码
- 提供更多开箱即用功能

#### MySQL Connector
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```
**作用**：
- 连接 MySQL 数据库的驱动
- Spring Boot 会自动管理版本

#### Druid
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
    <version>1.2.23</version>
</dependency>
```
**作用**：
- 数据库连接池管理
- 提供监控页面 `/druid/`
- SQL 性能分析。SQL 防火墙 WallFilter 当前没有在 `application.yml` 里开启

**为什么选 Druid？**
- 阿里巴巴开源，性能强大
- 内置监控功能
- 适合企业级应用

---

### 4️⃣ 缓存依赖

#### Spring Data Redis
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
**作用**：
- 缓存用户权限信息
- 加速接口响应速度
- 存储 Token 黑名单
- 分布式会话管理

**使用场景**：
- 缓存用户的角色和权限
- Token 验证时避免频繁查库
- 实现单点登录（SSO）

---

### 5️⃣ 工具库依赖

#### Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
**作用**：
- 自动生成 getter/setter
- `@Data` 注解简化实体类
- `@Slf4j` 日志注解
- 减少样板代码

**常用注解**：
- `@Data`：生成 getter、setter、toString 等
- `@NoArgsConstructor`：无参构造
- `@AllArgsConstructor`：全参构造
- `@Builder`：建造者模式

#### Hutool
```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.29</version>
</dependency>
```
**作用**：
- 丰富的工具类库
- 字符串、日期、加密、HTTP 等工具
- 简化常见开发任务

**常用工具**：
- `StrUtil`：字符串工具
- `DateUtil`：日期工具
- `SecureUtil`：加密工具
- `HttpUtil`：HTTP 请求工具

#### Fastjson2
```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.52</version>
</dependency>
```
**作用**：
- JSON 序列化/反序列化
- 性能优秀
- 阿里巴巴维护

---

### 6️⃣ API文档依赖

#### Knife4j
```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.5.0</version>
</dependency>
```
**作用**：
- 增强版 Swagger UI
- 自动生成接口文档
- 在线测试接口
- 访问地址：http://localhost:8080/doc.html

**界面特点**：
- 美观的中文界面
- 支持分组、搜索
- 离线文档导出

---

### 7️⃣ 校验和AOP依赖

#### Validation
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
**作用**：
- 参数校验（JSR-303/JSR-380）
- `@NotNull`、`@NotBlank` 等注解
- 统一校验规则

#### Spring AOP
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```
**作用**：
- 面向切面编程
- 实现操作日志记录
- 自定义权限注解
- 统一异常处理

---

### 8️⃣ 开发工具依赖

#### DevTools
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```
**作用**：
- 热部署（修改代码自动重启）
- 自动刷新浏览器
- 提升开发效率

**注意**：
- 仅开发环境使用
- 生产环境不会打包

---

### 9️⃣ 测试依赖

#### spring-boot-starter-test
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```
**包含**：
- JUnit 5
- Mockito
- AssertJ

#### spring-security-test
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```
**作用**：
- 测试 Spring Security 相关功能
- 模拟认证用户

---

## 🚀 快速验证配置

### 1. 刷新Maven项目
在 IntelliJ IDEA 中：
- 右键点击 `pom.xml`
- 选择 `Maven` → `Reload Project`
- 或点击右侧 Maven 面板的刷新按钮

### 2. 查看依赖树
```bash
mvn dependency:tree
```

### 3. 下载所有依赖
```bash
mvn clean install
```

---

## ⚠️ 注意事项

### 1. Java 版本要求
- 项目使用 Java 17
- 确保 IDEA 配置的 JDK 版本正确
- `File` → `Project Structure` → `Project SDK`

### 2. Maven 配置
- 已配置阿里云镜像，加速下载
- 如果还是很慢，检查本地 Maven settings.xml

### 3. 版本兼容性
- Spring Boot 3.x 要求 Java 17+
- Spring Boot 3.x 使用 Jakarta 命名空间（不是 Javax）
- MyBatis-Plus 3.5.7 完全兼容 Spring Boot 3.2.5

### 4. 生产环境优化建议
```yaml
# 生产环境配置建议
jwt:
  secret: 使用强密钥（至少32位随机字符）
  
spring:
  devtools:
    enabled: false  # 关闭热部署
    
mybatis-plus:
  configuration:
    log-impl: null  # 关闭SQL日志
    
logging:
  level:
    root: warn  # 只记录警告和错误
```

---

## 📚 学习资源

- **Spring Boot 官方文档**: https://spring.io/projects/spring-boot
- **MyBatis-Plus 文档**: https://baomidou.com/
- **Knife4j 文档**: https://doc.xiaominfo.com/
- **JWT 官网**: https://jwt.io/
- **Hutool 文档**: https://hutool.cn/

---

**配置完成时间**：2026-09-22  
**下一步**：执行 [schema.sql](../src/main/resources/db/schema.sql)，步骤见 [database-init.md](database-init.md)
