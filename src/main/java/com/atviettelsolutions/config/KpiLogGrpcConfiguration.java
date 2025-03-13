package com.atviettelsolutions.config;

import com.atviettelsolutions.services.KpiLogService;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

public class KpiLogGrpcConfiguration {
    @Bean
    @GrpcGlobalServerInterceptor
    @Order(1)
    public ServerInterceptor authInterceptor(ReactiveJwtDecoder jwtDecoder) {
        return new JwtReactiveGrpcInterceptor(jwtDecoder);
    }

    @Bean
    @GrpcGlobalServerInterceptor
    @Order(2)
    public ServerInterceptor loggingInterceptor(ApplicationInfo applicationInfo, KpiLogService kpiLogService) {
        return new LoggingGrpcInterceptor(applicationInfo, kpiLogService);
    }
}
