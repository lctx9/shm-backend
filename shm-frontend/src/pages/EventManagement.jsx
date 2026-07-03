import { useEffect, useMemo, useState } from 'react';
import axiosClient from '../api/axiosClient';

const createEmptyEvent = () => ({
    name: '',
    season: 'SPRING',
    year: new Date().getFullYear(),
    regStartDate: '',
    regEndDate: '',
    eventStartDate: '',
    eventEndDate: '',
    submissionDeadline: '',
    roundCount: 2,
    tracks: ['Software Engineering', 'AI Application'],
});

const toLocalInput = (value) => value ? value.slice(0, 16) : '';

export default function EventManagement() {
    const [events, setEvents] = useState([]);
    const [teams, setTeams] = useState([]);
    const [mentors, setMentors] = useState([]);
    const [judges, setJudges] = useState([]);
    const [selectedEventId, setSelectedEventId] = useState('');
    const [selectedMatrixId, setSelectedMatrixId] = useState('');
    const [formData, setFormData] = useState(createEmptyEvent);
    const [matrixForm, setMatrixForm] = useState({ guidelineUrl: '', submissionDeadline: '', mentorIds: [], judgeIds: [] });
    const [prizeForm, setPrizeForm] = useState({ name: '', description: '', teamId: '' });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    const selectedEvent = useMemo(
        () => events.find((event) => String(event.id) === String(selectedEventId)),
        [events, selectedEventId]
    );

    const selectedMatrix = useMemo(
        () => selectedEvent?.matrices?.find((matrix) => String(matrix.id) === String(selectedMatrixId)),
        [selectedEvent, selectedMatrixId]
    );

    const eventTeams = useMemo(
        () => teams.filter((team) => String(team.eventId) === String(selectedEventId)),
        [teams, selectedEventId]
    );

    const fetchAll = async () => {
        const [eventRes, teamRes, mentorRes, judgeRes] = await Promise.all([
            axiosClient.get('/events'),
            axiosClient.get('/teams'),
            axiosClient.get('/users/role/MENTOR').catch(() => ({ result: [] })),
            axiosClient.get('/users/role/JUDGE').catch(() => ({ result: [] })),
        ]);

        const loadedEvents = eventRes.result || [];
        setEvents(loadedEvents);
        setTeams(teamRes.result || []);
        setMentors(mentorRes.result || []);
        setJudges(judgeRes.result || []);

        const nextEventId = selectedEventId || loadedEvents[0]?.id || '';
        setSelectedEventId(nextEventId);
        const event = loadedEvents.find((item) => String(item.id) === String(nextEventId));
        setSelectedMatrixId(event?.matrices?.[0]?.id || '');
    };

    useEffect(() => {
        fetchAll().catch((err) => setMessage({ text: err.message || 'Không thể tải dữ liệu quản lý giải đấu.', type: 'error' }));
    }, []);

    useEffect(() => {
        if (!selectedMatrix) return;
        setMatrixForm({
            guidelineUrl: selectedMatrix.guidelineUrl || '',
            submissionDeadline: toLocalInput(selectedMatrix.submissionDeadline),
            mentorIds: selectedMatrix.mentors?.map((user) => user.id) || [],
            judgeIds: selectedMatrix.judges?.map((user) => user.id) || [],
        });
    }, [selectedMatrix]);

    const updateTrack = (index, value) => {
        setFormData((current) => ({
            ...current,
            tracks: current.tracks.map((track, idx) => idx === index ? value : track),
        }));
    };

    const toggleId = (field, id) => {
        setMatrixForm((current) => {
            const exists = current[field].some((item) => String(item) === String(id));
            return {
                ...current,
                [field]: exists ? current[field].filter((item) => String(item) !== String(id)) : [...current[field], id],
            };
        });
    };

    const handleCreateEvent = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ text: '', type: '' });

        try {
            const payload = {
                ...formData,
                roundCount: Number(formData.roundCount),
                tracks: formData.tracks.map((track) => track.trim()).filter(Boolean),
            };
            const response = await axiosClient.post('/events', payload);
            setMessage({ text: 'Tạo event, track, round và matrix thành công.', type: 'success' });
            setFormData(createEmptyEvent());
            await fetchAll();
            setSelectedEventId(response.result?.id || '');
            setSelectedMatrixId(response.result?.matrices?.[0]?.id || '');
        } catch (err) {
            setMessage({ text: err.message || 'Không thể tạo giải đấu.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleSaveMatrix = async (e) => {
        e.preventDefault();
        if (!selectedMatrixId) return;

        try {
            setLoading(true);
            await axiosClient.put(`/events/matrices/${selectedMatrixId}`, {
                guidelineUrl: matrixForm.guidelineUrl,
                submissionDeadline: matrixForm.submissionDeadline || null,
                mentorIds: matrixForm.mentorIds.map(Number),
                judgeIds: matrixForm.judgeIds.map(Number),
            });
            setMessage({ text: 'Cập nhật matrix, guideline, deadline và phân công thành công.', type: 'success' });
            await fetchAll();
        } catch (err) {
            setMessage({ text: err.message || 'Không thể cập nhật matrix.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleCreatePrize = async (e) => {
        e.preventDefault();
        if (!selectedEventId) return;

        try {
            setLoading(true);
            await axiosClient.post(`/events/${selectedEventId}/prizes`, {
                name: prizeForm.name,
                description: prizeForm.description,
                teamId: prizeForm.teamId ? Number(prizeForm.teamId) : null,
            });
            setMessage({ text: 'Lưu cơ cấu/trao giải thành công.', type: 'success' });
            setPrizeForm({ name: '', description: '', teamId: '' });
        } catch (err) {
            setMessage({ text: err.message || 'Không thể lưu giải thưởng.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="mx-auto max-w-7xl space-y-6">
            {message.text && (
                <div className={`rounded-lg border p-4 text-sm font-semibold ${
                    message.type === 'success'
                        ? 'border-green-200 bg-green-50 text-green-700'
                        : 'border-red-200 bg-red-50 text-red-700'
                }`}>
                    {message.text}
                </div>
            )}

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <div className="mb-7">
                    <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">1. Tạo giải đấu</h2>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                        Lưu vào HackathonEvent, sinh Track, Round và TrackRoundMatrix theo ma trận track x vòng.
                    </p>
                </div>

                <form onSubmit={handleCreateEvent} className="space-y-6">
                    <div className="grid gap-5 md:grid-cols-3">
                        <div className="md:col-span-2">
                            <label className="mb-1 block text-sm font-bold text-slate-700">Tên giải đấu</label>
                            <input required className="input-custom" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="SEAL Hackathon Spring 2026" />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Mùa</label>
                            <select className="input-custom" value={formData.season} onChange={(e) => setFormData({ ...formData, season: e.target.value })}>
                                <option value="SPRING">Spring</option>
                                <option value="SUMMER">Summer</option>
                                <option value="FALL">Fall</option>
                            </select>
                        </div>
                    </div>

                    <div className="grid gap-5 md:grid-cols-3">
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Năm</label>
                            <input required type="number" className="input-custom" value={formData.year} onChange={(e) => setFormData({ ...formData, year: Number(e.target.value) })} />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Số vòng thi</label>
                            <input required min="1" type="number" className="input-custom" value={formData.roundCount} onChange={(e) => setFormData({ ...formData, roundCount: e.target.value })} />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Deadline mặc định</label>
                            <input type="datetime-local" className="input-custom" value={formData.submissionDeadline} onChange={(e) => setFormData({ ...formData, submissionDeadline: e.target.value })} />
                        </div>
                    </div>

                    <div className="grid gap-5 md:grid-cols-2">
                        <input required type="datetime-local" className="input-custom" value={formData.regStartDate} onChange={(e) => setFormData({ ...formData, regStartDate: e.target.value })} aria-label="Mở đăng ký" />
                        <input required type="datetime-local" className="input-custom" value={formData.regEndDate} onChange={(e) => setFormData({ ...formData, regEndDate: e.target.value })} aria-label="Đóng đăng ký" />
                        <input required type="datetime-local" className="input-custom" value={formData.eventStartDate} onChange={(e) => setFormData({ ...formData, eventStartDate: e.target.value })} aria-label="Bắt đầu sự kiện" />
                        <input required type="datetime-local" className="input-custom" value={formData.eventEndDate} onChange={(e) => setFormData({ ...formData, eventEndDate: e.target.value })} aria-label="Kết thúc sự kiện" />
                    </div>

                    <div>
                        <div className="mb-3 flex items-center justify-between">
                            <label className="block text-sm font-bold text-slate-700">Track</label>
                            <button type="button" onClick={() => setFormData((current) => ({ ...current, tracks: [...current.tracks, ''] }))} className="btn-secondary">Thêm track</button>
                        </div>
                        <div className="space-y-3">
                            {formData.tracks.map((track, index) => (
                                <div key={index} className="flex gap-3">
                                    <input required className="input-custom" value={track} onChange={(e) => updateTrack(index, e.target.value)} placeholder="Tên hạng mục thi" />
                                    {formData.tracks.length > 1 && <button type="button" onClick={() => setFormData((current) => ({ ...current, tracks: current.tracks.filter((_, idx) => idx !== index) }))} className="btn-secondary">Xóa</button>}
                                </div>
                            ))}
                        </div>
                    </div>

                    <button type="submit" disabled={loading} className="btn-primary w-full">
                        {loading ? 'Đang lưu...' : 'Tạo event và sinh matrix'}
                    </button>
                </form>
            </section>

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">2. Cấu hình event đã tạo</h2>
                <div className="mt-5 grid gap-5 lg:grid-cols-[320px_1fr]">
                    <div>
                        <label className="mb-1 block text-sm font-bold text-slate-700">Chọn event</label>
                        <select className="input-custom" value={selectedEventId} onChange={(e) => {
                            const id = e.target.value;
                            const event = events.find((item) => String(item.id) === String(id));
                            setSelectedEventId(id);
                            setSelectedMatrixId(event?.matrices?.[0]?.id || '');
                        }}>
                            {events.map((event) => <option key={event.id} value={event.id}>{event.name}</option>)}
                        </select>

                        <div className="mt-5 rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900">
                            <p className="font-black">{selectedEvent?.name || 'Chưa có event'}</p>
                            <p>{selectedEvent?.tracks?.length || 0} track - {selectedEvent?.rounds?.length || 0} vòng - {selectedEvent?.teamCount || 0} đội</p>
                        </div>
                    </div>

                    <form onSubmit={handleSaveMatrix} className="space-y-5 rounded-lg border border-blue-100 p-5">
                        <h3 className="font-black uppercase tracking-wide text-slate-900">Cấu hình TrackRoundMatrix</h3>
                        <select className="input-custom" value={selectedMatrixId} onChange={(e) => setSelectedMatrixId(e.target.value)}>
                            {(selectedEvent?.matrices || []).map((matrix) => (
                                <option key={matrix.id} value={matrix.id}>{matrix.roundName} - {matrix.trackName}</option>
                            ))}
                        </select>

                        <input className="input-custom" value={matrixForm.guidelineUrl} onChange={(e) => setMatrixForm({ ...matrixForm, guidelineUrl: e.target.value })} placeholder="Link guideline / đề bài PDF" />
                        <input type="datetime-local" className="input-custom" value={matrixForm.submissionDeadline} onChange={(e) => setMatrixForm({ ...matrixForm, submissionDeadline: e.target.value })} />

                        <div className="grid gap-5 md:grid-cols-2">
                            <div>
                                <p className="mb-2 text-sm font-bold text-slate-700">Mentor</p>
                                <div className="max-h-40 space-y-2 overflow-auto rounded-lg border border-blue-100 p-3">
                                    {mentors.length === 0 ? <p className="text-sm text-slate-500">Chưa có mentor.</p> : mentors.map((user) => (
                                        <label key={user.id} className="flex items-center gap-2 text-sm">
                                            <input type="checkbox" checked={matrixForm.mentorIds.some((id) => String(id) === String(user.id))} onChange={() => toggleId('mentorIds', user.id)} />
                                            {user.fullName} ({user.email})
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <p className="mb-2 text-sm font-bold text-slate-700">Judge</p>
                                <div className="max-h-40 space-y-2 overflow-auto rounded-lg border border-blue-100 p-3">
                                    {judges.length === 0 ? <p className="text-sm text-slate-500">Chưa có judge.</p> : judges.map((user) => (
                                        <label key={user.id} className="flex items-center gap-2 text-sm">
                                            <input type="checkbox" checked={matrixForm.judgeIds.some((id) => String(id) === String(user.id))} onChange={() => toggleId('judgeIds', user.id)} />
                                            {user.fullName} ({user.email})
                                        </label>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <button type="submit" disabled={!selectedMatrixId || loading} className="btn-primary w-full">Lưu cấu hình matrix</button>
                    </form>
                </div>
            </section>

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">3. Cấu hình giải thưởng</h2>
                <form onSubmit={handleCreatePrize} className="mt-5 grid gap-4 md:grid-cols-[1fr_1fr_220px_auto]">
                    <input required className="input-custom" value={prizeForm.name} onChange={(e) => setPrizeForm({ ...prizeForm, name: e.target.value })} placeholder="Tên giải: Giải nhất" />
                    <input className="input-custom" value={prizeForm.description} onChange={(e) => setPrizeForm({ ...prizeForm, description: e.target.value })} placeholder="Mô tả / phần thưởng" />
                    <select className="input-custom" value={prizeForm.teamId} onChange={(e) => setPrizeForm({ ...prizeForm, teamId: e.target.value })}>
                        <option value="">Chưa trao đội</option>
                        {eventTeams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                    </select>
                    <button type="submit" disabled={!selectedEventId || loading} className="btn-primary">Lưu giải</button>
                </form>
            </section>
        </div>
    );
}
