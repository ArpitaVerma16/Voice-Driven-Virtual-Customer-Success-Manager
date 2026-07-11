package com.vcsm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.annotation.Bean;
import org.springframework.context.annotation.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("VCSM API - Voice Driven Virtual Customer Success Manager")
                .version("1.0.0")
                .description("""
                    REST API for the Voice-Driven Virtual Customer Success Manager application.
                    
                    Features:
                    - Voice command processing with sentiment analysis
                    - Complaint management (CRUD operations)
                    - Event management with registration
                    - Voice biometrics authentication
                    - Hindi/English multi-language support
                    """)
                .contact(new Contact()
                    .name("Bhakkti Gautam")
                    .email("bhakktigautam@gmail.com")
                    .url("https://github.com/BhakktiGautam"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url(System.getenv().getOrDefault("API_URL", "https://api.production.com")).description("Local Development Server"),
                new Server().url("https://vcsm.onrender.com").description("Production Server")
            ));
    }
}