package com.watermark.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.watermark.sdk.WatermarkEngine;

@Configuration
public class WebConfig {

    /**
     * CORS – cho phép frontend (Vite) và mobile gọi API.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:5173", // Vite frontend dev
                                "http://localhost:5174", // Mobile dev
                                "http://localhost:3000" // Alternate
                )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }

    /**
     * Watermark SDK bean – inject vào bất kỳ service nào.
     */
    @Bean
    public WatermarkEngine watermarkEngine() {
        return new WatermarkEngine();
    }
}
