import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function TeamExplorer() {
    const [teams, setTeams] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState({ text: '', type: '' });

    // State quản lý Modal nhập mật khẩu cho đội Private
    const [modal, setModal] = useState({ isOpen: false, teamId: null, password: '', teamName: '' });

    useEffect(() => {
        fetchTeams();
    }, []);

    const fetchTeams = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/teams');
            setTeams(response.result || []);
        } catch (err) {
            setMessage({ text: 'Không thể tải danh sách đội thi.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // 1. Xử lý xin gia nhập đội PUBLIC (Gửi yêu cầu chờ duyệt)
    const handleJoinPublic = async (teamId) => {
        try {
            setMessage({ text: '', type: '' });
            await axiosClient.post(`/teams/${teamId}/join-request`);
            setMessage({ text: '✅ Đã gửi yêu cầu gia nhập thành công! Đang chờ Đội trưởng duyệt.', type: 'success' });
        } catch (err) {
            setMessage({ text: err.response?.data?.message || err.message || 'Lỗi khi gửi yêu cầu gia nhập.', type: 'error' });
        }
    };

    // 2. Xử lý xin gia nhập đội PRIVATE (Nhập mật khẩu)
    const handleJoinPrivate = async (e) => {
        e.preventDefault();
        try {
            setMessage({ text: '', type: '' });
            await axiosClient.post(`/teams/${modal.teamId}/join-private`, { password: modal.password });

            setMessage({ text: `🎉 Chúc mừng! Bạn đã gia nhập đội ${modal.teamName} thành công.`, type: 'success' });
            setModal({ isOpen: false, teamId: null, password: '', teamName: '' });

            // Nếu gia nhập thành công, có thể dùng window.location.href = '/dashboard/my-team' để chuyển trang
        } catch (err) {
            setMessage({ text: err.response?.data?.message || err.message || 'Sai mật khẩu hoặc có lỗi xảy ra!', type: 'error' });
        }
    };

    if (loading) return <div className="p-8 text-center text-gray-500">Đang tải danh sách đội thi...</div>;

    return (
        <div className="max-w-5xl mx-auto space-y-6 relative">
            <div className="border-b pb-4">
                <h2 className="text-2xl font-bold text-gray-800">🔍 Khám Phá Đội Thi</h2>
                <p className="text-sm text-gray-500 mt-1">Tìm kiếm và xin gia nhập vào các đội thi đang thiếu thành viên.</p>
            </div>

            {message.text && (
                <div className={`p-4 text-sm rounded-lg border ${message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                    {message.text}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {teams.length === 0 ? (
                    <div className="col-span-2 text-center p-8 text-gray-500 bg-white rounded-xl border border-gray-200">
                        Chưa có đội thi nào được thành lập.
                    </div>
                ) : (
                    teams.map(team => (
                        <div key={team.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 flex flex-col justify-between hover:shadow-md transition-shadow">
                            <div>
                                <div className="flex justify-between items-start mb-3">
                                    <h3 className="text-xl font-bold text-gray-900">{team.name}</h3>
                                    <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                                        team.type === 'PUBLIC' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                                    }`}>
                                        {team.type === 'PUBLIC' ? '🌍 Public' : '🔒 Private'}
                                    </span>
                                </div>
                                <p className="text-sm text-gray-600 mb-2">Hạng mục: <span className="font-semibold">{team.trackName || team.trackId}</span></p>
                                <p className="text-sm text-gray-600">Thành viên: <span className="font-semibold">{team.members?.length || 1}/4</span></p>
                            </div>

                            <div className="mt-6 pt-4 border-t border-gray-100">
                                {team.type === 'PUBLIC' ? (
                                    <button
                                        onClick={() => handleJoinPublic(team.id)}
                                        className="w-full px-4 py-2 bg-indigo-50 text-indigo-700 font-semibold rounded-lg hover:bg-indigo-100 transition-colors"
                                    >
                                        👋 Xin gia nhập (Chờ duyệt)
                                    </button>
                                ) : (
                                    <button
                                        onClick={() => setModal({ isOpen: true, teamId: team.id, password: '', teamName: team.name })}
                                        className="w-full px-4 py-2 bg-amber-50 text-amber-700 font-semibold rounded-lg hover:bg-amber-100 transition-colors"
                                    >
                                        🔑 Nhập mã gia nhập
                                    </button>
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* MODAL NHẬP MẬT KHẨU CHO ĐỘI PRIVATE */}
            {modal.isOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded-xl shadow-lg max-w-sm w-full">
                        <h3 className="text-lg font-bold text-gray-900 mb-2">🔑 Gia nhập đội Private</h3>
                        <p className="text-sm text-gray-500 mb-4">Đội <span className="font-bold text-gray-800">{modal.teamName}</span> yêu cầu mật khẩu để tham gia.</p>

                        <form onSubmit={handleJoinPrivate}>
                            <input
                                type="text" required placeholder="Nhập mật khẩu đội..."
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 mb-4"
                                value={modal.password}
                                onChange={(e) => setModal({...modal, password: e.target.value})}
                            />
                            <div className="flex justify-end gap-3">
                                <button type="button" onClick={() => setModal({ isOpen: false, teamId: null, password: '', teamName: '' })} className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">
                                    Hủy
                                </button>
                                <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
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