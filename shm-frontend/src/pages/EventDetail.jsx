import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
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

export default function EventDetail() {
    const { eventId } = useParams();
    const [event, setEvent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchEvent = async () => {
            try {
                setLoading(true);
                const response = await axiosClient.get(`/events/${eventId}`);
                setEvent(response.result);
                setError('');
            } catch (err) {
                setError(err.message || 'Không thể tải chi tiết giải đấu.');
            } finally {
                setLoading(false);
            }
        };

        fetchEvent();
    }, [eventId]);

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
                    <Link to="/dashboard" className="btn-secondary">Dashboard</Link>
                </div>
            </header>

            <main className="section-shell">
                {loading ? (
                    <div className="rounded-lg border border-[#d7e6f8] bg-white p-8 text-center text-[#5c6d83]">
                        Đang tải chi tiết giải đấu...
                    </div>
                ) : error ? (
                    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                        {error}
                    </div>
                ) : (
                    <>
                        <div className="mb-8 rounded-lg border border-[#d7e6f8] bg-white p-7 shadow-sm">
                            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#0f63c9]">
                                {event.season} {event.year}
                            </p>
                            <h1 className="mt-3 text-3xl font-black uppercase tracking-[0.06em] text-[#071936]">
                                {event.name}
                            </h1>
                            <div className="mt-6 grid gap-4 text-sm text-[#5c6d83] md:grid-cols-4">
                                <div>
                                    <p className="font-bold text-[#0b1f3f]">Mở đăng ký</p>
                                    <p>{formatDate(event.regStartDate)}</p>
                                </div>
                                <div>
                                    <p className="font-bold text-[#0b1f3f]">Đóng đăng ký</p>
                                    <p>{formatDate(event.regEndDate)}</p>
                                </div>
                                <div>
                                    <p className="font-bold text-[#0b1f3f]">Bắt đầu</p>
                                    <p>{formatDate(event.eventStartDate)}</p>
                                </div>
                                <div>
                                    <p className="font-bold text-[#0b1f3f]">Kết thúc</p>
                                    <p>{formatDate(event.eventEndDate)}</p>
                                </div>
                            </div>
                            <div className="mt-7">
                                <Link to={`/dashboard/my-team?eventId=${event.id}`} className="btn-action-main">
                                    Đăng ký đội thi
                                </Link>
                            </div>
                        </div>

                        <div className="grid gap-5 lg:grid-cols-2">
                            <section className="feature-card">
                                <h2 className="text-lg font-black uppercase tracking-[0.08em] text-[#071936]">
                                    Hạng mục thi
                                </h2>
                                <div className="mt-5 space-y-3">
                                    {(event.tracks || []).map((track) => (
                                        <div key={track.id} className="rounded-lg border border-[#d7e6f8] bg-[#f8fbff] p-4">
                                            <p className="font-black text-[#0b1f3f]">{track.name}</p>
                                            <p className="mt-1 text-sm text-[#5c6d83]">{track.description || 'Chưa có mô tả.'}</p>
                                        </div>
                                    ))}
                                </div>
                            </section>

                            <section className="feature-card">
                                <h2 className="text-lg font-black uppercase tracking-[0.08em] text-[#071936]">
                                    Lịch vòng thi
                                </h2>
                                <div className="mt-5 space-y-3">
                                    {(event.matrices || []).map((matrix) => (
                                        <div key={matrix.id} className="rounded-lg border border-[#d7e6f8] bg-[#f8fbff] p-4">
                                            <p className="font-black text-[#0b1f3f]">
                                                {matrix.roundName} - {matrix.trackName}
                                            </p>
                                            <p className="mt-1 text-sm text-[#5c6d83]">
                                                Deadline nộp bài: {formatDate(matrix.submissionDeadline)}
                                            </p>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        </div>
                    </>
                )}
            </main>
        </div>
    );
}
