import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

export default function Header() {
    const navigate = useNavigate();
    const token = localStorage.getItem('token');

    // Giả định dữ liệu user được lưu khi đăng nhập thành công
    const user = JSON.parse(localStorage.getItem('user')) || { fullName: 'Thí sinh', avatarUrl: '' };
    const [showDropdown, setShowDropdown] = useState(false);

    const handleLogout = () => {
        localStorage.clear();
        navigate('/', { replace: true });
    };

    return (
        <header className="sticky top-0 z-50 w-full bg-white/90 backdrop-blur-md border-b border-slate-100 px-6 py-3">
            <div className="max-w-7xl mx-auto flex items-center justify-between">

                {/* 🧭 KHỐI MENU BÊN TRÁI (LOGO + NAV) */}
                <div className="flex items-center gap-8">
                    {/* Logo dự án */}
                    <Link to="/" className="flex items-center gap-2">
                        <span className="text-xl font-black text-[#1E5BB8] tracking-wider uppercase">SEAL</span>
                    </Link>

                    {/* Menu điều hướng thay đổi động */}
                    <nav className="hidden md:flex items-center gap-6 text-[13px] font-bold text-slate-600 uppercase tracking-wider">
                        <Link to="/" className="text-[#1E5BB8] border-b-2 border-[#1E5BB8] pb-1">Trang Chủ</Link>
                        <Link to="/events" className="hover:text-[#1E5BB8] transition-colors pb-1">Sự Kiện</Link>
                        <Link to="/leaderboard" className="hover:text-[#1E5BB8] transition-colors pb-1">Bảng Xếp Hạng</Link>

                        {/* Hiện thêm "Đội của tôi" nếu ĐÃ ĐĂNG NHẬP */}
                        {token && (
                            <Link to="/my-team" className="hover:text-[#1E5BB8] text-amber-600 transition-colors pb-1 animate-pulse">
                                ⚽ Đội Của Tôi
                            </Link>
                        )}

                        <Link to="/about" className="hover:text-[#1E5BB8] transition-colors pb-1">Về Chúng Tôi</Link>
                    </nav>
                </div>

                {/* 🔐 KHỐI GÓC PHẢI: ĐĂNG NHẬP HOẶC AVATAR CÁ NHÂN */}
                <div className="flex items-center gap-4">
                    {token ? (
                        /* ĐÃ ĐĂNG NHẬP: Hiện Avatar + Tên cá nhân */
                        <div className="relative">
                            <button
                                onClick={() => setShowDropdown(!showDropdown)}
                                className="flex items-center gap-2.5 px-3 py-1.5 rounded-full hover:bg-slate-50 border border-slate-200 transition-all"
                            >
                                <div className="w-8 h-8 rounded-full bg-blue-600 text-white font-bold text-sm flex items-center justify-center overflow-hidden border border-blue-200">
                                    {user.avatarUrl ? (
                                        <img src={user.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                                    ) : (
                                        user.fullName.charAt(0).toUpperCase()
                                    )}
                                </div>
                                <span className="text-xs font-bold text-slate-800">{user.fullName}</span>
                                <svg className="w-3 h-3 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" /></svg>
                            </button>

                            {/* Dropdown menu nhỏ khi bấm vào Avatar */}
                            {showDropdown && (
                                <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-100 py-1 z-50 animate-[fadeInUp_0.2s_ease-out]">
                                    <Link to="/dashboard" className="block px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">Vào Dashboard</Link>
                                    <button onClick={handleLogout} className="w-full text-left block px-4 py-2 text-xs font-semibold text-red-600 hover:bg-red-50">Đăng Xuất</button>
                                </div>
                            )}
                        </div>
                    ) : (
                        /* CHƯA ĐĂNG NHẬP: Hiện cụm nút login */
                        <div className="flex items-center gap-2">
                            <Link to="/login" className="px-4 py-2 text-xs font-bold text-slate-700 hover:text-[#1E5BB8] transition-all uppercase">Đăng Nhập</Link>
                            <Link to="/register" className="px-4 py-2 text-xs font-bold text-white bg-[#1E5BB8] rounded-xl hover:bg-[#164384] transition-all uppercase shadow-md shadow-blue-500/10">Đăng Ký</Link>
                        </div>
                    )}
                </div>

            </div>
        </header>
    );
}