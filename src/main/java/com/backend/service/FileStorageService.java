package com.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${seal.upload.directory:uploads}")
    private String uploadDir;

    @Value("${seal.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    public String storeStudentCard(MultipartFile file) {
        validateFile(file);

        try {
            Path uploadPath = Paths.get(uploadDir, "student-cards").toAbsolutePath();
            Files.createDirectories(uploadPath);

            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return baseUrl + "/uploads/student-cards/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh. Vui lòng thử lại.", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file ảnh JPG, PNG, WEBP, GIF.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File ảnh không được vượt quá 5MB.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
