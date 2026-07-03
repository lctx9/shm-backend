package com.backend.controller;

import com.backend.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    // ==========================================
    // API XUẤT VÀ TẢI FILE PDF BẰNG KHEN
    // ==========================================
    @GetMapping("/{prizeId}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable UUID prizeId) {
        try {
            // 1. Gọi Service để vẽ PDF và nhận về mảng byte
            byte[] pdfBytes = certificateService.generateCertificate(prizeId);

            // 2. Thiết lập Header để trình duyệt hiểu đây là file PDF cần tải về
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Bang_Khen_SEAL_Hackathon.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            // 3. Trả về file PDF
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            // Nếu lỗi (ví dụ: chưa công bố giải), trả về lỗi 400
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        }
    }
}