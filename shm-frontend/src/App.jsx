import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Homepage from './pages/Homepage'; // Chính là trang Home bọc Header/Footer động của bro
import Login from './pages/Login';
import Register from './pages/Register';
import DashboardLayout from './components/DashboardLayout';
import Dashboard from './pages/Dashboard';
import MyTeam from './pages/MyTeam';
import EventManagement from './pages/EventManagement';
import UserManagement from './pages/UserManagement';
import Grading from './pages/Grading';
import Submission from './pages/Submission';
import TeamExplorer from './pages/TeamExplorer';
import Leaderboard from './pages/Leaderboard';
import Profile from './pages/Profile';
import Events from './pages/Events';
import EventDetail from './pages/EventDetail';
import AuditLogs from './pages/AuditLogs';
import Notifications from './pages/Notifications';
import TeamChat from './pages/TeamChat';

// Hàm bảo vệ Route (Nếu chưa có token thì đá thẳng về Login, không cho xem Dashboard bậy)
const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem('token');
    if (!token) return <Navigate to="/login" replace />;
    return children;
};

function App() {
    return (
        <BrowserRouter>
            <div className="min-h-screen font-sans text-gray-900 bg-gray-50">
                <Routes>

                    {/* =========================================================
                        1. 🌐 LUỒNG TRANG CHỦ & CÁC TRANG PUBLIC (Không cần Login)
                       ========================================================= */}
                    {/* Vừa vào web sẽ hiện ngay trang Landing Page giống như mẫu giải đấu Pickleball */}
                    <Route path="/" element={<Homepage />} />
                    <Route path="/events" element={<Events />} />
                    <Route path="/events/:eventId" element={<EventDetail />} />

                    {/* Tuyến đường public xem giải đấu/bảng xếp hạng bên ngoài Landing Page */}
                    <Route path="/leaderboard" element={<Leaderboard />} />
                    {/* Bro có thể tạo thêm trang public hoặc giữ nguyên tùy ý ở đây */}


                    {/* =========================================================
                        2. 🔐 LUỒNG ĐĂNG NHẬP / ĐĂNG KÝ (Độc lập full màn hình)
                       ========================================================= */}
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />


                    {/* =========================================================
                        3. 📊 LUỒNG NỘI BỘ DASHBOARD MANAGEMENT (Bắt buộc Login)
                       ========================================================= */}
                    {/* Toàn bộ các trang quản lý nội bộ đều được bọc bởi ProtectedRoute */}
                    <Route path="/dashboard" element={
                        <ProtectedRoute>
                            <DashboardLayout />
                        </ProtectedRoute>
                    }>
                        {/* Index route: Vừa vào /dashboard sẽ hiển thị trang Tổng quan hệ thống */}
                        <Route index element={<Dashboard />} />

                        {/* Các chức năng quản lý riêng biệt phân quyền */}
                        <Route path="teams" element={<TeamExplorer />} />
                        <Route path="my-team" element={<MyTeam />} />
                        <Route path="submissions" element={<Submission />} />
                        <Route path="events" element={<EventManagement />} />
                        <Route path="users" element={<UserManagement />} />
                        <Route path="audit-logs" element={<AuditLogs />} />
                        <Route path="grading" element={<Grading />} />
                        <Route path="leaderboard" element={<Leaderboard />} />
                        <Route path="notifications" element={<Notifications />} />
                        <Route path="chat" element={<TeamChat />} />
                        <Route path="profile" element={<Profile />} />
                    </Route>

                    {/* Điều hướng các đường dẫn tầm bậy không tồn tại quay về trang chủ */}
                    <Route path="*" element={<Navigate to="/" replace />} />

                </Routes>
            </div>
        </BrowserRouter>
    );
}

export default App;
