package com.project.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {
    private static final String KEY_PREFIX = "blacklist_token:";
    private static final String BLACKLISTED_VALUE = "blacklisted";

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void blacklist(String accessToken) {
        Instant expiration = jwtTokenProvider.expiration(accessToken);
        Duration ttl = Duration.between(Instant.now(), expiration);
        if (ttl.isPositive()) {
            redisTemplate.opsForValue().set(key(accessToken), BLACKLISTED_VALUE, ttl);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(accessToken)));
    }

    private String key(String accessToken) {
        return KEY_PREFIX + sha256(accessToken);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
