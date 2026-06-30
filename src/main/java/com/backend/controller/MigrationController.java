package com.backend.controller;

import com.backend.entity.User;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/public") // Để nằm trong vùng public để không bị chặn 403
public class MigrationController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/migrate-passwords")
    public String migrate() {
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            // Kiểm tra: Nếu chưa bắt đầu bằng $2a$, tức là đang là mật khẩu trần
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
                count++;
            }
        }
        return "Đã hoàn thành băm mật khẩu cho " + count + " tài khoản.";
    }
}