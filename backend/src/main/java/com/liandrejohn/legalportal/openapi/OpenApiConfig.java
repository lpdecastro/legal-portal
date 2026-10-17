package com.liandrejohn.legalportal.openapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Springdoc builds the document from the controllers; this only sets its metadata. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenApiProperties.class)
class OpenApiConfig {

    @Bean
    OpenAPI openApi(OpenApiProperties properties) {
        return new OpenAPI().info(new Info().title(properties.title()).version(properties.version()));
    }

}
