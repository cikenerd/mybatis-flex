# MyBatis-Flex PostgreSQL 集成测试模块

这是 MyBatis-Flex 项目的 PostgreSQL 数据库集成测试模块，提供完整的 PostgreSQL 特性支持测试。

## 功能特性

### 支持的 PostgreSQL 特性
- ✅ SERIAL 主键自增支持
- ✅ 双引号关键字转义处理（`"order"`, `"user"`）  
- ✅ LIMIT/OFFSET 分页语法
- ✅ 外键约束和级联删除
- ✅ 时间戳默认值处理
- ✅ JSON 数据类型支持
- ✅ PostgreSQL 特有函数
- ✅ 事务管理和回滚

### 测试覆盖范围
- **基础 CRUD 操作**: 增删改查功能测试
- **复杂条件查询**: WHERE、AND、OR 等条件组合
- **连表查询**: INNER JOIN、LEFT JOIN、RIGHT JOIN
- **分组统计**: GROUP BY + HAVING
- **分页查询**: Page 对象分页功能
- **COUNT 查询优化**: `Page.setOptimizeCountQuery()` 功能测试
- **DISTINCT 查询**: 去重查询及其优化
- **特殊字符处理**: 关键字和特殊字符转义

## 项目结构

```
mybatis-flex-postgresql-test/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mybatisflex/test/postgresql/
│   │   │       ├── PostgreSQLTestApplication.java     # Spring Boot 启动类
│   │   │       ├── model/                            # 实体类
│   │   │       │   ├── Account.java                  # 账户实体
│   │   │       │   └── Article.java                  # 文章实体
│   │   │       └── mapper/                           # Mapper 接口
│   │   │           ├── AccountMapper.java
│   │   │           └── ArticleMapper.java
│   │   └── resources/
│   │       ├── application.yml                       # Spring Boot 配置
│   │       ├── schema-postgresql.sql                 # 数据库表结构
│   │       └── data-postgresql.sql                   # 测试数据
│   └── test/
│       └── java/
│           └── com/mybatisflex/test/postgresql/
│               ├── PostgreSQLBasicCrudTest.java      # 基础 CRUD 测试
│               ├── PostgreSQLJoinTest.java           # 连表查询测试
│               └── PostgreSQLSpecialFeaturesTest.java # 特有功能测试
├── pom.xml                                           # Maven 配置
└── README.md                                         # 本文档
```

## 环境配置

### 测试环境配置
项目使用 Maven Profile 来管理不同环境的测试执行：

#### 1. 本地开发环境（默认）
```bash
# 默认跳过集成测试，避免本地环境依赖 PostgreSQL
mvn test
# 输出: Tests are skipped.
```

#### 2. 手动集成测试
```bash
# 运行完整集成测试（需要本地 PostgreSQL 环境）
mvn test -Pintegration
# 运行所有 31 个测试用例
```

#### 3. CI 环境
```bash
# GitHub Actions 自动激活 CI Profile（设置 CI=true 环境变量）
mvn test -Pci
# 或者通过环境变量自动激活
export CI=true && mvn test
```

### 本地 PostgreSQL 环境搭建

#### 使用 Docker（推荐）
```bash
# 启动 PostgreSQL 容器
docker run --name mybatis-flex-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=mybatis_flex_test \
  -p 5432:5432 \
  -d postgres:15

# 初始化数据库
docker exec -i mybatis-flex-postgres psql -U postgres -d mybatis_flex_test < src/main/resources/schema-postgresql.sql
docker exec -i mybatis-flex-postgres psql -U postgres -d mybatis_flex_test < src/main/resources/data-postgresql.sql
```

#### 使用本地安装
```bash
# 创建测试数据库
createdb -U postgres mybatis_flex_test

# 初始化数据库结构和数据
psql -U postgres -d mybatis_flex_test -f src/main/resources/schema-postgresql.sql
psql -U postgres -d mybatis_flex_test -f src/main/resources/data-postgresql.sql
```

## 测试执行

### 运行所有测试
```bash
# 需要先启动 PostgreSQL 服务
mvn test -Pintegration -pl mybatis-flex-test/mybatis-flex-postgresql-test
```

### 运行特定测试类
```bash
# 基础 CRUD 测试
mvn test -Pintegration -Dtest="PostgreSQLBasicCrudTest" -pl mybatis-flex-test/mybatis-flex-postgresql-test

# 连表查询测试  
mvn test -Pintegration -Dtest="PostgreSQLJoinTest" -pl mybatis-flex-test/mybatis-flex-postgresql-test

# 特有功能测试
mvn test -Pintegration -Dtest="PostgreSQLSpecialFeaturesTest" -pl mybatis-flex-test/mybatis-flex-postgresql-test
```

### 运行特定测试方法
```bash
# COUNT 查询优化测试
mvn test -Pintegration -Dtest="PostgreSQLBasicCrudTest#testOptimizeCountQuery" -pl mybatis-flex-test/mybatis-flex-postgresql-test
```

## 数据库配置

### 连接信息
- **数据库**: `mybatis_flex_test`
- **用户名**: `postgres`
- **密码**: `postgres`
- **端口**: `5432`
- **JDBC URL**: `jdbc:postgresql://localhost:5432/mybatis_flex_test`

### 数据表结构

#### tb_account（账户表）
```sql
CREATE TABLE tb_account (
    id SERIAL PRIMARY KEY,              -- 自增主键
    user_name VARCHAR(100),             -- 用户名
    age INTEGER,                        -- 年龄
    sex INTEGER,                        -- 性别（1-男，2-女）
    birthday DATE,                      -- 生日
    is_normal INTEGER DEFAULT 1,        -- 正常状态
    is_delete INTEGER DEFAULT 0,        -- 删除标识
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);
```

#### tb_article（文章表）
```sql
CREATE TABLE tb_article (
    id SERIAL PRIMARY KEY,              -- 自增主键
    account_id INTEGER,                 -- 作者ID
    title VARCHAR(200),                 -- 标题
    content TEXT,                       -- 内容
    is_delete INTEGER DEFAULT 0,        -- 删除标识
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    FOREIGN KEY (account_id) REFERENCES tb_account(id)  -- 外键约束
);
```

## GitHub Actions 集成

项目配置了完善的 GitHub Actions 工作流，支持：

### 1. 完整测试套件
- **文件**: `.github/workflows/postgresql-tests.yml`
- **Java 版本**: 17, 21
- **PostgreSQL 版本**: 12, 13, 14, 15, 16
- **触发条件**: 推送到 main/develop 分支，相关 Pull Request

### 2. 快速测试
- **文件**: `.github/workflows/postgresql-quick-test.yml`  
- **环境**: Java 17 + PostgreSQL 15
- **触发条件**: 所有分支推送和 Pull Request

### 3. 特性验证
- **文件**: `.github/workflows/postgresql-features.yml`
- **触发方式**: 手动触发
- **可选参数**: 测试类型、PostgreSQL 版本

## 测试用例说明

### PostgreSQLBasicCrudTest（13 个测试）
- **INSERT 测试**: 单条和批量插入
- **SELECT 测试**: 条件查询、分页查询
- **UPDATE 测试**: 条件更新
- **DELETE 测试**: 软删除和物理删除
- **COUNT 优化测试**: 包含 DISTINCT 场景的查询优化
- **批量操作测试**: 批量插入和更新验证

### PostgreSQLJoinTest（12 个测试）
- **INNER JOIN**: 内连接查询
- **LEFT JOIN**: 左外连接查询
- **RIGHT JOIN**: 右外连接查询
- **复合条件**: JOIN + WHERE + ORDER BY
- **多表关联**: 复杂的多表联合查询

### PostgreSQLSpecialFeaturesTest（10 个测试）
- **关键字转义**: 双引号处理
- **SERIAL 主键**: 自增ID测试
- **JSON 支持**: JSON 数据类型操作
- **外键约束**: 级联删除测试
- **PostgreSQL 特有函数**: 数据库特性验证

### PostgreSQLPerformanceTest（性能测试）
- **大数据量测试**: 可配置规模的性能基准测试
- **分页性能**: 大数据集分页查询优化
- **JOIN 性能**: 复杂关联查询性能验证
- **COUNT 优化**: 分页计数查询性能对比

## 常见问题

### 1. 本地测试被跳过
**现象**: 运行 `mvn test` 显示 "Tests are skipped"

**解决**: 这是正常行为，本地默认跳过集成测试。使用 `mvn test -Pintegration` 运行集成测试。

### 2. PostgreSQL 连接失败
**现象**: 测试报错 "Connection refused" 或超时

**解决**: 
1. 确认 PostgreSQL 服务已启动
2. 检查连接配置（用户名、密码、端口）
3. 验证防火墙设置

### 3. 数据库初始化失败
**现象**: 测试开始前数据库初始化报错

**解决**:
1. 检查 `schema-postgresql.sql` 语法
2. 确认数据库用户权限
3. 验证数据库已创建

### 4. 测试数据问题
**现象**: 测试结果不符合预期

**解决**:
1. 检查 `data-postgresql.sql` 测试数据
2. 确认 `@Transactional` 注解正确回滚
3. 验证测试隔离性

### 5. Maven Profile 配置
**现象**: 测试环境切换不生效

**解决**:
1. 确认 Maven profile 激活条件（环境变量 CI=true）
2. 检查 `pom.xml` 中的 skipTests 属性配置
3. 使用 `-P` 参数明确指定 profile

## 性能考虑

### COUNT 查询优化
项目特别测试了 MyBatis-Flex 的 COUNT 查询优化功能：

```java
// 启用 COUNT 查询优化
Page<Account> page = new Page<>(1, 10);
page.setOptimizeCountQuery(true);

// MyBatis-Flex 会自动优化 COUNT 查询
Page<Account> result = accountMapper.paginate(page, query);
```

### DISTINCT 优化
支持 DISTINCT 场景的 COUNT 优化：

```java
QueryWrapper distinctQuery = QueryWrapper.create()
    .select(distinct(ACCOUNT.AGE), ACCOUNT.SEX)
    .where(ACCOUNT.IS_DELETE.eq(0))
    .orderBy(ACCOUNT.AGE.asc());
    
page.setOptimizeCountQuery(true);
Page<Record> result = accountMapper.paginateAs(page, distinctQuery, Record.class);
```

### 性能测试配置
项目包含可配置规模的性能测试：

```bash
# 启用性能测试
mvn test -Pintegration -Dperformance.test=true

# 配置测试规模
-Dperf.scale=small   # 1000账户, 3000文章
-Dperf.scale=medium  # 10000账户, 50000文章
-Dperf.scale=large   # 50000账户, 200000文章
```

## 贡献指南

1. **添加新测试**: 遵循现有测试结构和命名规范
2. **数据库变更**: 同时更新 `schema-postgresql.sql`
3. **文档更新**: 保持 README 和代码注释同步  
4. **版本兼容**: 考虑多版本 PostgreSQL 兼容性

## 作者
kk

---
更多信息请参考 [MyBatis-Flex 官方文档](https://mybatis-flex.com)