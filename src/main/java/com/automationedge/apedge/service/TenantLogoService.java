package com.automationedge.apedge.service;

import com.automationedge.apedge.entity.APTenant;
import com.automationedge.apedge.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantLogoService {

    private static final Logger logger = LoggerFactory.getLogger(TenantLogoService.class);

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    private final TenantRepository tenantRepository;

    public TenantLogoService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public String uploadLogo(Integer tenantId, MultipartFile file) throws IOException {
        logger.info("Uploading logo for tenant_id={}", tenantId);

        Optional<APTenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found for ID: " + tenantId);
        }

        // Prepare tenant directory
        Path tenantDir = Paths.get(baseUploadDir, String.valueOf(tenantId));
        if (!Files.exists(tenantDir)) {
            Files.createDirectories(tenantDir);
            logger.info("Created directory for tenant: {}", tenantDir);
        }

        // Generate unique filename
        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = "Logo_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path targetPath = tenantDir.resolve(newFileName);

        // Save file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Saved logo file: {}", targetPath);

        // Update DB
        String relativePath = Paths.get("files", String.valueOf(tenantId), newFileName).toString();
        APTenant tenant = tenantOpt.get();
        tenant.setLogoPath(relativePath);
        tenantRepository.save(tenant);
        logger.info("Updated tenant logo path in DB for tenant_id={}", tenantId);

        return relativePath;
    }

    public String updateLogo(Integer tenantId, MultipartFile file) throws IOException {
        logger.info("Updating logo for tenant_id={}", tenantId);

        Optional<APTenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found for ID: " + tenantId);
        }

        APTenant tenant = tenantOpt.get();
        deleteFileIfExists(tenant.getLogoPath());
        return uploadLogo(tenantId, file);
    }

    public void deleteLogo(Integer tenantId) {
        logger.info("Deleting logo for tenant_id={}", tenantId);

        Optional<APTenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found for ID: " + tenantId);
        }

        APTenant tenant = tenantOpt.get();
        deleteFileIfExists(tenant.getLogoPath());
        tenant.setLogoPath(null);
        tenantRepository.save(tenant);
        logger.info("Logo deleted successfully for tenant_id={}", tenantId);
    }

    private void deleteFileIfExists(String logoPath) {
        if (logoPath == null || logoPath.isEmpty()) return;

        try {
            Path basePath = Paths.get(baseUploadDir);
            Path filePath = basePath.resolveSibling(logoPath); // adjust for relative "files" folder
            File file = filePath.toFile();
            if (file.exists()) {
                if (file.delete()) {
                    logger.info("Deleted existing logo file: {}", filePath);
                } else {
                    logger.warn("Failed to delete logo file: {}", filePath);
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting logo file: {}", logoPath, e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}
