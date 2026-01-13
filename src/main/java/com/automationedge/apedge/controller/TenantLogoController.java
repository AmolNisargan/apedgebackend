package com.automationedge.apedge.controller;

import com.automationedge.apedge.service.TenantLogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tenant/logo")
public class TenantLogoController {

    private static final Logger logger = LoggerFactory.getLogger(TenantLogoController.class);

    private final TenantLogoService tenantLogoService;

    public TenantLogoController(TenantLogoService tenantLogoService) {
        this.tenantLogoService = tenantLogoService;
    }

    @PostMapping("/getlogo")
    public ResponseEntity<Map<String, Object>> uploadLogo(
            @RequestParam("tenant_id") Integer tenantId,
            @RequestPart("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        try {
            String path = tenantLogoService.uploadLogo(tenantId, file);
            response.put("message", "Logo uploaded successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading logo for tenant_id={}", tenantId, e);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/updatelogo")
    public ResponseEntity<Map<String, Object>> updateLogo(
            @RequestParam("tenant_id") Integer tenantId,
            @RequestPart("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        try {
            String path = tenantLogoService.updateLogo(tenantId, file);
            response.put("message", "Logo updated successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating logo for tenant_id={}", tenantId, e);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/deletelogo")
    public ResponseEntity<Map<String, Object>> deleteLogo(@RequestParam("tenant_id") Integer tenantId) {
        Map<String, Object> response = new HashMap<>();
        try {
            tenantLogoService.deleteLogo(tenantId);
            response.put("message", "Logo deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting logo for tenant_id={}", tenantId, e);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
