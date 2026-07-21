package com.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevSeedController {

    private final DataSource dataSource;

    @PostMapping("/reset-db")
    public String resetDb() {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("dev-seed.sql"));
            populator.execute(dataSource);
            return "Đã nạp lại toàn bộ dữ liệu mẫu (dev-seed) thành công!";
        } catch (Exception e) {
            return "Lỗi nạp dữ liệu mẫu: " + e.getMessage();
        }
    }
}
