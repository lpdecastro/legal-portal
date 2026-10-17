package com.liandrejohn.legalportal.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API metadata shown in the OpenAPI document ({@code /v3/api-docs}) and the Scalar UI ({@code /scalar}).
 *
 * @param title API title
 * @param version API version
 */
@ConfigurationProperties("app.openapi")
public record OpenApiProperties(
        @DefaultValue("Legal Portal API") String title,
        @DefaultValue("v1") String version) {
}
