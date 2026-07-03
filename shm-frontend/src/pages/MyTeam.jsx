import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

export default function MyTeam() {
    const [searchParams] = useSearchParams();
    const preselectedEventId = searchParams.get('eventId');
    const role = localStorage.getItem('role');

    const [team, setTeam] = useState(null);
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [creating, setCreating] = useState(false);
    const [error, setError] = useState('');
    const [formData, setFormData] = useState({
        name: '',
        type: 'PUBLIC',
        joinPassword: '',
        eventId: '',
        trackId: '',
    });

    useEffect(() => {
        const bootstrap = async () => {
            try {
                setLoading(true);
                const [teamRes, eventsRes] = await Promise.allSettled([
                    axiosClient.get('/teams/my-team'),
                    axiosClient.get('/events'),
                ]);

                if (teamRes.status === 'fulfilled') {
                    setTeam(teamRes.value.result || null);
                }

                if (eventsRes.status === 'fulfilled') {
                    const loadedEvents = eventsRes.value.result || [];
                    setEvents(loadedEvents);
                    const firstEvent = loadedEvents.find((item) => String(item.id) === String(preselectedEventId)) || loadedEvents[0];
                    const firstTrack = firstEvent?.tracks?.[0];
                    setFormData((current) => ({
                        ...current,
                        eventId: firstEvent?.id || '',
                        trackId: firstTrack?.id || '',
                    }));
                }
            } catch (err) {
                setError(err.message || 'Không thể tải dữ liệu đội thi.');
            } finally {
                setLoading(false);
            }
        };

        bootstrap();
    }, [preselectedEventId]);

    const selectedEvent = useMemo(
        () => events.find((event) => String(event.id) === String(formData.eventId)),
        [events, formData.eventId]
    );

    const handleEventChange = (eventId) => {
        const nextEvent = events.find((event) => String(event.id) === String(eventId));
        setFormData((current) => ({
            ...current,
            eventId,
            trackId: nextEvent?.tracks?.[0]?.id || '',
        }));
    };

    const handleCreateTeam = async (e) => {
        e.preventDefault();
        setError('');

        if (!formData.eventId || !formData.trackId) {
            setError('Vui lòng chọn giải đấu và hạng mục thi từ dữ liệu backend.');
            return;
        }

        try {
            setCreating(true);
            const payload = {
                ...formData,
                eventId: Number(formData.eventId),
                trackId: Number(formData.trackId),
                joinPassword: formData.type === 'PRIVATE' ? formData.joinPassword : '',
            };
            const response = await axiosClient.post('/teams/create', payload);
            localStorage.setItem('role', 'LEADER');
            setTeam(response.result);
        } catch (err) {
            setError(err.message || 'Không thể tạo đội thi.');
        } finally {
            setCreating(false);
        }
    };

    if (loading) {
        return <div className="rounded-lg bg-white p-8 text-center text-gray-500">Đang tải dữ liệu đội thi...</div>;
    }

    if (team) {
        return (
            <div className="mx-auto max-w-5xl space-y-6">
                <div className="rounded-lg border border-blue-100 bg-white p-6 shadow-sm">
                    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                        <div>
                            <p className="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                                {team.eventName || 'Chưa gắn giải đấu'}
                            </p>
                            <h2 className="mt-2 text-2xl font-black uppercase tracking-wide text-slate-900">
                                {team.name}
                            </h2>
                            <p className="mt-2 text-sm text-slate-600">
                                Hạng mục: <span className="font-bold text-blue-700">{team.trackName || 'Chưa cập nhật'}</span>
                            </p>
                        </div>
                        <span className="w-fit rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-black uppercase text-blue-700">
                            {team.type}
                        </span>
                    </div>
                </div>

                <div className="rounded-lg border border-blue-100 bg-white shadow-sm">
                    <div className="border-b border-blue-100 bg-blue-50 px-6 py-4">
                        <h3 className="font-black uppercase tracking-wide text-slate-900">
                            Thành viên ({team.memberCount || team.members?.length || 0})
                        </h3>
                    </div>
                    <div className="divide-y divide-blue-50">
                        {(team.members || []).map((member) => (
                            <div key={member.id} className="flex items-center justify-between px-6 py-4">
                                <div>
                                    <p className="font-bold text-slate-900">{member.fullName || member.email}</p>
                                    <p className="text-sm text-slate-500">
                                        {member.email} {member.studentId ? `- ${member.studentId}` : ''}
                                    </p>
                                </div>
                                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-700">
                                    {member.role}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="flex flex-wrap gap-3">
                    <Link to="/dashboard/submissions" className="btn-primary">Nộp bài</Link>
                    <Link to="/dashboard/teams" className="btn-secondary">Xem sảnh đội</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-3xl rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
            <div className="mb-7">
                <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">Tạo đội thi</h2>
                <p className="mt-2 text-sm leading-6 text-slate-600">
                    Đội sẽ được lưu vào bảng Team và gắn trực tiếp với Hackathon Event cùng Track đã chọn.
                </p>
            </div>

            {error && (
                <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                    {error}
                </div>
            )}

            {events.length === 0 ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">
                    Chưa có giải đấu trong database. Coordinator cần tạo giải đấu trước khi thí sinh tạo đội.
                </div>
            ) : (
                <form onSubmit={handleCreateTeam} className="space-y-5">
                    <div>
                        <label className="mb-1 block text-sm font-bold text-slate-700">Tên đội</label>
                        <input
                            required
                            className="input-custom"
                            value={formData.name}
                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            placeholder="Nhập tên đội thi"
                        />
                    </div>

                    <div className="grid gap-5 md:grid-cols-2">
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Giải đấu</label>
                            <select
                                required
                                className="input-custom"
                                value={formData.eventId}
                                onChange={(e) => handleEventChange(e.target.value)}
                            >
                                {events.map((event) => (
                                    <option key={event.id} value={event.id}>
                                        {event.name} - {event.season} {event.year}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Hạng mục</label>
                            <select
                                required
                                className="input-custom"
                                value={formData.trackId}
                                onChange={(e) => setFormData({ ...formData, trackId: e.target.value })}
                            >
                                {(selectedEvent?.tracks || []).map((track) => (
                                    <option key={track.id} value={track.id}>{track.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-bold text-slate-700">Chế độ đội</label>
                        <div className="grid gap-3 sm:grid-cols-2">
                            {['PUBLIC', 'PRIVATE'].map((type) => (
                                <label key={type} className="flex cursor-pointer items-center gap-3 rounded-lg border border-blue-100 bg-blue-50/40 p-4">
                                    <input
                                        type="radio"
                                        value={type}
                                        checked={formData.type === type}
                                        onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                                    />
                                    <span className="font-bold text-slate-800">{type === 'PUBLIC' ? 'Công khai' : 'Riêng tư'}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {formData.type === 'PRIVATE' && (
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Mật khẩu gia nhập</label>
                            <input
                                required
                                className="input-custom"
                                value={formData.joinPassword}
                                onChange={(e) => setFormData({ ...formData, joinPassword: e.target.value })}
                                placeholder="Mật khẩu dành cho thành viên được mời"
                            />
                        </div>
                    )}

                    <button type="submit" disabled={creating} className="btn-primary w-full">
                        {creating ? 'Đang tạo đội...' : 'Tạo đội thi'}
                    </button>
                </form>
            )}
        </div>
    );
}
