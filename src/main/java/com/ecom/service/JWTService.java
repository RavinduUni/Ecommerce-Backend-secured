package com.ecom.service;

import com.ecom.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JWTService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";

    private static final long VALIDITY_MS = 1000L * 60 * 30; // 30 minutes

    /** Built once at startup instead of decoding the secret on every call. */
    private final SecretKey key;

    public JWTService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * The token carries the user's id and role, so requests can be authenticated
     * without a database lookup. Trade-off: a role change or deleted account takes
     * effect when the token expires (at most 30 minutes).
     */
    public String generateToken(Long userId, String email, UserRole role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, (role != null ? role : UserRole.CUSTOMER).name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + VALIDITY_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry and returns the claims, in one pass.
     * Throws JwtException (including ExpiredJwtException) when the token is invalid.
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserName(String token) {
        return parse(token).getSubject();
    }
}