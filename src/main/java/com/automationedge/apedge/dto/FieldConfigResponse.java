package com.automationedge.apedge.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FieldConfigResponse {
    private List<FieldDto> fields = new ArrayList<>();
    private List<FieldDto> lineItems = new ArrayList<>();
}
