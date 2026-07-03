import { useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function Profile() {
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    const [passwords, setPasswords] = useState({
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    const handleChange = (e) => {
        setPasswords({ ...passwords, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });

        if (passwords.newPassword !== passwords.confirmPassword) {
            setMessage({ text: 'Mật khẩu mới không khớp!', type: 'error' });
            return;
        }

        setLoading(true);
        try {
            // API đổi mật khẩu (Cần code thêm ở backend: PUT /api/users/change-password)
            await axiosClient.put('/users/change-password', {
                oldPassword: passwords.oldPassword,
                newPassword: passwords.newPassword
            });

            setMessage({ text: 'Cập nhật mật khẩu thành công!', type: 'success' });
            setPasswords({ oldPassword: '', newPassword: '', confirmPassword: '' }); // Xóa trắng form
        } catch (err) {
            setMessage({ text: err.response?.data?.message || 'Mật khẩu cũ không đúng hoặc có lỗi hệ thống.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-2xl mx-auto">
            {/* Thông tin cá nhân */}
            <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100 mb-8 flex items-center space-x-6">
                <div className="w-20 h-20 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-700 text-3xl font-bold">
                    {email ? email.charAt(0).toUpperCase() : 'U'}
                </div>
                <div>
                    <h2 className="text-2xl font-bold text-gray-800">{email}</h2>
                    <p className="text-sm font-medium text-indigo-600 mt-1">Quyền truy cập: {role}</p>
                </div>
            </div>

            {/* Form đổi mật khẩu */}
            <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100">
                <h3 className="text-xl font-bold text-gray-800 mb-6 border-b pb-4">🔒 Đổi mật khẩu</h3>

                {message.text && (
                    <div className={`mb-6 p-4 text-sm rounded-lg border ${message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                        {message.text}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Mật khẩu hiện tại</label>
                        <input
                            type="password" name="oldPassword" required
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                            value={passwords.oldPassword} onChange={handleChange}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Mật khẩu mới</label>
                        <input
                            type="password" name="newPassword" required minLength={6}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                            value={passwords.newPassword} onChange={handleChange}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Xác nhận mật khẩu mới</label>
                        <input
                            type="password" name="confirmPassword" required minLength={6}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-gray-50"
                            value={passwords.confirmPassword} onChange={handleChange}
                        />
                    </div>

                    <div className="pt-4">
                        <button
                            type="submit" disabled={loading}
                            className="w-full px-8 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 transition-all shadow-md"
                        >
                            {loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}