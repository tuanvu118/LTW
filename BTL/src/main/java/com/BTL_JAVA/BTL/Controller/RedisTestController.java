package com.BTL_JAVA.BTL.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/test/redis")
    public String testRedis() {
        try {
            redisTemplate.opsForValue().set("test:key", "hello redis");
            Object value = redisTemplate.opsForValue().get("test:key");
            return "Redis OK4, value = " + value;
        } catch (Exception e) {
            e.printStackTrace();
            return "Redis ERROR: " + e.getMessage();
        }
    }
}