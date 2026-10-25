package com.teamflow.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI teamFlowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TeamFlow 项目与任务接口")
                .version("demo-1.0")
                .description("实训基础演示：项目查询/创建、任务查询/创建/状态流转。尚未接入登录，创建者由请求传递。"));
    }
}
