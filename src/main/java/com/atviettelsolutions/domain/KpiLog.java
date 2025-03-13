package com.atviettelsolutions.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiLog {

    private String applicationCode;
    private String serviceCode;
    private String sessionId;
    private String ipPortParentNode;
    private String ipPortCurrentNode;
    private String requestContent;
    private String responseContent;
    private String startTime;
    private String endTime;
    private String duration;
    private String errorCode;
    private String errorDescription;
    private Integer transactionStatus;
    private String actionName;
    private String username;
    private String account;
}