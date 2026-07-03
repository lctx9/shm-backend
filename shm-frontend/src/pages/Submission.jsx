import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function Submission() {
    // Vẫn giữ teamId: 1 để tránh lỗi 400 Bad Request ở Backend như đợt trước
    const [formData, setFormData] = useState({
        fileUrl: '',
        matrixId: 1,
        teamId: 1
    });

    const [submissionStatus, setSubmissionStatus] = useState(null);
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(true);
    const [message, setMessage] = useState({ text: '', type: '' });

    const role = localStorage.getItem('role');

    useEffect(() => {
        checkCurrentSubmission();
    }, []);

    // Kiểm tra xem đã có bài nộp trước đó chưa
    const checkCurrentSubmission = async () => {
        try {
            setFetching(true);
            const res = await axiosClient.get('/submissions/my-submission');
            if (res.result) {
                setSubmissionStatus(res.result);
                setFormData({
                    fileUrl: res.result.fileUrl,
                    matrixId: res.result.matrixId || 1,
                    teamId: res.result.teamId || 1
                });
            }
        } catch (err) {
            // Lỗi 404 nghĩa là chưa nộp bài, hệ thống sẽ hiện form nộp mới (POST)
        } finally {
            setFetching(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (role !== 'LEADER') {
            setMessage({ text: 'Chỉ có Đội trưởng (LEADER) mới có quyền nộp hoặc sửa bài.', type: 'error' });
            return;
        }

        setLoading(true);
        setMessage({ text: '', type: '' });

        try {
            if (submissionStatus) {
                // Đã có bài -> Gọi API Update (PUT)
                await axiosClient.put(`/submissions/${submissionStatus.id}`, formData);
                setMessage({ text: '🔄 Cập nhật bài dự thi thành công!', type: 'success' });
            } else {
                // Chưa có bài -> Gọi API Create (POST)
                await axiosClient.post('/submissions', formData);
                setMessage({ text: '🎉 Nộp bài thành công! Dự án của đội bạn đã được ghi nhận.', type: 'success' });
                checkCurrentSubmission(); // Reload lại để load trạng thái đã nộp
            }
        } catch (err) {
            setMessage({ text: err.response?.data?.message || err.message || 'Có lỗi xảy ra. Vui lòng kiểm tra lại quyền hoặc dữ liệu!', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    if (fetching) return <div className="p-8 text-center text-gray-500">Đang kiểm tra dữ liệu bài nộp...</div>;

    return (
        <div className="max-w-3xl mx-auto space-y-6">

            {/* Feedback khu vực nếu bài đã được chấm điểm */}
            {submissionStatus && submissionStatus.isGraded && (
                <div className="bg-gradient-to-r from-green-50 to-emerald-50 p-6 rounded-xl shadow-sm border border-green-200">
                    <h3 className="text-lg font-bold text-green-800 mb-4">⚖️ Kết quả chấm điểm</h3>
                    <div className="flex justify-between bg-white p-4 rounded-lg border border-green-100 mb-4">
                        <span className="text-gray-600 font-medium">Điểm số trung bình:</span>
                        <span className="text-3xl font-black text-green-600">{submissionStatus.score}/100</span>
                    </div>
                    <div>
                        <span className="text-sm font-medium text-gray-600">Nhận xét từ BGK:</span>
                        <p className="mt-1 text-gray-800 italic bg-white p-4 rounded-lg border border-green-100">
                            "{submissionStatus.feedback || 'Chưa có nhận xét chi tiết.'}"
                        </p>
                    </div>
                </div>
            )}

            <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100">
                <div className="mb-8 border-b pb-4">
                    <h2 className="text-2xl font-bold text-gray-800">
                        {submissionStatus ? '📝 Cập nhật Bài Dự Thi' : '📤 Nộp Bài Dự Thi'}
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">
                        {submissionStatus ? 'Bạn có thể cập nhật link tài liệu trước hạn chót.' : 'Gửi tài liệu tổng hợp về dự án của đội bạn đến Ban giám khảo.'}
                    </p>
                </div>

                {message.text && (
                    <div className={`mb-6 p-4 text-sm rounded-lg border ${message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                        {message.text}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Đường dẫn tài liệu dự án (File URL) *</label>
                        <input
                            type="url" required
                            placeholder="VD: Link GitHub hoặc Drive chứa mã nguồn hệ thống nhận diện vật cản qua BLE..."
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                            value={formData.fileUrl}
                            onChange={(e) => setFormData({...formData, fileUrl: e.target.value})}
                            disabled={role !== 'LEADER'} // Chỉ Leader mới được phép sửa
                        />
                        <p className="mt-2 text-xs text-gray-500">
                            Hãy nén toàn bộ tài liệu (Slide, Video Demo, tài liệu kiến trúc hệ thống xử lý offline-first, mã nguồn) vào một thư mục Drive/GitHub và dán link vào đây.
                        </p>
                    </div>

                    <div className="pt-4 flex justify-end">
                        <button
                            type="submit" disabled={loading || role !== 'LEADER'}
                            className="px-8 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 transition-all shadow-md"
                        >
                            {loading ? 'Đang gửi dữ liệu...' : submissionStatus ? 'Cập nhật tài liệu' : 'Gửi Bài Dự Thi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}