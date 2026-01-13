package com.automationedge.apedge.service;

import com.automationedge.apedge.entity.ApMasters;
import com.automationedge.apedge.repository.ApMastersRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
public class ExcelSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelSearchService.class);

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    private final ApMastersRepository apMastersRepository;

    public ExcelSearchService(ApMastersRepository apMastersRepository) {
        this.apMastersRepository = apMastersRepository;
    }

    public Map<String, Object> searchInExcel(Integer tenantId, String uniqueKey, String fieldName, String searchString) {
        Map<String, Object> response = new HashMap<>();

        logger.info("🔍 Starting Excel search for tenant_id={} | unique_key={} | field={} | search='{}'",
                tenantId, uniqueKey, fieldName, searchString);

        try {
            // 1️⃣ Find file path by tenantId and uniqueKey
            logger.debug("Fetching file path from database for tenant_id={} and unique_key={}", tenantId, uniqueKey);

            ApMasters master = apMastersRepository
                    .findByTenantIdAndUniqueKey(tenantId, uniqueKey)
                    .orElseThrow(() -> new RuntimeException(
                            "No record found for tenant_id=" + tenantId + " and unique_key=" + uniqueKey));

            String dbFilePath = master.getFilePath();
            logger.info("Found DB file path: {}", dbFilePath);

            // 2️⃣ Build actual file path safely (avoid double /files/)
            File excelFile;
            if (dbFilePath.startsWith(baseUploadDir)) {
                excelFile = new File(dbFilePath);
            } else {
                excelFile = new File(baseUploadDir, dbFilePath.replaceFirst(".*files[/\\\\]", ""));
            }

            logger.debug("Resolved full Excel file path: {}", excelFile.getAbsolutePath());

            if (!excelFile.exists()) {
                logger.error("❌ File not found at path: {}", excelFile.getAbsolutePath());
                throw new RuntimeException("File not found: " + excelFile.getAbsolutePath());
            }

            // 3️⃣ Read Excel
            List<String> matchingValues = new ArrayList<>();

            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    logger.error("❌ No sheet found in Excel file");
                    throw new RuntimeException("No sheet found in Excel");
                }

                logger.info("✅ Opened Excel file successfully, sheet name: {}", sheet.getSheetName());

                // Header row
                Row headerRow = sheet.getRow(0);
                int targetColumn = -1;
                for (Cell cell : headerRow) {
                    if (cell.getStringCellValue().trim().equalsIgnoreCase(fieldName.trim())) {
                        targetColumn = cell.getColumnIndex();
                        logger.debug("Found target column '{}' at index {}", fieldName, targetColumn);
                        break;
                    }
                }

                if (targetColumn == -1) {
                    logger.error("❌ Field name '{}' not found in Excel header", fieldName);
                    throw new RuntimeException("Field name '" + fieldName + "' not found in Excel");
                }

                // 4️⃣ Loop rows and match
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Cell cell = row.getCell(targetColumn);
                    if (cell == null) continue;

                    String cellValue = cell.toString().trim();

                    if (cellValue.toLowerCase().contains(searchString.toLowerCase())) {
                        matchingValues.add(cellValue);
                        logger.trace("Matched row {} → {}", i, cellValue);
                    }
                }
            }

            // ✅ Success response
            response.put("success", true);
            response.put("count", matchingValues.size());
            response.put("data", matchingValues);

            logger.info("✅ Search completed. {} matching values found for field '{}'", matchingValues.size(), fieldName);

        } catch (Exception e) {
            logger.error("❌ Error while processing Excel search: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}
