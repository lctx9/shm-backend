import { useEffect, useMemo, useState } from 'react';
import axiosClient from '../api/axiosClient';

function matrixLabel(matrix) {
    return `${matrix.roundName} - ${matrix.trackName}`;
}

export default function Submission() {
    const role = localStorage.getItem('role');
    const [team, setTeam] = useState(null);
    const [matrices, setMatrices] = useState([]);
    const [submission, setSubmission] = useState(null);
    const [formData, setFormData] = useState({ fileUrl: '', matrixId: '' });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    useEffect(() => {
        const bootstrap = async () => {
            try {
                setLoading(true);
                const teamRes = await axiosClient.get('/teams/my-team');
                const loadedTeam = teamRes.result;
                setTeam(loadedTeam);

                if (loadedTeam?.eventId) {
                    const matrixRes = await axiosClient.get(`/events/${loadedTeam.eventId}/matrices`);
                    const teamMatrices = (matrixRes.result || []).filter(
                        (matrix) => String(matrix.trackId) === String(loadedTeam.trackId)
                    );
                    setMatrices(teamMatrices);
                    setFormData((current) => ({
                        ...current,
                        matrixId: teamMatrices[0]?.id || '',
                    }));
                }

                const submissionRes = await axiosClient.get('/submissions/my-submission');
                if (submissionRes.result) {
                    setSubmission(submissionRes.result);
                    setFormData({
                        fileUrl: submissionRes.result.fileUrl || '',
                        matrixId: submissionRes.result.matrixId || '',
                    });
                }
            } catch (err) {
                setMessage({ text: err.message || 'Không thể tải dữ liệu nộp bài.', type: 'error' });
            } finally {
                setLoading(false);
            }
        };

        bootstrap();
    }, []);

    const selectedMatrix = useMemo(
        () => matrices.find((matrix) => String(matrix.id) === String(formData.matrixId)),
        [matrices, formData.matrixId]
    );

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });

        if (role !== 'LEADER') {
            setMessage({ text: 'Chỉ Team Leader mới có quyền nộp hoặc cập nhật bài.', type: 'error' });
            return;
        }

        if (!team?.id || !formData.matrixId) {
            setMessage({ text: 'Thiếu đội thi hoặc vòng thi hợp lệ từ backend.', type: 'error' });
            return;
        }

        try {
            setSaving(true);
            const payload = {
                teamId: team.id,
                matrixId: Number(formData.matrixId),
                fileUrl: formData.fileUrl,
            };

            const response = submission
                ? await axiosClient.put(`/submissions/${submission.id}`, payload)
                : await axiosClient.post('/submissions', payload);

            setSubmission(response.result);
            setMessage({ text: submission ? 'Cập nhật bài nộp thành công.' : 'Nộp bài thành công.', type: 'success' });
        } catch (err) {
            setMessage({ text: err.message || 'Không thể lưu bài nộp.', type: 'error' });
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="rounded-lg bg-white p-8 text-center text-gray-500">Đang tải dữ liệu bài nộp...</div>;
    }

    if (!team) {
        return (
            <div className="mx-auto max-w-3xl rounded-lg border border-amber-200 bg-amber-50 p-6 text-amber-900">
                Bạn cần tạo hoặc tham gia một đội thi trước khi nộp bài.
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-4xl space-y-6">
            {submission?.graded && (
                <section className="rounded-lg border border-green-200 bg-green-50 p-6">
                    <h2 className="text-lg font-black uppercase tracking-wide text-green-900">Kết quả chấm điểm</h2>
                    <div className="mt-4 rounded-lg bg-white p-4">
                        <p className="text-sm font-bold text-slate-600">Điểm số</p>
                        <p className="text-4xl font-black text-green-700">{submission.score}/100</p>
                    </div>
                    <p className="mt-4 text-sm leading-6 text-green-900">{submission.feedback || 'Chưa có nhận xét.'}</p>
                </section>
            )}

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <div className="mb-7">
                    <p className="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                        {team.eventName} - {team.trackName}
                    </p>
                    <h2 className="mt-2 text-2xl font-black uppercase tracking-wide text-slate-900">
                        {submission ? 'Cập nhật bài dự thi' : 'Nộp bài dự thi'}
                    </h2>
                    <p className="mt-2 text-sm text-slate-600">
                        Bài nộp được lưu vào bảng Submission và gắn với Team cùng TrackRoundMatrix đã chọn.
                    </p>
                </div>

                {message.text && (
                    <div className={`mb-6 rounded-lg border p-4 text-sm font-semibold ${
                        message.type === 'success'
                            ? 'border-green-200 bg-green-50 text-green-700'
                            : 'border-red-200 bg-red-50 text-red-700'
                    }`}>
                        {message.text}
                    </div>
                )}

                {matrices.length === 0 ? (
                    <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">
                        Giải đấu của đội chưa có vòng thi cho hạng mục này. Coordinator cần tạo cấu trúc Track/Round trước.
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-5">
                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Vòng thi</label>
                            <select
                                required
                                className="input-custom"
                                value={formData.matrixId}
                                onChange={(e) => setFormData({ ...formData, matrixId: e.target.value })}
                                disabled={role !== 'LEADER'}
                            >
                                {matrices.map((matrix) => (
                                    <option key={matrix.id} value={matrix.id}>{matrixLabel(matrix)}</option>
                                ))}
                            </select>
                            {selectedMatrix?.submissionDeadline && (
                                <p className="mt-2 text-xs font-semibold text-slate-500">
                                    Deadline: {new Date(selectedMatrix.submissionDeadline).toLocaleString('vi-VN')}
                                </p>
                            )}
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-bold text-slate-700">Link tài liệu dự án</label>
                            <input
                                required
                                type="url"
                                className="input-custom"
                                value={formData.fileUrl}
                                onChange={(e) => setFormData({ ...formData, fileUrl: e.target.value })}
                                placeholder="https://github.com/... hoặc link Drive"
                                disabled={role !== 'LEADER'}
                            />
                        </div>

                        <button type="submit" disabled={saving || role !== 'LEADER'} className="btn-primary w-full">
                            {saving ? 'Đang lưu...' : submission ? 'Cập nhật bài nộp' : 'Gửi bài nộp'}
                        </button>
                    </form>
                )}
            </section>
        </div>
    );
}
