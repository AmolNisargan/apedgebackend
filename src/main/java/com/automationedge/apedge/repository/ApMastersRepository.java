package com.automationedge.apedge.repository;

import com.automationedge.apedge.entity.ApMasters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApMastersRepository extends JpaRepository<ApMasters, Long> {
    Optional<ApMasters> findByTenantIdAndUniqueKey(Integer tenantId, String uniqueKey);
}

