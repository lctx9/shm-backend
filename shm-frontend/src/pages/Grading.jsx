import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function Grading() {
    const [submissions, setSubmissions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedSub, setSelectedSub] = useState(null);
    const [gradeForm, setGradeForm] = useState({ score: '', feedback: '', editReason: '' });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const fetchSubmissions = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/submissions');
            setSubmissions(response.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải danh sách bài nộp.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSubmissions();
    }, []);

    const handleSelect = (submission) => {
        setSelectedSub(submission);
        setGradeForm({
            score: submission.score || '',
            feedback: submission.feedback || '',
            editReason: '',
        });
    };

    const handleSubmitGrade = async (e) => {
        e.preventDefault();

        if (Number(gradeForm.score) < 0 || Number(gradeForm.score) > 100) {
            setError('Điểm số phải nằm trong khoảng 0 đến 100.');
            return;
        }

        try {
            setSaving(true);
            await axiosClient.post('/scores/grade', {
                submissionId: selectedSub.id,
                scoreValue: Number(gradeForm.score),
                comment: gradeForm.feedback,
                editReason: selectedSub.graded ? gradeForm.editReason : '',
            });
            setSelectedSub(null);
            await fetchSubmissions();
        } catch (err) {
            setError(err.message || 'Không thể lưu điểm.');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="rounded-lg bg-white p-8 text-center text-gray-500">Đang tải danh sách bài nộp...</div>;
    }

    return (
        <div className="mx-auto grid max-w-6xl gap-6 lg:grid-cols-[1fr_420px]">
            <section className="rounded-lg border border-blue-100 bg-white shadow-sm">
                <div className="border-b border-blue-100 bg-blue-50 px-6 py-4">
                    <h2 className="text-lg font-black uppercase tracking-wide text-slate-900">Bài nộp cần chấm</h2>
                </div>

                {error && (
                    <div className="m-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                        {error}
                    </div>
                )}

                {submissions.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">Chưa có bài nộp nào trong database.</div>
                ) : (
                    <div className="divide-y divide-blue-50">
                        {submissions.map((submission) => (
                            <button
                                type="button"
                                key={submission.id}
                                onClick={() => handleSelect(submission)}
                                className={`block w-full px-6 py-5 text-left transition hover:bg-blue-50 ${
                                    selectedSub?.id === submission.id ? 'bg-blue-50' : ''
                                }`}
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <p className="font-black text-slate-900">{submission.teamName || `Đội #${submission.teamId}`}</p>
                                        <p className="mt-1 text-sm text-slate-500">
                                            {submission.roundName || 'Vòng thi'} - {submission.trackName || 'Track'}
                                        </p>
                                        <p className="mt-1 truncate text-sm text-blue-700">{submission.fileUrl}</p>
                                    </div>
                                    <span className={`rounded-full px-3 py-1 text-xs font-black ${
                                        submission.graded ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                                    }`}>
                                        {submission.graded ? 'Đã chấm' : 'Chờ chấm'}
                                    </span>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </section>

            <section className="rounded-lg border border-blue-100 bg-white p-6 shadow-sm">
                {selectedSub ? (
                    <>
                        <p className="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                            {selectedSub.roundName} - {selectedSub.trackName}
                        </p>
                        <h2 className="mt-2 text-xl font-black uppercase tracking-wide text-slate-900">
                            {selectedSub.teamName || `Đội #${selectedSub.teamId}`}
                        </h2>
                        <a
                            href={selectedSub.fileUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-4 block break-all rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm font-bold text-blue-700"
                        >
                            {selectedSub.fileUrl}
                        </a>

                        <form onSubmit={handleSubmitGrade} className="mt-6 space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-bold text-slate-700">Điểm số</label>
                                <input
                                    required
                                    type="number"
                                    step="0.1"
                                    min="0"
                                    max="100"
                                    className="input-custom"
                                    value={gradeForm.score}
                                    onChange={(e) => setGradeForm({ ...gradeForm, score: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="mb-1 block text-sm font-bold text-slate-700">Nhận xét</label>
                                <textarea
                                    required
                                    rows="6"
                                    className="input-custom"
                                    value={gradeForm.feedback}
                                    onChange={(e) => setGradeForm({ ...gradeForm, feedback: e.target.value })}
                                />
                            </div>
                            {selectedSub.graded && (
                                <div>
                                    <label className="mb-1 block text-sm font-bold text-slate-700">Lý do sửa điểm</label>
                                    <input
                                        required
                                        className="input-custom"
                                        value={gradeForm.editReason}
                                        onChange={(e) => setGradeForm({ ...gradeForm, editReason: e.target.value })}
                                        placeholder="Ví dụ: rà soát lại rubric sau khi trao đổi với BTC"
                                    />
                                </div>
                            )}
                            <button type="submit" disabled={saving} className="btn-primary w-full">
                                {saving ? 'Đang lưu...' : 'Lưu điểm'}
                            </button>
                        </form>
                    </>
                ) : (
                    <div className="rounded-lg border border-blue-100 bg-blue-50 p-6 text-sm font-semibold text-blue-900">
                        Chọn một bài nộp ở bên trái để bắt đầu chấm điểm.
                    </div>
                )}
            </section>
        </div>
    );
}
