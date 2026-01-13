package com.automationedge.apedge.repository;

import com.automationedge.apedge.entity.APTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<APTenant, Integer> {
}
