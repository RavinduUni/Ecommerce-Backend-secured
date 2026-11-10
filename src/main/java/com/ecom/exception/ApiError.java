package com.ecom.exception;

import java.time.Instant;

/** The JSON body for every error response: { status, error, message, timestamp }. */
public record ApiError(int status, String error, String message, String timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now().toString());
    }

    /** Handwritten JSON for places outside Spring MVC (security filters). */
    public String toJson() {
        return "{\"status\":" + status
                + ",\"error\":\"" + escape(error) + "\""
                + ",\"message\":\"" + escape(message) + "\""
                + ",\"timestamp\":\"" + escape(timestamp) + "\"}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
