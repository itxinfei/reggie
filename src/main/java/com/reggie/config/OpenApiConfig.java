package com.reggie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI/Swagger 配置类
 * 提供API文档的详细信息
 *
 * @author reggie
 * @since 2026-07-09
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档信息
     * 包括API标题、描述、版本、联系人和服务器信息
     *
     * @return OpenAPI配置对象
     */
    @Bean
    public OpenAPI reggieOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("瑞吉外卖 API 文档")
                        .description("瑞吉外卖 - 搭载 AI 智能调度的餐饮全栈系统 API 文档\n\n" +
                                "## 项目介绍\n" +
                                "瑞吉外卖是一个完整的餐饮外卖管理系统，专为餐饮企业提供外卖订单管理解决方案。\n" +
                                "项目采用前后端分离架构，后端基于 Spring Boot 2.4.5 + MyBatis Plus 3.4.2 构建。\n\n" +
                                "## 主要功能\n" +
                                "- 员工管理：登录/退出/CRUD，Session 会话管理、账号锁定\n" +
                                "- 分类管理：菜品分类、套餐分类\n" +
                                "- 菜品管理：菜品信息、图片上传、口味管理\n" +
                                "- 套餐管理：套餐信息、套餐详情、起售/停售\n" +
                                "- 订单管理：分页查询、状态流转、订单明细\n" +
                                "- 购物车：增加/减少数量、清空购物车\n" +
                                "- 地址管理：收货地址增删改查、默认地址\n" +
                                "- AI 智能助手：智能点餐推荐、菜品描述生成、经营数据分析\n\n" +
                                "## 认证方式\n" +
                                "本系统使用 Session 认证，登录后通过 Cookie 保持会话。\n\n" +
                                "## 错误码说明\n" +
                                "| 错误码 | 说明 |\n" +
                                "|--------|------|\n" +
                                "| 1 | 操作成功 |\n" +
                                "| 0 | 操作失败 |\n" +
                                "| 50001 | 用户名或密码错误 |\n" +
                                "| 50002 | 用户已禁用 |\n" +
                                "| 50003 | 用户被锁定 |\n" +
                                "| 50004 | 未登录 |\n" +
                                "| 50005 | 无权限 |\n" +
                                "| 50006 | 资源不存在 |")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("itxinfei")
                                .email("747011882@qq.com")
                                .url("https://gitee.com/itxinfei"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发环境")));
    }
}