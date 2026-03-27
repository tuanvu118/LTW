package com.BTL_JAVA.BTL.configuration;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ConfigCloudinary {

    @Autowired
    private Environment env;

    @Bean
    public Cloudinary configKey() {
        Map<String,String> config = new HashMap<String,String>();
        config.put("cloud_name", env.getProperty("cloudinary.cloud-name"));
        config.put("api_key", env.getProperty("cloudinary.api-key"));
        config.put("api_secret", env.getProperty("cloudinary.api-secret"));
        return new Cloudinary(config);
    }
}
