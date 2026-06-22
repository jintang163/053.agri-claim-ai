package com.agri.claim.common.utils;

import com.agri.claim.common.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    private static final String BASE64_SECRET = Base64.getEncoder()
            .encodeToString(Constants.TOKEN_SECRET.getBytes(StandardCharsets.UTF_8));

    public static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(BASE64_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String createToken(Map<String, Object> claims) {
        return createToken(claims, Constants.TOKEN_EXPIRE);
    }

    public static String createToken(Map<String, Object> claims, Long expireSeconds) {
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(getSecretKey());
        return builder.compact();
    }

    public static String createToken(Long userId, String userName, Long deptId, String roleKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_ID, userId);
        claims.put(Constants.USER_NAME, userName);
        claims.put(Constants.DEPT_ID, deptId);
        claims.put(Constants.ROLE_KEY, roleKey);
        return createToken(claims);
    }

    public static Claims parseToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
        return claimsJws.getPayload();
    }

    public static Long getUserId(Claims claims) {
        Object value = claims.get(Constants.USER_ID);
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    public static String getUserName(Claims claims) {
        return (String) claims.get(Constants.USER_NAME);
    }

    public static Long getDeptId(Claims claims) {
        Object value = claims.get(Constants.DEPT_ID);
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    public static String getRoleKey(Claims claims) {
        return (String) claims.get(Constants.ROLE_KEY);
    }

    public static Boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public static Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }
}
