package com.BTL_JAVA.BTL.Service;

import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {

    RedisTemplate<String, String> redisTemplate;
    RedissonClient redissonClient;

    ObjectMapper objectMapper;

    // Lưu dữ liệu thành JSON String
    public void set(String key, Object value, Duration timeout){

        try{
            String jsonToCache = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonToCache, timeout);
        } catch (JsonProcessingException e){
            throw new AppException(ErrorCode.CACHE_WRITE_ERROR);
        }

    }

    // Lấy dữ liệu String thuần (để check NULL_VALUE)
    public String getString(String key){

        return redisTemplate.opsForValue().get(key);

    }

    // Lấy Object đơn (ví dụ: ProductDetailResponse)
    public <T> T get(String key, Class<T> clazz) {

        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Parse Json error");
            return null;
        }

    }

    // Lấy danh sách (ví dụ: List<ProductResponse>)
    public <T> List<T> getList(String key, TypeReference<List<T>> typeReference) {

        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Parse Json error");
            return null;
        }

    }

    public List<String> multiGet(List<String> keys) {

        return redisTemplate.opsForValue().multiGet(keys);

    }

    public void delete(String key) {

        redisTemplate.delete(key);
    }

}
