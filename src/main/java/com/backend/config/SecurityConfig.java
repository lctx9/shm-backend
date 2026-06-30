package com.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Kích hoạt tính năng bảo mật của Spring Security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF vì chúng ta dùng JWT (stateless)
                .csrf(csrf -> csrf.disable())

                // 2. Kích hoạt CORS
                .cors(Customizer.withDefaults())

                // 3. Cấu hình session thành stateless (không lưu session trên server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Cấu hình quyền truy cập
                .authorizeHttpRequests(auth -> auth
                        // Các API công khai không cần đăng nhập
                        .requestMatchers("/api/public/**").permitAll()
                        // Các API khác bắt buộc phải xác thực
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // 5. Khai báo Bean PasswordEncoder để sửa lỗi khởi động ứng dụng
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}