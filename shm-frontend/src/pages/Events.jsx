import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

function formatDate(value) {
    if (!value) return 'Chưa cập nhật';
    return new Date(value).toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    });
}

function eventStatus(event) {
    const now = new Date();
    const regStart = event.regStartDate ? new Date(event.regStartDate) : null;
    const regEnd = event.regEndDate ? new Date(event.regEndDate) : null;

    if (regStart && now < regStart) return 'Sắp mở đăng ký';
    if (regEnd && now > regEnd) return 'Đã đóng đăng ký';
    return 'Đang mở đăng ký';
}

export default function Events() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchEvents = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/events');
            setEvents(response.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải danh sách giải đấu.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchEvents();
    }, []);

    return (
        <div className="min-h-screen bg-[#f4f8ff]">
            <header className="site-header">
                <div className="site-header-inner">
                    <Link to="/" className="brand-mark">
                        <span className="brand-mark-text">SEAL</span>
                    </Link>
                    <nav className="hidden items-center gap-9 md:flex">
                        <Link to="/" className="nav-link-item">Trang chủ</Link>
                        <Link to="/events" className="nav-link-active">Giải đấu</Link>
                        <Link to="/leaderboard" className="nav-link-item">Kết quả</Link>
                    </nav>
                    <Link to="/login" className="btn-secondary">Đăng nhập</Link>
                </div>
            </header>

            <main className="section-shell">
                <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                    <div>
                        <h1 className="section-title">Danh sách giải đấu</h1>
                        <p className="section-copy">
                            Dữ liệu được tải trực tiếp từ bảng Hackathon Event, Track, Round và Team trong backend.
                        </p>
                    </div>
                    <button type="button" onClick={fetchEvents} className="btn-secondary">Làm mới</button>
                </div>

                {error && (
                    <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                        {error}
                    </div>
                )}

                {loading ? (
                    <div className="rounded-lg border border-[#d7e6f8] bg-white p-8 text-center text-[#5c6d83]">
                        Đang tải giải đấu...
                    </div>
                ) : events.length === 0 ? (
                    <div className="rounded-lg border border-[#d7e6f8] bg-white p-8 text-center text-[#5c6d83]">
                        Chưa có giải đấu nào trong database.
                    </div>
                ) : (
                    <div className="grid gap-5 md:grid-cols-2">
                        {events.map((event) => (
                            <article key={event.id} className="feature-card">
                                <div className="mb-4 flex items-start justify-between gap-4">
                                    <div>
                                        <p className="text-xs font-black uppercase tracking-[0.18em] text-[#0f63c9]">
                                            {event.season} {event.year}
                                        </p>
                                        <h2 className="mt-2 text-xl font-black uppercase tracking-[0.06em] text-[#071936]">
                                            {event.name}
                                        </h2>
                                    </div>
                                    <span className="rounded-full border border-[#8ec5ff] bg-[#eaf3ff] px-3 py-1 text-xs font-black uppercase text-[#0f63c9]">
                                        {eventStatus(event)}
                                    </span>
                                </div>

                                <dl className="grid gap-3 text-sm text-[#5c6d83] sm:grid-cols-2">
                                    <div>
                                        <dt className="font-bold text-[#0b1f3f]">Mở đăng ký</dt>
                                        <dd>{formatDate(event.regStartDate)}</dd>
                                    </div>
                                    <div>
                                        <dt className="font-bold text-[#0b1f3f]">Đóng đăng ký</dt>
                                        <dd>{formatDate(event.regEndDate)}</dd>
                                    </div>
                                    <div>
                                        <dt className="font-bold text-[#0b1f3f]">Hạng mục</dt>
                                        <dd>{event.tracks?.length || 0}</dd>
                                    </div>
                                    <div>
                                        <dt className="font-bold text-[#0b1f3f]">Đội đã đăng ký</dt>
                                        <dd>{event.teamCount || 0}</dd>
                                    </div>
                                </dl>

                                <div className="mt-6 flex flex-wrap gap-3">
                                    <Link to={`/events/${event.id}`} className="btn-primary">
                                        Xem chi tiết
                                    </Link>
                                    <Link to={`/dashboard/my-team?eventId=${event.id}`} className="btn-secondary">
                                        Đăng ký đội
                                    </Link>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </main>
        </div>
    );
}
