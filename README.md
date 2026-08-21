# OpsFlow · 智能工单与自动化运维平台

面向中大型企业内网（50–500 人团队、并发峰值 ≤200）的一体化运维自动化平台，覆盖 **申请 → 审批 → 自动化执行 → 可追溯** 全链路，解决"人的审批流程"与"机器的自动执行"脱节问题。

模块化单体架构（Modular Monolith），单 Spring Boot 应用按业务域划分模块，部署简单、扩展灵活。

## 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 后端 | Java / Spring Boot / Spring Security / MyBatis-Plus | 17 / 3.3.5 / 6.x / 3.5.7 |
| 工作流 | 自研线性审批引擎（支持条件分支、会签、或签、退回） | — |
| 中间件 | MySQL / Redis / RabbitMQ / MinIO | 8.0+ / 7.0+ / 3.12+ / 8.5+ |
| 前端 | Vue 3 / TypeScript / Vite / Element Plus / Pinia / ECharts | 3.5 / 5.x / 5 / 2.8 / 2.x / 5.5 |
| CI/CD | Gitea Actions（纯 Shell + Host 执行器，push main 自动部署） | 1.22 / v3.3.0 |

## 功能模块

已交付 M0–M8 共 9 个里程碑，覆盖 7 大业务域：

| 模块 | 说明 |
|---|---|
| **M1 权限中心** | 用户、角色、菜单、部门、岗位管理；RBAC 权限模型；JWT 认证；登录日志与操作审计 |
| **M2 工单管理** | 工单分类、模板、提交、处理、转派、关闭；SLA 计时；工单关联资产 |
| **M3 工作流引擎** | 可视化流程设计；线性审批引擎；条件分支、会签/或签、退回、加签；流程实例追踪 |
| **M4 自动化执行** | 剧本（Playbook）编排；SSH/WinRM 远程执行；实时日志（WebSocket）；定时任务 |
| **M5 CMDB 资产管理** | 主机、数据库、中间件、应用资源台账；资产分类与标签；关联工单与告警 |
| **M6 告警监控** | 告警规则配置；多渠道通知（邮件/钉钉/企业微信）；告警收敛与升级；告警工单联动 |
| **M7 知识库** | 知识分类与全文检索；Markdown 编辑器；RAG 智能问答（对接 LLM）；知识审核 |
| **M8 数据报表** | 工单统计、自动化执行统计、资产报表；ECharts 可视化；导出 Excel |

## 工程规范

- **统一响应**：`R<T>` 成功/失败包装 + `PageResult<T>` 分页结构
- **错误码**：集中 `ErrorCode` 枚举，按业务域分段（400xx 参数 / 401xx 认证 / 403xx 权限 / 500xx 系统）
- **审计日志**：`@AuditLog` 注解 + AOP 切面，敏感字段 JSON 级脱敏（password → `***`）
- **数据安全**：AES-256 加密存储主机凭据（密码、私钥）；逻辑删除 + 唯一索引兼容处理
- **数据库**：utf8mb4 / utf8mb4_unicode_ci / InnoDB；统一审计字段（create_by、create_time、update_by、update_time、deleted、remark）
- **异步解耦**：RabbitMQ 投递任务，事务提交后触发消费；WebSocket 实时推送日志
- **代码风格**：MapStruct 实体转换，Hutool 工具类，Knife4j 接口文档

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+、Redis 7.0+、RabbitMQ 3.12+、MinIO

### 后端启动

```bash
cd backend

# 1. 创建数据库并导入 schema
mysql -u root -p < sql/opsflow_schema.sql
mysql -u root -p opsflow < sql/opsflow_data.sql

# 2. 修改 application-dev.yml 中的数据库/Redis/RabbitMQ/MinIO 连接配置
#    或通过环境变量注入（推荐）

# 3. 启动
mvn spring-boot:run
```

启动后访问接口文档：http://localhost:8080/doc.html

默认管理员账号：`admin` / `admin123`

> ⚠️ **生产部署第一件事就是修改默认管理员密码！**

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

启动后访问：http://localhost:5173

## 项目结构

```
opsflow/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/opsflow/
│       ├── common/                   # 公共层（响应、异常、工具、配置）
│       ├── module/auth/              # M1 权限中心
│       ├── module/ticket/            # M2 工单管理
│       ├── module/workflow/          # M3 工作流引擎
│       ├── module/automation/        # M4 自动化执行
│       ├── module/cmdb/              # M5 CMDB 资产
│       ├── module/alert/             # M6 告警监控
│       ├── module/knowledge/         # M7 知识库 + RAG
│       ├── module/report/            # M8 数据报表
│       └── module/system/            # 系统管理
├── frontend/                         # Vue 3 前端
│   ├── src/api/                      # 接口封装
│   ├── src/views/                    # 页面视图
│   ├── src/store/                    # Pinia 状态管理
│   ├── src/router/                   # 路由配置
│   └── src/layout/                   # 布局组件
├── .gitea/workflows/deploy.yml       # CI/CD 流水线
└── .gitignore
```

## CI/CD

基于 Gitea Actions 的自动化流水线，push 到 `main` 分支自动触发：

1. 拉取最新代码
2. Maven 构建后端（jar 包）
3. Vite 构建前端（dist）
4. 部署后端 + 重启 systemd 服务
5. 部署前端 + reload Nginx
6. 登录接口健康检查

## 安全说明

- **凭据注入**：所有生产环境凭据通过环境变量注入，不硬编码在代码中
  - `OPSFLOW_AES_KEY`：AES-256 主密钥（GCM 模式），**生产必须设置强随机密钥**
  - `OPSFLOW_AES_LEGACY_KEY`：可选，兼容历史 ECB 密文的旧密钥，迁移完成后建议移除
  - `OPSFLOW_JWT_SECRET`：JWT 签名密钥
  - `DB_PASSWORD` / `RABBITMQ_PASS` / `REDIS_PASSWORD` / `MINIO_SECRET_KEY`：中间件凭据
- **默认密码**：配置文件和初始数据中的默认值（`admin/admin123`、`opsflow@123` 等）仅为开发环境占位，**生产部署必须全部替换**
- **数据库**：MySQL / Redis / RabbitMQ / MinIO 端口建议绑定 `127.0.0.1`，仅通过 Nginx 反向代理对外暴露 80/443
- **备份**：建议定期备份 MySQL，启用磁盘快照
- **审计**：操作日志自动脱敏敏感字段（password 等），但数据库层仍以密文存储主机凭据

## License

MIT License
