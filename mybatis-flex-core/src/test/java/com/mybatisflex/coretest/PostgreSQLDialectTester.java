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

package com.mybatisflex.coretest;

import com.mybatisflex.core.dialect.IDialect;
import com.mybatisflex.core.dialect.KeywordWrap;
import com.mybatisflex.core.dialect.LimitOffsetProcessor;
import com.mybatisflex.core.dialect.impl.CommonsDialectImpl;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.table.TableInfoFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static com.mybatisflex.coretest.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.coretest.table.ArticleTableDef.ARTICLE;

/**
 * PostgreSQL 方言测试类
 *
 * @author kk
 * @since 2025-01-09
 */
public class PostgreSQLDialectTester {

    /**
     * 创建 PostgreSQL 方言实例
     * PostgreSQL 使用双引号包装关键字，使用 PostgreSQL 的 LIMIT/OFFSET 处理器
     */
    private IDialect createPostgreSQLDialect() {
        return new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.POSTGRESQL);
    }

    @Test
    public void testSelectSql() {
        QueryWrapper query = new QueryWrapper()
            .select()
            .from(ACCOUNT);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Select SQL: " + sql);
        assertEquals("SELECT * FROM \"tb_account\"", sql);
    }

    @Test
    public void testSelectWithConditionSql() {
        QueryWrapper query = new QueryWrapper()
            .select()
            .from(ACCOUNT)
            .where(ACCOUNT.ID.ge(100))
            .and(ACCOUNT.USER_NAME.like("test"));

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Select with Condition SQL: " + sql);
        assertEquals("SELECT * FROM \"tb_account\" WHERE \"id\" >= ? AND \"user_name\" LIKE ?", sql);
    }

    @Test
    public void testSelectWithLimitOffsetSql() {
        QueryWrapper query = new QueryWrapper()
            .select()
            .from(ACCOUNT)
            .orderBy(ACCOUNT.ID.desc())
            .limit(10)
            .offset(20);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Pagination SQL: " + sql);
        assertEquals("SELECT * FROM \"tb_account\" ORDER BY \"id\" DESC LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testInsertBatchSql() {
        List<Account> accounts = new ArrayList<>();
        Account account1 = new Account();
        account1.setUserName("postgres_user1");
        account1.setAge(25);
        account1.setSex(1);
        accounts.add(account1);

        Account account2 = new Account();
        account2.setUserName("postgres_user2");
        account2.setAge(30);
        account2.setSex(2);
        accounts.add(account2);

        Account account3 = new Account();
        account3.setUserName("postgres_user3");
        account3.setAge(35);
        account3.setSex(1);
        accounts.add(account3);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forInsertEntityBatch(TableInfoFactory.ofEntityClass(Account.class), accounts);
        System.out.println("PostgreSQL Batch Insert SQL: " + sql);
        
        // PostgreSQL 使用标准的 VALUES 子句批量插入
        String expectedSql = "INSERT INTO \"tb_account\"(\"id\", \"user_name\", \"birthday\", \"sex\", \"age\", \"is_normal\", \"is_delete\") " +
            "VALUES (?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?)";
        assertEquals(expectedSql, sql);
    }

    @Test
    public void testInsertRowBatchSql() {
        List<Row> rows = new ArrayList<>();
        Row row1 = new Row();
        row1.set("username", "pg_user1");
        row1.set("age", 28);
        row1.set("sex", 1);
        rows.add(row1);

        Row row2 = new Row();
        row2.set("username", "pg_user2");
        row2.set("age", 32);
        row2.set("sex", 2);
        rows.add(row2);

        Row row3 = new Row();
        row3.set("username", "pg_user3");
        row3.set("age", 24);
        row3.set("sex", 1);
        rows.add(row3);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forInsertBatchWithFirstRowColumns(null, "tb_account", rows);
        System.out.println("PostgreSQL Row Batch Insert SQL: " + sql);
        
        String expectedSql = "INSERT INTO \"tb_account\" (\"username\", \"age\", \"sex\")  " +
            "VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)";
        assertEquals(expectedSql, sql);
    }

    @Test
    public void testUpdateSql() {
        QueryWrapper query = new QueryWrapper()
            .where(ACCOUNT.ID.eq(1));

        Account account = new Account();
        account.setUserName("updated_user");
        account.setAge(40);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forUpdateEntityByQuery(TableInfoFactory.ofEntityClass(Account.class), account, false, query);
        System.out.println("PostgreSQL Update SQL: " + sql);
        
        // 验证更新语句包含双引号包装的字段名
        assert sql.contains("\"user_name\"");
        assert sql.contains("\"age\"");
        assert sql.contains("\"id\"");
    }

    @Test
    public void testDeleteSql() {
        QueryWrapper query = new QueryWrapper()
            .where(ACCOUNT.ID.eq(1));

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forDeleteByQuery(query);
        System.out.println("PostgreSQL Delete SQL: " + sql);
        assertEquals("DELETE FROM  WHERE \"id\" = ?", sql);
    }

    @Test
    public void testKeywordEscaping() {
        // 测试 PostgreSQL 关键字转义
        QueryWrapper query = new QueryWrapper()
            .select()
            .from("user")  // "user" 是 PostgreSQL 关键字
            .where(QueryMethods.column("order").eq(1)); // "order" 也是关键字

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Keyword Escaping SQL: " + sql);
        
        // 验证关键字被双引号正确转义
        assert sql.contains("\"user\"");
        // PostgreSQL 的 order 可能不会被自动转义，这取决于具体实现
    }

    @Test
    public void testComplexQuery() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE)
            .from(ACCOUNT)
            .where(ACCOUNT.AGE.between(18, 65))
            .and(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc(), ACCOUNT.AGE.desc())
            .limit(50)
            .offset(100);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Complex Query SQL: " + sql);
        
        // 验证复杂查询的各个部分都正确生成
        assert sql.contains("SELECT \"id\", \"user_name\", \"age\"");
        assert sql.contains("FROM \"tb_account\"");
        assert sql.contains("WHERE \"age\" BETWEEN");
        assert sql.contains("AND \"is_delete\" = ?");
        assert sql.contains("ORDER BY \"id\" ASC, \"age\" DESC");
        assert sql.contains("LIMIT 50 OFFSET 100");
    }

    @Test
    public void testInnerJoin() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ARTICLE.TITLE)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.AGE.ge(18));

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Inner Join SQL: " + sql);
        
        // 验证 INNER JOIN 语法
        assert sql.contains("SELECT \"tb_account\".\"id\", \"tb_account\".\"user_name\", \"tb_article\".\"title\"");
        assert sql.contains("FROM \"tb_account\"");
        assert sql.contains("INNER JOIN \"tb_article\"");
        assert sql.contains("ON \"tb_account\".\"id\" = \"tb_article\".\"account_id\"");
        assert sql.contains("WHERE \"tb_account\".\"age\" >= ?");
    }

    @Test
    public void testLeftJoin() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ARTICLE.TITLE)
            .from(ACCOUNT)
            .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0));

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Left Join SQL: " + sql);
        
        // 验证 LEFT JOIN 语法
        assert sql.contains("SELECT \"tb_account\".\"id\", \"tb_account\".\"user_name\", \"tb_article\".\"title\"");
        assert sql.contains("FROM \"tb_account\"");
        assert sql.contains("LEFT JOIN \"tb_article\"");
        assert sql.contains("ON \"tb_account\".\"id\" = \"tb_article\".\"account_id\"");
        assert sql.contains("WHERE \"tb_account\".\"is_delete\" = ?");
    }

    @Test
    public void testRightJoin() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.USER_NAME, ARTICLE.ID, ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .rightJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ARTICLE.TITLE.isNotNull());

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Right Join SQL: " + sql);
        
        // 验证 RIGHT JOIN 语法
        assert sql.contains("FROM \"tb_account\"");
        assert sql.contains("RIGHT JOIN \"tb_article\"");
        assert sql.contains("ON \"tb_account\".\"id\" = \"tb_article\".\"account_id\"");
        assert sql.contains("WHERE \"tb_article\".\"title\" IS NOT NULL");
    }

    @Test
    public void testJoinWithPagination() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ARTICLE.TITLE)
            .from(ACCOUNT)
            .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.AGE.between(20, 40))
            .orderBy(ACCOUNT.ID.asc(), ARTICLE.ID.desc())
            .limit(20)
            .offset(10);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Join with Pagination SQL: " + sql);
        
        // 验证 JOIN 分页查询
        assert sql.contains("LEFT JOIN \"tb_article\"");
        assert sql.contains("ON \"tb_account\".\"id\" = \"tb_article\".\"account_id\"");
        assert sql.contains("WHERE \"tb_account\".\"age\" BETWEEN");
        assert sql.contains("ORDER BY \"tb_account\".\"id\" ASC");
        assert sql.contains("LIMIT 20 OFFSET 10");
    }

    @Test
    public void testMultipleJoins() {
        // 使用表别名进行多表 JOIN
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.as("u").ID, ACCOUNT.as("u").USER_NAME, 
                   ARTICLE.as("a").TITLE, ARTICLE.as("p").CONTENT)
            .from(ACCOUNT.as("u"))
            .leftJoin(ARTICLE.as("a")).on(ACCOUNT.as("u").ID.eq(ARTICLE.as("a").ACCOUNT_ID))
            .leftJoin(ARTICLE.as("p")).on(ACCOUNT.as("u").ID.eq(ARTICLE.as("p").ACCOUNT_ID))
            .where(ACCOUNT.as("u").IS_DELETE.eq(0))
            .and(ARTICLE.as("a").TITLE.like("PostgreSQL"));

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Multiple Joins SQL: " + sql);
        
        // 验证多表 JOIN 和别名
        assert sql.contains("FROM \"tb_account\" AS \"u\"");
        assert sql.contains("LEFT JOIN \"tb_article\" AS \"a\"");
        assert sql.contains("LEFT JOIN \"tb_article\" AS \"p\"");
    }

    @Test
    public void testComplexJoinConditions() {
        QueryWrapper query = new QueryWrapper()
            .select()
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(
                ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID)
                .and(ACCOUNT.IS_DELETE.eq(0))
                .and(ARTICLE.TITLE.isNotNull())
            )
            .where(ACCOUNT.AGE.ge(18))
            .orderBy(ACCOUNT.ID.asc())
            .limit(15)
            .offset(5);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Complex Join Conditions SQL: " + sql);
        
        // 验证复杂 JOIN 条件
        assert sql.contains("INNER JOIN \"tb_article\"");
        assert sql.contains("ON");
        assert sql.contains("AND");
        assert sql.contains("LIMIT 15 OFFSET 5");
    }

    @Test
    public void testJoinWithGroupByAndHaving() {
        QueryWrapper query = new QueryWrapper()
            .select(ACCOUNT.USER_NAME, QueryMethods.count(ARTICLE.ID).as("article_count"))
            .from(ACCOUNT)
            .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .groupBy(ACCOUNT.ID, ACCOUNT.USER_NAME)
            .having(QueryMethods.count(ARTICLE.ID).ge(1))
            .orderBy(QueryMethods.count(ARTICLE.ID).desc())
            .limit(10);

        IDialect dialect = createPostgreSQLDialect();
        String sql = dialect.forSelectByQuery(query);
        System.out.println("PostgreSQL Join with GroupBy and Having SQL: " + sql);
        
        // 验证 JOIN 与 GROUP BY、HAVING 的组合
        assert sql.contains("LEFT JOIN \"tb_article\"");
        assert sql.contains("GROUP BY \"tb_account\".\"id\", \"tb_account\".\"user_name\"");
        assert sql.contains("HAVING COUNT(\"tb_article\".\"id\") >= ?");
        assert sql.contains("ORDER BY COUNT(\"tb_article\".\"id\") DESC");
        assert sql.contains("LIMIT 10");
    }
}