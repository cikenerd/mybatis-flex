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

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.test.postgresql.mapper.AccountMapper;
import com.mybatisflex.test.postgresql.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mybatisflex.test.postgresql.model.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.test.postgresql.model.table.ArticleTableDef.ARTICLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL JOIN 查询集成测试
 *
 * @author kk
 */
@SpringBootTest
@Transactional // 确保每个测试方法后自动回滚
class PostgreSQLJoinTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void testInnerJoinQuery() {
        // 测试 INNER JOIN 查询
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE, 
                   ARTICLE.ID.as("article_id"), ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc(), ARTICLE.ID.asc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertFalse(results.isEmpty());

        System.out.println("INNER JOIN 查询结果:");
        for (Row row : results) {
            System.out.printf("用户: %s (ID: %d, 年龄: %d) -> 文章: %s (ID: %d)%n",
                row.getString("user_name"),
                row.getLong("id"),
                row.getInt("age"),
                row.getString("title"),
                row.getLong("article_id")
            );
        }

        // 验证所有结果都有用户和文章数据
        for (Row row : results) {
            assertNotNull(row.get("user_name"));
            assertNotNull(row.get("title"));
            assertNotNull(row.get("id"));
            assertNotNull(row.get("article_id"));
        }
    }

    @Test
    void testLeftJoinQuery() {
        // 测试 LEFT JOIN 查询 - 包括没有文章的用户
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE,
                   ARTICLE.ID.as("article_id"), ARTICLE.TITLE)
            .from(ACCOUNT)
            .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID).and(ARTICLE.IS_DELETE.eq(0)))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertFalse(results.isEmpty());

        int usersWithArticles = 0;
        int usersWithoutArticles = 0;

        System.out.println("LEFT JOIN 查询结果:");
        for (Row row : results) {
            String userName = row.getString("user_name");
            String title = row.getString("title");
            
            if (title != null) {
                usersWithArticles++;
                System.out.printf("用户: %s -> 有文章: %s%n", userName, title);
            } else {
                usersWithoutArticles++;
                System.out.printf("用户: %s -> 无文章%n", userName);
            }
        }

        System.out.printf("有文章的用户: %d, 无文章的用户: %d%n", usersWithArticles, usersWithoutArticles);
        assertTrue(usersWithoutArticles > 0, "应该存在没有文章的用户");
    }

    @Test
    void testRightJoinQuery() {
        // 测试 RIGHT JOIN 查询 - 以文章为主表
        // 只使用基础测试数据，限制查询范围确保稳定性
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ACCOUNT.AGE,
                   ARTICLE.ID.as("article_id"), ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .rightJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ARTICLE.IS_DELETE.eq(0))
            .and(ARTICLE.ID.le(20)) // 限制只查询ID<=20的文章，确保使用基础测试数据
            .orderBy(ARTICLE.ID.asc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        
        System.out.println("RIGHT JOIN 查询结果数量: " + results.size());
        if (!results.isEmpty()) {
            System.out.println("RIGHT JOIN 查询结果 (前3条):");
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                Row row = results.get(i);
                System.out.printf("文章: %s (ID: %d) -> 作者: %s%n",
                    row.getString("title"),
                    row.getLong("article_id"),
                    row.getString("user_name")
                );
            }
        }

        // 基于基础测试数据的合理预期：从data-postgresql.sql可以看到有12篇基础文章(ID 1-12)
        // 其中2篇被软删除(ID=8, ID=11对应的文章)，所以实际应该有10篇可用文章
        assertTrue(results.size() >= 10, "RIGHT JOIN查询应该返回至少10条基础测试数据（考虑软删除）");
        
        // 验证RIGHT JOIN的特性：结果中应该包含所有匹配的文章，限制ID<=20确保只使用基础数据  
        assertTrue(results.size() <= 20, "查询结果不应超过基础测试数据范围（限制ID<=20）");

        // 验证所有结果都有文章数据
        for (Row row : results) {
            assertNotNull(row.get("title"));
            assertNotNull(row.get("article_id"));
            // 用户数据可能为空（如果存在孤立的文章）
        }
    }

    @Test
    void testJoinWithPagination() {
        // 测试 JOIN 查询分页
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE, ARTICLE.CREATED_AT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ARTICLE.CREATED_AT.desc());

        // 分页查询：第1页，每页3条
        Page<Row> page = accountMapper.paginateAs(1, 3, queryWrapper, Row.class);
        assertNotNull(page);
        assertTrue(page.getTotalRow() > 0);
        assertTrue(page.getRecords().size() <= 3);

        System.out.println("JOIN 分页查询结果:");
        System.out.printf("总记录数: %d, 总页数: %d, 当前页记录数: %d%n",
            page.getTotalRow(), page.getTotalPage(), page.getRecords().size());

        for (Row row : page.getRecords()) {
            System.out.printf("作者: %s, 文章: %s, 创建时间: %s%n",
                row.getString("user_name"),
                row.getString("title"),
                row.get("created_at")
            );
        }
    }

    @Test
    void testMultipleJoins() {
        // 测试多表 JOIN（使用表别名）
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(
                ACCOUNT.as("a").USER_NAME.as("author_name"),
                ARTICLE.as("art1").TITLE.as("article_title"),
                ARTICLE.as("art2").TITLE.as("other_article_title")
            )
            .from(ACCOUNT.as("a"))
            .innerJoin(ARTICLE.as("art1")).on(ACCOUNT.as("a").ID.eq(ARTICLE.as("art1").ACCOUNT_ID))
            .leftJoin(ARTICLE.as("art2")).on(
                ACCOUNT.as("a").ID.eq(ARTICLE.as("art2").ACCOUNT_ID)
                .and(ARTICLE.as("art1").ID.ne(ARTICLE.as("art2").ID))
            )
            .where(ACCOUNT.as("a").IS_DELETE.eq(0))
            .and(ARTICLE.as("art1").IS_DELETE.eq(0))
            .orderBy(ACCOUNT.as("a").ID.asc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertNotNull(results);

        System.out.println("多表 JOIN 查询结果:");
        for (Row row : results) {
            System.out.printf("作者: %s, 文章1: %s, 文章2: %s%n",
                row.getString("author_name"),
                row.getString("article_title"),
                row.getString("other_article_title")
            );
        }
    }

    @Test
    void testJoinWithComplexConditions() {
        // 测试 JOIN 查询复杂条件
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ACCOUNT.AGE, ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(
                ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID)
                .and(ACCOUNT.IS_DELETE.eq(0))
                .and(ARTICLE.IS_DELETE.eq(0))
            )
            .where(ACCOUNT.AGE.between(20, 35))
            .and(ARTICLE.TITLE.like("%MyBatis%").or(ARTICLE.TITLE.like("%PostgreSQL%")))
            .orderBy(ACCOUNT.AGE.desc(), ARTICLE.CREATED_AT.desc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertNotNull(results);

        System.out.println("复杂条件 JOIN 查询结果:");
        for (Row row : results) {
            System.out.printf("作者: %s (年龄: %d), 文章: %s%n",
                row.getString("user_name"),
                row.getInt("age"),
                row.getString("title")
            );
        }

        // 验证所有结果都符合条件
        for (Row row : results) {
            int age = row.getInt("age");
            String title = row.getString("title");
            assertTrue(age >= 20 && age <= 35, "年龄应该在20-35之间");
            assertTrue(title.contains("MyBatis") || title.contains("PostgreSQL"), 
                "标题应该包含 MyBatis 或 PostgreSQL");
        }
    }

    @Test
    void testJoinWithGroupByAndHaving() {
        // 测试 JOIN 查询 + GROUP BY + HAVING
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(
                ACCOUNT.USER_NAME,
                ACCOUNT.AGE,
                QueryMethods.count(ARTICLE.ID).as("article_count"),
                QueryMethods.max(ARTICLE.CREATED_AT).as("latest_article")
            )
            .from(ACCOUNT)
            .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID).and(ARTICLE.IS_DELETE.eq(0)))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .groupBy(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE)
            .having(QueryMethods.count(ARTICLE.ID).ge(1))
            .orderBy(QueryMethods.count(ARTICLE.ID).desc());

        List<Row> results = accountMapper.selectRowsByQuery(queryWrapper);
        assertNotNull(results);

        System.out.println("GROUP BY + HAVING JOIN 查询结果:");
        for (Row row : results) {
            System.out.printf("作者: %s (年龄: %d), 文章数: %d, 最新文章时间: %s%n",
                row.getString("user_name"),
                row.getInt("age"),
                row.getLong("article_count"),
                row.get("latest_article")
            );
        }

        // 验证所有结果的文章数都 >= 1
        for (Row row : results) {
            long articleCount = row.getLong("article_count");
            assertTrue(articleCount >= 1, "文章数应该 >= 1");
        }
    }

    @Test
    void testJoinPaginationWithComplexQuery() {
        // 测试复杂 JOIN 查询分页
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(
                ACCOUNT.USER_NAME.as("author"),
                ACCOUNT.AGE,
                ARTICLE.TITLE,
                ARTICLE.CONTENT
            )
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.ge(25))
            .orderBy(ACCOUNT.AGE.desc(), ARTICLE.CREATED_AT.desc());

        // 第2页，每页2条记录
        Page<Row> page = accountMapper.paginateAs(2, 2, queryWrapper, Row.class);
        assertNotNull(page);

        System.out.println("复杂 JOIN 分页查询结果 (第2页):");
        System.out.printf("总记录数: %d, 总页数: %d, 当前页: %d, 当前页记录数: %d%n",
            page.getTotalRow(), page.getTotalPage(), page.getPageNumber(), page.getRecords().size());

        for (Row row : page.getRecords()) {
            System.out.printf("作者: %s (年龄: %d), 文章: %s%n",
                row.getString("author"),
                row.getInt("age"),
                row.getString("title")
            );
        }

        // 验证 PostgreSQL LIMIT OFFSET 语法正确执行
        assertTrue(page.getTotalRow() > 0);
        if (page.getTotalRow() > 2) {  // 如果总记录数大于2，第2页应该有数据
            assertFalse(page.getRecords().isEmpty());
        }
    }

    @Test
    void testLargeDataJoinPagination() {
        // 测试 JOIN 分页性能（使用基础测试数据）
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(
                ACCOUNT.USER_NAME,
                ACCOUNT.AGE,
                ARTICLE.TITLE,
                ARTICLE.CREATED_AT
            )
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ARTICLE.CREATED_AT.desc(), ACCOUNT.ID.asc());

        long startTime = System.currentTimeMillis();
        
        // 测试不同页码的分页查询（基于基础测试数据，预期约10-15条记录）
        for (int pageNum = 1; pageNum <= 3; pageNum++) {
            Page<Row> page = accountMapper.paginateAs(pageNum, 5, queryWrapper, Row.class);
            assertNotNull(page);
            
            System.out.printf("第%d页: 总记录数=%d, 当前页记录数=%d%n", 
                pageNum, page.getTotalRow(), page.getRecords().size());
            
            // 验证分页数据的正确性
            assertTrue(page.getTotalRow() > 0, "应该有记录返回");
            if (pageNum == 1) {
                assertTrue(page.getRecords().size() > 0, "第1页应该有数据");
            }
            
            // 如果总记录数少于当前页的起始位置，则这页应该为空
            if (page.getTotalRow() < (pageNum - 1) * 5) {
                assertEquals(0, page.getRecords().size(), "超出数据范围的页应该为空");
                break; // 提前结束循环
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("JOIN 分页测试耗时: %d ms%n", endTime - startTime);
        
        // 性能验证：3页查询应该在合理时间内完成
        assertTrue(endTime - startTime < 3000, "3页JOIN分页查询应该在3秒内完成");
    }

    @Test
    void testJoinPaginationWithDifferentPageSizes() {
        // 测试不同页面大小的 JOIN 分页
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE, ARTICLE.ID)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ARTICLE.ID.asc());

        // 测试不同的页面大小
        int[] pageSizes = {5, 10, 20, 50};
        
        for (int pageSize : pageSizes) {
            Page<Row> page = accountMapper.paginateAs(1, pageSize, queryWrapper, Row.class);
            assertNotNull(page);
            
            System.out.printf("页面大小 %d: 总记录数=%d, 实际返回记录数=%d, 总页数=%d%n",
                pageSize, page.getTotalRow(), page.getRecords().size(), page.getTotalPage());
            
            // 验证返回的记录数不超过请求的页面大小
            assertTrue(page.getRecords().size() <= pageSize, 
                String.format("返回记录数(%d)不应超过页面大小(%d)", page.getRecords().size(), pageSize));
            
            // 如果总记录数大于页面大小，第1页应该返回完整的页面大小
            if (page.getTotalRow() >= pageSize) {
                assertEquals(pageSize, page.getRecords().size(), 
                    String.format("当总记录数(%d) >= 页面大小(%d)时，第1页应该返回完整页面大小的数据", 
                        page.getTotalRow(), pageSize));
            }
        }
    }

    @Test
    void testJoinPaginationWithCountOptimization() {
        // 测试 JOIN 分页的 COUNT 查询优化
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.ge(20))  // 年龄大于等于20岁的用户（应该匹配大部分测试数据）
            .orderBy(ACCOUNT.ID.asc());

        // 创建分页对象并启用 COUNT 查询优化
        Page<Row> page = new Page<>(1, 15);
        page.setOptimizeCountQuery(true);
        
        long startTime = System.currentTimeMillis();
        Page<Row> result = accountMapper.paginateAs(page, queryWrapper, Row.class);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(result);
        System.out.printf("JOIN COUNT 优化测试: 总记录数=%d, 当前页记录数=%d, 耗时=%d ms%n",
            result.getTotalRow(), result.getRecords().size(), endTime - startTime);
        
        // 验证优化后的查询结果
        assertTrue(result.getTotalRow() > 0, "应该有匹配的记录");
        assertTrue(result.getRecords().size() <= 15, "第1页记录数不应超过15");
        
        // 验证每条记录都包含完整的 JOIN 数据
        for (Row row : result.getRecords()) {
            assertNotNull(row.getString("user_name"), "用户名不应为空");
            assertNotNull(row.getString("title"), "文章标题不应为空");
        }
    }

    @Test 
    void testJoinPaginationEdgeCases() {
        // 测试 JOIN 分页的边界情况
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc());

        // 测试第一页
        Page<Row> firstPage = accountMapper.paginateAs(1, 10, queryWrapper, Row.class);
        assertNotNull(firstPage);
        assertTrue(firstPage.getTotalRow() > 0, "应该有数据");
        assertTrue(firstPage.getRecords().size() > 0, "第1页应该有记录");
        
        // 测试最后一页
        int lastPageNum = (int) firstPage.getTotalPage();
        if (lastPageNum > 1) {
            Page<Row> lastPage = accountMapper.paginateAs(lastPageNum, 10, queryWrapper, Row.class);
            assertNotNull(lastPage);
            assertTrue(lastPage.getRecords().size() > 0, "最后一页应该有记录");
            System.out.printf("最后一页(第%d页)记录数: %d%n", lastPageNum, lastPage.getRecords().size());
        }
        
        // 测试超出范围的页码
        Page<Row> outOfRangePage = accountMapper.paginateAs(lastPageNum + 10, 10, queryWrapper, Row.class);
        assertNotNull(outOfRangePage);
        assertEquals(0, outOfRangePage.getRecords().size(), "超出范围的页码应该返回空结果");
        
        // 测试页面大小为1的情况
        Page<Row> singleRecordPage = accountMapper.paginateAs(1, 1, queryWrapper, Row.class);
        assertNotNull(singleRecordPage);
        if (singleRecordPage.getTotalRow() > 0) {
            assertEquals(1, singleRecordPage.getRecords().size(), "页面大小为1时应该只返回1条记录");
        }
        
        System.out.printf("边界测试完成 - 总记录数: %d, 总页数: %d%n", 
            firstPage.getTotalRow(), firstPage.getTotalPage());
    }
}