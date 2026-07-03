import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function Dashboard() {
    const role = localStorage.getItem('role');

    // Khởi tạo state chứa dữ liệu thống kê
    const [stats, setStats] = useState({
        activeEvents: 0,
        totalTeams: 0,
        pendingSubmissions: 0
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchDashboardStats();
    }, []);

    const fetchDashboardStats = async () => {
        try {
            setLoading(true);
            setError(null);

            // Gọi API lấy dữ liệu thống kê.
            // Lưu ý: Backend cần có endpoint GET /api/stats
            const response = await axiosClient.get('/stats');

            // Cập nhật state nếu có dữ liệu trả về từ Spring Boot
            if (response.result) {
                setStats({
                    activeEvents: response.result.activeEvents || 0,
                    totalTeams: response.result.totalTeams || 0,
                    pendingSubmissions: response.result.pendingSubmissions || 0
                });
            }
        } catch (err) {
            setError('Không thể tải dữ liệu thống kê. Đang chờ API từ hệ thống.');
            console.error('Lỗi khi tải Dashboard:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-8 rounded-lg shadow border border-gray-200">
            <div className="flex justify-between items-center border-b pb-4 mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Chào mừng đến với Hệ thống SEAL Hackathon</h1>
                    <p className="mt-2 text-gray-600">
                        Bạn đang đăng nhập với vai trò: <span className="font-semibold text-indigo-600">{role || 'Khách'}</span>
                    </p>
                </div>
                <button
                    onClick={fetchDashboardStats}
                    disabled={loading}
                    className="px-4 py-2 bg-gray-100 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-200 text-gray-700 transition-colors disabled:opacity-50"
                >
                    🔄 Làm mới
                </button>
            </div>

            {error && (
                <div className="mb-6 p-4 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Thẻ: Giải đấu đang diễn ra */}
                <div className="bg-indigo-50 p-6 rounded-lg border border-indigo-100 flex flex-col justify-between">
                    <h3 className="text-lg font-semibold text-indigo-800">Giải đấu đang diễn ra</h3>
                    <p className="text-4xl font-bold text-indigo-600 mt-4">
                        {loading ? <span className="animate-pulse">...</span> : stats.activeEvents}
                    </p>
                </div>

                {/* Thẻ: Đội thi đăng ký */}
                <div className="bg-green-50 p-6 rounded-lg border border-green-100 flex flex-col justify-between">
                    <h3 className="text-lg font-semibold text-green-800">Đội thi đăng ký</h3>
                    <p className="text-4xl font-bold text-green-600 mt-4">
                        {loading ? <span className="animate-pulse">...</span> : stats.totalTeams}
                    </p>
                </div>

                {/* Thẻ: Bài thi chờ chấm */}
                <div className="bg-amber-50 p-6 rounded-lg border border-amber-100 flex flex-col justify-between">
                    <h3 className="text-lg font-semibold text-amber-800">Bài thi chờ chấm</h3>
                    <p className="text-4xl font-bold text-amber-600 mt-4">
                        {loading ? <span className="animate-pulse">...</span> : stats.pendingSubmissions}
                    </p>
                </div>
            </div>
        </div>
    );
}