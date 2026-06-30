package com.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF cho REST API dùng Token
                .csrf(csrf -> csrf.disable())

                // Kích hoạt CORS đồng bộ với @CrossOrigin ở các Controller
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        // PRIVATE ZONE: Tất cả các request còn lại bắt buộc phải xác thực (đăng nhập)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // CHỈ THÊM DUY NHẤT ĐOẠN NÀY: Khai báo Bean để AuthService và RegisterController gọi được
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}