# MyBatis-Flex GitHub Actions 工作流

本目录包含了 MyBatis-Flex 项目的自动化 CI/CD 工作流配置，专门针对 PostgreSQL 数据库集成测试。

## 工作流文件说明

### 1. `postgresql-tests.yml` - 完整 PostgreSQL 测试套件
**触发条件:** 
- 推送到 `main`/`develop` 分支
- 针对 `main`/`develop` 分支的 Pull Request
- 相关文件变更时

**测试矩阵:**
- Java 版本: 17, 21
- PostgreSQL 版本: 12, 13, 14, 15, 16

**测试内容:**
- 基础 CRUD 操作测试
- JOIN 查询测试
- PostgreSQL 特有功能测试
- 分页查询优化测试
- 性能基准测试

### 2. `postgresql-quick-test.yml` - 快速 PostgreSQL 测试
**触发条件:**
- 所有分支推送
- 所有 Pull Request

**测试环境:**
- Java 17
- PostgreSQL 15 (Alpine 版本，启动更快)

**用途:**
- 快速验证代码变更
- 提供即时反馈
- 减少 CI 资源消耗

### 3. `postgresql-features.yml` - PostgreSQL 特性验证
**触发条件:**
- 手动触发 (workflow_dispatch)

**可选参数:**
- 测试类型: `all`, `basic-crud`, `join-queries`, `special-features`, `count-optimization`
- PostgreSQL 版本: 12, 13, 14, 15, 16

**用途:**
- 针对性功能测试
- 特定版本兼容性验证
- 问题调试和分析

## 测试覆盖范围

### PostgreSQL 特有功能测试
- ✅ SERIAL 主键自增
- ✅ 双引号关键字转义 (`"order"`, `"user"`)
- ✅ LIMIT/OFFSET 分页语法
- ✅ 外键约束和级联删除
- ✅ 时间戳默认值处理
- ✅ JSON 数据类型支持
- ✅ PostgreSQL 特有函数
- ✅ 事务管理和回滚

### 查询功能测试
- ✅ 基础 CRUD 操作
- ✅ 复杂条件查询
- ✅ INNER/LEFT/RIGHT JOIN
- ✅ GROUP BY + HAVING
- ✅ 分页查询
- ✅ COUNT 查询优化
- ✅ DISTINCT 查询
- ✅ 特殊字符处理

## 测试数据库配置

每个工作流都会自动创建和配置 PostgreSQL 测试环境：

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: mybatis_flex_test
    ports:
      - 5432:5432
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

## 测试报告

所有工作流都会生成详细的测试报告：

1. **控制台输出**: 实时测试执行日志
2. **Surefire 报告**: JUnit XML 格式的测试结果
3. **构件上传**: 测试报告文件上传到 GitHub Actions
4. **测试统计**: 成功率、失败详情、执行时间

## 使用建议

### 开发流程
1. **日常开发**: `postgresql-quick-test.yml` 提供快速反馈
2. **功能完成**: `postgresql-tests.yml` 进行全面测试
3. **问题调试**: `postgresql-features.yml` 针对性测试

### 手动触发特性测试
```bash
# 在 GitHub Actions 页面选择 "PostgreSQL 特性验证"
# 选择测试类型和 PostgreSQL 版本
# 点击 "Run workflow"
```

### 本地测试环境
```bash
# 启动 PostgreSQL 容器
docker run --name postgres-test -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:15

# 初始化数据库
psql -h localhost -U postgres -c "CREATE DATABASE mybatis_flex_test;"
psql -h localhost -U postgres -d mybatis_flex_test -f src/main/resources/schema-postgresql.sql
psql -h localhost -U postgres -d mybatis_flex_test -f src/main/resources/data-postgresql.sql

# 运行测试
mvn test -pl mybatis-flex-test/mybatis-flex-postgresql-test
```

## 故障排除

### 常见问题
1. **PostgreSQL 连接失败**: 检查服务健康检查配置
2. **数据库初始化失败**: 验证 SQL 脚本语法
3. **测试超时**: 调整健康检查间隔和重试次数
4. **依赖缓存问题**: 清除 Maven 缓存重新构建

### 调试步骤
1. 查看工作流日志
2. 检查测试报告
3. 使用 `postgresql-features.yml` 针对性测试
4. 本地复现问题环境
