package com.atviettelsolutions.config;

import io.grpc.*;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.util.ArrayList;
import java.util.List;

import static com.atviettelsolutions.config.GrpcContext.JWT_KEY;

@AllArgsConstructor
@Order(1)
public class JwtReactiveGrpcInterceptor implements ServerInterceptor {
    private final ReactiveJwtDecoder reactiveJwtDecoder;
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        String authHeader = headers.get(authKey);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return next.startCall(call, headers);
        }
        String token = authHeader.substring(7);
        DelayedListener<ReqT> delayedListener = new DelayedListener<>();
        reactiveJwtDecoder.decode(token)
                .subscribe(
                        jwt -> {
                        Context ctx = Context.current().withValue(JWT_KEY, jwt);
                        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctx, call, headers, next);
                        delayedListener.setDelegate(delegate);
                        },
                        ex -> {
                            call.close(Status.UNAUTHENTICATED.withDescription("Token validation failed: " + ex.getMessage()), headers);
                        });
        return delayedListener;
    }
    private static class DelayedListener<ReqT> extends ServerCall.Listener<ReqT> {

        private ServerCall.Listener<ReqT> delegate;
        private final List<Runnable> events = new ArrayList<>();

        @Override
        public synchronized void onMessage(ReqT message) {
            if (delegate == null) {
                events.add(() -> delegate.onMessage(message));
            } else {
                delegate.onMessage(message);
            }
        }

        @Override
        public synchronized void onHalfClose() {
            if (delegate == null) {
                events.add(delegate::onHalfClose);
            } else {
                delegate.onHalfClose();
            }
        }

        @Override
        public synchronized void onCancel() {
            if (delegate == null) {
                events.add(delegate::onCancel);
            } else {
                delegate.onCancel();
            }
        }

        @Override
        public synchronized void onComplete() {
            if (delegate == null) {
                events.add(delegate::onComplete);
            } else {
                delegate.onComplete();
            }
        }

        @Override
        public synchronized void onReady() {
            if (delegate == null) {
                events.add(delegate::onReady);
            } else {
                delegate.onReady();
            }
        }

        public synchronized void setDelegate(ServerCall.Listener<ReqT> delegate) {
            this.delegate = delegate;
            for (Runnable event : events) {
                event.run();
            }
            events.clear();
        }
    }
}
