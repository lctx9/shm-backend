package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    /**
     * Upload ảnh thẻ sinh viên trong quá trình đăng ký.
     * Không yêu cầu đăng nhập vì đây là bước trong luồng đăng ký tài khoản.
     *
     * @param file File ảnh thẻ sinh viên (JPG, PNG, WEBP, GIF — max 5MB)
     * @return URL công khai để truy cập ảnh đã upload
     */
    @PostMapping("/student-card")
    public ApiResponse<String> uploadStudentCard(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeStudentCard(file);
        return ApiResponse.<String>builder()
                .result(fileUrl)
                .build();
    }
}
