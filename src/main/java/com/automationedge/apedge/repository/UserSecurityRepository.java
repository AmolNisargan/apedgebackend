package com.automationedge.apedge.repository;

import com.automationedge.apedge.dto.UserSecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class UserSecurityRepository {

    private static final Logger log = LoggerFactory.getLogger(UserSecurityRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public UserSecurityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserSecurityDTO> findByEmail(String email) {
        String sql = """
            SELECT id, email, user_password, user_role, tenant_id, is_active,
                   failed_login_attempts, last_failed_login, account_locked_until
            FROM ap_users
            WHERE email = ?
        """;

        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(sql, this::mapRow, email)
            );
        } catch (Exception e) {
            log.warn("USER_LOOKUP_FAILED | email={}", email);
            return Optional.empty();
        }
    }

    public void updateFailure(Integer userId,
                              int attempts,
                              OffsetDateTime lastFailed,
                              OffsetDateTime lockUntil) {

        String sql = """
            UPDATE ap_users
            SET failed_login_attempts = ?,
                last_failed_login = ?,
                account_locked_until = ?
            WHERE id = ?
        """;

        jdbcTemplate.update(sql,
                attempts,
                lastFailed,
                lockUntil,
                userId);

        log.info("USER_SECURITY_UPDATED | userId={} attempts={} lockUntil={}",
                userId, attempts, lockUntil);
    }

    public void resetFailures(Integer userId) {

        String sql = """
            UPDATE ap_users
            SET failed_login_attempts = 0,
                last_failed_login = NULL,
                account_locked_until = NULL
            WHERE id = ?
        """;

        jdbcTemplate.update(sql, userId);

        log.info("USER_SECURITY_RESET | userId={}", userId);
    }

    private UserSecurityDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserSecurityDTO u = new UserSecurityDTO();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("user_password"));
        u.setTenantId(rs.getInt("tenant_id"));
        u.setActive(rs.getBoolean("is_active"));
        u.setFailedAttempts(rs.getInt("failed_login_attempts"));
        u.setLastFailedLogin(rs.getObject("last_failed_login", OffsetDateTime.class));
        u.setAccountLockedUntil(rs.getObject("account_locked_until", OffsetDateTime.class));
        return u;
    }
}
