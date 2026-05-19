package com.suios.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI adminOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("School Uniform Admin API")
                        .version("1.0.0")
                        .description("校服智能订购管理后台接口文档")
                        .contact(new Contact()
                                .name("SUIOS Admin")
                                .url("https://localhost")))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }

    @Bean
    public OpenApiCustomizer adminSecurityOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                if (isPublicOperation(path, httpMethod)) {
                    return;
                }
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
            }));
        };
    }

    private boolean isPublicOperation(String path, PathItem.HttpMethod method) {
        return "/auth/login".equals(path) && PathItem.HttpMethod.POST.equals(method);
    }
}
