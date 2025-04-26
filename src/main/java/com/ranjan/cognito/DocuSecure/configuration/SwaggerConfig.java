package com.ranjan.cognito.DocuSecure.configuration;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;




@Configuration
@SecurityScheme(
    name = "bearerAuth", 
    type = SecuritySchemeType.HTTP, 
    scheme = "bearer", 
    bearerFormat = "JWT"
)
public class SwaggerConfig {
    
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocuSecure API")
                        .description("API for secure Document Storage")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Ranjanabha Bhattacharya")
                                .url("https://ranjanabha.com")
                                .email("ranjanabha@gmail.com")
                        )

                )
                .externalDocs(new ExternalDocumentation()
                        .description("More API Documentation")
                        .url("https://ranjanabha-docs-url.com"));
    }

   
}
