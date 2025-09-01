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
import com.mybatisflex.core.row.Row;
import com.mybatisflex.test.postgresql.mapper.AccountMapper;
import com.mybatisflex.test.postgresql.mapper.ArticleMapper;
import com.mybatisflex.test.postgresql.model.Account;
import com.mybatisflex.test.postgresql.model.Article;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mybatisflex.test.postgresql.model.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.test.postgresql.model.table.ArticleTableDef.ARTICLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 性能测试
 * 
 * 这个测试类专门用于性能测试，会动态生成大量测试数据
 * 只在性能测试环境下运行，避免影响日常开发
 *
 * @author kk
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PostgreSQLPerformanceTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ArticleMapper articleMapper;

    // 性能测试数据规模配置
    private static final int ACCOUNT_COUNT = getAccountCount();
    private static final int ARTICLES_PER_ACCOUNT = getArticlesPerAccount();
    
    private static int getAccountCount() {
        String scale = System.getProperty("perf.scale", "small").toLowerCase();
        switch (scale) {
            case "large":
                return 50000;    // 大规模测试
            case "medium":
                return 10000;   // 中等规模测试
            case "small":
                return 1000;     // 小规模测试
            default:
                return 1000;
        }
    }
    
    private static int getArticlesPerAccount() {
        String scale = System.getProperty("perf.scale", "small").toLowerCase();
        switch (scale) {
            case "large":
                return 4;        // 20万文章
            case "medium":
                return 5;       // 5万文章
            case "small":
                return 3;        // 3千文章
            default:
                return 3;
        }
    }

    private final Random random = new Random(42); // 固定种子确保测试可重复

    @BeforeAll
    void setupPerformanceData() {
        // 先清理已有的性能测试数据
        cleanupPerformanceData();
        
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过性能测试数据生成（未开启性能测试）");
            return;
        }

        System.out.printf("🚀 开始生成性能测试数据: %d 个账户, 约 %d 篇文章%n", 
            ACCOUNT_COUNT, ACCOUNT_COUNT * ARTICLES_PER_ACCOUNT);
        
        long startTime = System.currentTimeMillis();
        
        // 生成大量账户数据
        generateAccounts();
        
        // 生成大量文章数据
        generateArticles();
        
        long endTime = System.currentTimeMillis();
        System.out.printf("✅ 性能测试数据生成完成，耗时: %d ms%n", endTime - startTime);
        
        // 验证数据生成结果
        verifyGeneratedData();
    }

    @AfterAll
    void cleanupAfterPerformanceTest() {
        if (isPerformanceTestEnabled()) {
            System.out.println("🧹 开始清理性能测试数据...");
            cleanupPerformanceData();
            System.out.println("✅ 性能测试数据清理完成");
        }
    }

    /**
     * 清理性能测试生成的数据
     */
    private void cleanupPerformanceData() {
        try {
            // 删除性能测试生成的文章数据
            // 基于用户名模式识别性能测试数据
            long deletedArticles = articleMapper.deleteByQuery(
                QueryWrapper.create()
                    .from(ARTICLE)
                    .where(ARTICLE.ACCOUNT_ID.in(
                        QueryWrapper.create()
                            .select(ACCOUNT.ID)
                            .from(ACCOUNT)
                            .where(ACCOUNT.USER_NAME.like("%性能测试用户%")
                                .or(ACCOUNT.USER_NAME.like("%测试账户%"))
                                .or(ACCOUNT.USER_NAME.like("%Test User%")))
                    ))
            );
            
            // 删除性能测试生成的账户数据
            long deletedAccounts = accountMapper.deleteByQuery(
                QueryWrapper.create()
                    .from(ACCOUNT)
                    .where(ACCOUNT.USER_NAME.like("%性能测试用户%")
                        .or(ACCOUNT.USER_NAME.like("%测试账户%"))
                        .or(ACCOUNT.USER_NAME.like("%Test User%")))
            );
            
            if (deletedAccounts > 0 || deletedArticles > 0) {
                System.out.printf("🗑️ 清理性能测试数据: %d 个账户, %d 篇文章%n", 
                    deletedAccounts, deletedArticles);
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ 清理性能测试数据时出现异常: " + e.getMessage());
            // 不抛出异常，避免影响测试执行
        }
    }

    /**
     * 检查是否启用性能测试
     */
    private boolean isPerformanceTestEnabled() {
        return "true".equals(System.getProperty("performance.test")) || 
               "true".equals(System.getenv("PERFORMANCE_TEST"));
    }

    /**
     * 批量生成账户数据
     */
    private void generateAccounts() {
        System.out.println("📊 生成账户数据...");
        
        List<Account> accounts = new ArrayList<>();
        String[] nameTemplates = {
            "性能测试用户", "测试账户", "用户", "账号", "Test User"
        };
        
        for (int i = 1; i <= ACCOUNT_COUNT; i++) {
            Account account = new Account();
            account.setUserName(nameTemplates[i % nameTemplates.length] + String.format("%06d", i));
            account.setAge(18 + random.nextInt(50)); // 18-67岁
            account.setSex(random.nextInt(2) + 1);   // 1或2
            account.setBirthday(generateRandomDate());
            account.setOptions(generateRandomOptions());
            account.setIsNormal(random.nextDouble() > 0.05 ? 1 : 0); // 95%正常
            account.setIsDelete(random.nextDouble() > 0.02 ? 0 : 1);  // 2%删除
            
            accounts.add(account);
            
            // 批量插入，避免内存溢出
            if (accounts.size() >= 1000) {
                accountMapper.insertBatch(accounts);
                accounts.clear();
                
                if (i % 10000 == 0) {
                    System.out.printf("  已生成 %d 个账户...%n", i);
                }
            }
        }
        
        // 插入剩余数据
        if (!accounts.isEmpty()) {
            accountMapper.insertBatch(accounts);
        }
        
        System.out.printf("✅ 账户数据生成完成: %d 条记录%n", ACCOUNT_COUNT);
    }

    /**
     * 批量生成文章数据
     */
    private void generateArticles() {
        System.out.println("📝 生成文章数据...");
        
        // 获取所有有效账户ID
        List<Long> accountIds = getActiveAccountIds();
        
        List<Article> articles = new ArrayList<>();
        String[] titleTemplates = {
            "Java开发实战指南", "Spring Boot最佳实践", "MyBatis-Flex深度解析",
            "PostgreSQL性能优化", "分布式系统设计", "微服务架构实践",
            "数据库索引优化", "高并发系统设计", "容器化部署方案", "DevOps工程实践"
        };
        
        int articleCount = 0;
        for (Long accountId : accountIds) {
            int articlesForThisAccount = 1 + random.nextInt(ARTICLES_PER_ACCOUNT);
            
            for (int j = 0; j < articlesForThisAccount; j++) {
                Article article = new Article();
                article.setAccountId(accountId);
                article.setTitle(titleTemplates[random.nextInt(titleTemplates.length)] + 
                    " - " + String.format("%08d", ++articleCount));
                article.setContent(generateRandomContent(articleCount));
                article.setIsDelete(random.nextDouble() > 0.05 ? 0 : 1); // 5%删除
                
                articles.add(article);
                
                // 批量插入
                if (articles.size() >= 1000) {
                    articleMapper.insertBatch(articles);
                    articles.clear();
                    
                    if (articleCount % 50000 == 0) {
                        System.out.printf("  已生成 %d 篇文章...%n", articleCount);
                    }
                }
            }
        }
        
        // 插入剩余数据
        if (!articles.isEmpty()) {
            articleMapper.insertBatch(articles);
        }
        
        System.out.printf("✅ 文章数据生成完成: %d 条记录%n", articleCount);
    }

    /**
     * 获取有效账户ID列表
     */
    private List<Long> getActiveAccountIds() {
        QueryWrapper query = QueryWrapper.create()
            .select(ACCOUNT.ID)
            .from(ACCOUNT)
            .where(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc());
        
        List<Row> rows = accountMapper.selectRowsByQuery(query);
        return rows.stream().map(row -> row.getLong("id")).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 生成随机日期
     */
    private Date generateRandomDate() {
        // 生成1970年到2020年之间的随机时间戳
        long minTime = 0L; // 1970-01-01
        long maxTime = 1577836800000L; // 2020-01-01
        long randomTime = minTime + (long) (random.nextDouble() * (maxTime - minTime));
        return new Date(randomTime);
    }

    /**
     * 生成随机选项JSON
     */
    private String generateRandomOptions() {
        String[] themes = {"dark", "light", "auto"};
        String[] languages = {"zh", "en", "ja"};
        boolean premium = random.nextDouble() > 0.7;
        int level = random.nextInt(20) + 1;
        
        return String.format("{\"theme\":\"%s\",\"language\":\"%s\",\"premium\":%s,\"level\":%d}",
            themes[random.nextInt(themes.length)],
            languages[random.nextInt(languages.length)],
            premium,
            level);
    }

    /**
     * 生成随机文章内容
     */
    private String generateRandomContent(int articleNumber) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("这是第%d篇性能测试文章的详细内容。", articleNumber));
        
        String[] paragraphs = {
            "本文深入探讨了现代软件开发中的关键技术和最佳实践。",
            "通过大量的实战案例，我们展示了如何在复杂的业务场景中应用这些技术。",
            "性能优化是软件开发中的重要环节，需要从多个维度进行考虑和实施。",
            "数据库设计和查询优化对系统整体性能有着至关重要的影响。",
            "分布式系统的设计需要考虑一致性、可用性和分区容错性之间的权衡。"
        };
        
        int paragraphCount = 3 + random.nextInt(3); // 3-5段
        for (int i = 0; i < paragraphCount; i++) {
            content.append(" ").append(paragraphs[random.nextInt(paragraphs.length)]);
        }
        
        return content.toString();
    }

    /**
     * 验证生成的数据
     */
    private void verifyGeneratedData() {
        long accountCount = accountMapper.selectCountByQuery(
            QueryWrapper.create().from(ACCOUNT).where(ACCOUNT.IS_DELETE.eq(0)));
        long articleCount = articleMapper.selectCountByQuery(
            QueryWrapper.create().from(ARTICLE).where(ARTICLE.IS_DELETE.eq(0)));
        
        System.out.printf("📈 数据验证结果: %d 个有效账户, %d 篇有效文章%n", accountCount, articleCount);
        assertTrue(accountCount > ACCOUNT_COUNT * 0.9, "生成的账户数量不足");
        assertTrue(articleCount > 0, "没有生成文章数据");
    }

    @Test
    void testLargeDataCRUDPerformance() {
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过CRUD性能测试");
            return;
        }

        System.out.println("🧪 开始CRUD性能测试...");
        
        long startTime = System.currentTimeMillis();
        
        // 测试条件查询性能
        QueryWrapper query = QueryWrapper.create()
            .select(ACCOUNT.ID, ACCOUNT.USER_NAME, ACCOUNT.AGE)
            .from(ACCOUNT)
            .where(ACCOUNT.AGE.between(25, 45))
            .and(ACCOUNT.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.AGE.desc())
            .limit(100);
        
        List<Row> results = accountMapper.selectRowsByQuery(query);
        
        long endTime = System.currentTimeMillis();
        
        System.out.printf("📊 条件查询结果: %d 条记录, 耗时: %d ms%n", 
            results.size(), endTime - startTime);
        
        assertTrue(results.size() > 0, "查询应该返回结果");
        assertTrue(endTime - startTime < 2000, "查询耗时应该小于2秒");
    }

    @Test
    void testLargeDataJoinPerformance() {
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过JOIN性能测试");
            return;
        }

        System.out.println("🔗 开始JOIN性能测试...");
        
        long startTime = System.currentTimeMillis();
        
        // 测试JOIN查询性能
        QueryWrapper query = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE, ARTICLE.CREATED_AT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.ge(30))
            .orderBy(ARTICLE.CREATED_AT.desc())
            .limit(200);
        
        List<Row> results = accountMapper.selectRowsByQuery(query);
        
        long endTime = System.currentTimeMillis();
        
        System.out.printf("📊 JOIN查询结果: %d 条记录, 耗时: %d ms%n", 
            results.size(), endTime - startTime);
        
        assertTrue(results.size() > 0, "JOIN查询应该返回结果");
        assertTrue(endTime - startTime < 3000, "JOIN查询耗时应该小于3秒");
    }

    @Test
    void testLargeDataPaginationPerformance() {
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过分页性能测试");
            return;
        }

        System.out.println("📄 开始分页性能测试...");
        
        QueryWrapper query = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .orderBy(ACCOUNT.ID.asc(), ARTICLE.ID.asc());

        long totalStartTime = System.currentTimeMillis();
        
        // 测试多页分页查询
        int pageSize = 50;
        int maxPages = 10; // 测试前10页
        
        for (int pageNum = 1; pageNum <= maxPages; pageNum++) {
            long pageStartTime = System.currentTimeMillis();
            
            Page<Row> page = accountMapper.paginateAs(pageNum, pageSize, query, Row.class);
            
            long pageEndTime = System.currentTimeMillis();
            
            System.out.printf("  第%d页: %d条记录, 耗时: %d ms%n", 
                pageNum, page.getRecords().size(), pageEndTime - pageStartTime);
            
            assertTrue(pageEndTime - pageStartTime < 1000, 
                String.format("第%d页查询耗时应该小于1秒", pageNum));
            
            // 如果没有更多数据，停止测试
            if (page.getRecords().size() < pageSize) {
                break;
            }
        }
        
        long totalEndTime = System.currentTimeMillis();
        System.out.printf("📊 分页性能测试完成，总耗时: %d ms%n", totalEndTime - totalStartTime);
        assertTrue(totalEndTime - totalStartTime < 15000, "总分页测试应该在15秒内完成");
    }

    @Test
    void testCountOptimizationPerformance() {
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过COUNT优化性能测试");
            return;
        }

        System.out.println("📊 开始COUNT优化性能测试...");
        
        QueryWrapper query = QueryWrapper.create()
            .select(ACCOUNT.USER_NAME, ARTICLE.TITLE, ARTICLE.CONTENT)
            .from(ACCOUNT)
            .innerJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
            .where(ACCOUNT.IS_DELETE.eq(0))
            .and(ARTICLE.IS_DELETE.eq(0))
            .and(ACCOUNT.AGE.between(20, 50))
            .orderBy(ACCOUNT.ID.asc());

        // 测试优化的COUNT查询
        Page<Row> optimizedPage = new Page<>(1, 100);
        optimizedPage.setOptimizeCountQuery(true);
        
        long optimizedStartTime = System.currentTimeMillis();
        Page<Row> optimizedResult = accountMapper.paginateAs(optimizedPage, query, Row.class);
        long optimizedEndTime = System.currentTimeMillis();
        
        // 测试非优化的COUNT查询
        Page<Row> normalPage = new Page<>(1, 100);
        normalPage.setOptimizeCountQuery(false);
        
        long normalStartTime = System.currentTimeMillis();
        Page<Row> normalResult = accountMapper.paginateAs(normalPage, query, Row.class);
        long normalEndTime = System.currentTimeMillis();
        
        System.out.printf("📊 COUNT查询对比:%n");
        System.out.printf("  优化版: 总记录数=%d, 耗时=%d ms%n", 
            optimizedResult.getTotalRow(), optimizedEndTime - optimizedStartTime);
        System.out.printf("  普通版: 总记录数=%d, 耗时=%d ms%n", 
            normalResult.getTotalRow(), normalEndTime - normalStartTime);
        
        assertEquals(optimizedResult.getTotalRow(), normalResult.getTotalRow(), 
            "优化和非优化的COUNT结果应该相同");
        assertTrue(optimizedEndTime - optimizedStartTime < 5000, "优化版COUNT查询应该在5秒内完成");
        assertTrue(normalEndTime - normalStartTime < 5000, "普通版COUNT查询应该在5秒内完成");
    }

    @Test 
    void performanceTestSummary() {
        if (!isPerformanceTestEnabled()) {
            System.out.println("⏭️ 跳过性能测试总结");
            return;
        }

        System.out.println("\n📈 === PostgreSQL 性能测试总结 ===");
        System.out.printf("测试规模: %s (%d 账户, ~%d 文章)%n", 
            System.getProperty("perf.scale", "small"), 
            ACCOUNT_COUNT, 
            ACCOUNT_COUNT * ARTICLES_PER_ACCOUNT);
        System.out.println("测试时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("数据库: PostgreSQL");
        System.out.println("ORM框架: MyBatis-Flex");
        
        // 查询数据库统计信息
        long accountCount = accountMapper.selectCountByQuery(
            QueryWrapper.create().from(ACCOUNT).where(ACCOUNT.IS_DELETE.eq(0)));
        long articleCount = articleMapper.selectCountByQuery(
            QueryWrapper.create().from(ARTICLE).where(ARTICLE.IS_DELETE.eq(0)));
        
        System.out.printf("实际数据量: %d 个有效账户, %d 篇有效文章%n", accountCount, articleCount);
        System.out.println("✅ 所有性能测试完成！");
    }
}