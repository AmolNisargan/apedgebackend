package com.automationedge.apedge.repository;

import com.automationedge.apedge.entity.FieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldConfigRepository extends JpaRepository<FieldConfig, Long> {
    List<FieldConfig> findByTenantIdAndUseLlm(Integer tenantId, String useLlm);
}
