package com.automationedge.apedge.service;

import com.automationedge.apedge.dto.FieldConfigResponse;
import com.automationedge.apedge.dto.FieldDto;
import com.automationedge.apedge.entity.FieldConfig;
import com.automationedge.apedge.repository.FieldConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FieldConfigService {

    private final FieldConfigRepository repository;

    public FieldConfigService(FieldConfigRepository repository) {
        this.repository = repository;
    }

    public FieldConfigResponse buildFieldConfigJson(Integer tenantId) {
        List<FieldConfig> configs = repository.findByTenantIdAndUseLlm(tenantId, "true");

        FieldConfigResponse response = new FieldConfigResponse();

        for (FieldConfig config : configs) {
            FieldDto dto = new FieldDto(config.getFieldName(), config.getFieldType(), config.getDescription());

            if ("LineItems".equalsIgnoreCase(config.getParent())) {
                response.getLineItems().add(dto);
            } else {
                response.getFields().add(dto);
            }
        }

        return response;
    }
}
