package com.atviettelsolutions.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationInfo {
    @Value("${app.application.code:N/A}")
    private String applicationCode;
    @Value("${app.service.code:N/A}")
    private String serviceCode;
}
