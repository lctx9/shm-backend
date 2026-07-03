import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Bộ lọc tự động chèn Token vào Header trước khi request bay đi
axiosClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Bộ lọc tự động xử lý data trả về hoặc bắt lỗi tập trung
axiosClient.interceptors.response.use(
    (response) => {
        if (response.data) {
            return response.data; // Trả thẳng về cấu hình ApiResponse { code, message, result }
        }
        return response;
    },
    (error) => {
        if (error.response) {
            const status = error.response.status;

            // CHUẨN DEV: CHỈ đá ra trang Login nếu lỗi 401 (Hết hạn Token hoặc chưa đăng nhập)
            if (status === 401) {
                console.error("Lỗi 401: Token không hợp lệ hoặc đã hết hạn.");
                // Dọn dẹp sạch sẽ toàn bộ thông tin phiên cũ
                localStorage.removeItem('token');
                localStorage.removeItem('role');
                localStorage.removeItem('email');
                window.location.href = '/login';
            }
            // Nếu lỗi 403 (Cấm truy cập/Chưa cấu hình API), chỉ in ra Console chứ KHÔNG đá văng
            else if (status === 403) {
                console.error("Lỗi 403: Không có quyền truy cập. Vui lòng kiểm tra lại cấu hình @PreAuthorize ở Backend.");
            }
        }

        return Promise.reject(error.response?.data || error.message);
    }
);

export default axiosClient;