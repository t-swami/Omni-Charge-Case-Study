package com.omnicharge.recharge_service.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Enter your JWT token. Obtain it from POST /api/auth/login"
)
public class SwaggerConfig {

    @Value("${swagger.gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Value("${swagger.direct.url:http://localhost:8083}")
    private String directUrl;

    @Bean
    public OpenAPI rechargeServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OmniCharge - Recharge Service API")
                .version("1.0")
                .description("Handles recharge initiation, status tracking, and history.")
                .contact(new Contact().name("OmniCharge Team").email("support@omnicharge.com")))
            .addServersItem(new Server().url(gatewayUrl).description("Via API Gateway (Docker/Local)"))
            .addServersItem(new Server().url(directUrl).description("Direct - Recharge Service"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}