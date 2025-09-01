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
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.test.postgresql.mapper.AccountMapper;
import com.mybatisflex.test.postgresql.mapper.ArticleMapper;
import com.mybatisflex.test.postgresql.model.Account;
import com.mybatisflex.test.postgresql.model.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mybatisflex.test.postgresql.model.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.test.postgresql.model.table.ArticleTableDef.ARTICLE;
import static com.mybatisflex.core.query.QueryMethods.distinct;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 基础 CRUD 集成测试
 *
 * @author kk
 */
@SpringBootTest
@Transactional // 确保每个测试方法后自动回滚
class PostgreSQLBasicCrudTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void testInsertAccount() {
        // 测试单条插入
        Account account = new Account("集成测试用户", 25, 1);
        account.setBirthday(new Date());
        account.setOptions("{\"test\": true}");

        int result = accountMapper.insert(account);
        assertEquals(1, result);
        assertNotNull(account.getId());
        assertTrue(account.getId() > 0);

        System.out.println("插入成功，生成的ID: " + account.getId());
    }

    @Test
    void testBatchInsertAccounts() {
        // 测试批量插入
        List<Account> accounts = Arrays.asList(
            new Account("批量用户1", 20, 1),
            new Account("批量用户2", 22, 2),
            new Account("批量用户3", 24, 1)
        );

        int result = accountMapper.insertBatch(accounts);
        assertEquals(3, result);

        // 注意：批量插入不支持主键回填，这是ORM框架的常见限制
        // 验证数据库中确实插入了数据
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.USER_NAME.like("批量用户%"))
            .orderBy(ACCOUNT.USER_NAME.asc());
        
        List<Account> insertedAccounts = accountMapper.selectListByQuery(queryWrapper);
        assertEquals(3, insertedAccounts.size());
        
        for (int i = 0; i < insertedAccounts.size(); i++) {
            Account insertedAccount = insertedAccounts.get(i);
            assertNotNull(insertedAccount.getId());
            assertTrue(insertedAccount.getId() > 0);
            assertEquals("批量用户" + (i + 1), insertedAccount.getUserName());
            System.out.println("批量插入验证成功，ID: " + insertedAccount.getId() + ", 用户名: " + insertedAccount.getUserName());
        }
    }

    @Test
    void testSelectById() {
        // 测试根据ID查询
        Account account = accountMapper.selectOneById(1L);
        assertNotNull(account);
        assertEquals("张三", account.getUserName());
        assertEquals(Integer.valueOf(25), account.getAge());
        assertEquals(Integer.valueOf(1), account.getSex());

        System.out.println("根据ID查询成功: " + account);
    }

    @Test
    void testSelectByCondition() {
        // 测试条件查询
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.AGE.ge(25))
            .and(ACCOUNT.SEX.eq(1))
            .and(ACCOUNT.IS_DELETE.eq(0));

        List<Account> accounts = accountMapper.selectListByQuery(queryWrapper);
        assertFalse(accounts.isEmpty());

        for (Account account : accounts) {
            assertTrue(account.getAge() >= 25);
            assertEquals(Integer.valueOf(1), account.getSex());
            assertEquals(Integer.valueOf(0), account.getIsDelete());
            System.out.println("条件查询结果: " + account.getUserName() + ", 年龄: " + account.getAge());
        }
    }

    @Test
    void testSelectWithPagination() {
        // 测试分页查询
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.AGE.asc());

        Page<Account> page = accountMapper.paginate(1, 5, queryWrapper);
        assertNotNull(page);
        assertTrue(page.getTotalRow() > 0);
        assertTrue(page.getRecords().size() <= 5);

        System.out.println("分页查询结果:");
        System.out.println("总记录数: " + page.getTotalRow());
        System.out.println("总页数: " + page.getTotalPage());
        System.out.println("当前页记录数: " + page.getRecords().size());

        for (Account account : page.getRecords()) {
            System.out.println("  - " + account.getUserName() + ", 年龄: " + account.getAge());
        }
    }

    @Test
    void testUpdateById() {
        // 测试根据ID更新
        Account account = accountMapper.selectOneById(1L);
        assertNotNull(account);

        String originalUserName = account.getUserName();
        account.setUserName("更新后的用户名");
        account.setAge(30);

        int result = accountMapper.update(account);
        assertEquals(1, result);

        // 验证更新结果
        Account updatedAccount = accountMapper.selectOneById(1L);
        assertEquals("更新后的用户名", updatedAccount.getUserName());
        assertEquals(Integer.valueOf(30), updatedAccount.getAge());

        System.out.println("更新成功: " + originalUserName + " -> " + updatedAccount.getUserName());
    }

    @Test
    void testUpdateByCondition() {
        // 测试条件更新
        Account updateEntity = new Account();
        updateEntity.setAge(26);
        
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.USER_NAME.eq("李四"));

        int result = accountMapper.updateByQuery(updateEntity, queryWrapper);
        assertEquals(1, result);

        // 验证更新结果
        QueryWrapper selectWrapper = QueryWrapper.create()
            .where(ACCOUNT.USER_NAME.eq("李四"));
        Account updatedAccount = accountMapper.selectOneByQuery(selectWrapper);
        assertEquals(Integer.valueOf(26), updatedAccount.getAge());

        System.out.println("条件更新成功，李四的年龄更新为: " + updatedAccount.getAge());
    }

    @Test
    void testDeleteById() {
        // 先插入一条测试数据
        Account testAccount = new Account("待删除用户", 25, 1);
        accountMapper.insert(testAccount);
        Long testId = testAccount.getId();
        assertNotNull(testId);

        // 测试根据ID删除
        int result = accountMapper.deleteById(testId);
        assertEquals(1, result);

        // 验证删除结果
        Account deletedAccount = accountMapper.selectOneById(testId);
        assertNull(deletedAccount);

        System.out.println("删除成功，ID: " + testId);
    }

    @Test
    void testDeleteByCondition() {
        // 先插入测试数据
        Account testAccount1 = new Account("条件删除用户1", 99, 1);
        Account testAccount2 = new Account("条件删除用户2", 99, 2);
        accountMapper.insertBatch(Arrays.asList(testAccount1, testAccount2));

        // 测试条件删除
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.AGE.eq(99));

        int result = accountMapper.deleteByQuery(queryWrapper);
        assertEquals(2, result);

        // 验证删除结果
        List<Account> remainingAccounts = accountMapper.selectListByQuery(queryWrapper);
        assertTrue(remainingAccounts.isEmpty());

        System.out.println("条件删除成功，删除了 " + result + " 条记录");
    }

    @Test
    void testSelectCount() {
        // 测试计数查询
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.IS_DELETE.eq(0));

        long count = accountMapper.selectCountByQuery(queryWrapper);
        assertTrue(count > 0);

        System.out.println("未删除的账户总数: " + count);
    }

    @Test
    void testComplexConditionQuery() {
        // 测试复杂条件查询
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where(ACCOUNT.AGE.between(20, 35))
            .and(ACCOUNT.SEX.in(1, 2))
            .and(ACCOUNT.USER_NAME.like("三"))
            .or(ACCOUNT.AGE.gt(40));

        List<Account> accounts = accountMapper.selectListByQuery(queryWrapper);
        assertNotNull(accounts);

        System.out.println("复杂条件查询结果数量: " + accounts.size());
        for (Account account : accounts) {
            System.out.println("  - " + account.getUserName() + ", 年龄: " + account.getAge() + ", 性别: " + account.getSex());
        }
    }

    @Test
    void testHandleSpecialCharacters() {
        // 测试特殊字符处理
        Account specialAccount = new Account("特殊字符'\"用户", 25, 1);
        specialAccount.setOptions("{\"key\": \"value with 'quotes' and \\\"double quotes\\\"\"}");

        int result = accountMapper.insert(specialAccount);
        assertEquals(1, result);

        // 验证查询结果
        Account retrieved = accountMapper.selectOneById(specialAccount.getId());
        assertEquals("特殊字符'\"用户", retrieved.getUserName());
        assertNotNull(retrieved.getOptions());

        System.out.println("特殊字符处理测试成功: " + retrieved);
    }

    @Test
    void testOptimizeCountQuery() {
        // 测试 Page.setOptimizeCountQuery 功能
        System.out.println("=== 测试分页查询 COUNT 优化功能 ===");

        QueryWrapper queryWrapper = QueryWrapper.create()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.AGE.desc(), ACCOUNT.ID.asc());

        // 1. 测试默认行为：optimizeCountQuery = true (默认值)
        Page<Account> optimizedPage = new Page<>(1, 3);
        assertTrue(optimizedPage.needOptimizeCountQuery(), "默认应该启用 COUNT 查询优化");
        
        Page<Account> result1 = accountMapper.paginate(optimizedPage, queryWrapper);
        assertTrue(result1.getTotalRow() > 0);
        System.out.printf("优化 COUNT 查询 - 总记录数: %d, 当前页记录数: %d%n", 
            result1.getTotalRow(), result1.getRecords().size());
        
        // 2. 测试禁用优化：optimizeCountQuery = false
        Page<Account> nonOptimizedPage = new Page<>(1, 3);
        nonOptimizedPage.setOptimizeCountQuery(false);
        assertFalse(nonOptimizedPage.needOptimizeCountQuery(), "应该禁用 COUNT 查询优化");
        
        Page<Account> result2 = accountMapper.paginate(nonOptimizedPage, queryWrapper);
        assertTrue(result2.getTotalRow() > 0);
        System.out.printf("非优化 COUNT 查询 - 总记录数: %d, 当前页记录数: %d%n", 
            result2.getTotalRow(), result2.getRecords().size());
        
        // 3. 验证两种方式的结果应该相同
        assertEquals(result1.getTotalRow(), result2.getTotalRow(), 
            "优化和非优化的 COUNT 查询结果应该相同");
        assertEquals(result1.getRecords().size(), result2.getRecords().size(),
            "优化和非优化的分页数据数量应该相同");
        
        // 4. 测试复杂查询的优化效果
        QueryWrapper complexQuery = QueryWrapper.create()
            .select(
                ACCOUNT.ID,
                ACCOUNT.USER_NAME, 
                ACCOUNT.AGE,
                ACCOUNT.SEX,
                ACCOUNT.BIRTHDAY
            )
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.between(18, 50))
            .orderBy(
                ACCOUNT.AGE.desc(),
                ACCOUNT.USER_NAME.asc(), 
                ACCOUNT.ID.desc()
            );

        Page<Account> complexOptimizedPage = new Page<>(1, 2);
        Page<Account> complexResult1 = accountMapper.paginate(complexOptimizedPage, complexQuery);
        
        Page<Account> complexNonOptimizedPage = new Page<>(1, 2);
        complexNonOptimizedPage.setOptimizeCountQuery(false);
        Page<Account> complexResult2 = accountMapper.paginate(complexNonOptimizedPage, complexQuery);
        
        assertEquals(complexResult1.getTotalRow(), complexResult2.getTotalRow(),
            "复杂查询的优化和非优化 COUNT 结果应该相同");
        
        System.out.printf("复杂查询测试 - 优化版总记录数: %d, 非优化版总记录数: %d%n",
            complexResult1.getTotalRow(), complexResult2.getTotalRow());
        
        // 5. 测试 DISTINCT 查询的优化效果
        QueryWrapper distinctQuery = QueryWrapper.create()
            .select(distinct(ACCOUNT.AGE), ACCOUNT.SEX)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.between(20, 40))
            .orderBy(ACCOUNT.AGE.asc());

        Page<Account> distinctOptimizedPage = new Page<>(1, 5);
        Page<Account> distinctResult1 = accountMapper.paginate(distinctOptimizedPage, distinctQuery);
        
        Page<Account> distinctNonOptimizedPage = new Page<>(1, 5);
        distinctNonOptimizedPage.setOptimizeCountQuery(false);
        Page<Account> distinctResult2 = accountMapper.paginate(distinctNonOptimizedPage, distinctQuery);
        
        assertEquals(distinctResult1.getTotalRow(), distinctResult2.getTotalRow(),
            "DISTINCT 查询的优化和非优化 COUNT 结果应该相同");
        
        System.out.printf("DISTINCT 查询测试 - 优化版总记录数: %d, 非优化版总记录数: %d%n",
            distinctResult1.getTotalRow(), distinctResult2.getTotalRow());
        
        // 6. 测试复杂 DISTINCT + GROUP BY 的情况
        QueryWrapper complexDistinctQuery = QueryWrapper.create()
            .select(distinct(ACCOUNT.AGE))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .groupBy(ACCOUNT.AGE)
            .orderBy(ACCOUNT.AGE.desc());

        Page<Account> complexDistinctOptimizedPage = new Page<>(1, 3);
        Page<Account> complexDistinctResult1 = accountMapper.paginate(complexDistinctOptimizedPage, complexDistinctQuery);
        
        Page<Account> complexDistinctNonOptimizedPage = new Page<>(1, 3);
        complexDistinctNonOptimizedPage.setOptimizeCountQuery(false);
        Page<Account> complexDistinctResult2 = accountMapper.paginate(complexDistinctNonOptimizedPage, complexDistinctQuery);
        
        assertEquals(complexDistinctResult1.getTotalRow(), complexDistinctResult2.getTotalRow(),
            "复杂 DISTINCT + GROUP BY 查询的优化和非优化 COUNT 结果应该相同");
        
        System.out.printf("复杂 DISTINCT + GROUP BY 测试 - 优化版总记录数: %d, 非优化版总记录数: %d%n",
            complexDistinctResult1.getTotalRow(), complexDistinctResult2.getTotalRow());
        
        System.out.println("COUNT 查询优化功能测试通过！");
    }
}