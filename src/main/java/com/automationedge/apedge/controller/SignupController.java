package com.automationedge.apedge.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api") // protected under JWT
@RequiredArgsConstructor
public class SignupController {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(SignupController.class);

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String contact = payload.get("contact");
        String password = payload.get("password");
        String userName = payload.get("user_name");
        String userRole = payload.get("user_role");
        String tenantIdStr = payload.get("tenant_id");

        if (email == null || contact == null || password == null || userName == null || userRole == null || tenantIdStr == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "All required fields must be provided"));
        }

        try {
            // Check if email already exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ap_users WHERE email = ?", Integer.class, email
            );
            if (count != null && count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email is already registered"));
            }

            // Hash password before storing
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert new user
            jdbcTemplate.update(
                    "INSERT INTO ap_users (email, contact, user_password, user_role, tenant_id, user_name) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    email, contact, hashedPassword, userRole,
                    tenantIdStr != null ? Integer.parseInt(tenantIdStr) : null,
                    userName
            );

            log.info("New user registered with email={}", email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully"));

        } catch (Exception e) {
            log.error("Error during signup for email={}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register user"));
        }
    }
}

