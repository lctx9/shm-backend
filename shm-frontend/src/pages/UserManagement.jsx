import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function UserManagement() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Tự động gọi API lấy danh sách user khi vào trang
    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            // Gọi API lấy danh sách user (giả định backend hỗ trợ endpoint này)
            const response = await axiosClient.get('/users');

            // AxiosClient trả về response.result dựa theo cấu trúc ApiResponse của Spring Boot
            setUsers(response.result || []);
        } catch (err) {
            setError('Không thể tải danh sách người dùng. Có thể API chưa được thiết lập ở Backend.');
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateStatus = async (userId, newStatus) => {
        if (!window.confirm(`Bạn có chắc muốn ${newStatus === 'APPROVED' ? 'DUYỆT' : 'TỪ CHỐI'} tài khoản này?`)) return;

        try {
            // Gọi API cập nhật trạng thái tài khoản
            await axiosClient.put(`/users/${userId}/status`, { status: newStatus });

            // Cập nhật lại giao diện ngay lập tức mà không cần load lại trang
            setUsers(users.map(user =>
                user.id === userId ? { ...user, status: newStatus } : user
            ));

        } catch (err) {
            alert('Lỗi cập nhật: ' + (err.message || 'Hệ thống đang gặp sự cố!'));
        }
    };

    return (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-6 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                <div>
                    <h2 className="text-xl font-bold text-gray-800">Quản Lý Tài Khoản</h2>
                    <p className="text-sm text-gray-500 mt-1">Duyệt hoặc từ chối các đăng ký tham gia từ sinh viên.</p>
                </div>
                <button onClick={fetchUsers} className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 text-gray-700 shadow-sm transition-all">
                    🔄 Làm mới
                </button>
            </div>

            {error && <div className="p-4 m-6 mb-0 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">{error}</div>}

            <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                    <thead>
                    <tr className="bg-gray-50 text-gray-600 text-sm uppercase tracking-wider border-b border-gray-200">
                        <th className="px-6 py-4 font-medium">Họ & Tên / MSSV</th>
                        <th className="px-6 py-4 font-medium">Email</th>
                        <th className="px-6 py-4 font-medium">Trường Đại Học</th>
                        <th className="px-6 py-4 font-medium">Trạng Thái</th>
                        <th className="px-6 py-4 font-medium text-right">Thao Tác</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                    {loading ? (
                        <tr>
                            <td colSpan="5" className="px-6 py-8 text-center text-gray-500">Đang tải dữ liệu...</td>
                        </tr>
                    ) : users.length === 0 ? (
                        <tr>
                            <td colSpan="5" className="px-6 py-8 text-center text-gray-500">Chưa có người dùng nào trên hệ thống.</td>
                        </tr>
                    ) : (
                        users.map((user) => (
                            <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                                <td className="px-6 py-4">
                                    <div className="font-medium text-gray-900">{user.fullName}</div>
                                    <div className="text-xs text-gray-500 mt-1">{user.studentId || 'N/A'}</div>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-600">{user.email}</td>
                                <td className="px-6 py-4">
                                    <span className="text-sm text-gray-600">{user.universityName}</span>
                                </td>
                                <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border
                      ${user.status === 'APPROVED' ? 'bg-green-50 text-green-700 border-green-200' :
                        user.status === 'REJECTED' ? 'bg-red-50 text-red-700 border-red-200' :
                            'bg-amber-50 text-amber-700 border-amber-200'}
                    `}>
                      {user.status === 'PENDING' ? '⏳ Chờ Duyệt' :
                          user.status === 'APPROVED' ? '✅ Đã Duyệt' : '❌ Đã Từ Chối'}
                    </span>
                                </td>
                                <td className="px-6 py-4 text-right space-x-2">
                                    {user.status === 'PENDING' && (
                                        <>
                                            <button
                                                onClick={() => handleUpdateStatus(user.id, 'APPROVED')}
                                                className="px-3 py-1.5 bg-green-600 text-white text-xs font-medium rounded hover:bg-green-700 transition-colors"
                                            >
                                                Duyệt
                                            </button>
                                            <button
                                                onClick={() => handleUpdateStatus(user.id, 'REJECTED')}
                                                className="px-3 py-1.5 bg-red-100 text-red-700 text-xs font-medium rounded hover:bg-red-200 transition-colors"
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                </td>
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}