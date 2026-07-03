import React, { useState, useEffect } from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { Link } from 'react-router-dom';

export default function Home() {
    // Trạng thái giải đấu mẫu để test. Đổi thành 'UPCOMING' để xem giao diện đếm ngược đăng ký
    const [eventStatus, setEventStatus] = useState('FINISHED');

    // State xử lý bộ đếm ngược thời gian hết hạn đăng ký
    const [countdown, setCountdown] = useState({ days: 3, hours: 12, mins: 45, secs: 30 });

    useEffect(() => {
        if (eventStatus !== 'UPCOMING') return;
        const timer = setInterval(() => {
            setCountdown(prev => {
                if (prev.secs > 0) return { ...prev, secs: prev.secs - 1 };
                return { ...prev, secs: 59, mins: prev.mins > 0 ? prev.mins - 1 : 59 };
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [eventStatus]);

    return (
        <div className="min-h-screen flex flex-col bg-white antialiased">
            <Header />

            {/* 🌟 1. HERO SECTION (BỌC ẢNH NỀN KIỂU MẪU ĐÃ GỬI) */}
            <div className="relative min-h-[550px] flex items-center justify-center text-center px-4 bg-slate-900 overflow-hidden">
                {/* Ảnh nền mờ phía sau */}
                <div className="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?q=80&w=1200')] bg-cover bg-center opacity-30 blur-[2px]"></div>

                {/* Khối bọc nội dung mờ phủ màu sáng */}
                <div className="relative max-w-4xl w-full bg-white/5 backdrop-blur-md rounded-[40px] border border-white/10 p-8 sm:p-16 space-y-6 shadow-2xl">
                    <p className="text-xs sm:text-sm font-extrabold text-[#1E5BB8] bg-blue-50/10 px-4 py-1.5 rounded-full inline-block uppercase tracking-widest border border-white/10">
                        🏆 GIẢI ĐẤU ĐÔI NAM DO SAIGON REF TEAM TỔ CHỨC
                    </p>

                    <h1 className="text-4xl sm:text-7xl font-black text-white tracking-tight leading-none uppercase drop-shadow">
                        T&T CHAMPIONS <br/> CUP 2026
                    </h1>

                    {/* --- TRƯỜNG HỢP A: GIẢI ĐẤU ĐÃ KẾT THÚC (MẪU BRO GỬI) --- */}
                    {eventStatus === 'FINISHED' && (
                        <div className="space-y-6 transform transition-all duration-500">
                            <div className="inline-flex items-center gap-2 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-5 py-2 rounded-full text-xs font-bold uppercase tracking-wider">
                                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                                🏆 Giải đấu đã kết thúc
                            </div>
                            <p className="text-sm text-slate-300 font-medium">Cảm ơn tất cả VĐV, trọng tài và người hâm mộ!</p>
                            <Link to="/results" className="inline-flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase tracking-wider px-8 py-3.5 rounded-xl shadow-lg transition-all active:scale-95">
                                📊 Xem kết quả chi tiết
                            </Link>
                        </div>
                    )}

                    {/* --- TRƯỜNG HỢP B: ĐANG MỞ ĐĂNG KÝ + ĐẾM NGƯỢC --- */}
                    {eventStatus === 'UPCOMING' && (
                        <div className="space-y-6 transform transition-all duration-500">
                            <div className="inline-flex items-center gap-2 bg-amber-500/20 text-amber-400 border border-amber-500/30 px-5 py-2 rounded-full text-xs font-bold uppercase tracking-wider">
                                🔥 Đang mở cổng đăng ký thi đấu
                            </div>

                            {/* Khối đồng hồ đếm ngược thời gian */}
                            <div className="flex justify-center gap-3 sm:gap-4 text-white">
                                {[
                                    { label: 'Ngày', value: countdown.days },
                                    { label: 'Giờ', value: countdown.hours },
                                    { label: 'Phút', value: countdown.mins },
                                    { label: 'Giây', value: countdown.secs }
                                ].map((item, index) => (
                                    <div key={index} className="bg-slate-900/60 border border-white/10 rounded-2xl w-16 h-16 sm:w-20 sm:h-20 flex flex-col justify-center items-center shadow-lg">
                                        <span className="text-xl sm:text-2xl font-black tracking-normal">{String(item.value).padStart(2, '0')}</span>
                                        <span className="text-[9px] sm:text-[10px] text-slate-400 uppercase font-bold tracking-wider mt-0.5">{item.label}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="pt-2">
                                <Link to="/register" className="inline-flex items-center gap-2 bg-[#1E5BB8] hover:bg-blue-700 text-white font-bold text-xs uppercase tracking-wider px-10 py-4 rounded-xl shadow-xl transition-all hover:scale-105 active:scale-95">
                                    📝 Đăng Ký Đội Thi Đấu Ngay
                                </Link>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* 🚀 ĐƠN VỊ PHÁT TRIỂN NÊN THÊM CÁC KHỐI NÀY Ở DƯỚI ĐỂ TRANG CHỦ HOÀN HẢO HƠN */}
            <main className="flex-grow max-w-7xl mx-auto w-full px-6 py-16 space-y-20">

                {/* SECTION 1: TÓM TẮT BẢNG VÀNG KẾT QUẢ (Nếu giải đã kết thúc) */}
                {eventStatus === 'FINISHED' && (
                    <section className="space-y-6 animate-fade-in">
                        <div className="text-center">
                            <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight">🏆 Bảng Vàng Vinh Danh Nhà Vô Địch</h2>
                            <p className="text-xs text-slate-500 mt-1 font-medium">Những gương mặt xuất sắc nhất mùa giải Cup 2026</p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto pt-4">
                            {/* Á Quân 1 */}
                            <div className="bg-slate-50 rounded-2xl p-6 text-center border border-slate-100 flex flex-col justify-center order-2 md:order-1">
                                <span className="text-3xl">🥈</span>
                                <h4 className="font-bold text-slate-800 text-sm mt-2">Đội Tam & Chí</h4>
                                <p className="text-xs text-slate-500 font-bold">Giải Nhì (Á Quân)</p>
                            </div>
                            {/* Nhà Vô Địch */}
                            <div className="bg-gradient-to-b from-amber-50 to-amber-100/30 rounded-3xl p-8 text-center border-2 border-amber-400 flex flex-col justify-center shadow-xl scale-105 order-1 md:order-2">
                                <span className="text-5xl animate-bounce">👑</span>
                                <h4 className="font-black text-slate-900 text-base mt-3">Đội SEAL Warrior</h4>
                                <p className="text-xs text-amber-700 font-black uppercase tracking-wider mt-1">Quán Quân Vô Địch</p>
                            </div>
                            {/* Đồng Giải Ba */}
                            <div className="bg-slate-50 rounded-2xl p-6 text-center border border-slate-100 flex flex-col justify-center order-3">
                                <span className="text-3xl">🥉</span>
                                <h4 className="font-bold text-slate-800 text-sm mt-2">Đội FPT Ref</h4>
                                <p className="text-xs text-slate-500 font-bold">Giải Ba</p>
                            </div>
                        </div>
                    </section>
                )}

                {/* SECTION 2: THÔNG TIN TỔNG QUAN GIẢI ĐẤU */}
                <section id="info" className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
                    <div className="space-y-4">
                        <h3 className="text-xl font-extrabold text-slate-900 uppercase">Thông tin giải đấu tổng quan</h3>
                        <p className="text-sm text-slate-600 leading-relaxed font-medium">
                            Giải đấu quy tụ hơn 32 cặp vận động viên xuất sắc tham gia tranh tài ở nội dung đôi nam. Với cơ cấu giải thưởng lớn cùng hệ thống trọng tài chuẩn quốc gia từ Saigon Ref Team đảm bảo tính minh bạch, cống hiến những trận cầu mãn nhãn.
                        </p>
                        <div className="grid grid-cols-2 gap-4 pt-2">
                            <div className="p-4 bg-slate-50 rounded-xl"><h5 className="text-sm font-black text-[#1E5BB8]">32 Cặp Đấu</h5><p className="text-[11px] text-slate-500 font-medium">Tham gia tranh tài</p></div>
                            <div className="p-4 bg-slate-50 rounded-xl"><h5 className="text-sm font-black text-[#1E5BB8]">Cụm Sân Đại Học</h5><p className="text-[11px] text-slate-500 font-medium">Địa điểm thi đấu</p></div>
                        </div>
                    </div>
                    {/* Hình ảnh banner hoặc minh họa sân đấu */}
                    <div className="h-64 rounded-3xl bg-slate-100 overflow-hidden border shadow-inner">
                        <img src="https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?q=80&w=600" alt="Sân đấu" className="w-full h-full object-cover" />
                    </div>
                </section>
            </main>

            <Footer />
        </div>
    );
}