# SEAL – Software Engineering Hackathon Management System

## Thông tin chung

| | |
|---|---|
| **Tên tiếng Việt** | Hệ thống quản lý cuộc thi SEAL Hackathon ngành Kỹ thuật Phần mềm |
| **Tên tiếng Anh** | SEAL – Software Engineering Hackathon Management System |

## Giới thiệu

**"Software Engineering Agile League (SEAL)"** là cuộc thi hackathon học thuật thường niên do Khoa Kỹ thuật Phần mềm phối hợp với PDP tổ chức tại Trường Đại học FPT TP.HCM.

- Mỗi năm, SEAL tổ chức **ba hackathon**: Spring, Summer, Fall.
- Mỗi sự kiện hackathon có thể bao gồm **nhiều vòng thi** (ví dụ: Vòng sơ khảo và Vòng chung kết).
- Các sự kiện SEAL mở cửa cho nhiều trường đại học cùng tham gia — đội thi có thể gồm:
  - Toàn sinh viên FPT
  - Hỗn hợp sinh viên FPT và sinh viên ngoài trường
  - Toàn sinh viên từ các trường đối tác

Hiện tại, công tác quản lý sự kiện chủ yếu được thực hiện thủ công, dễ xảy ra sai sót và thiếu minh bạch. Bên cạnh việc phát triển hệ thống, đề tài còn nghiên cứu **tính nhất quán trong chấm điểm của giám khảo** tại các cuộc thi hackathon — một yếu tố quan trọng nhưng chưa được nghiên cứu đầy đủ liên quan đến sự công bằng trong thi cử.

Hệ thống đóng vai trò vừa là **nền tảng quản lý cuộc thi**, vừa là **công cụ thu thập dữ liệu** cho nghiên cứu về độ tin cậy liên đánh giá viên (inter-rater reliability) trong đánh giá kỹ thuật phần mềm.

## Vấn đề hiện tại

Quy trình quản lý sự kiện hiện tại đang tồn tại nhiều vấn đề:

1. **Đăng ký đội thi và quản lý hạng mục thủ công** dẫn đến chậm trễ và sai sót dữ liệu.
2. **Chấm điểm thực hiện qua file Excel riêng lẻ** của từng giám khảo; phải thu thập và nhập lại toàn bộ kết quả thủ công, dẫn đến chậm trễ và dễ xảy ra sai sót.
3. **Kênh thông tin liên lạc hạn chế** giữa ban tổ chức, mentor, đội thi và người tham gia.
4. **Không có nhật ký kiểm tra (audit log)** cho các quyết định chấm điểm, làm giảm tính minh bạch và độ tin cậy của kết quả.

## Vai trò người dùng (Actors)

- Team Member
- Team Leader
- Mentor
- Judge
- Event Coordinator (SE Dept / PDP Staff)
- Admin

## Các thực thể chính (Key Entities)

- Hackathon Event
- Track (competition category)
- Round (competition stage within an event)
- Team
- Team Member
- Mentor
- Judge (Internal or Guest)
- Submission
- Score/Ranking
- Prize

## Chức năng theo vai trò (Features by Role)

> *Đề xuất dựa trên các vấn đề hiện tại và các thực thể đã xác định, nhằm giải quyết trực tiếp bài toán quản lý thủ công, chấm điểm rời rạc và thiếu minh bạch.*

### 1. Team Member

- Đăng ký tài khoản / đăng nhập hệ thống.
- Tham gia một đội thi (Team) đã có sẵn thông qua lời mời hoặc mã mời (invite code).
- Xem thông tin sự kiện (Hackathon Event), hạng mục thi (Track), lịch trình các vòng thi (Round).
- Xem thông tin đội của mình: thành viên, mentor được phân công.
- Xem trạng thái bài nộp (Submission) của đội.
- Nhận thông báo / tin nhắn từ ban tổ chức, mentor.
- Xem kết quả chấm điểm và bảng xếp hạng (Score/Ranking) sau khi công bố.

### 2. Team Leader

Có đầy đủ chức năng của **Team Member**, cộng thêm:

- Tạo đội thi (Team) mới, đặt tên đội, chọn hạng mục (Track) tham gia.
- Mời / thêm / xóa thành viên trong đội.
- Đăng ký đội thi vào sự kiện (Hackathon Event) và vòng thi (Round) tương ứng.
- Nộp bài dự thi (Submission) theo từng vòng, cập nhật/chỉnh sửa bài nộp trước hạn chót (deadline).
- Liên hệ / nhắn tin với mentor được phân công cho đội.
- Theo dõi tiến độ chấm điểm của đội qua các vòng.
- Xem phản hồi (feedback) từ giám khảo (nếu được công khai).

### 3. Mentor

- Xem danh sách đội thi được phân công hướng dẫn.
- Xem thông tin chi tiết đội thi: thành viên, hạng mục, tiến độ bài nộp.
- Trao đổi / nhắn tin trực tiếp với đội thi được phân công.
- Ghi chú hỗ trợ (mentoring notes) cho từng đội trong quá trình thi.
- Xem lịch trình các vòng thi để sắp xếp thời gian hỗ trợ.
- (Tùy chọn) Đề xuất/góp ý cho ban tổ chức về tình hình đội thi.

### 4. Judge (Internal / Guest)

- Xem danh sách đội thi và bài nộp (Submission) được phân công chấm.
- Chấm điểm (Score) theo tiêu chí (rubric/criteria) được ban tổ chức thiết lập cho từng vòng thi.
- Ghi nhận xét/phản hồi (feedback) cho từng đội thi.
- Chỉnh sửa điểm đã chấm trong thời gian cho phép, hệ thống lưu lại **nhật ký chỉnh sửa (audit log)** để đảm bảo minh bạch.
- Xem bảng phân công chấm thi (judge–team assignment) của bản thân.
- Xem thống kê/so sánh điểm số của chính mình với các giám khảo khác trong cùng vòng thi (phục vụ nghiên cứu độ tin cậy liên đánh giá viên — inter-rater reliability), nếu được ban tổ chức cho phép.

### 5. Event Coordinator (SE Dept / PDP Staff)

Vai trò quản trị cấp cao, có toàn quyền quản lý hệ thống:

**Quản lý sự kiện**
- Tạo/chỉnh sửa/xóa sự kiện hackathon (Hackathon Event) theo mùa: Spring, Summer, Fall.
- Tạo/chỉnh sửa hạng mục thi (Track) và các vòng thi (Round) trong mỗi sự kiện.
- Thiết lập lịch trình, deadline nộp bài cho từng vòng.

**Quản lý đội thi & thành viên**
- Quản lý danh sách đội thi, thành viên đội (bao gồm sinh viên FPT và sinh viên trường đối tác).
- Phân công mentor cho từng đội.

**Quản lý giám khảo & chấm điểm**
- Tạo tài khoản cho nhân viên (Mentor và Judge) (Các tài khoản này chỉ được tạo thông qua Coordinator chứ không thể tự tạo)
- Thêm/quản lý danh sách giám khảo (Judge – nội bộ hoặc khách mời).
- Phân công giám khảo chấm cho từng đội/từng vòng thi (judge assignment).
- Thiết lập tiêu chí chấm điểm (rubric) cho từng vòng.
- Theo dõi tiến độ chấm điểm theo thời gian thực, nhắc nhở giám khảo chưa hoàn thành.
- Tổng hợp điểm số tự động từ tất cả giám khảo, tính điểm trung bình/xếp hạng (Score/Ranking) — thay thế quy trình tổng hợp thủ công từ Excel.
- Xem nhật ký kiểm tra (audit log) toàn bộ các thay đổi điểm số, đảm bảo minh bạch và có thể truy vết.
- Xuất báo cáo, thống kê phục vụ nghiên cứu độ tin cậy liên đánh giá viên (inter-rater reliability).

**Quản lý giải thưởng**
- Thiết lập cơ cấu giải thưởng (Prize) theo hạng mục/vòng thi.
- Công bố kết quả và trao giải dựa trên bảng xếp hạng cuối cùng.

**Truyền thông**
- Gửi thông báo/tin nhắn hàng loạt đến toàn bộ đội thi, mentor, giám khảo.
- Quản lý kênh liên lạc tập trung giữa ban tổ chức – mentor – đội thi – giám khảo, thay thế các kênh liên lạc rời rạc hiện tại.

### 6. Admin
- Quản lý toàn bộ hệ thống, bao gồm:
  - Quản lý tài khoản người dùng (User Account Management)
  - Phân quyền vai trò (Role-Based Access Control)
  - Giám sát hoạt động hệ thống (System Monitoring)
  - Sao lưu và phục hồi dữ liệu (Backup & Restore)
  - Cấu hình hệ thống (System Configuration)

### Luồng hoạt động chính (Workflow):
## user:
- Đăng ký: Sinh viên vào trang chủ của SEAL Hackathon -> điền đầy đủ các thông tin các nhân (HỌ TÊN, EMAIL (OTP XÁC THỰC EMAL), TRƯỜNG (FPT HAY TRƯỜNG KHÁC - NẾU CHỌN PhẦN TRƯỜNG KHÁC THÌ NHẬP TÊN TRƯỜNG), MÃ SỐ SINH VIÊN, MẬT KHẨU, XÁC NHẬT MẬT KHẨU, UPLOAD ẢNH THẺ SINH VIÊN (ẢNH THẺ SINH VIÊN CÓ CHỨA TÊN, MÃ SỐ SINH VIÊN, TRƯỜNG) -> NHẬP MÃ OTP XÁC THỰC EMAIL -> HOÀN THÀNH ĐĂNG KÝ TÀI KHOẢN)
- Đăng nhập: Sinh viên đăng nhập vào hệ thống bằng email và mật khẩu đã đăng ký
- Đăng ký giải đấu: Sinh viên xem các giải đấu tại trang chủ -> Chọn giải đấu muốn tham gia -> Nhấn nút "Đăng ký" -> Chọn "Tạo đội" hoặc là "Tham gia đội" -> Nếu chọn "Tạo đội" thì nhập tên đội, chọn hạng mục thi, chọn "PUBLIC" hoặc "PRIVATE" (Nếu chọn public thì đội sẽ nhận được các thông báo yêu cầu vào đội của người khác, còn nếu chọn private thì phải nhập thêm password của nhóm, người khác có thể vào nếu bạn cung cấp password), nhấn nút "Tạo đội" -> Nếu chọn "Tham gia đội" thì sẽ vào trang sảnh chờ các đội của giải đấu, bạn có thể gửi lời của các team tại đó, hoặc nhập password để tham gia các team PRIVATE  -> Hoàn tất đăng ký giải đấu
- Tạo bài nộp: Team Leader là người duy nhất trong team có thể nộp bài. Sau khi đã thống nhất bài làm với cả nhóm, team leader sẽ upload bài nộp lên hệ thống.
- Check lại bài nộp: Team Leader có thể check lại bài nộp của team mình, nếu chưa đúng thì có thể chỉnh sửa và upload lại bài nộp mới (TRƯỚC THỜI GIAN KẾT THÚC VÒNG ĐẤU). Team member sẽ không thể chỉnh sửa bài nộp, nhưng có thể xem bài nộp của team mình.
- Nhận thông báo: Team member và team leader sẽ nhận được thông báo từ hệ thống
- Xem kết quả: Team member và team leader có thể xem kết quả của team mình sau khi ban tổ chức công bố kết quả.
- Trao đổi với mentor: Team member và team leader có thể trao đổi trực tiếp với mentor được phân công cho team mình. (real-time chat)
- Profile cá nhân: User có thể xem và chỉnh sửa ảnh đại diện, đổi biệt danh, đổi mật khẩu, đổi avatar (Không thể đổi Họ tên đầy đủ, email, mã số sinh viên và trường học đã đăng ký). Ở trang tổng quan của profile còn có thể hiển thị các thành tích của user, ví dụ như: Giải nhất giải đấu SEAL Hackathon 2023, Giải nhì giải đấu SEAL Hackathon 2024.. mỗi thành tích sẽ có một huy hiệu (badge) riêng, khi click vào huy hiệu sẽ hiển thị chi tiết thông tin về giải thưởng đó.
- Xem bảng xếp hạng: User có thể truy cập vào "Bảng xếp hạng" để xem các thứ hạng của các đội ở các giải đã kết thúc, và có thể truy cập vào profile của các thí sinh đạt giải. Ví dụ: Bảng xếp hạng HACKATHON SEAL 2023, Giải nhất: Team A (thành viên: A1, A2, A3), Giải nhì: Team B (thành viên: B1, B2, B3), Giải ba: Team C (thành viên: C1, C2, C3). Khi click vào tên team sẽ hiển thị chi tiết thông tin về team đó. Và có thể ấn vào profile cá nhân của từng thành viên.
- Xuất bằng khen: Các team đạt giải nhất, nhì, ba. Khi truy cập profile cá nhân -> Ấn vào chi tiết giải đấu đạt giải đó trên profile. Sẽ có mục xuất pdf như là 1 bằng khen số.

## coordinator
- Quy trình tạo giải đấu:
  - Tạo giải đấu: Coordinator vào trang quản lý -> Chọn "Tạo giải đấu" -> Nhập tên giải đấu, chọn mùa giải (Spring, Summer, Fall), nhập năm, nhập thời gian mở đăng ký và đóng cổng đăng ký giải, nhập thời gian bắt đầu và kết thúc, nhập deadline nộp bài, nhập số lượng vòng thi, nhập hạng mục thi (Track), cấu hình giải thưởng: thêm/xóa/sửa-> Nhấn nút "Tạo giải đấu"
  - Cấu hình & Sinh ma trận: Sau khi Coordinator nhấn nút "Khởi tạo cấu trúc trận đấu" -> Hệ thống Backend lúc này mới lấy số lượng vòng thi và danh sách Track đã nhập ở bước trước để chạy thuật toán nhân chéo tự động -> Sinh ra cấu trúc bảng Ma trận trận đấu (TrackRoundMatrix). Tại giao diện chi tiết của từng ô ma trận (hoặc từng Vòng thi) -> Coordinator tiến hành upload tệp tài liệu, đề bài (file PDF, Guideline quy chế thi) và thiết lập thời gian làm bài riêng cho từng vòng đấu nếu có. Cũng như là phân công Mentor (Mỗi track sẽ từ 1-2 mentor), Judge (Mỗi round thì sẽ 2-5).
- Phê duyệt tài khoản: Coordinator chọn "Quản lý tài khoản" -> Xem danh sách các tài khoản đăng ký mới (Coordinator sẽ đối chiếu thông tin và ảnh thẻ sinh viên) -> Phê duyệt hoặc từ chối tài khoản (kèm lý do nếu từ chối).
- Tạo tài khoản Mentor và Judge: Coordinator chọn "Quản lý tài khoản" -> Chọn "Tạo tài khoản Staff" -> Nhập thông tin cá nhân của Mentor hoặc Judge -> Chọn vai trò (Mentor hoặc Judge) -> Nhấn nút "Tạo tài khoản"
- Phân công Mentor và Judge: Coordinator chọn "Quản lý giải đấu" -> Chọn giải đấu -> Chọn Track hoặc Round -> Phân công Mentor và Judge cho từng Track hoặc Round. (Trước khi bắt đầu giải)
- Xem nhật ký chỉnh sửa (Audit Log): Coordinator có quyền truy cập vào bảng Nhật ký kiểm tra để kiểm soát tính minh bạch của giải đấu -> Xem chi tiết tất cả các hành động chỉnh sửa điểm số của Giám khảo: Giám khảo nào sửa, sửa điểm của đội nào, từ bao nhiêu điểm thành bao nhiêu điểm, vào lúc mấy giờ -> Đảm bảo truy vết tuyệt đối, chống gian lận điểm số.
- Xem thống kê chấm điểm: Coordinator có thể xem thống kê chấm điểm của từng Giám khảo, so sánh điểm số của các Giám khảo với nhau để nghiên cứu độ tin cậy liên đánh giá viên (inter-rater reliability).
- Xem bảng xếp hạng: Coordinator có thể xem bảng xếp hạng của từng Track hoặc Round, cũng như tổng hợp điểm số cuối cùng để công bố kết quả.
- Quản lý thông báo: Coordinator có thể gửi thông báo đến toàn bộ đội thi, Mentor, Judge hoặc từng nhóm riêng lẻ.
- Quản lý tài khoản: Coordinator có thể quản lý tài khoản của tất cả người dùng, bao gồm Team Member, Team Leader, Mentor, Judge. Có thể khóa hoặc xóa tài khoản nếu cần thiết.
- Xem thống kê: Coordinator có thể xem thống kê tổng quan về số lượng đội thi, số lượng bài nộp, số lượng Mentor và Judge tham gia, cũng như các thông tin liên quan đến giải đấu.

## judge
- Xem bài nộp: Judge có thể xem danh sách các bài nộp của các đội thi được phân công chấm điểm. Judge có thể xem chi tiết từng bài nộp, bao gồm thông tin đội thi, thành viên, và các tài liệu nộp.
- Chấm điểm: Judge có thể chấm điểm cho từng bài nộp dựa trên tiêu chí chấm điểm (rubric) đã được thiết lập bởi Coordinator. Judge có thể nhập điểm số và nhận xét cho từng tiêu chí, sau đó lưu lại kết quả chấm điểm
- Cấm cờ: Judge có thể cấm cờ (flag) các bài nộp nếu phát hiện vi phạm quy định, gian lận hoặc nội dung không phù hợp. Judge cần cung cấp lý do cấm cờ và gửi thông báo đến Coordinator.

## mentor
- Xem danh sách đội thi: Mentor có thể xem danh sách các đội thi được phân công hướng dẫn. Mentor có thể xem thông tin chi tiết của từng đội, bao gồm thành viên, bài nộp, và tiến độ làm việc.
- Trao đổi với đội thi: Mentor có thể trao đổi trực tiếp với các đội thi

## admin
- Tạo tài khoản cho Coordinator: Admin có thể tạo tài khoản cho các Event Coordinator (SE Dept / PDP Staff) để họ có quyền quản lý giải đấu.
- Xem được thống kê của toàn bộ hệ thống: Admin có thể xem thống kê tổng quan về số lượng người dùng, số lượng giải đấu, số lượng Mentor và Judge, cũng như các thông tin liên quan đến hoạt động của hệ thống.
- Quản lý bảo mật và sao lưu dữ liệu: Admin có thể thiết lập các biện pháp bảo mật, sao lưu dữ liệu định kỳ và phục hồi dữ liệu khi cần thiết để đảm bảo an toàn thông tin và tính liên tục của hệ thống.