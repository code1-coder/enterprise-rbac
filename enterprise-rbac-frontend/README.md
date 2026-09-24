# enterprise-rbac-frontend

企业权限角色分配系统的管理端前端目录。目前只完成架构和联调设计，前端工程尚未初始化；本阶段不包含页面、组件或运行时代码。

## 设计文档

- [前端架构与技术选型](docs/architecture.md)：技术栈、分层、路由、权限与页面逻辑。
- [后端接口联调文档](docs/api-integration.md)：认证流程、统一响应、错误处理和完整接口矩阵。

## 后端

后端启动、数据库初始化及开发账号见 [后端 README](../enterprise-rbac-backend/README.md)。联调接口的详细设计见后端 [API 设计](../enterprise-rbac-backend/docs/api-design.md) 与 [API 速查](../enterprise-rbac-backend/docs/api-reference.md)；字段和权限标识以当前后端实现及 [schema.sql](../enterprise-rbac-backend/src/main/resources/db/schema.sql) 为准。
