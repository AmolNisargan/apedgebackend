package com.automationedge.apedge.controller;

import com.automationedge.apedge.service.ExcelSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/excel")
public class ExcelSearchController {

    private final ExcelSearchService excelSearchService;

    public ExcelSearchController(ExcelSearchService excelSearchService) {
        this.excelSearchService = excelSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchExcel(@RequestBody Map<String, String> payload) {
        Integer tenantId = Integer.parseInt(payload.get("tenant_id"));
        String uniqueKey = payload.get("unique_key");
        String fieldName = payload.get("field_name");
        String searchString = payload.get("search_string");

        Map<String, Object> result = excelSearchService.searchInExcel(tenantId, uniqueKey, fieldName, searchString);
        return ResponseEntity.ok(result);
    }
}

