package com.atviettelsolutions.services;

import com.atviettelsolutions.domain.KpiLog;
import reactor.core.publisher.Mono;

public interface KpiLogRepository {
    Mono<Void> writeLog(KpiLog kpiLog);
}
