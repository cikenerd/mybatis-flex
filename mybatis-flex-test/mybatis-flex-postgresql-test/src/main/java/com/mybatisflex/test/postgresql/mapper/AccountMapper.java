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
package com.mybatisflex.test.postgresql.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.test.postgresql.model.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户 Mapper 接口 - PostgreSQL 版本
 *
 * @author kk
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    // 这里继承了 BaseMapper，已经包含了所有基本的 CRUD 方法
    // 可以添加自定义的查询方法

}