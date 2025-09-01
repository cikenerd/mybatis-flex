/*
 *  Copyright (c) 2022-2025, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mybatisflex.test.postgresql;

import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.RawQueryColumn;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.test.postgresql.mapper.AccountMapper;
import com.mybatisflex.test.postgresql.mapper.ArticleMapper;
import com.mybatisflex.test.postgresql.model.Account;
import com.mybatisflex.test.postgresql.model.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static com.mybatisflex.test.postgresql.model.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.test.postgresql.model.table.ArticleTableDef.ARTICLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 特有功能集成测试
 *
 * @author kk
 */
@SpringBootTest
@Transactional // 确保每个测试方法后自动回滚
class PostgreSQLSpecialFeaturesTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void testPostgreSQLDialectFeatures() {
        // 测试 PostgreSQL 特有的 SQL 生成特性
        System.out.println("=== 测试 PostgreSQL 方言特性 ===");

        // 1. 测试双引号关键字转义
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME.as("user"), ACCOUNT.AGE.as("order"))  // "order" 是 PostgreSQL 关键字
            .from(ACCOUNT)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc())
            .limit(3);

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertFalse(results.isEmpty());

        System.out.println("关键字转义测试结果:");
        for (Row row : results) {
            System.out.printf("User: %s, Order(Age): %d%n", 
                row.getString("user"), row.getInt("order"));
        }
    }

    @Test
    void testPostgreSQLLimitOffsetSyntax() {
        // 测试 PostgreSQL 的 LIMIT OFFSET 语法
        System.out.println("=== 测试 LIMIT OFFSET 语法 ===");

        QueryWrapper queryWrapper = QueryWrapper.create()
            .select()
            .from(ACCOUNT)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc())
            .limit(3)
            .offset(2);  // 跳过前2条，取3条

        List<Account> results = accountMapper.selectListByQuery(queryWrapper);
        assertTrue(results.size() <= 3);

        System.out.printf("LIMIT 3 OFFSET 2 查询结果数量: %d%n", results.size());
        for (Account account : results) {
            System.out.printf("ID: %d, 用户名: %s%n", account.getId(), account.getUserName());
        }
    }

    @Test
    void testSerialPrimaryKey() {
        // 测试 PostgreSQL SERIAL 主键自增
        System.out.println("=== 测试 SERIAL 主键自增 ===");

        Account account1 = new Account("SERIAL测试用户1", 25, 1);
        Account account2 = new Account("SERIAL测试用户2", 30, 2);

        // 插入前ID应该为空
        assertNull(account1.getId());
        assertNull(account2.getId());

        int result1 = accountMapper.insert(account1);
        int result2 = accountMapper.insert(account2);

        assertEquals(1, result1);
        assertEquals(1, result2);

        // 插入后应该分配了ID
        assertNotNull(account1.getId());
        assertNotNull(account2.getId());
        assertTrue(account1.getId() > 0);
        assertTrue(account2.getId() > 0);
        assertTrue(account2.getId() > account1.getId()); // ID应该是递增的

        System.out.printf("SERIAL主键测试: 用户1 ID=%d, 用户2 ID=%d%n", 
            account1.getId(), account2.getId());
    }

    @Test
    void testForeignKeyConstraint() {
        // 测试外键约束
        System.out.println("=== 测试外键约束 ===");

        // 1. 正常情况：插入有效的外键关联
        Account account = new Account("外键测试用户", 25, 1);
        accountMapper.insert(account);
        
        Article validArticle = new Article(account.getId(), "有效外键文章", "这是一篇测试文章");
        int result = articleMapper.insert(validArticle);
        assertEquals(1, result);
        System.out.printf("正常外键关联插入成功: 账户ID=%d, 文章ID=%d%n", 
            account.getId(), validArticle.getId());

        // 2. 异常情况：插入无效的外键关联（应该会抛出异常）
        Article invalidArticle = new Article(999999L, "无效外键文章", "这应该会失败");
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            articleMapper.insert(invalidArticle);
        }, "插入无效外键应该抛出约束异常");

        System.out.println("无效外键插入正确抛出异常");
    }

    @Test
    void testCascadeDelete() {
        // 测试级联删除
        System.out.println("=== 测试级联删除 ===");

        // 创建测试数据
        Account account = new Account("级联删除测试用户", 25, 1);
        accountMapper.insert(account);
        
        Article article1 = new Article(account.getId(), "级联删除文章1", "内容1");
        Article article2 = new Article(account.getId(), "级联删除文章2", "内容2");
        articleMapper.insertBatch(Arrays.asList(article1, article2));

        System.out.printf("创建测试数据: 账户ID=%d, 文章ID=[%d, %d]%n", 
            account.getId(), article1.getId(), article2.getId());

        // 验证数据存在
        List<Article> articlesBefore = articleMapper.selectListByQuery(
            QueryWrapper.create().where(ARTICLE.ACCOUNT_ID.eq(account.getId())));
        assertEquals(2, articlesBefore.size());

        // 删除账户（应该级联删除文章）
        int deleteResult = accountMapper.deleteById(account.getId());
        assertEquals(1, deleteResult);

        // 验证文章被级联删除
        List<Article> articlesAfter = articleMapper.selectListByQuery(
            QueryWrapper.create().where(ARTICLE.ACCOUNT_ID.eq(account.getId())));
        assertTrue(articlesAfter.isEmpty(), "文章应该被级联删除");

        System.out.println("级联删除测试成功");
    }

    @Test
    void testTimestampDefaultValues() {
        // 测试时间戳默认值
        System.out.println("=== 测试时间戳默认值 ===");

        Article article = new Article(1L, "时间戳测试文章", "测试 PostgreSQL 时间戳默认值");
        
        // 插入时不设置时间戳字段
        assertNull(article.getCreatedAt());
        assertNull(article.getUpdatedAt());

        int result = articleMapper.insert(article, true); // ignoreNulls=true，让null字段不出现在SQL中，使数据库DEFAULT生效
        assertEquals(1, result);

        // 重新查询验证默认值
        Article retrieved = articleMapper.selectOneById(article.getId());
        assertNotNull(retrieved.getCreatedAt(), "created_at 应该有默认值");
        assertNotNull(retrieved.getUpdatedAt(), "updated_at 应该有默认值");

        System.out.printf("时间戳默认值测试: created_at=%s, updated_at=%s%n", 
            retrieved.getCreatedAt(), retrieved.getUpdatedAt());
    }

    @Test
    void testPostgreSQLFunctions() {
        // 测试 PostgreSQL 特有函数
        System.out.println("=== 测试 PostgreSQL 特有函数 ===");

        // 1. 测试字符串函数
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(
                ACCOUNT.USER_NAME,
                new RawQueryColumn("LENGTH(user_name)").as("name_length"),
                new RawQueryColumn("UPPER(user_name)").as("upper_name"),
                new RawQueryColumn("CURRENT_TIMESTAMP").as("current_time")
            )
            .from(ACCOUNT)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .limit(3);

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertFalse(results.isEmpty());

        System.out.println("PostgreSQL 函数测试结果:");
        for (Row row : results) {
            System.out.printf("用户名: %s, 长度: %d, 大写: %s, 当前时间: %s%n",
                row.getString("user_name"),
                row.getInt("name_length"),
                row.getString("upper_name"),
                row.get("current_time")
            );
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 不使用Spring事务，让Db.tx()完全控制事务
    void testTransactionRollback() {
        // 测试事务回滚
        System.out.println("=== 测试事务回滚 ===");

        // 记录初始数据量
        long initialCount = accountMapper.selectCountByQuery(
            QueryWrapper.create().where(ACCOUNT.IS_DELETE.eq(0)));

        try {
            // 在事务中进行操作
            Db.tx(() -> {
                Account account1 = new Account("事务测试用户1", 25, 1);
                Account account2 = new Account("事务测试用户2", 30, 2);
                
                accountMapper.insert(account1);
                accountMapper.insert(account2);

                // 人为制造异常，触发回滚
                throw new RuntimeException("测试事务回滚");
            });
        } catch (RuntimeException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
        }

        // 验证事务回滚后数据量没有变化
        long finalCount = accountMapper.selectCountByQuery(
            QueryWrapper.create().where(ACCOUNT.IS_DELETE.eq(0)));
        
        assertEquals(initialCount, finalCount, "事务回滚后数据量应该保持不变");
        System.out.printf("事务回滚测试成功: 初始数量=%d, 最终数量=%d%n", initialCount, finalCount);
    }

    @Test
    void testComplexJSONQuery() {
        // 测试 JSON 数据查询（如果支持）
        System.out.println("=== 测试 JSON 数据查询 ===");

        // 查询包含 JSON 数据的记录
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ACCOUNT.OPTIONS)
            .from(ACCOUNT)
            .where(ACCOUNT.OPTIONS.isNotNull())
            .and(ACCOUNT.IS_DELETE.eq(0))
            .limit(3);

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        
        System.out.println("JSON 数据查询结果:");
        for (Row row : results) {
            System.out.printf("用户: %s, JSON选项: %s%n",
                row.getString("user_name"),
                row.getString("options")
            );
        }

        // 如果有结果，验证 JSON 格式
        if (!results.isEmpty()) {
            for (Row row : results) {
                String options = row.getString("options");
                if (options != null) {
                    assertTrue(options.startsWith("{") && options.endsWith("}"), 
                        "选项字段应该是有效的 JSON 格式");
                }
            }
        }
    }

    @Test
    void testIndexUsage() {
        // 测试索引使用情况
        System.out.println("=== 测试索引使用 ===");

        // 使用带索引的查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select()
            .from(ACCOUNT)
            .where(ACCOUNT.USER_NAME.like("张%"))  // user_name 字段有索引
            .and(ACCOUNT.AGE.between(20, 40))     // age 字段有索引
            .orderBy(ACCOUNT.ID.asc());

        List<Account> results = accountMapper.selectListByQuery(queryWrapper);
        
        System.out.printf("索引查询结果数量: %d%n", results.size());
        for (Account account : results) {
            System.out.printf("用户: %s, 年龄: %d%n", account.getUserName(), account.getAge());
        }

        // 验证查询能正常执行（实际的索引效果需要通过 EXPLAIN 分析）
        assertNotNull(results);
    }
}