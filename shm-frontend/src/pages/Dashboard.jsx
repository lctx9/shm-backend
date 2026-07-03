import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function Dashboard() {
    const role = localStorage.getItem('role') || 'Guest';

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
        <div className="min-h-screen bg-[#F1F5F9] px-4 py-8 sm:px-6 lg:px-8 antialiased selection:bg-[#1E5BB8]/10 selection:text-[#1E5BB8]">
            {/* Khối bọc chính sử dụng hiệu ứng fadeInUp đồng bộ hệ thống */}
            <div className="max-w-[1100px] mx-auto bg-white rounded-[32px] shadow-xl border border-slate-100 p-6 sm:p-10 md:p-12 transition-all duration-500 ease-out transform opacity-0 translate-y-4 animate-[fadeInUp_0.6s_ease-out_forwards]">

                {/* Phần Header Tiêu đề */}
                <div className="mb-8 border-b border-slate-100 pb-6 flex justify-between items-start">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1E293B] tracking-tight mb-2">
                            Chào mừng đến với Hệ thống SEAL Hackathon
                        </h1>
                        <p className="text-[13px] text-slate-600 leading-relaxed flex items-center gap-2">
                            <span>Bạn đang đăng nhập với vai trò:</span>
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#1E5BB8]/10 text-[#1E5BB8] uppercase tracking-wide">
                                {role || 'Khách'}
                            </span>
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

                {/* Grid 3 cột cho các thẻ thống kê phẳng mượt mà */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                    {/* Thẻ 1: Giải đấu */}
                    <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200/60 transition-all duration-300 ease-out hover:scale-[1.02] hover:border-[#1E5BB8]/30 group">
                        <div className="flex justify-between items-start">
                            <h3 className="text-sm font-bold text-slate-800 transition-colors duration-200 group-hover:text-[#1E5BB8]">
                                Giải đấu đang diễn ra
                            </h3>
                            <span className="p-1.5 bg-white rounded-lg border border-slate-200 text-slate-400 group-hover:text-[#1E5BB8] group-hover:border-[#1E5BB8]/20 transition-all">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 013 3h-15a3 3 0 013-3m9 0v-3.375c0-.621-.504-1.125-1.125-1.125h-2.25a1.125 1.125 0 00-1.125 1.125v3.375m9 0M16.5 13.5v-2.25a2.25 2.25 0 00-2.25-2.25h-4.5A2.25 2.25 0 007.5 11.25v2.25m9-6V5.25A2.25 2.25 0 0014.25 3h-4.5A2.25 2.25 0 007.5 5.25v2.25" />
                                </svg>
                            </span>
                        </div>
                        <p className="text-4xl font-extrabold text-[#1E293B] mt-4 tracking-tight">
                            {loading ? <span className="animate-pulse">...</span> : stats.activeEvents}
                        </p>
                    </div>

                    {/* Thẻ 2: Đội thi */}
                    <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200/60 transition-all duration-300 ease-out hover:scale-[1.02] hover:border-[#1E5BB8]/30 group">
                        <div className="flex justify-between items-start">
                            <h3 className="text-sm font-bold text-slate-800 transition-colors duration-200 group-hover:text-[#1E5BB8]">
                                Đội thi đăng ký
                            </h3>
                            <span className="p-1.5 bg-white rounded-lg border border-slate-200 text-slate-400 group-hover:text-[#1E5BB8] group-hover:border-[#1E5BB8]/20 transition-all">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
                                </svg>
                            </span>
                        </div>
                        <p className="text-4xl font-extrabold text-[#1E293B] mt-4 tracking-tight">
                            {loading ? <span className="animate-pulse">...</span> : stats.totalTeams}
                        </p>
                    </div>

                    {/* Thẻ 3: Bài thi */}
                    <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200/60 transition-all duration-300 ease-out hover:scale-[1.02] hover:border-[#1E5BB8]/30 group">
                        <div className="flex justify-between items-start">
                            <h3 className="text-sm font-bold text-slate-800 transition-colors duration-200 group-hover:text-[#1E5BB8]">
                                Bài thi chờ chấm
                            </h3>
                            <span className="p-1.5 bg-white rounded-lg border border-slate-200 text-slate-400 group-hover:text-[#1E5BB8] group-hover:border-[#1E5BB8]/20 transition-all">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M11.35 3.836c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.03 0 1.9.693 2.166 1.636m-5.8 0A2.229 2.229 0 0010.5 4.5v.75m7.416-1.414A2.23 2.23 0 0118.75 4.5v.75m0 12.75a2.25 2.25 0 01-2.25 2.25h-9a2.25 2.25 0 01-2.25-2.25v-9a2.25 2.25 0 012.25-2.25h9a2.25 2.25 0 012.25 2.25v9z" />
                                </svg>
                            </span>
                        </div>
                        <p className="text-4xl font-extrabold text-[#1E293B] mt-4 tracking-tight">
                            {loading ? <span className="animate-pulse">...</span> : stats.pendingSubmissions}
                        </p>
                    </div>

                </div>

            </div>
        </div>
    );
}