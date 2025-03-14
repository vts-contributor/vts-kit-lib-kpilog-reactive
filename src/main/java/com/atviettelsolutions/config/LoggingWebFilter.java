package com.atviettelsolutions.config;

import com.atviettelsolutions.domain.KpiLog;
import com.atviettelsolutions.services.KpiLogService;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
public class LoggingWebFilter implements WebFilter {
    private final KpiLogService kpiLogService;
    private final Gson gson;
    private final Log logger = LogFactory.getLog(LoggingWebFilter.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private ApplicationInfo applicationInfo;
    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();
        StringBuilder requestBody = new StringBuilder();
        StringBuilder sessionId = new StringBuilder();
        StringBuilder username = new StringBuilder();
        StringBuilder account = new StringBuilder();
        var startTime = LocalDateTime.now();
        Mono<Void> prepareMono = Mono.zip(
                exchange
                        .getSession()
                        .doOnNext(session -> {
                            sessionId.append(session.getId());
                        }),
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .map(Authentication::getName)
                        .defaultIfEmpty("[anonymous]")
                        .doOnNext(username::append)
                        .doOnNext(account::append))
                .then();
        KpiLog kpiLog = new KpiLog();
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(originalRequest) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody()
                        .doOnNext(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            dataBuffer.readPosition(0);
                            String bodyContent = new String(content, StandardCharsets.UTF_8);
                            requestBody.append(bodyContent);
                        });
            }
        };
        StringBuilder responseBody = new StringBuilder();
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(
                        Flux.from(body).doOnNext(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            dataBuffer.readPosition(0);
                            String bodyContent = new String(content, StandardCharsets.UTF_8);
                            responseBody.append(bodyContent);
                            log(
                                    requestBody.toString(),
                                    responseBody.toString(),
                                    sessionId.toString(),
                                    username.toString(),
                                    account.toString(),
                                    originalRequest,
                                    originalResponse,
                                    startTime).subscribe();
                        }));
            }
        };
        ServerWebExchange serverWebExchange = exchange.mutate().request(requestDecorator).response(responseDecorator)
                .build();
        return prepareMono.then(chain.filter(serverWebExchange));
    }

    private Mono<Void> log(
            String requestBody,
            String responseBody,
            String sessionId,
            String username,
            String account,
            ServerHttpRequest request,
            ServerHttpResponse response,
            LocalDateTime startTime) {
        KpiLog kpiLog = new KpiLog();
        kpiLog.setApplicationCode(applicationInfo.getApplicationCode());
        kpiLog.setServiceCode(applicationInfo.getServiceCode());
        kpiLog.setAccount(account);
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("The host name could not be determined, using `localhost` as fallback");
        }
        kpiLog.setIpPortParentNode(hostAddress);
        kpiLog.setIpPortCurrentNode(getClientIpAddress(request));
        kpiLog.setRequestContent(gson.toJson(requestBody));
        kpiLog.setResponseContent(gson.toJson(responseBody));
        kpiLog.setSessionId(sessionId);
        kpiLog.setUsername(username);
        kpiLog.setStartTime(startTime.format(formatter));
        var endTime = LocalDateTime.now();
        var duration = Duration.between(startTime, endTime).toMillis();
        kpiLog.setEndTime(endTime.toString());
        kpiLog.setDuration(String.valueOf(duration));
        if (response.getStatusCode() != HttpStatus.OK) {
            kpiLog.setErrorCode(String.valueOf(response.getStatusCode()));
            kpiLog.setErrorDescription(responseBody);
        }
        return kpiLogService.save(kpiLog);
    }

    private String getClientIpAddress(ServerHttpRequest request) {
        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeaders().getFirst(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getHostString();
        }
        return "[unknown]";
    }
}