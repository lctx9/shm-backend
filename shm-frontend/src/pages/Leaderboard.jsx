import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function Leaderboard() {
    const [rankings, setRankings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchLeaderboard();
    }, []);

    const fetchLeaderboard = async () => {
        try {
            setLoading(true);
            // Gọi API thực tế xuống Backend (GET /api/leaderboard)
            const response = await axiosClient.get('/leaderboard');

            // AxiosClient bọc sẵn response.result, ta chỉ cần set vào state
            setRankings(response.result || []);
            setError('');
        } catch (err) {
            setError('Không thể tải dữ liệu bảng xếp hạng. API Backend có thể chưa được cấu hình.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-5xl mx-auto bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-6 border-b border-gray-200 flex justify-between items-center bg-gradient-to-r from-indigo-50 to-white">
                <div>
                    <h2 className="text-2xl font-bold text-gray-800">🏆 Bảng Xếp Hạng Giải Đấu</h2>
                    <p className="text-sm text-gray-500 mt-1">Điểm số được tổng hợp từ Ban Giám Khảo (Cập nhật liên tục)</p>
                </div>
                <button onClick={fetchLeaderboard} className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 text-gray-700 shadow-sm transition-all">
                    🔄 Làm mới
                </button>
            </div>

            {error && <div className="p-4 m-6 mb-0 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">{error}</div>}

            <div className="p-0">
                {loading ? (
                    <div className="p-8 text-center text-gray-500">Đang tải dữ liệu xếp hạng...</div>
                ) : rankings.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">Chưa có dữ liệu chấm điểm nào từ Ban giám khảo.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50 text-gray-600 text-sm uppercase tracking-wider border-b border-gray-200">
                            <th className="px-6 py-4 font-medium text-center w-20">Hạng</th>
                            <th className="px-6 py-4 font-medium">Đội Thi</th>
                            <th className="px-6 py-4 font-medium">Dự án</th>
                            <th className="px-6 py-4 font-medium text-right text-indigo-600">Điểm Số</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {rankings.map((team, index) => {
                            // Nếu backend không trả về field rank, ta dùng index + 1 làm thứ hạng
                            const currentRank = team.rank || index + 1;

                            return (
                                <tr key={team.id || index} className="hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4 text-center">
                                        {currentRank === 1 ? <span className="text-3xl" title="Top 1">🥇</span> :
                                            currentRank === 2 ? <span className="text-3xl" title="Top 2">🥈</span> :
                                                currentRank === 3 ? <span className="text-3xl" title="Top 3">🥉</span> :
                                                    <span className="text-lg font-bold text-gray-500">{currentRank}</span>}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="font-bold text-gray-900 text-lg">{team.teamName}</div>
                                        <div className="text-xs text-gray-500 bg-gray-100 inline-block px-2 py-1 rounded mt-1">
                                            {team.track || 'Track chung'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="font-medium text-indigo-700">{team.projectName || 'Chưa cập nhật tên dự án'}</div>
                                        <div className="text-sm text-gray-600 mt-1 line-clamp-2 max-w-md">
                                            {team.description || 'Đang chờ nội dung mô tả...'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <span className="text-2xl font-black text-indigo-600">{team.score || 0}</span>
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}