package com.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevSeedController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/reset-db")
    public String resetDb() {
        try {
            ClassPathResource resource = new ClassPathResource("dev-seed.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
            return "Đã nạp lại toàn bộ dữ liệu mẫu (dev-seed) thành công!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi nạp dữ liệu mẫu: " + e.getMessage();
        }
    }
}
