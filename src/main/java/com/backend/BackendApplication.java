package com.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	// THÊM ĐOẠN NÀY VÀO: Khai báo một PasswordEncoder "giả" không băm mật khẩu
	@Bean
	public PasswordEncoder passwordEncoder() {
		// Trả về mật khẩu trần nguyên bản, giải quyết triệt để lỗi thiếu Bean cho toàn bộ dự án
		return NoOpPasswordEncoder.getInstance();
	}
}