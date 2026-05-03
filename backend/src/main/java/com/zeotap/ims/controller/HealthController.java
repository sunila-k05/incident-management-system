package com.zeotap.ims.controller;

import com.zeotap.ims.engine.SignalBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {

    private final SignalBuffer signalBuffer;
    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();

        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());

        // Check PostgreSQL
        health.put("postgres", checkPostgres());

        // Check MongoDB
        health.put("mongodb", checkMongo());

        // Check Redis
        health.put("redis", checkRedis());

        // Buffer stats
        health.put("bufferSize", signalBuffer.getCurrentSize());
        health.put("bufferCapacity", signalBuffer.getCapacity());
        health.put("totalSignalsReceived", signalBuffer.getTotalReceived());

        return ResponseEntity.ok(health);
    }

    private String checkPostgres() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(
                new org.bson.Document("ping", 1));
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            redisTemplate.getConnectionFactory()
                .getConnection().ping();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
