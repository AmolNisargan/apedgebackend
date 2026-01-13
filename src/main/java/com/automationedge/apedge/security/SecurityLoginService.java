package com.automationedge.apedge.security;
import com.automationedge.apedge.custom_Exceptions.AccountLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class SecurityLoginService {

    private static final Logger log = LoggerFactory.getLogger(SecurityLoginService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.login.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.login.lock-duration-minutes}")
    private int lockDurationMinutes;

    public SecurityLoginService(JdbcTemplate jdbcTemplate,
                                PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= MAIN AUTH METHOD =================

    public Map<String, Object> authenticate(String email,
                                            String password,
                                            String ip,
                                            String userAgent) {

        log.info("AUTH_ATTEMPT | email={} ip={}", email, ip);

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM ap_users WHERE email = ?", email);

        Integer userId = (Integer) user.get("id");
        Integer tenantId = (Integer) user.get("tenant_id");

        Boolean isActive = (Boolean) user.get("is_active");
        if (Boolean.FALSE.equals(isActive)) {
            audit(userId, tenantId, "FAILED", ip, userAgent, "Inactive account");
            throw new RuntimeException("Account is inactive");
        }

        // ---- LOCK CHECK ----
        Timestamp ts = (Timestamp) user.get("account_locked_until");

        if (ts != null) {

            OffsetDateTime lockedUntil =
                    ts.toInstant().atOffset(ZoneOffset.UTC);

            if (lockedUntil.isAfter(OffsetDateTime.now())) {

                long minutesLeft = Duration.between(
                        OffsetDateTime.now(),
                        lockedUntil
                ).toMinutes();

                audit(userId, tenantId, "LOCKED", ip, userAgent,
                        "Account locked until " + lockedUntil);

                throw new AccountLockedException(
                        "Your account is locked. Try again in " + minutesLeft + " minute(s).",
                        lockedUntil
                );
            }
        }

        String storedHash = (String) user.get("user_password");

        // ---- PASSWORD CHECK ----
        if (!passwordEncoder.matches(password, storedHash)) {
            handleFailure(user, ip, userAgent);
            throw new RuntimeException("Invalid email or password");
        }

        // ---- SUCCESS ----
        handleSuccess(user, ip, userAgent);
        return user;
    }

    // ================= FAILURE =================

    private void handleFailure(Map<String, Object> user,
                               String ip,
                               String ua) {

        Integer userId = (Integer) user.get("id");
        Integer tenantId = (Integer) user.get("tenant_id");

        Integer attempts = (Integer) user.get("failed_login_attempts");
        if (attempts == null) attempts = 0;

        attempts++;
        OffsetDateTime now = OffsetDateTime.now();

        OffsetDateTime lockUntil = null;
        String reason = "Invalid credentials";

        if (attempts >= maxFailedAttempts) {
            lockUntil = now.plusMinutes(lockDurationMinutes);
            reason = "Max failed attempts exceeded. Account locked.";
            log.warn("ACCOUNT_LOCKED | userId={} attempts={} until={}",
                    userId, attempts, lockUntil);
        } else {
            log.warn("AUTH_FAILED | userId={} attempts={}", userId, attempts);
        }

        jdbcTemplate.update("""
                UPDATE ap_users
                   SET failed_login_attempts = ?,
                       last_failed_login = ?,
                       account_locked_until = ?
                 WHERE id = ?
                """,
                attempts, now, lockUntil, userId);

        audit(userId, tenantId, "FAILED", ip, ua, reason);
    }

    // ================= SUCCESS =================

    private void handleSuccess(Map<String, Object> user,
                               String ip,
                               String ua) {

        Integer userId = (Integer) user.get("id");
        Integer tenantId = (Integer) user.get("tenant_id");

        jdbcTemplate.update("""
                UPDATE ap_users
                   SET failed_login_attempts = 0,
                       last_failed_login = NULL,
                       account_locked_until = NULL
                 WHERE id = ?
                """, userId);

        log.info("AUTH_RESET | userId={}", userId);

        audit(userId, tenantId, "SUCCESS", ip, ua, null);
    }

    // ================= AUDIT =================

    private void audit(Integer userId,
                       Integer tenantId,
                       String status,
                       String ip,
                       String ua,
                       String reason) {

        jdbcTemplate.update("""
            INSERT INTO ap_user_login_audit
                (user_id, tenant_id, login_status, ip_address, user_agent, failure_reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
                userId,
                tenantId,
                status,
                ip,
                ua,
                reason
        );

        log.info("LOGIN_AUDIT | userId={} tenantId={} status={}",
                userId, tenantId, status);
    }
}
