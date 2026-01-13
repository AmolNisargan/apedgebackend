package com.automationedge.apedge.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadFileController {

    private static final Logger log = LoggerFactory.getLogger(UploadFileController.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final JdbcTemplate jdbcTemplate;

    public UploadFileController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/uploaddoc")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tenant_id") String tenantId,
            @RequestParam("user_id") String userId,
            @RequestParam("doc_type") String docType,
            @RequestParam("input_source") String inputSource) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("File upload request received. tenant_id={}, user_id={}, doc_type={}, input_source={}, originalFilename={}",
                    tenantId, userId, docType, inputSource, file.getOriginalFilename());

            // Create tenant-specific folder inside uploadDir
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
                extension = originalFilename.substring(dotIndex); // includes "."
            }

            // Generate unique filename with original name + UUID
            String uniqueFilename = baseName + "_" + UUID.randomUUID().toString() + extension;

            // Full path to save file
            String filePath = tenantFolder.getAbsolutePath() + File.separator + uniqueFilename;
            file.transferTo(new File(filePath));
            log.info("File successfully uploaded. Saved at {}", filePath);

            // Build relative path for response and DB
            String folderName = Paths.get(uploadDir).getFileName().toString();
            String relativePath = folderName + "/" + tenantId + "/" + uniqueFilename;

            // Insert record into ap_documents
            String sql = "INSERT INTO ap_documents (tenant_id, user_id, file_path, status, stage, doc_type, input_source, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING doc_id";

            Integer docId = jdbcTemplate.queryForObject(sql, new Object[]{
                    Integer.valueOf(tenantId),
                    Integer.valueOf(userId),
                    relativePath.replace("/", "\\"),
                    "Processing",    // default status
                    "New",           // default stage
                    docType,
                    inputSource,
                    Integer.valueOf(userId)
            }, Integer.class);

            log.info("Document record inserted in DB. doc_id={}, tenant_id={}, user_id={}, stored_filename={}",
                    docId, tenantId, userId, uniqueFilename);

            // Prepare response
            response.put("status", "success");
            response.put("filepath", relativePath.replace("/", "\\"));
            response.put("stored_filename", uniqueFilename);
            response.put("tenant_id", tenantId);
            response.put("doc_id", docId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File upload failed for tenant_id={} filename={}. Error: {}", tenantId, file.getOriginalFilename(), e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("DB insert failed for tenant_id={} user_id={} filename={}. Error: {}", tenantId, userId, file.getOriginalFilename(), e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Database insert failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
