import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function MyTeam() {
    // State quản lý thông tin đội đã có
    const [team, setTeam] = useState(null);
    const [loadingTeam, setLoadingTeam] = useState(true);
    const role = localStorage.getItem('role');

    // State cho Form tạo đội mới
    const [formData, setFormData] = useState({
        name: '',
        type: 'PUBLIC',
        joinPassword: '',
        trackId: 1,
        eventId: 1
    });
    const [loadingCreate, setLoadingCreate] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchMyTeam();
    }, []);

    // 1. Hàm lấy thông tin đội hiện tại
    const fetchMyTeam = async () => {
        try {
            setLoadingTeam(true);
            const response = await axiosClient.get('/teams/my-team');
            setTeam(response.result); // Nếu có đội, lưu vào state
        } catch (err) {
            setTeam(null); // Không có đội (báo lỗi 404) thì set null để hiện form tạo
        } finally {
            setLoadingTeam(false);
        }
    };

    // 2. Hàm xử lý Tạo đội mới
    const handleCreateTeam = async (e) => {
        e.preventDefault();
        setLoadingCreate(true);
        setError('');
        try {
            await axiosClient.post('/teams/create', formData);
            alert('🎉 Tạo đội thành công!');
            window.location.reload(); // Reload lại để load giao diện quản lý đội
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Có lỗi xảy ra khi tạo đội. Tên đội có thể đã tồn tại!');
        } finally {
            setLoadingCreate(false);
        }
    };

    // 3. Hàm xử lý Xóa thành viên (Dành cho Leader)
    const handleRemoveMember = async (memberId, memberName) => {
        if (!window.confirm(`Bạn có chắc chắn muốn xóa ${memberName} khỏi đội?`)) return;
        try {
            await axiosClient.delete(`/teams/${team.id}/members/${memberId}`);
            alert('Đã xóa thành viên thành công!');
            fetchMyTeam(); // Tải lại danh sách
        } catch (err) {
            alert('Lỗi khi xóa thành viên: ' + (err.response?.data?.message || err.message));
        }
    };

    if (loadingTeam) return <div className="p-8 text-center text-gray-500">Đang kiểm tra dữ liệu đội thi...</div>;

    // ==========================================
    // GIAO DIỆN 1: NẾU ĐÃ CÓ ĐỘI -> HIỆN QUẢN LÝ
    // ==========================================
    if (team) {
        return (
            <div className="max-w-4xl mx-auto space-y-6">
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 flex justify-between items-start">
                    <div>
                        <div className="flex items-center space-x-3 mb-2">
                            <h2 className="text-2xl font-bold text-gray-900">{team.name}</h2>
                            <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                                team.type === 'PUBLIC' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                            }`}>
                                {team.type === 'PUBLIC' ? '🌍 Public' : '🔒 Private'}
                            </span>
                        </div>
                        <p className="text-gray-600 font-medium">Hạng mục thi (Track): <span className="text-indigo-600">{team.trackName || team.trackId || 'Chưa cập nhật'}</span></p>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="p-5 border-b border-gray-200 bg-gray-50">
                        <h3 className="text-lg font-bold text-gray-800">👥 Thành viên ({team.members?.length || 1}/4)</h3>
                    </div>
                    <ul className="divide-y divide-gray-100">
                        {team.members?.map((member) => (
                            <li key={member.id} className="p-5 flex justify-between items-center hover:bg-gray-50">
                                <div>
                                    <p className="font-semibold text-gray-900">{member.name || member.email}</p>
                                    <p className="text-sm text-gray-500">{member.role === 'LEADER' ? '👑 Đội trưởng' : 'Thành viên'}</p>
                                </div>
                                {role === 'LEADER' && member.role !== 'LEADER' && (
                                    <button
                                        onClick={() => handleRemoveMember(member.id, member.email)}
                                        className="px-3 py-1.5 text-sm text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-colors"
                                    >
                                        Xóa
                                    </button>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        );
    }

    // ==========================================
    // GIAO DIỆN 2: CHƯA CÓ ĐỘI -> HIỆN FORM TẠO
    // ==========================================
    return (
        <div className="max-w-2xl mx-auto bg-white p-8 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-800 mb-2">Đăng ký Đội Thi Mới</h2>
            <p className="text-sm text-gray-500 mb-6">Trở thành Team Leader và mời các thành viên khác gia nhập đội của bạn.</p>

            {error && <div className="mb-6 p-3 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">{error}</div>}

            <form onSubmit={handleCreateTeam} className="space-y-5">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Tên Đội Thi</label>
                    <input
                        type="text" required placeholder="Nhập tên đội của bạn..."
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                        value={formData.name}
                        onChange={(e) => setFormData({...formData, name: e.target.value})}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Hạng Mục (Track)</label>
                    <select
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                        value={formData.trackId}
                        onChange={(e) => setFormData({...formData, trackId: Number(e.target.value)})}
                    >
                        <option value={1}>Ứng dụng AI</option>
                        <option value={2}>Phát triển Web/App</option>
                        <option value={3}>Thiết bị đeo thông minh & Cảm biến (Wearables/IoT)</option>
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Chế độ đội thi</label>
                    <div className="flex space-x-6 bg-gray-50 p-3 rounded-lg border border-gray-200">
                        <label className="flex items-center cursor-pointer">
                            <input type="radio" className="w-4 h-4 text-indigo-600" value="PUBLIC"
                                   checked={formData.type === 'PUBLIC'}
                                   onChange={(e) => setFormData({...formData, type: e.target.value})} />
                            <span className="ml-2 text-sm text-gray-700">Công khai</span>
                        </label>
                        <label className="flex items-center cursor-pointer">
                            <input type="radio" className="w-4 h-4 text-indigo-600" value="PRIVATE"
                                   checked={formData.type === 'PRIVATE'}
                                   onChange={(e) => setFormData({...formData, type: e.target.value})} />
                            <span className="ml-2 text-sm text-gray-700">Riêng tư</span>
                        </label>
                    </div>
                </div>

                {formData.type === 'PRIVATE' && (
                    <div className="animate-fade-in-down">
                        <label className="block text-sm font-medium text-gray-700 mb-1">Mật khẩu gia nhập</label>
                        <input
                            type="text" required placeholder="Thiết lập mật khẩu..."
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                            value={formData.joinPassword}
                            onChange={(e) => setFormData({...formData, joinPassword: e.target.value})}
                        />
                    </div>
                )}

                <div className="pt-2">
                    <button
                        type="submit" disabled={loadingCreate}
                        className="w-full px-4 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-all"
                    >
                        {loadingCreate ? 'Đang khởi tạo...' : 'Xác Nhận Tạo Đội'}
                    </button>
                </div>
            </form>
        </div>
    );
}