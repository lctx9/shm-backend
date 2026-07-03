import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function Grading() {
    const [submissions, setSubmissions] = useState([]);
    const [loading, setLoading] = useState(true);

    // State quản lý bài đang được chọn để chấm
    const [selectedSub, setSelectedSub] = useState(null);
    const [gradeForm, setGradeForm] = useState({ score: '', feedback: '' });
    const [submitLoading, setSubmitLoading] = useState(false);

    useEffect(() => {
        fetchSubmissions();
    }, []);

    const fetchSubmissions = async () => {
        try {
            setLoading(true);
            const res = await axiosClient.get('/submissions');
            setSubmissions(res.result || []);
        } catch (err) {
            console.error('Lỗi lấy danh sách bài nộp:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSelectToGrade = (sub) => {
        setSelectedSub(sub);
        setGradeForm({
            score: sub.score || '',
            feedback: sub.feedback || ''
        });
    };

    const handleSubmitGrade = async (e) => {
        e.preventDefault();
        if (gradeForm.score < 0 || gradeForm.score > 100) {
            alert("Điểm số phải nằm trong khoảng từ 0 đến 100");
            return;
        }

        try {
            setSubmitLoading(true);
            await axiosClient.put(`/submissions/${selectedSub.id}/grade`, {
                score: parseFloat(gradeForm.score),
                feedback: gradeForm.feedback
            });

            alert('✅ Chấm điểm thành công!');
            setSelectedSub(null); // Đóng form
            fetchSubmissions();   // Tải lại danh sách để cập nhật trạng thái
        } catch (err) {
            alert(err.response?.data?.message || 'Có lỗi khi chấm điểm!');
        } finally {
            setSubmitLoading(false);
        }
    };

    if (loading) return <div className="p-8 text-center text-gray-500">Đang tải danh sách bài thi...</div>;

    return (
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row gap-6">

            {/* CỘT TRÁI: DANH SÁCH BÀI THI */}
            <div className={`flex-1 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden ${selectedSub ? 'hidden md:block md:w-1/2' : 'w-full'}`}>
                <div className="p-5 border-b border-gray-200 bg-gray-50">
                    <h2 className="text-xl font-bold text-gray-800">📋 Danh sách bài dự thi</h2>
                </div>

                {submissions.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">Hiện chưa có đội nào nộp bài.</div>
                ) : (
                    <ul className="divide-y divide-gray-100 max-h-[70vh] overflow-y-auto">
                        {submissions.map((sub) => (
                            <li key={sub.id} className={`p-5 hover:bg-indigo-50 transition-colors cursor-pointer ${selectedSub?.id === sub.id ? 'bg-indigo-50 border-l-4 border-indigo-600' : ''}`}
                                onClick={() => handleSelectToGrade(sub)}>
                                <div className="flex justify-between items-start mb-2">
                                    <h3 className="font-bold text-gray-900 text-lg">Đội thi ID: {sub.teamId || 'Chưa rõ'}</h3>
                                    <span className={`px-2 py-1 text-xs font-bold rounded-full ${sub.isGraded ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                    {sub.isGraded ? 'Đã chấm' : 'Chờ chấm'}
                  </span>
                                </div>
                                <p className="text-sm text-gray-600 truncate">Link: {sub.fileUrl}</p>
                                {sub.isGraded && (
                                    <p className="text-sm font-medium text-indigo-600 mt-2">Điểm: {sub.score}/100</p>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {/* CỘT PHẢI: FORM CHẤM ĐIỂM (Chỉ hiện khi click vào 1 bài) */}
            {selectedSub && (
                <div className="flex-1 bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col">
                    <div className="p-5 border-b border-gray-200 flex justify-between items-center bg-indigo-50">
                        <h2 className="text-xl font-bold text-indigo-900">⚖️ Chấm điểm Đội {selectedSub.teamId}</h2>
                        <button onClick={() => setSelectedSub(null)} className="text-gray-500 hover:text-red-500 md:hidden">✕ Đóng</button>
                    </div>

                    <div className="p-6 flex-1 overflow-y-auto">
                        <div className="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
                            <p className="text-sm text-gray-500 font-medium mb-1">Tài liệu dự án (Click để xem):</p>
                            <a href={selectedSub.fileUrl} target="_blank" rel="noopener noreferrer" className="text-indigo-600 font-bold hover:underline break-all">
                                {selectedSub.fileUrl}
                            </a>
                        </div>

                        <form onSubmit={handleSubmitGrade} className="space-y-5">
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Điểm số (0 - 100) <span className="text-red-500">*</span></label>
                                <input
                                    type="number" required step="0.1" min="0" max="100"
                                    className="w-full text-2xl px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 text-indigo-600 font-black"
                                    value={gradeForm.score} onChange={(e) => setGradeForm({...gradeForm, score: e.target.value})}
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Nhận xét chi tiết <span className="text-red-500">*</span></label>
                                <textarea
                                    required rows="5"
                                    placeholder="Ghi chú điểm mạnh, điểm yếu và gợi ý cải thiện cho nhóm..."
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                                    value={gradeForm.feedback} onChange={(e) => setGradeForm({...gradeForm, feedback: e.target.value})}
                                ></textarea>
                            </div>

                            <div className="pt-4 flex gap-3">
                                <button type="button" onClick={() => setSelectedSub(null)} className="px-6 py-2.5 bg-gray-100 text-gray-700 font-semibold rounded-lg hover:bg-gray-200 transition-colors">
                                    Hủy bỏ
                                </button>
                                <button type="submit" disabled={submitLoading} className="flex-1 px-6 py-2.5 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors shadow-md">
                                    {submitLoading ? 'Đang lưu...' : 'Lưu Điểm Số'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

        </div>
    );
}