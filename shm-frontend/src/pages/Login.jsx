import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

export default function Login() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            // Gọi API sang Spring Boot (đã được cấu hình ở axiosClient)
            const response = await axiosClient.post('/auth/login', formData);
            const { token, role, email } = response.result;

            // Lưu token vào localStorage
            localStorage.setItem('token', token);
            localStorage.setItem('role', role);
            localStorage.setItem('email', email);

            alert('Đăng nhập thành công!');
            navigate('/dashboard'); // Sẽ làm trang này sau
        } catch (err) {
            setError(err.message || 'Sai tài khoản hoặc mật khẩu!');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-slate-100">
            <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-xl shadow-lg border border-slate-200">
                <div className="text-center">
                    <h1 className="text-3xl font-extrabold text-indigo-600">SEAL Hackathon</h1>
                    <p className="mt-2 text-sm text-gray-500">Đăng nhập vào hệ thống quản lý</p>
                </div>

                {error && <div className="p-3 text-sm text-red-600 bg-red-100 rounded-lg">{error}</div>}

                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email FPT / Cá nhân</label>
                        <input
                            type="email"
                            required
                            className="w-full px-4 py-2 mt-1 border rounded-lg focus:ring-indigo-500 focus:border-indigo-500 bg-slate-50"
                            placeholder="example@fpt.edu.vn"
                            value={formData.email}
                            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Mật khẩu</label>
                        <input
                            type="password"
                            required
                            className="w-full px-4 py-2 mt-1 border rounded-lg focus:ring-indigo-500 focus:border-indigo-500 bg-slate-50"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full px-4 py-2 font-bold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50"
                    >
                        {loading ? 'Đang xử lý...' : 'Đăng Nhập'}
                    </button>
                </form>

                <p className="text-sm text-center text-gray-600">
                    Chưa có tài khoản?{' '}
                    <Link to="/register" className="font-medium text-indigo-600 hover:underline">
                        Đăng ký ngay
                    </Link>
                </p>
            </div>
        </div>
    );
}