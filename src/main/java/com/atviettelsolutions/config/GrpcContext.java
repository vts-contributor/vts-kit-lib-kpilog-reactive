package com.atviettelsolutions.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.annotation.RequestScope;

import java.util.concurrent.locks.ReentrantLock;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RequestScope
public class GrpcContext {
    @Getter
    private Jwt jwt;
    private final ReentrantLock lock = new ReentrantLock();

    public void setJwt(Jwt jwt) {
        lock.lock();
        try {
            this.jwt = jwt;
        } finally {
            lock.unlock();
        }
    }
}
