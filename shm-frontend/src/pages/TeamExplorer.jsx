import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function TeamExplorer() {
    const [teams, setTeams] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // State phục vụ cho việc nhập pass của đội Private
    const [selectedTeam, setSelectedTeam] = useState(null);
    const [joinPassword, setJoinPassword] = useState('');

    useEffect(() => {
        fetchTeams();
    }, []);

    const fetchTeams = async () => {
        try {
            setLoading(true);
            // Gọi API lấy danh sách đội thi
            const response = await axiosClient.get('/teams');
            setTeams(response.result || []);
        } catch (err) {
            setError('Không thể tải danh sách đội thi. Có thể API GET /api/teams chưa mở.');
        } finally {
            setLoading(false);
        }
    };

    const handleJoinTeam = async (e) => {
        e.preventDefault();
        if (!selectedTeam) return;

        try {
            // Gọi API xin gia nhập đội
            await axiosClient.post(`/teams/${selectedTeam.id}/join`, {
                password: joinPassword
            });
            alert('🎉 Gia nhập đội thành công! Chào mừng bạn đến với ' + selectedTeam.name);
            setSelectedTeam(null);
            setJoinPassword('');
            fetchTeams(); // Tải lại danh sách
        } catch (err) {
            alert('Lỗi: ' + (err.message || 'Mật khẩu không đúng hoặc đội đã đầy!'));
        }
    };

    return (
        <div className="max-w-6xl mx-auto">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h2 className="text-2xl font-bold text-gray-800">🔍 Khám Phá Đội Thi</h2>
                    <p className="text-sm text-gray-500 mt-1">Tìm kiếm và gia nhập các đội thi đang mở để cùng nhau tranh tài.</p>
                </div>
                <button onClick={fetchTeams} className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 text-gray-700 shadow-sm transition-all">
                    🔄 Làm mới
                </button>
            </div>

            {error && <div className="p-4 mb-6 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">{error}</div>}

            {loading ? (
                <div className="text-center py-10 text-gray-500">Đang tải dữ liệu...</div>
            ) : teams.length === 0 ? (
                <div className="text-center py-10 bg-white rounded-xl border border-gray-200 text-gray-500">
                    Hiện tại chưa có đội thi nào trên hệ thống.
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {teams.map((team) => (
                        <div key={team.id} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow">
                            <div className="p-6">
                                <div className="flex justify-between items-start mb-4">
                                    <h3 className="text-lg font-bold text-gray-900 truncate pr-2">{team.name}</h3>
                                    <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                                        team.type === 'PUBLIC' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                                    }`}>
                    {team.type === 'PUBLIC' ? '🌍 Mở' : '🔒 Riêng tư'}
                  </span>
                                </div>

                                <div className="space-y-2 mb-6">
                                    <p className="text-sm text-gray-600">
                                        <span className="font-medium">Hạng mục:</span> {team.trackId === 1 ? 'Ứng dụng AI' : team.trackId === 2 ? 'Phát triển Web/App' : 'Thiết bị thông minh & Cảm biến'}
                                    </p>
                                    <p className="text-sm text-gray-600">
                                        <span className="font-medium">Thành viên:</span> {team.memberCount || 1}/4
                                    </p>
                                </div>

                                <button
                                    onClick={() => team.type === 'PUBLIC' ? handleJoinTeam({ preventDefault: () => {}, selectedTeam: team }) : setSelectedTeam(team)}
                                    className="w-full px-4 py-2 bg-indigo-50 text-indigo-700 font-medium rounded-lg hover:bg-indigo-100 transition-colors border border-indigo-200"
                                >
                                    {team.type === 'PUBLIC' ? 'Gia nhập ngay' : 'Xin gia nhập'}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Modal nhập Password cho đội Private */}
            {selectedTeam && selectedTeam.type === 'PRIVATE' && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded-xl shadow-lg max-w-sm w-full mx-4">
                        <h3 className="text-lg font-bold text-gray-900 mb-2">Gia nhập đội: {selectedTeam.name}</h3>
                        <p className="text-sm text-gray-500 mb-4">Đội thi này được thiết lập riêng tư. Vui lòng nhập mật khẩu để tham gia.</p>

                        <form onSubmit={handleJoinTeam}>
                            <input
                                type="text" required placeholder="Nhập mật khẩu đội..."
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 mb-4"
                                value={joinPassword}
                                onChange={(e) => setJoinPassword(e.target.value)}
                            />
                            <div className="flex space-x-3">
                                <button
                                    type="button" onClick={() => { setSelectedTeam(null); setJoinPassword(''); }}
                                    className="flex-1 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                                >
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