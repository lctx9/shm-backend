import React from 'react';
import { Link } from 'react-router-dom';
import heroCourt from '../assets/blue-hero-reference.png';

const features = [
    {
        title: 'Quản lý giải đấu',
        copy: 'Tạo sự kiện, chia vòng thi, theo dõi đội tham gia và điều phối toàn bộ mùa giải từ một nơi.',
        icon: (
            <path strokeLinecap="round" strokeLinejoin="round" d="M8 21h8m-4-4v4m6-17v5a6 6 0 0 1-12 0V4h12ZM5 7H3v2a4 4 0 0 0 4 4m12-6h2v2a4 4 0 0 1-4 4" />
        ),
    },
    {
        title: 'Đội thi & thành viên',
        copy: 'Hỗ trợ tạo đội, mời thành viên, quản lý hồ sơ và giữ thông tin thi đấu luôn rõ ràng.',
        icon: (
            <path strokeLinecap="round" strokeLinejoin="round" d="M16 11a4 4 0 1 0-8 0m8 0a4 4 0 1 1-8 0m8 0v1a4 4 0 0 1-8 0v-1m12 9a8 8 0 0 0-16 0m20 0a6.5 6.5 0 0 0-4.5-6.2" />
        ),
    },
    {
        title: 'Nộp bài & chấm điểm',
        copy: 'Thí sinh nộp bài trực tuyến, giám khảo chấm minh bạch và bảng xếp hạng được cập nhật nhanh.',
        icon: (
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M7 3.75h10A2.25 2.25 0 0 1 19.25 6v12A2.25 2.25 0 0 1 17 20.25H7A2.25 2.25 0 0 1 4.75 18V6A2.25 2.25 0 0 1 7 3.75Z" />
        ),
    },
];

function Icon({ children }) {
    return (
        <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" aria-hidden="true">
            {children}
        </svg>
    );
}

export default function Homepage() {
    return (
        <div className="page-shell">
            <header className="site-header">
                <div className="site-header-inner">
                    <Link to="/" className="flex items-center gap-3" aria-label="SEAL trang chủ">
                        <span className="brand-mark">
                            <span className="brand-mark-text">SEAL</span>
                        </span>
                    </Link>

                    <nav className="hidden items-center gap-9 md:flex" aria-label="Điều hướng chính">
                        <Link to="/" className="nav-link-active">Trang chủ</Link>
                        <Link to="/leaderboard" className="nav-link-item">Kết quả</Link>
                        <Link to="/register" className="nav-link-item">VĐV</Link>
                        <Link to="/events" className="nav-link-item">Sự kiện</Link>
                    </nav>

                    <div className="flex items-center gap-1 sm:gap-2">
                        <button className="icon-button" type="button" aria-label="Tìm kiếm">
                            <Icon>
                                <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-4.35-4.35M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z" />
                            </Icon>
                        </button>
                        <Link className="icon-button" to="/leaderboard" aria-label="Bảng xếp hạng">
                            <Icon>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M12 21a9 9 0 1 0 0-18m0 18a9 9 0 1 1 0-18m0 18c2.25-2.45 3.5-5.38 3.5-9S14.25 5.45 12 3m0 18c-2.25-2.45-3.5-5.38-3.5-9S9.75 5.45 12 3M3.6 9h16.8M3.6 15h16.8" />
                            </Icon>
                        </Link>
                        <Link className="icon-button" to="/login" aria-label="Đăng nhập">
                            <Icon>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 7.5a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.5 20.25a7.5 7.5 0 0 1 15 0" />
                            </Icon>
                        </Link>
                    </div>
                </div>
            </header>

            <main>
                <section className="hero-stage">
                    <div className="hero-bg" style={{ backgroundImage: `url(${heroCourt})` }} />
                    <div className="hero-overlay" />

                    <div className="hero-content animate-fade-up">
                        <p className="hero-eyebrow">Giải đấu Hackathon SEAL 2026</p>
                        <h1 className="hero-title">
                            SEAL
                            <br />
                            CHAMPIONS
                        </h1>
                        <div className="mt-9">
                            <span className="badge-status-pill">
                                <Icon>
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M8 21h8m-4-4v4m6-17v5a6 6 0 0 1-12 0V4h12Z" />
                                </Icon>
                                Nền tảng đang mở
                            </span>
                        </div>
                        <p className="hero-subtitle">
                            Tổ chức mùa giải, quản lý đội thi, nhận bài nộp và công bố kết quả với giao diện sáng,
                            rõ ràng và mang tinh thần thể thao hiện đại.
                        </p>
                        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
                            <Link to="/events" className="btn-action-main">
                                <Icon>
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 5v14m7-7H5" />
                                </Icon>
                                Xem giải đấu
                            </Link>
                            <Link to="/leaderboard" className="btn-secondary">
                                Xem kết quả
                            </Link>
                        </div>
                    </div>
                </section>

                <section className="section-shell">
                    <div className="mb-8">
                        <h2 className="section-title">Vận hành mùa giải gọn hơn</h2>
                        <p className="section-copy">
                            Bộ công cụ chung cho ban tổ chức, giám khảo và đội thi, tối ưu cho thao tác nhanh trên nền trắng xanh dễ nhìn.
                        </p>
                    </div>

                    <div className="grid gap-5 md:grid-cols-3">
                        {features.map((feature) => (
                            <article className="feature-card" key={feature.title}>
                                <span className="feature-icon">
                                    <Icon>{feature.icon}</Icon>
                                </span>
                                <h3 className="text-base font-black uppercase tracking-[0.08em] text-[#071936]">
                                    {feature.title}
                                </h3>
                                <p className="mt-3 text-sm leading-7 text-[#5c6d83]">
                                    {feature.copy}
                                </p>
                            </article>
                        ))}
                    </div>
                </section>

                <section className="section-shell pt-0">
                    <div className="flex flex-col items-start justify-between gap-5 rounded-lg border border-[#d7e6f8] bg-white px-6 py-7 shadow-sm sm:flex-row sm:items-center">
                        <div>
                            <h2 className="text-lg font-black uppercase tracking-[0.08em] text-[#071936]">
                                Theo dõi bảng xếp hạng công khai
                            </h2>
                            <p className="mt-2 text-sm text-[#5c6d83]">
                                Người xem có thể cập nhật kết quả mà không cần đăng nhập.
                            </p>
                        </div>
                        <Link to="/leaderboard" className="btn-primary">
                            Mở bảng xếp hạng
                        </Link>
                    </div>
                </section>
            </main>

            <footer className="border-t border-[#d7e6f8] bg-white px-5 py-7 text-center text-xs font-semibold text-[#5c6d83]">
                © {new Date().getFullYear()} SEAL Hackathon Management System
            </footer>
        </div>
    );
}
