package com.atviettelsolutions.config;

import io.grpc.Context;
import org.springframework.security.oauth2.jwt.Jwt;

public class GrpcContext {
    public static final Context.Key<Jwt> JWT_KEY = Context.key("jwt");
}
