package com.automationedge.apedge.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadMasterController {

    private static final Logger log = LoggerFactory.getLogger(UploadMasterController.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/uploadmaster")
    public ResponseEntity<Map<String, Object>> uploadMaster(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tenant_id") String tenantId,
            @RequestParam("user_id") String userId,
            @RequestParam("unique_key") String unique_key,
            @RequestParam("desc") String description, 
            @RequestParam("metadata") String metadata ) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("AP Master upload request received. tenant_id={}, user_id={}, unique_key={}, filename={}",
                    tenantId, userId, unique_key, file.getOriginalFilename());

            // Create tenant-specific folder if not exists
            File tenantFolder = new File(uploadDir + File.separator + tenantId);
            if (!tenantFolder.exists()) {
                tenantFolder.mkdirs();
                log.debug("Created tenant folder: {}", tenantFolder.getAbsolutePath());
            }

            // Extract original filename
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename;
            String extension = "";

            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex != -1) {
                baseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex);
            }

            // Generate unique filename
            String uniqueFilename = baseName + "_" + UUID.randomUUID() + extension;

            // Full path to save file
            String filePath = tenantFolder.getAbsolutePath() + File.separator + uniqueFilename;
            file.transferTo(new File(filePath));
            log.info("AP Master file successfully uploaded. Saved at {}", filePath);

            // Relative path for DB
            String folderName = Paths.get(uploadDir).getFileName().toString();
            String relativePath = folderName + File.separator + tenantId + File.separator + uniqueFilename;

            // Insert into ap_masters table
            String sql = "INSERT INTO ap_masters (tenant_id, unique_key, file_path, description, metadata, created_by) " +
                         "VALUES (?, ?, ?, ?, ?, ?) RETURNING master_id";

            Integer masterId = jdbcTemplate.queryForObject(
                    sql,
                    new Object[]{
                            Integer.parseInt(tenantId),
                            unique_key,
                            relativePath,
                            description,
                            metadata,
                            Integer.parseInt(userId)
                    },
                    Integer.class
            );

            // Build response
            response.put("status", "success");
            response.put("master_id", masterId);
            response.put("filepath", relativePath);
            response.put("stored_filename", uniqueFilename);
            response.put("tenant_id", tenantId);
            response.put("unique_key", unique_key);
            response.put("metadata", metadata);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File upload failed. tenant_id={} unique_key={} error={}", tenantId, unique_key, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            log.error("DB insert failed. tenant_id={} unique_key={} error={}", tenantId, unique_key, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Database insert failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
