package com.automationedge.apedge.security;

import com.automationedge.apedge.custom_Exceptions.AuthenticationException;
import com.automationedge.apedge.custom_Exceptions.AccountLockedException;
import com.automationedge.apedge.dto.UserSecurityDTO;
import com.automationedge.apedge.repository.LoginAuditJdbcRepository;
import com.automationedge.apedge.repository.UserSecurityRepository;
import com.automationedge.apedge.util.ClientInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserSecurityRepository userRepo;

    @Autowired
    private LoginAuditJdbcRepository auditRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.login.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.login.lock-duration-minutes}")
    private int lockDurationMinutes;

    public UserSecurityDTO authenticate(String email,
                                        String password,
                                        HttpServletRequest request)
    {

    String ip = ClientInfoUtil.getClientIp(request);
        String ua = ClientInfoUtil.getUserAgent(request);

        log.info("AUTH_ATTEMPT | email={} ip={}", email, ip);

        UserSecurityDTO user = userRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("AUTH_FAILED | reason=USER_NOT_FOUND email={} ip={}", email, ip);
                    throw new AuthenticationException("Invalid username or password");
                });

        if (!Boolean.TRUE.equals(user.getActive())) {
            log.warn("AUTH_BLOCKED | reason=INACTIVE_USER userId={}", user.getId());
            throw new AuthenticationException("Account is inactive. Contact admin.");
        }

        // 1. lock check
        if (isLocked(user)) {
            log.warn("AUTH_BLOCKED | reason=ACCOUNT_LOCKED userId={} until={}",
                    user.getId(), user.getAccountLockedUntil());

            auditRepo.save(
                    user.getId(),
                    user.getTenantId(),
                    "LOCKED",
                    ip,
                    ua,
                    "Account locked until " + user.getAccountLockedUntil()
            );

            OffsetDateTime lockedUntil = user.getAccountLockedUntil();

            throw new AccountLockedException(
                    "Your account is locked. Try again after " + lockedUntil,
                    lockedUntil
            );

        }

        // 2. password check
        if (!passwordEncoder.matches(password, user.getPassword())) {
            handleFailure(user, ip, ua);
            throw new AuthenticationException("Invalid username or password");
        }

        // 3. success
        handleSuccess(user, ip, ua);
        log.info("AUTH_SUCCESS | userId={} ip={}", user.getId(), ip);
        handleSuccess(user, ip, ua);
        log.info("AUTH_SUCCESS | userId={} ip={}", user.getId(), ip);

        return user;   // ✅ ADD THIS

    }

    private boolean isLocked(UserSecurityDTO u) {
        return u.getAccountLockedUntil() != null &&
                OffsetDateTime.now().isBefore(u.getAccountLockedUntil());
    }

    private void handleFailure(UserSecurityDTO u, String ip, String ua) {

        int attempts = u.getFailedAttempts() + 1;
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime lockUntil = null;

        String reason = "Invalid credentials";

        if (attempts >= maxFailedAttempts) {
            lockUntil = now.plusMinutes(lockDurationMinutes);
            reason = "Max failed attempts exceeded. Account locked.";

            log.warn("ACCOUNT_LOCKED | userId={} attempts={} lockUntil={}",
                    u.getId(), attempts, lockUntil);
        } else {
            log.warn("AUTH_FAILED | userId={} attempts={} ip={}",
                    u.getId(), attempts, ip);
        }

        userRepo.updateFailure(
                u.getId(),
                attempts,
                now,
                lockUntil
        );

        auditRepo.save(
                u.getId(),
                u.getTenantId(),
                "FAILED",
                ip,
                ua,
                reason
        );
    }

    private void handleSuccess(UserSecurityDTO u, String ip, String ua) {

        if (u.getFailedAttempts() > 0) {
            log.info("AUTH_RESET | userId={} previousFailedAttempts={}",
                    u.getId(), u.getFailedAttempts());
        }

        userRepo.resetFailures(u.getId());

        auditRepo.save(
                u.getId(),
                u.getTenantId(),
                "SUCCESS",
                ip,
                ua,
                null
        );
    }
}
