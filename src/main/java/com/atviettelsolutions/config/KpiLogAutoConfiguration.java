package com.atviettelsolutions.config;

import com.atviettelsolutions.services.KpiLogRepository;
import com.atviettelsolutions.services.KpiLogService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties({ ApplicationInfo.class })
@Slf4j
public class KpiLogAutoConfiguration {
    @Bean
    public KpiLogService kpiLogService(KpiLogRepository kpiLogRepository) {
        return new KpiLogService(kpiLogRepository);
    }

    @Bean
    @ConditionalOnMissingBean(KpiLogRepository.class)
    public KpiLogRepository defaultKpiLogRepository(Gson gson) {
        return kpiLog -> {
            log.info(gson.toJson(kpiLog));
            return Mono.empty();
        };
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }
}
