package com.automationedge.apedge.controller;

import com.automationedge.apedge.dto.FieldConfigResponse;
import com.automationedge.apedge.dto.RequestPayload;
import com.automationedge.apedge.service.FieldConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/field-config")
public class FieldConfigController {

    private final FieldConfigService service;

    public FieldConfigController(FieldConfigService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public FieldConfigResponse generateConfig(@RequestBody RequestPayload payload) {
        return service.buildFieldConfigJson(payload.getTenantId());
    }
}
