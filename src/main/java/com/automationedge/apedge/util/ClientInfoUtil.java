package com.automationedge.apedge.util;


import jakarta.servlet.http.HttpServletRequest;

public final class ClientInfoUtil {

    private ClientInfoUtil() {}

    public static String getClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank())
                ? xf.split(",")[0].trim()
                : req.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }
}
