# BÁO CÁO RÀ SOÁT NGHIỆP VỤ VÀ KIỂM THỬ HỆ THỐNG SEAL

Ngày kiểm thử: 20/07/2026  
Phạm vi: `shm-backend`, `shm-frontend` và đặc tả `C:\Users\MSI\Downloads\SEAL_Hackathon_Management_System.md`

## 1. Kết luận nhanh

Các màn hình và luồng cơ bản của hệ thống đã hình thành khá đầy đủ: đăng nhập, sự kiện, tạo/gia nhập đội, nộp bài, chấm điểm, quản lý sinh viên, nhân sự, thông báo, sao lưu và trang quản trị đều có thể khởi chạy.

Tuy nhiên, phiên bản hiện tại **chưa nên đưa vào môi trường production hoặc dùng để công bố kết quả chính thức**. Những rủi ro lớn nhất là:

1. Điểm của nhiều giám khảo không được tổng hợp đúng; điểm của người chấm cuối cùng ghi đè điểm trước đó.
2. API chấp nhận điểm ngoài phạm vi, bài nộp trùng và cập nhật bài sang vòng/sự kiện đã hết hạn.
3. Kết quả chưa công bố vẫn hiển thị công khai cho người chưa đăng nhập.
4. Tài khoản đã bị khóa vẫn dùng được token cũ.
5. API quản lý người dùng trả cả trường mật khẩu đã băm và cho phép xem ảnh thẻ sinh viên không đúng phạm vi.
6. Vòng chung kết dùng `trackId = null` nhưng frontend lại lọc theo `trackId` của đội, khiến đội được thăng hạng không thấy vòng chung kết để nộp bài.

Mức đánh giá tổng thể: **Rủi ro cao**.

## 2. Cách kiểm thử

Đã thực hiện:

- Đọc đặc tả nghiệp vụ tổng thể.
- Rà soát controller, service, entity, repository, DTO, security và cấu hình backend.
- Rà soát routing, phân quyền giao diện, API client và các trang nghiệp vụ frontend.
- Build và lint frontend.
- Chạy test Maven backend.
- Khởi chạy PostgreSQL, backend và frontend tại máy local.
- Kiểm thử giao diện bằng trình duyệt với các vai trò USER (leader/member), STAFF (mentor/judge), COORDINATOR và ADMIN.
- Kiểm thử API bằng script có dọn sạch dữ liệu tạm sau khi chạy.

Không thực hiện:

- Không chạy khôi phục database vì đây là thao tác phá hủy dữ liệu đang hoạt động.
- Không gửi OTP qua email thật.
- Không kiểm thử tải lớn, kiểm thử xâm nhập chuyên sâu, đa trình duyệt hoặc toàn bộ kích thước màn hình di động.
- Không kiểm thử production deployment vì cấu hình hiện tại là cấu hình local.

## 3. Kết quả build và test tự động

### Frontend

- `npm ci`: thành công.
- `npm run lint`: thành công nhưng có 9 cảnh báo, chủ yếu là biến không dùng và thiếu dependency trong React Hook.
- `npm run build`: thành công.
- `npm audit`: 0 lỗ hổng được npm báo cáo.
- Bundle JavaScript chính khoảng 565 KB; ảnh `OIP.png` khoảng 6,9 MB; Vite cảnh báo chunk lớn hơn 500 KB.

### Backend

- `mvn test`: thành công.
- Chỉ có 1 test `contextLoads`; chưa có unit/integration test cho nghiệp vụ quan trọng.
- Có cảnh báo Lombok `@Builder` bỏ qua giá trị khởi tạo mặc định.
- Có cảnh báo `open-in-view`.
- Có dependency Redis và cảnh báo dò repository Redis nhưng hiện không thấy nghiệp vụ sử dụng Redis.

## 4. Ma trận luồng nghiệp vụ đã kiểm thử

| Nhóm | Kết quả | Ghi chú |
|---|---|---|
| Trang công khai / sự kiện | Chạy nhưng sai bảo mật | Sự kiện tải được; kết quả đang hoạt động bị công khai sớm |
| Đăng nhập | Chạy một phần | Sai mật khẩu báo đúng; tài khoản pending báo lỗi hệ thống chung |
| Khóa tài khoản | Không đạt | Token cấp trước khi khóa vẫn hoạt động |
| Tạo/gia nhập đội | Chạy nhưng thiếu ràng buộc | Không kiểm tra kỳ đăng ký, giới hạn thành viên hoặc phạm vi sự kiện |
| Leader nộp/cập nhật bài | Không đạt | Cho phép trùng bài và chuyển bài sang vòng quá hạn/sai sự kiện |
| Member nộp bài | Đạt ở UI | Nút nộp bị vô hiệu hóa với member |
| Mentor xem đội/chat | Chạy một phần | Xem được đội theo track; thiếu mentor notes; chat là polling |
| Judge chấm bài | Không đạt về tính toàn vẹn | Điểm cuối ghi đè, cho nhập 999, có đường chấm thứ hai bỏ qua audit |
| Coordinator quản lý | Chạy nhưng thiếu validation | Các màn hình tải được; dữ liệu sai vẫn được API chấp nhận |
| Admin quản trị | Chạy một phần | Monitoring/settings/backup tải được; restore không kiểm thử |
| Thông báo | Chạy cơ bản | Thiếu phạm vi gửi chi tiết, lịch gửi, hết hạn và đánh dấu đọc từng mục |
| Thành tích/chứng nhận | Không đúng đặc tả | Tải HTML, không phải PDF; chưa có xác thực chứng nhận |

## 5. Phát hiện mức Critical

### C-01 — Tính điểm nhiều giám khảo sai

**Bằng chứng động:** chấm cùng một bài lần lượt 80 và 100 bởi hai giám khảo, `submission.score` trở thành 100 thay vì trung bình 90.

**Nguyên nhân:** `ScoreService` lưu từng `Score` nhưng sau mỗi lần chấm lại ghi thẳng điểm vừa nhập vào `Submission.score`.

Vị trí: `shm-backend/src/main/java/com/backend/service/ScoreService.java:91`

**Tác động:** bảng xếp hạng, thăng hạng và giải thưởng có thể sai hoàn toàn theo thứ tự giám khảo chấm.

**Khuyến nghị:** tính điểm tổng hợp bằng một hàm duy nhất theo chính sách rõ ràng (trung bình/trọng số), cập nhật sau transaction, và dùng chính giá trị tổng hợp cho leaderboard cùng promotion.

### C-02 — Chấp nhận điểm ngoài phạm vi và rubric tùy ý

**Bằng chứng động:** API chấp nhận `scoreValue = 999`.

`ScoreRequest` không có validation min/max; dữ liệu `criteria` cũng không được đối chiếu với rubric của vòng.

Vị trí:

- `shm-backend/src/main/java/com/backend/dto/request/ScoreRequest.java:8`
- `shm-backend/src/main/java/com/backend/service/ScoreService.java:38`

**Tác động:** người có quyền judge có thể tạo điểm không hợp lệ, làm sai bảng xếp hạng và promotion.

**Khuyến nghị:** đặt `@DecimalMin`, `@DecimalMax`, kiểm tra tổng điểm từ từng tiêu chí phía server, từ chối tiêu chí lạ/thiếu và khóa rubric khi vòng đã bắt đầu.

### C-03 — Kết quả chưa công bố bị lộ công khai

**Bằng chứng động:** người chưa đăng nhập truy cập được `/events/1/results` và `/api/leaderboard`, thấy 3 bài đã chấm của các sự kiện đang hoạt động. Điểm mới chấm cũng xuất hiện ngay trên API công khai.

Không có trạng thái `resultsPublished` hoặc mốc công bố kết quả. Toàn bộ `/api/events/**` và `/api/leaderboard/**` được `permitAll`.

Vị trí:

- `shm-backend/src/main/java/com/backend/config/SecurityConfig.java:42-48`
- `shm-backend/src/main/java/com/backend/controller/LeaderboardController.java`

**Tác động:** rò rỉ điểm và kết quả trước thời điểm công bố, ảnh hưởng tính công bằng của cuộc thi.

**Khuyến nghị:** thêm trạng thái/mốc công bố theo event; API public chỉ trả kết quả đã publish. Dữ liệu tạm thời chỉ coordinator/admin được xem.

### C-04 — Khóa tài khoản không vô hiệu hóa token hiện tại

**Bằng chứng động:** ADMIN chuyển user sang `BANNED`, nhưng JWT đã cấp trước đó vẫn gọi `/api/users/me` thành công HTTP 200.

`CustomUserDetails` luôn trả `true` cho `isAccountNonLocked` và `isEnabled`; filter không từ chối trạng thái hiện tại của user.

Vị trí:

- `shm-backend/src/main/java/com/backend/security/CustomUserDetails.java:41`
- `shm-backend/src/main/java/com/backend/security/CustomUserDetails.java:52`

**Tác động:** tài khoản bị khóa vẫn dùng hệ thống cho tới khi token hết hạn.

**Khuyến nghị:** kiểm tra `AccountStatus.APPROVED` ở mỗi request hoặc dùng `tokenVersion`/revocation store; tăng `tokenVersion` khi khóa, đổi mật khẩu hoặc đổi role.

## 6. Phát hiện mức High

### H-01 — Bỏ qua thời gian đăng ký và hạn nộp bài

**Bằng chứng động:**

- Tạo đội thành công cho sự kiện có ngày mở đăng ký ở tương lai.
- Leader cập nhật bài nộp sang một matrix đã hết hạn và thuộc sự kiện khác.

`TeamService` không kiểm tra event active/registration window. `SubmissionService.updateSubmission` chỉ kiểm tra đội và leader, không kiểm tra event, track, vòng hoặc deadline mới.

Vị trí:

- `shm-backend/src/main/java/com/backend/service/TeamService.java:47-77`
- `shm-backend/src/main/java/com/backend/service/SubmissionService.java:71-94`

**Khuyến nghị:** gom policy thời gian vào service dùng chung; tất cả create/join/invite/submit/update phải kiểm tra event, track, round, deadline và trạng thái promotion ở backend.

### H-02 — Cho phép nhiều bài nộp cho cùng đội/vòng

**Bằng chứng động:** cùng một đội và matrix tạo được thêm bài nộp mới.

Không có unique constraint `(team_id, matrix_id)` và service không kiểm tra tồn tại.

**Tác động:** một đội có nhiều bản ghi cho một vòng, chấm trùng hoặc leaderboard chọn sai bản.

**Khuyến nghị:** unique constraint ở database, kiểm tra trong transaction và dùng update/version cho lần nộp tiếp theo.

### H-03 — Luồng vòng chung kết bị gãy ở frontend

Matrix chung kết được tạo với `trackId = null`, nhưng frontend chỉ hiển thị matrix có `matrix.trackId === team.trackId`.

Vị trí:

- `shm-frontend/src/pages/MyTeam.jsx:73`
- `shm-frontend/src/pages/Submission.jsx:29`

**Tác động:** đội được promotion không thấy vòng chung kết để nộp/cập nhật bài. Backend có thể tạo placeholder submission cho vòng sau nhưng UI vẫn không cung cấp đúng luồng.

**Khuyến nghị:** hiển thị final matrix khi đội đã được promotion; backend phải trả danh sách `eligibleMatrices` cho đội thay vì để frontend tự suy luận.

### H-04 — Dữ liệu nhạy cảm của người dùng bị trả quá mức

**Bằng chứng động:**

- Coordinator gọi `/api/users` nhận cả trường password hash.
- Một USER bất kỳ gọi `/api/users/{id}` xem được `studentCardUrl` của sinh viên khác.
- Danh sách đội trả email và mã sinh viên của thành viên cho mọi tài khoản đã đăng nhập.

Vị trí:

- `shm-backend/src/main/java/com/backend/controller/UserController.java:46`
- `shm-backend/src/main/java/com/backend/controller/UserController.java:166`
- `shm-backend/src/main/java/com/backend/controller/UserController.java:207`

**Khuyến nghị:** tuyệt đối không trả entity `User` trực tiếp; tạo DTO theo từng use case. Chỉ coordinator/admin được xem thẻ sinh viên, và nên dùng URL ký có hạn thay vì data URL công khai.

### H-05 — Có hai đường chấm điểm không đồng nhất

Ngoài `/api/scores/grade`, hệ thống có `/api/submissions/{id}/grade` ghi trực tiếp vào submission, bỏ qua `Score`, rubric nhiều tiêu chí và audit chấm điểm.

Vị trí:

- `shm-backend/src/main/java/com/backend/controller/ScoreController.java:18`
- `shm-backend/src/main/java/com/backend/controller/SubmissionController.java:58`

**Tác động:** cùng một nghiệp vụ có hai nguồn sự thật, dễ tạo điểm không truy vết được.

**Khuyến nghị:** xóa hoặc khóa endpoint chấm trực tiếp; mọi thao tác chấm phải đi qua một service, audit đầy đủ và có optimistic locking.

### H-06 — Coordinator có thể thay đổi trạng thái tài khoản qua endpoint ít bảo vệ hơn

`AdminController` có kiểm tra không tự khóa và bảo vệ admin cuối cùng, nhưng `UserController /users/{id}/status` cho COORDINATOR/ADMIN đổi trạng thái trực tiếp mà không có các kiểm tra đó.

Vị trí:

- `shm-backend/src/main/java/com/backend/controller/UserController.java:176-186`
- `shm-backend/src/main/java/com/backend/controller/AdminController.java:81-103`

**Tác động:** coordinator có thể khóa admin hoặc tự khóa qua đường API thứ hai.

**Khuyến nghị:** chỉ giữ một endpoint quản lý trạng thái; policy quyền hạn đặt ở service và kiểm thử đầy đủ các trường hợp self/last-admin/role hierarchy.

### H-07 — Mật khẩu đội private lưu và so sánh dạng rõ

Vị trí:

- `shm-backend/src/main/java/com/backend/entity/Team.java:25`
- `shm-backend/src/main/java/com/backend/service/TeamService.java:266-282`

**Khuyến nghị:** hash PIN/mật khẩu; không bao giờ trả trường này trong response; thêm giới hạn thử sai và thời gian khóa ngắn.

### H-08 — Ràng buộc giải thưởng sai phạm vi sự kiện

**Bằng chứng động:** API cho phép tạo giải của event 2 và gán cho team thuộc event 1.

**Khuyến nghị:** kiểm tra `prize.event.id == team.event.id`; chỉ cho trao giải khi kết quả đã khóa/publish; thêm unique constraint nếu một hạng chỉ được trao một lần.

### H-09 — Một người chỉ có thể tham gia một đội trên toàn hệ thống

`existsByUser(user)` được dùng cho tạo đội, mời thành viên, duyệt request và join private.

Vị trí:

- `shm-backend/src/main/java/com/backend/repository/TeamMemberRepository.java:19`
- `shm-backend/src/main/java/com/backend/service/TeamService.java:53`

**Tác động:** người đã thi một mùa không thể tham gia sự kiện sau, trái với mô hình nhiều mùa và lịch sử thành tích.

**Khuyến nghị:** ràng buộc một đội **trong cùng event**, tức `(event_id, user_id)`, thay vì một đội toàn hệ thống.

### H-10 — JWT secret và thông tin kết nối nằm trong source config

JWT secret và credential hạ tầng được đặt trực tiếp trong cấu hình/source. Không ghi lại giá trị nhạy cảm trong báo cáo này.

Vị trí: `shm-backend/src/main/resources/application.properties` và `JwtProvider`.

**Khuyến nghị:** chuyển toàn bộ secret sang biến môi trường/secret manager, xoay lại các secret đã commit, tách profile dev/test/prod và không dùng `ddl-auto=update` ở production.

## 7. Phát hiện mức Medium

### M-01 — Sai mã HTTP và thông báo lỗi chung

`GlobalExceptionHandler` bắt mọi `Exception` và trả HTTP 400 với “Lỗi hệ thống không xác định”. Vì vậy:

- Tài khoản pending báo lỗi hệ thống chung.
- Truy cập không đúng vai trò trả 400 thay vì 401/403.
- Validation/domain error khó phân biệt với lỗi server.

Vị trí: `shm-backend/src/main/java/com/backend/exception/GlobalExceptionHandler.java:27-34`

### M-02 — Route frontend thiếu guard theo vai trò

Các trang student approval, staff, audit, grading, scoring stats, events và submissions không được bao bằng route guard phù hợp. Menu có ẩn, nhưng người dùng vẫn gõ URL trực tiếp.

**Bằng chứng động:** tài khoản judge mở được `/dashboard/student-approval`; trang render rồi mới hiện lỗi API chung.

Vị trí: `shm-frontend/src/App.jsx`

Backend vẫn là lớp bảo vệ chính, nhưng frontend cần guard để tránh lộ UI và trải nghiệm lỗi.

### M-03 — Leaderboard có dữ liệu demo thay cho dữ liệu thật

Khi API lỗi hoặc không có ranking, frontend hiển thị `demoWinners`.

Vị trí:

- `shm-frontend/src/pages/Leaderboard.jsx:214`
- `shm-frontend/src/pages/Homepage.jsx:44`

Điều này có thể khiến người dùng tưởng kết quả giả là kết quả chính thức. Nên hiển thị trạng thái trống/lỗi rõ ràng.

### M-04 — Leaderboard chưa có phạm vi event/track/round chuẩn

Repository lấy tất cả submission đã chấm trên toàn hệ thống và sắp xếp theo `Submission.score`. Có nguy cơ:

- Nhiều bài của cùng một đội cùng xuất hiện.
- Trộn nhiều event, track và round.
- Không xử lý đồng hạng.
- Không có quy tắc kết hợp điểm qua các vòng.

Nên thiết kế bảng kết quả theo khóa `(event, round, track, team)` và snapshot khi công bố.

### M-05 — Cập nhật bài không xóa/đóng điểm và audit cũ

Khi leader đổi bài hoặc matrix, submission bị reset trường điểm/feedback nhưng các bản ghi `Score` và `ScoringAuditLog` liên quan có thể vẫn còn. Lần chấm sau có thể dùng dữ liệu cũ hoặc yêu cầu “edit reason” không đúng ngữ cảnh.

**Khuyến nghị:** version hóa submission; score gắn với `submission_version`; khi nộp lại phải tạo version mới và đóng bộ điểm cũ.

### M-06 — Thiếu validation sự kiện

**Bằng chứng động:** API chấp nhận thứ tự thời gian không hợp lệ. `roundCount = 1` không bị từ chối mà âm thầm lưu thành 2.

Vị trí: `shm-backend/src/main/java/com/backend/service/EventService.java:61`

Nên validate tên, năm, registrationStart/end, eventStart/end, số vòng, track, judge/mentor tối thiểu và không tự sửa dữ liệu đầu vào mà không báo người dùng.

### M-07 — Thiếu ràng buộc đội

Backend chưa bảo đảm:

- Số thành viên 2–5.
- User/event đang hợp lệ khi mời hoặc tham gia.
- Mỗi user một đội trong cùng event.
- Tên đội chỉ unique trong event; hiện có xu hướng unique toàn hệ thống.
- Invite cần người được mời chấp nhận.
- Luồng rời đội, giải tán đội, chuyển leader.
- Chống race condition khi duyệt nhiều request đồng thời.

### M-08 — “Chat realtime” thực tế là polling

Frontend gọi lại API khoảng mỗi 2,5 giây, không dùng WebSocket/SSE.

Vị trí:

- `shm-frontend/src/pages/TeamChat.jsx:90`
- `shm-frontend/src/pages/TeamExplorer.jsx:363`

Thiếu validate độ dài/nội dung phía server, trạng thái đọc thực, xóa/moderation và attachment.

### M-09 — Thiếu mentor notes và cơ chế gán mentor đúng như đặc tả

Đặc tả yêu cầu mentor được gán đội và ghi nhận nhận xét. Code hiện chủ yếu gán mentor theo track/matrix, cho thấy mentor xem nhiều đội trong track; chưa có mentor note riêng.

### M-10 — Thiếu nghiệp vụ flag bài nộp

Entity có trường flag nhưng chưa có luồng controller/service/UI hoàn chỉnh để judge báo bài đáng ngờ, coordinator xử lý và lưu audit.

### M-11 — Chứng nhận tải về không phải PDF

UI tạo một file HTML:

Vị trí: `shm-frontend/src/pages/Profile.jsx:25`

Đặc tả yêu cầu PDF. File hiện chưa có mã xác minh, QR/signature, chống sửa hoặc trang verify công khai. Nội dung HTML cũng cần escape dữ liệu người dùng.

### M-12 — Profile thiếu nickname

Đặc tả cho phép đổi nickname, avatar và password. Code hiện hỗ trợ avatar/password nhưng chưa có nickname riêng.

### M-13 — Upload ảnh/bài nộp chưa phải luồng upload an toàn

- Thẻ sinh viên và avatar được đọc thành base64/data URL và lưu trong database.
- Backend chưa kiểm tra chặt content type, kích thước, malware hoặc quyền tải.
- Bài nộp là một URL do client gửi, chưa có storage/upload lifecycle.
- Chưa thấy xử lý đầy đủ schema biểu mẫu nộp bài theo từng event.

### M-14 — OTP và đăng nhập thiếu chống brute force

OTP được lưu trong bộ nhớ tiến trình với TTL, mất khi restart, không có giới hạn số lần thử hoặc rate limit rõ ràng. Login và PIN đội cũng chưa có rate limit.

### M-15 — Thống kê “inter-rater” chưa đo độ đồng thuận thực sự

Trang thống kê hiện chủ yếu dựa trên điểm scalar của submission và lịch sử chỉnh sửa, chưa tính phân phối từng judge, variance, bias, ICC/Kappa hoặc phát hiện outlier.

### M-16 — Thông báo còn thiếu phạm vi nghiệp vụ

Thiếu gửi theo cá nhân/event/team, lập lịch, hạn hết hiệu lực, deep link hành động và đánh dấu đọc từng thông báo. Trang notification có xu hướng đánh dấu tất cả đã đọc khi mở.

## 8. Phát hiện mức Low / chất lượng kỹ thuật

- 9 cảnh báo lint cần xử lý, đặc biệt dependency của React Hook có thể gây dữ liệu cũ hoặc polling sai.
- Bundle chính lớn, chưa lazy-load theo route.
- Ảnh `OIP.png` khoảng 6,9 MB cần nén/chuyển WebP/AVIF.
- API base URL frontend đang hard-code local thay vì dùng biến môi trường.
- `spring.jpa.show-sql=true`, `ddl-auto=update` và đường dẫn PostgreSQL binary cố định không phù hợp production.
- Backend chỉ có một test context load; chưa có regression suite.
- Một số thông báo tiếng Việt có dấu hiệu lỗi encoding.
- Redis dependency/cấu hình đang tạo warning nhưng chưa có use case rõ ràng.

## 9. Thứ tự sửa được đề xuất

### P0 — Phải sửa trước khi demo chấm điểm thật

1. Sửa mô hình tổng hợp điểm nhiều judge; giới hạn điểm và xác thực rubric.
2. Thêm trạng thái công bố; chặn toàn bộ kết quả chưa publish.
3. Chặn token của tài khoản không còn `APPROVED`.
4. Đóng endpoint chấm điểm trực tiếp và thống nhất audit.
5. Che password hash, student card và dữ liệu cá nhân khỏi response không cần thiết.
6. Thêm policy event/round/deadline cho mọi thao tác team/submission.
7. Sửa eligibility của vòng chung kết.

### P1 — Sửa trước khi pilot với người dùng

1. Unique constraint cho team membership, submission và score.
2. Sửa ràng buộc prize/team/event và quyền thay đổi trạng thái tài khoản.
3. Thiết kế leaderboard theo event/track/round và xử lý tie.
4. Version hóa bài nộp, điểm và audit.
5. Bổ sung validation event/team/member.
6. Di chuyển secret/config ra khỏi source.
7. Thêm route guard và thông báo lỗi 401/403/validation đúng chuẩn.

### P2 — Hoàn thiện đúng đặc tả

1. Mentor notes, flag workflow và notification nâng cao.
2. WebSocket/SSE cho chat và unread state.
3. PDF certificate có verify.
4. Nickname/profile.
5. Upload storage an toàn và submission form schema.
6. Dashboard inter-rater thực sự.
7. Tối ưu bundle, ảnh và cấu hình deployment.

## 10. Bộ test hồi quy tối thiểu cần bổ sung

Backend integration tests nên có ít nhất:

1. Banned/pending user không dùng được cả token cũ và token mới.
2. USER/STAFF/COORDINATOR/ADMIN nhận đúng 401/403 theo endpoint.
3. Một user được tham gia một đội mỗi event nhưng được thi nhiều event.
4. Không create/join team ngoài registration window hoặc vượt 5 thành viên.
5. Chỉ leader được nộp; không trùng `(team, matrix)`.
6. Không nộp/cập nhật sau deadline hoặc sang matrix không eligible.
7. Final matrix chỉ dành cho đội đã promotion.
8. Điểm 0–100, rubric hợp lệ, một score/judge/submission, tổng hợp đúng nhiều judge.
9. Re-submit tạo version mới và không tái sử dụng score cũ.
10. Leaderboard chưa publish trả rỗng/404; publish mới hiển thị đúng event/round/track.
11. Prize không nhận team khác event.
12. DTO user không bao giờ chứa password; student card chỉ đúng role được xem.
13. Last-admin/self-lock policy áp dụng qua mọi endpoint.
14. Backup tạo/kiểm tra file; restore chỉ chạy trong database test riêng.

## 11. Tệp hỗ trợ kiểm thử

Script smoke test API có hoàn tác dữ liệu:

`D:\seal\qa\seal_business_smoke.ps1`

Sau lần chạy cuối, dữ liệu kiểm thử tạm đã được dọn; số event, leaderboard seed và danh sách backup trở về trạng thái trước kiểm thử.
