package com.automationedge.apedge.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class UpdateUploadMasterController {

    private static final Logger log = LoggerFactory.getLogger(UploadMasterController.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ----------------- UPDATE MASTER FILE -----------------
    @PutMapping("/updateuploadmaster/{masterId}")
    public ResponseEntity<Map<String, Object>> updateMaster(
            @PathVariable("masterId") Integer masterId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tenant_id") String tenantId,
            @RequestParam("user_id") String userId,
            @RequestParam("desc") String description,
            @RequestParam("metadata") String metadata 
            ) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("AP Master update request. master_id={}, tenant_id={}, user_id={}", masterId, tenantId, userId);

            // -------- Fetch existing record safely --------
            String fetchSql = "SELECT file_path FROM ap_masters WHERE master_id = ? AND tenant_id = ?";
            String oldFilePath = null;
            try {
                oldFilePath = jdbcTemplate.queryForObject(
                        fetchSql,
                        new Object[]{masterId, Integer.parseInt(tenantId)},
                        String.class
                );
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                log.warn("No record found for master_id={} and tenant_id={}", masterId, tenantId);
                response.put("status", "error");
                response.put("message", "Record not found for given master_id and tenant_id");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // -------- Delete old file if exists --------
            if (oldFilePath != null) {
                // Normalize path (remove leading "files\" if present)
                String normalizedPath = oldFilePath.replaceFirst("^files[\\\\/]", "");

                // Build absolute file path
                File oldFile = Paths.get(uploadDir, normalizedPath).toFile();

                if (oldFile.exists()) {
                    if (oldFile.delete()) {
                        log.info("Old file deleted: {}", oldFile.getAbsolutePath());
                    } else {
                        log.warn("Failed to delete old file: {}", oldFile.getAbsolutePath());
                    }
                } else {
                    log.warn("Old file not found on disk: {}", oldFile.getAbsolutePath());
                }
            }

            // -------- Create tenant-specific folder --------
            File tenantFolder = new File(uploadDir + File.separator + tenantId);
            if (!tenantFolder.exists()) {
                tenantFolder.mkdirs();
                log.debug("Created tenant folder: {}", tenantFolder.getAbsolutePath());
            }

            // -------- Generate new filename --------
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename;
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex != -1) {
                baseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex);
            }
            String uniqueFilename = baseName + "_" + UUID.randomUUID() + extension;

            // -------- Save new file --------
            String newFilePath = tenantFolder.getAbsolutePath() + File.separator + uniqueFilename;
            file.transferTo(new File(newFilePath));
            log.info("New file uploaded: {}", newFilePath);

            // -------- Relative path for DB --------
            String folderName = Paths.get(uploadDir).getFileName().toString();
            String relativePath = folderName + File.separator + tenantId + File.separator + uniqueFilename;

            // -------- Update DB record --------
            String updateSql = "UPDATE ap_masters " +
                    "SET file_path = ?, description = ?, metadata = ?, updated_by = ?, updated_at = NOW() " +
                    "WHERE master_id = ? AND tenant_id = ?";

            int rows = jdbcTemplate.update(updateSql,
                    relativePath,
                    description,
                    metadata,
                    Integer.parseInt(userId),
                    masterId,
                    Integer.parseInt(tenantId));

            if (rows > 0) {
                response.put("status", "updated");
                response.put("master_id", masterId);
                response.put("tenant_id", tenantId);
                response.put("filepath", relativePath);
                response.put("stored_filename", uniqueFilename);
                response.put("description", description);
                response.put("metadata", metadata);
            } else {
                response.put("status", "error");
                response.put("message", "Update failed: No rows affected");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File update failed. master_id={} error={}", masterId, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "File update failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            log.error("DB update failed. master_id={} error={}", masterId, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Database update failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
