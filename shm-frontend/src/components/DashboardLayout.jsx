import React from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';

export default function DashboardLayout() {
    const navigate = useNavigate();
    const location = useLocation();

    // Lấy thông tin user từ LocalStorage
    const role = localStorage.getItem('role') || 'GUEST';
    const email = localStorage.getItem('email') || 'user@example.com';

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('email');
        navigate('/dashboard');
    };

    // Hàm kiểm tra xem tab nào đang được active theo chuẩn màu hệ thống [#1E5BB8]
    const isActive = (path) =>
        location.pathname === path
            ? 'bg-[#1E5BB8]/10 text-[#1E5BB8] font-bold'
            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 font-medium';

    return (
        <div className="flex h-screen bg-[#F1F5F9] antialiased">

            {/* 1. SIDEBAR BÊN TRÁI: Thiết kế phẳng, tối giản sạch sẽ */}
            <div className="w-64 bg-white flex flex-col border-r border-slate-200 shadow-sm">

                {/* Brand Header */}
                <div className="flex items-center justify-center h-16 border-b border-slate-100 px-6">
                    <h1 className="text-lg font-extrabold text-[#1E293B] uppercase tracking-wider">
                        SEAL Hackathon
                    </h1>
                </div>

                {/* Danh sách Menu điều hướng */}
                <div className="flex-1 overflow-y-auto py-6 px-3">
                    <nav className="space-y-1">

                        {/* Menu Chung: Tổng quan */}
                        <Link to="/dashboard" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard')}`}>
                            <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6a7.5 7.5 0 107.5 7.5h-7.5V6z" />
                                <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0013.5 3v7.5z" />
                            </svg>
                            <span>Tổng quan</span>
                        </Link>

                        {/* Menu dành riêng cho COORDINATOR / ADMIN */}
                        {(role === 'COORDINATOR' || role === 'ADMIN') && (
                            <>
                                <Link to="/dashboard/events" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard/events')}`}>
                                    <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 013 3h-15a3 3 0 013-3m9 0v-3.375c0-.621-.504-1.125-1.125-1.125h-2.25a1.125 1.125 0 00-1.125 1.125v3.375m9 0M16.5 13.5v-2.25a2.25 2.25 0 00-2.25-2.25h-4.5A2.25 2.25 0 007.5 11.25v2.25m9-6V5.25A2.25 2.25 0 0014.25 3h-4.5A2.25 2.25 0 007.5 5.25v2.25" />
                                    </svg>
                                    <span>Quản lý Giải đấu</span>
                                </Link>
                                <Link to="/dashboard/users" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard/users')}`}>
                                    <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
                                    </svg>
                                    <span>Quản lý Tài khoản (Duyệt)</span>
                                </Link>
                            </>
                        )}

                        {/* Menu dành riêng cho LEADER / MEMBER */}
                        {(role === 'LEADER' || role === 'MEMBER') && (
                            <>
                                <Link to="/dashboard/my-team" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard/my-team')}`}>
                                    <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751A11.956 11.956 0 0112 2.714z" />
                                    </svg>
                                    <span>Đội thi của tôi</span>
                                </Link>
                                <Link to="/dashboard/submissions" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard/submissions')}`}>
                                    <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
                                    </svg>
                                    <span>Nộp bài dự thi</span>
                                </Link>
                            </>
                        )}

                        {/* Menu dành riêng cho JUDGE */}
                        {role === 'JUDGE' && (
                            <Link to="/dashboard/grading" className={`group flex items-center gap-3 px-3 py-2.5 text-xs rounded-lg transition-all duration-200 ${isActive('/dashboard/grading')}`}>
                                <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v18m0-18l4 4m-4-4L8 7m4 14l4-4m-4 4l-4-4" />
                                </svg>
                                <span>Chấm bài thi</span>
                            </Link>
                        )}
                    </nav>
                </div>
            </div>

            {/* 2. KHU VỰC NỘI DUNG CHÍNH BÊN PHẢI */}
            <div className="flex-1 flex flex-col overflow-hidden">

                {/* Header thanh công cụ trên cùng */}
                <header className="h-16 flex items-center justify-between px-8 bg-white border-b border-slate-200/80 shadow-sm">
                    <h2 className="text-sm font-bold text-slate-800 capitalize tracking-tight">
                        {location.pathname.split('/').pop().replace('-', ' ') || 'Tổng quan'}
                    </h2>

                    <div className="flex items-center gap-5">
                        <div className="text-right">
                            <span className="block text-xs font-bold text-slate-800">{email}</span>
                            <span className="inline-block mt-0.5 px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600 uppercase tracking-wide">
                                {role}
                            </span>
                        </div>

                        {/* Nút đăng xuất Flat chuẩn chỉnh nhưng mượt mà */}
                        <button
                            onClick={handleLogout}
                            className="inline-flex items-center justify-center rounded-lg bg-red-50 text-red-600 border border-red-100 px-3.5 py-1.5 text-xs font-bold hover:bg-red-600 hover:text-white active:scale-[0.98] transition-all duration-200 ease-in-out"
                        >
                            Đăng xuất
                        </button>
                    </div>
                </header>

                {/* Phân vùng chứa các nội dung Route con (Outlet) */}
                <main className="flex-1 overflow-x-hidden overflow-y-auto bg-[#F1F5F9] p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}