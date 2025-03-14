package com.atviettelsolutions.config;

import com.atviettelsolutions.services.KpiLogService;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;

public class KpiLogRestConfiguration {
    @Bean
    public LoggingWebFilter loggingWebFilter(KpiLogService kpiLogService, Gson gson, ApplicationInfo applicationInfo) {
        return new LoggingWebFilter(kpiLogService, gson, applicationInfo);
    }
}
