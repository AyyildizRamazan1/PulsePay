package com.kurumsal.wallet_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PulsePay Wallet API")
                        .description("Kurumsal e-cüzdan sistemi için RESTful API — kullanıcı, cüzdan ve işlem yönetimi.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Ramazan Architecture Team")));
    }
}
