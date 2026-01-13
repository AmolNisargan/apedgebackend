package com.automationedge.apedge.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoginAuditJdbcRepository {

    private static final Logger log =
            LoggerFactory.getLogger(LoginAuditJdbcRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INSERT_AUDIT =
            """
            INSERT INTO ap_user_login_audit
            (user_id, tenant_id, login_status, ip_address, user_agent, failure_reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    public void save(Integer userId,
                     Integer tenantId,
                     String status,
                     String ip,
                     String userAgent,
                     String reason) {

        jdbcTemplate.update(
                INSERT_AUDIT,
                userId,
                tenantId,
                status,
                ip,
                userAgent,
                reason
        );

        log.info("LOGIN_AUDIT | userId={} tenantId={} status={} ip={} reason={}",
                userId, tenantId, status, ip, reason);
    }
}
