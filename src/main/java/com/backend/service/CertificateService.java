package com.backend.service;

import com.backend.entity.Prize;
import com.backend.entity.TeamMember;
import com.backend.repository.PrizeRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    @Autowired
    private PrizeRepository prizeRepository;

    // ==========================================
    // XUẤT BẰNG KHEN PDF (TRẢ VỀ BYTE[] ĐỂ CONTROLLER STREAM VỀ CLIENT)
    // ==========================================
    public byte[] generateCertificate(UUID prizeId) {
        // 1. Lấy thông tin giải thưởng
        Prize prize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải thưởng!"));

        // 2. Validate: Chỉ cho xuất bằng khen khi BTC đã công bố giải
        if (!prize.isAnnounced()) {
            throw new RuntimeException("Giải thưởng này chưa được công bố, không thể xuất bằng khen!");
        }

        try {
            // 3. Tạo luồng nhớ (memory stream) để chứa file PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate()); // Khổ A4 nằm ngang cho đẹp
            PdfWriter.getInstance(document, baos);
            document.open();

            // 4. Tạo font tiếng Việt (DroidSans có sẵn trong OpenPDF)
            BaseFont bf = BaseFont.createFont(
                    "C:/Windows/Fonts/arial.ttf",  // Font Arial của Windows
                    BaseFont.IDENTITY_H,           // Hỗ trợ Unicode
                    BaseFont.EMBEDDED
            );
            Font titleFont = new Font(bf, 32, Font.BOLD, new java.awt.Color(0, 51, 102));
            Font subtitleFont = new Font(bf, 18, Font.ITALIC, new java.awt.Color(80, 80, 80));
            Font prizeFont = new Font(bf, 40, Font.BOLD, new java.awt.Color(204, 102, 0));
            Font bodyFont = new Font(bf, 16, Font.NORMAL, java.awt.Color.BLACK);
            Font teamFont = new Font(bf, 24, Font.BOLD, new java.awt.Color(0, 102, 51));
            Font smallFont = new Font(bf, 12, Font.ITALIC, new java.awt.Color(100, 100, 100));

            // 5. Vẽ TIÊU ĐỀ "CERTIFICATE OF ACHIEVEMENT"
            Paragraph title = new Paragraph("CERTIFICATE OF ACHIEVEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Đường kẻ trang trí
            Paragraph line = new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    new Font(bf, 14, Font.NORMAL, new java.awt.Color(204, 102, 0)));
            line.setAlignment(Element.ALIGN_CENTER);
            line.setSpacingAfter(20);
            document.add(line);

            // 6. Vẽ LỜI MỞ ĐẦU
            Paragraph intro = new Paragraph("This certificate is proudly presented to", subtitleFont);
            intro.setAlignment(Element.ALIGN_CENTER);
            intro.setSpacingAfter(30);
            document.add(intro);

            // 7. Vẽ TÊN ĐỘI (highlight)
            Paragraph teamName = new Paragraph(prize.getTeam().getName(), teamFont);
            teamName.setAlignment(Element.ALIGN_CENTER);
            teamName.setSpacingAfter(20);
            document.add(teamName);

            // 8. Vẽ NỘI DUNG CHÍNH: Đạt giải gì
            Paragraph forPrize = new Paragraph("for achieving", subtitleFont);
            forPrize.setAlignment(Element.ALIGN_CENTER);
            forPrize.setSpacingAfter(15);
            document.add(forPrize);

            Paragraph prizeName = new Paragraph(prize.getPrizeName(), prizeFont);
            prizeName.setAlignment(Element.ALIGN_CENTER);
            prizeName.setSpacingAfter(20);
            document.add(prizeName);

            // 9. Vẽ THÔNG TIN GIẢI ĐẤU & HẠNG MỤC
            String eventInfo = String.format("at %s - %s %s",
                    prize.getEvent().getName(),
                    prize.getEvent().getSeason(),
                    prize.getEvent().getAcademicYear());
            Paragraph eventPara = new Paragraph(eventInfo, bodyFont);
            eventPara.setAlignment(Element.ALIGN_CENTER);
            eventPara.setSpacingAfter(10);
            document.add(eventPara);

            String trackInfo = "Track: " + prize.getTeam().getTrack().getName();
            Paragraph trackPara = new Paragraph(trackInfo, bodyFont);
            trackPara.setAlignment(Element.ALIGN_CENTER);
            trackPara.setSpacingAfter(30);
            document.add(trackPara);

            // 10. Vẽ DANH SÁCH THÀNH VIÊN (dùng bảng cho đẹp)
            Paragraph membersTitle = new Paragraph("Team Members:", bodyFont);
            membersTitle.setAlignment(Element.ALIGN_CENTER);
            membersTitle.setSpacingAfter(10);
            document.add(membersTitle);

            // Tạo bảng 1 cột chứa tên các thành viên
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(60);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            for (TeamMember member : prize.getTeam().getMembers()) {
                PdfPCell cell = new PdfPCell(new Phrase(
                        member.getUser().getFullName() + "  (" + member.getRole().name() + ")",
                        bodyFont
                ));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            document.add(table);

            // Khoảng trống cuối
            document.add(new Paragraph(" "));

            // 11. Vẽ NGÀY CẤP BẰNG
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Paragraph datePara = new Paragraph("Issued on: " + dateStr, smallFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingAfter(5);
            document.add(datePara);

            // 12. Vẽ MÔ TẢ GIẢI (nếu có)
            if (prize.getDescription() != null && !prize.getDescription().isBlank()) {
                Paragraph desc = new Paragraph(prize.getDescription(), smallFont);
                desc.setAlignment(Element.ALIGN_CENTER);
                desc.setSpacingAfter(5);
                document.add(desc);
            }

            // Đóng document
            document.close();

            // 13. Trả về mảng byte của file PDF
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file PDF bằng khen: " + e.getMessage());
        }
    }
}