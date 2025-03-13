package com.atviettelsolutions.config;

import com.atviettelsolutions.services.KpiLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(KpiLogAutoConfiguration.class)
public class KpiLogRestConfiguration {
    @Bean
    public LoggingWebFilter loggingWebFilter(ApplicationInfo applicationInfo, KpiLogService kpiLogService) {
        return new LoggingWebFilter(kpiLogService, applicationInfo);
    }
}
