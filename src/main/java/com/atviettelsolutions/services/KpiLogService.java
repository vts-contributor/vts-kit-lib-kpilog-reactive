package com.atviettelsolutions.services;

import com.atviettelsolutions.domain.KpiLog;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class KpiLogService {
    private final KpiLogRepository kpiLogRepository;
    public Mono<Void> save(KpiLog kpiLog) {
        return kpiLogRepository.writeLog(kpiLog);
    }
}
