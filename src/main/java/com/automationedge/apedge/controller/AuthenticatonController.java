package com.automationedge.apedge.controller;

import com.automationedge.apedge.security.SecurityLoginService;
import com.automationedge.platform.security.jwt.JwtUtil;
import com.automationedge.apedge.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.automationedge.apedge.custom_Exceptions.AccountLockedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;

@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthenticatonController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private static final Logger log = LoggerFactory.getLogger(AuthenticatonController.class);
    @Autowired
    private SecurityLoginService securityLoginService;

    // ---------------- LOGIN API -----------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpServletRequest request) {

        String grantType = payload.get("grant_type");

        if (grantType == null || grantType.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "grant_type is required"));
        }

        try {
            // ------------------- Case 1: Password grant (SECURITY TRACKING ENABLED) -------------------
            if ("password".equalsIgnoreCase(grantType)) {

                String email = payload.get("email");
                String password = payload.get("password");

                if (email == null || email.isBlank() || password == null || password.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Email and password are required"));
                }

                log.info("Login attempt started for email={}", email);

                try {
                    String ip = request.getRemoteAddr();
                    String ua = request.getHeader("User-Agent");

                    // 🔐 This call does EVERYTHING:
                    // - checks lock
                    // - validates password
                    // - updates ap_users
                    // - inserts ap_user_login_audit
                    Map<String, Object> user =
                            securityLoginService.authenticate(email, password, ip, ua);

                    // ⚠️ NEVER use Map.of here (caused your NPE earlier)
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("user_id", user.get("id"));
                    claims.put("email", user.get("email"));
                    claims.put("roles", user.get("user_role"));
                    claims.put("tenant_id", user.get("tenant_id"));

                    String token = jwtUtil.createToken(email, "", claims);

                    log.info("Login successful for email={}, userId={}",
                            email, user.get("id"));

                    return ResponseEntity.ok(Map.of("token", token));

                }
                catch (AccountLockedException ex) {

                OffsetDateTime until = ex.getLockedUntil();

                log.warn("Login blocked for email={} until={}", email, until);

                    ZoneId istZone = ZoneId.of("Asia/Kolkata");

                    // Convert lock time to IST
                    OffsetDateTime istTime = until
                            .atZoneSameInstant(istZone)
                            .toOffsetDateTime();

                    // Calculate minutes left correctly
                    long minutesLeft = Math.max(
                            Duration.between(OffsetDateTime.now(), until).toMinutes(),
                            1
                    );

                    // Format with AM/PM
                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

                    return ResponseEntity.status(HttpStatus.LOCKED) // 423
                            .body(Map.of(
                                    "message",
                                    "Your account is temporarily locked until "
                                            + istTime.format(formatter)   // ✅ FIXED
                                            + ". Please try again after "
                                            + minutesLeft
                                            + " minute(s)."
                            ));


                } catch (RuntimeException ex) {

                log.warn("Login failed for email={} reason={}", email, ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", ex.getMessage()));

            } catch (Exception ex) {

                log.error("Unexpected error during login", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Login failed. Please try again later."));
            }

        }
            // ------------------- Case 2: Refresh token grant -------------------
            else if ("refresh_token".equalsIgnoreCase(grantType)) {

                String refreshToken = payload.get("refresh_token");
                if (refreshToken == null || refreshToken.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "refresh_token is required"));
                }

                String email = jwtUtil.extractUsername(refreshToken);

                if (!jwtUtil.validateToken(refreshToken, email)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Invalid or expired refresh token"));
                }

                Map<String, Object> user = jdbcTemplate.queryForMap(
                        "SELECT * FROM ap_users WHERE email = ?", email
                );

                // -------- SAFE JWT CLAIMS --------
                Map<String, Object> claims = new HashMap<>();
                claims.put("user_id", user.get("id"));
                claims.put("email", user.get("email"));
                claims.put("roles", user.get("user_role"));
                claims.put("tenant_id", user.get("tenant_id"));

                String token = jwtUtil.createToken(email, "", claims);

                log.info("Access token refreshed for email={}", email);
                return ResponseEntity.ok(Map.of("token", token));
            }
            // ------------------- Unsupported grant type -------------------
            else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Unsupported grant_type"));
            }

        } catch (EmptyResultDataAccessException ex) {
            log.warn("Login failed: no user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong. Please try again later."));
        }
    }

    // ---------------- Forgot Password -----------------
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT is_active FROM ap_users WHERE email = ?", email
            );

            Boolean isActive = (Boolean) user.get("is_active");
            if (Boolean.FALSE.equals(isActive)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Your account is inactive."));
            }

            String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

            jdbcTemplate.update(
                    "INSERT INTO ap_password_reset (email, otp, expires_at, used_flag) VALUES (?, ?, ?, false)",
                    email, otp, expiry
            );

            mailService.sendOtpEmail(email, otp);

            return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));

        } catch (Exception e) {
            log.error("Error during forgot-password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process forgot password"));
        }
    }

    // ---------------- Verify OTP -----------------

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        String otp = payload.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email and OTP are required"));
        }

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(
                    "SELECT * FROM ap_password_reset WHERE email = ? AND otp = ? AND used_flag = false AND expires_at > now() ORDER BY created_at DESC LIMIT 1",
                    email, otp
            );

            String resetToken = UUID.randomUUID().toString();

            jdbcTemplate.update(
                    "UPDATE ap_password_reset SET reset_token = ?, used_flag = true WHERE id = ?",
                    UUID.fromString(resetToken), record.get("id")
            );

            return ResponseEntity.ok(Map.of("reset_token", resetToken));

        } catch (EmptyResultDataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired OTP"));
        } catch (Exception e) {
            log.error("Verify OTP failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify OTP"));
        }
    }

    // ---------------- Reset Password -----------------

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {

        String resetToken = payload.get("reset_token");
        String newPassword = payload.get("new_password");

        if (resetToken == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Reset token and new password are required"));
        }

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(
                    "SELECT * FROM ap_password_reset WHERE reset_token = ? AND expires_at > now()",
                    UUID.fromString(resetToken)
            );

            String email = (String) record.get("email");

            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT is_active FROM ap_users WHERE email = ?", email
            );

            Boolean isActive = (Boolean) user.get("is_active");
            if (Boolean.FALSE.equals(isActive)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Your account is inactive."));
            }

            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            jdbcTemplate.update(
                    "UPDATE ap_users SET user_password = ? WHERE email = ?",
                    hashedPassword, email
            );

            jdbcTemplate.update(
                    "DELETE FROM ap_password_reset WHERE id = ?",
                    record.get("id")
            );

            return ResponseEntity.ok(Map.of("message", "Password reset successful"));

        } catch (EmptyResultDataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired reset token"));
        } catch (Exception e) {
            log.error("Reset password failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password"));
        }
    }
}
