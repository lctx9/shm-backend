import { useEffect, useMemo, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function TeamExplorer() {
    const [teams, setTeams] = useState([]);
    const [events, setEvents] = useState([]);
    const [eventFilter, setEventFilter] = useState('ALL');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedTeam, setSelectedTeam] = useState(null);
    const [joinPassword, setJoinPassword] = useState('');

    const fetchData = async () => {
        try {
            setLoading(true);
            const [teamRes, eventRes] = await Promise.all([
                axiosClient.get('/teams'),
                axiosClient.get('/events'),
            ]);
            setTeams(teamRes.result || []);
            setEvents(eventRes.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải dữ liệu đội thi.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const filteredTeams = useMemo(() => {
        if (eventFilter === 'ALL') return teams;
        return teams.filter((team) => String(team.eventId) === String(eventFilter));
    }, [teams, eventFilter]);

    const handleJoinPublic = async (teamId) => {
        try {
            await axiosClient.post(`/teams/${teamId}/join-request`);
            await fetchData();
            alert('Gia nhập đội công khai thành công.');
        } catch (err) {
            alert(err.message || 'Không thể gia nhập đội.');
        }
    };

    const handleJoinPrivate = async (e) => {
        e.preventDefault();
        if (!selectedTeam) return;

        try {
            await axiosClient.post(`/teams/${selectedTeam.id}/join-private`, {
                password: joinPassword,
            });
            setSelectedTeam(null);
            setJoinPassword('');
            window.location.href = '/dashboard/my-team';
        } catch (err) {
            alert(err.message || 'Không thể gia nhập đội riêng tư.');
        }
    };

    return (
        <div className="mx-auto max-w-6xl space-y-6">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                <div>
                    <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">Sảnh đội thi</h2>
                    <p className="mt-2 text-sm text-slate-600">
                        Danh sách được đọc từ bảng Team, liên kết với Hackathon Event và Track thật.
                    </p>
                </div>
                <button type="button" onClick={fetchData} className="btn-secondary">Làm mới</button>
            </div>

            <div className="rounded-lg border border-blue-100 bg-white p-4">
                <label className="mb-1 block text-sm font-bold text-slate-700">Lọc theo giải đấu</label>
                <select className="input-custom max-w-md" value={eventFilter} onChange={(e) => setEventFilter(e.target.value)}>
                    <option value="ALL">Tất cả giải đấu</option>
                    {events.map((event) => (
                        <option key={event.id} value={event.id}>
                            {event.name} - {event.season} {event.year}
                        </option>
                    ))}
                </select>
            </div>

            {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                    {error}
                </div>
            )}

            {loading ? (
                <div className="rounded-lg bg-white p-8 text-center text-gray-500">Đang tải đội thi...</div>
            ) : filteredTeams.length === 0 ? (
                <div className="rounded-lg border border-blue-100 bg-white p-8 text-center text-gray-500">
                    Chưa có đội thi phù hợp.
                </div>
            ) : (
                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                    {filteredTeams.map((team) => (
                        <article key={team.id} className="feature-card">
                            <div className="mb-4 flex items-start justify-between gap-4">
                                <div>
                                    <h3 className="text-lg font-black uppercase tracking-wide text-slate-900">{team.name}</h3>
                                    <p className="mt-1 text-sm text-slate-500">{team.eventName || 'Chưa gắn giải đấu'}</p>
                                </div>
                                <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-black text-blue-700">
                                    {team.type}
                                </span>
                            </div>

                            <dl className="space-y-2 text-sm text-slate-600">
                                <div className="flex justify-between gap-4">
                                    <dt className="font-bold text-slate-800">Hạng mục</dt>
                                    <dd className="text-right">{team.trackName || 'Chưa cập nhật'}</dd>
                                </div>
                                <div className="flex justify-between gap-4">
                                    <dt className="font-bold text-slate-800">Thành viên</dt>
                                    <dd>{team.memberCount || 0}</dd>
                                </div>
                            </dl>

                            <button
                                type="button"
                                onClick={() => team.type === 'PUBLIC' ? handleJoinPublic(team.id) : setSelectedTeam(team)}
                                className="btn-primary mt-6 w-full"
                            >
                                {team.type === 'PUBLIC' ? 'Gia nhập đội' : 'Nhập mật khẩu'}
                            </button>
                        </article>
                    ))}
                </div>
            )}

            {selectedTeam && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4">
                    <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
                        <h3 className="text-lg font-black uppercase tracking-wide text-slate-900">
                            Gia nhập {selectedTeam.name}
                        </h3>
                        <p className="mt-2 text-sm text-slate-600">
                            Đội riêng tư yêu cầu mật khẩu do Team Leader cung cấp.
                        </p>

                        <form onSubmit={handleJoinPrivate} className="mt-5 space-y-4">
                            <input
                                required
                                className="input-custom"
                                value={joinPassword}
                                onChange={(e) => setJoinPassword(e.target.value)}
                                placeholder="Mật khẩu đội"
                            />
                            <div className="flex gap-3">
                                <button type="button" onClick={() => setSelectedTeam(null)} className="btn-secondary flex-1">
                                    Hủy
                                </button>
                                <button type="submit" className="btn-primary flex-1">
                                    Xác nhận
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
