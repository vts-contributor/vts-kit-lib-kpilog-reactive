package com.atviettelsolutions.config;

import com.atviettelsolutions.domain.KpiLog;
import com.atviettelsolutions.services.KpiLogService;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class LoggingGrpcInterceptor implements ServerInterceptor {
    private final ApplicationInfo applicationInfo;
    private final KpiLogService kpiLogService;
    private final GrpcContext grpcContext;
    private final Gson gson;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer()
            .includingDefaultValueFields()
            .preservingProtoFieldNames();
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
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        KpiLog kpiLog = new KpiLog();
        kpiLog.setApplicationCode(applicationInfo.getApplicationCode());
        kpiLog.setServiceCode(applicationInfo.getServiceCode());
        kpiLog.setActionName(extractActionName(fullMethodName));
        kpiLog.setSessionId(UUID.randomUUID().toString());
        kpiLog.setIpPortParentNode(extractClientIp(headers));
        var startTime = LocalDateTime.now();
        kpiLog.setStartTime(startTime.format(formatter));
        String hostAddress = "localhost";
        List<String> responseContents = new ArrayList<>();
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        kpiLog.setIpPortCurrentNode(hostAddress);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void sendMessage(RespT response) {
                        var endTime = LocalDateTime.now();
                        var duration = Duration.between(startTime, endTime).toMillis();
                        kpiLog.setEndTime(endTime.format(formatter));
                        kpiLog.setDuration(String.valueOf(duration));
                        responseContents.add(messageToJson(response));
                        kpiLog.setTransactionStatus(1);
                        super.sendMessage(response);
                    }

                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (!status.isOk()) {
                            var endTime = LocalDateTime.now();
                            var duration = Duration.between(startTime, endTime).toMillis();
                            kpiLog.setEndTime(endTime.format(formatter));
                            kpiLog.setDuration(String.valueOf(duration));
                            kpiLog.setErrorCode(status.getCode().toString());
                            kpiLog.setErrorDescription(status.getDescription());
                            kpiLog.setTransactionStatus(0);
                        }
                        Jwt jwt = grpcContext.getJwt();
                        if (jwt != null) {
                            kpiLog.setUsername(extractUserName(jwt));
                            kpiLog.setAccount(extractAccount(jwt));
                        }
                        kpiLog.setResponseContent(gson.toJson(responseContents));
                        grpcContext.setJwt(null);
                        kpiLogService.save(kpiLog).subscribe();
                        super.close(status, trailers);
                    }
                }, headers)) {
            @Override
            public void onMessage(ReqT request) {
                kpiLog.setRequestContent(gson.toJson(messageToJson(request)));
                super.onMessage(request);
            }

            @Override
            public void onCancel() {
                var endTime = LocalDateTime.now();
                var duration = Duration.between(startTime, endTime).toMillis();
                kpiLog.setEndTime(endTime.format(formatter));
                kpiLog.setDuration(String.valueOf(duration));
                kpiLog.setErrorCode("CANCELLED");
                kpiLog.setErrorDescription("Request cancelled by client");
                kpiLog.setTransactionStatus(0);
                super.onCancel();
            }
        };
    }

    private String extractActionName(String fullMethodName) {
        int slashIndex = fullMethodName.indexOf('/');
        if (slashIndex > 0) {
            return fullMethodName.substring(slashIndex + 1);
        }
        return fullMethodName;
    }

    private String extractClientIp(Metadata headers) {
        for (String header : HEADERS_TO_TRY) {
            Metadata.Key<String> headerKey = Metadata.Key.of(header.toLowerCase(), Metadata.ASCII_STRING_MARSHALLER);
            String ipAddress = headers.get(headerKey);
            if (ipAddress != null)
                return ipAddress;
        }
        return "[unknown]";
    }

    private String extractUserName(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_name");
        return username != null ? username : jwt.getSubject();
    }

    private String extractAccount(Jwt jwt) {
        return jwt.getSubject();
    }

    private String messageToJson(Object message) {
        try {
            if (message instanceof MessageOrBuilder) {
                return jsonPrinter.print((MessageOrBuilder) message);
            } else
                return message.toString();
        } catch (InvalidProtocolBufferException e) {
            log.warn("Failed to convert message to JSON", e);
            return "{\"error\": \"Failed to convert message to JSON\"}";
        }
    }
}
