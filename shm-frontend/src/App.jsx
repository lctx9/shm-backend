import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
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

// Hàm bảo vệ Route (Nếu chưa có token thì đá về Login)
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
                    {/* Khi vào trang chủ, tự động điều hướng đến Dashboard (Public) */}
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />

                    {/* Các trang đăng nhập / đăng ký độc lập */}
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />

                    {/* Layout chính của ứng dụng - Đã mở khóa (Public) */}
                    <Route path="/dashboard" element={<DashboardLayout />}>

                        {/* --- CÁC TRANG PUBLIC (Ai cũng vào xem được) --- */}
                        {/* Index route: Vừa vào /dashboard sẽ hiển thị trang Tổng quan */}
                        <Route index element={<Dashboard />} />
                        <Route path="leaderboard" element={<Leaderboard />} />

                        {/* --- CÁC TRANG PRIVATE (Bắt buộc phải đăng nhập) --- */}
                        <Route path="teams" element={
                            <ProtectedRoute><TeamExplorer /></ProtectedRoute>
                        } />
                        <Route path="my-team" element={
                            <ProtectedRoute><MyTeam /></ProtectedRoute>
                        } />
                        <Route path="submissions" element={
                            <ProtectedRoute><Submission /></ProtectedRoute>
                        } />
                        <Route path="events" element={
                            <ProtectedRoute><EventManagement /></ProtectedRoute>
                        } />
                        <Route path="users" element={
                            <ProtectedRoute><UserManagement /></ProtectedRoute>
                        } />
                        <Route path="grading" element={
                            <ProtectedRoute><Grading /></ProtectedRoute>
                        } />
                        <Route path="profile" element={
                            <ProtectedRoute><Profile /></ProtectedRoute>
                        } />

                    </Route>
                </Routes>
            </div>
        </BrowserRouter>
    );
}

export default App;