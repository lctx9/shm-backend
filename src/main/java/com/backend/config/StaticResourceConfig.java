package com.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Cấu hình serve file tĩnh từ thư mục uploads/ trên local disk.
 * Ánh xạ URL /uploads/** đến thư mục vật lý {@code seal.upload.directory}.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${seal.upload.directory:uploads}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDirectory).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}
