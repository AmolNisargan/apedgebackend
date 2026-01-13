package com.automationedge.apedge.dto;

import java.time.OffsetDateTime;

public class UserSecurityDTO {

    private Integer id;
    private String email;
    private String password;
    private String userRole;
    private Integer tenantId;
    private Boolean active;

    private int failedAttempts;
    private OffsetDateTime lastFailedLogin;
    private OffsetDateTime accountLockedUntil;

    // getters & setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public OffsetDateTime getLastFailedLogin() { return lastFailedLogin; }
    public void setLastFailedLogin(OffsetDateTime lastFailedLogin) { this.lastFailedLogin = lastFailedLogin; }

    public OffsetDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(OffsetDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }
}
