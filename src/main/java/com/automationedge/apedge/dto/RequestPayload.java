package com.automationedge.apedge.dto;

import lombok.Data;

@Data
public class RequestPayload {
    private Integer userId;
    private Integer tenantId;
    private Integer docId;
}
