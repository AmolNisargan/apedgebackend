package com.automationedge.apedge.dto;

import lombok.Data;
import java.util.Map;

@Data
public class MailRequest {
    private String to;
    private String subject;
    private String templateName;  // e.g. "welcome-email"
    private Map<String, Object> variables; // dynamic data
}
