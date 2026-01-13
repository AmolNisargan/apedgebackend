package com.automationedge.apedge.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ap_masters")
public class ApMasters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long masterId;

    private Integer tenantId;
    private String uniqueKey;
    private String filePath;
    private String description;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Integer createdBy;
    private Integer updatedBy;

    // Getters and Setters
    public Long getMasterId() { return masterId; }
    public void setMasterId(Long masterId) { this.masterId = masterId; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public String getUniqueKey() { return uniqueKey; }
    public void setUniqueKey(String uniqueKey) { this.uniqueKey = uniqueKey; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}

