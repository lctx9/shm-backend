import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

export default function Register() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        password: '',
        confirmPassword: '',
        studentId: '',
        isFptStudent: true,
        universityName: 'Đại học FPT',
        studentCardUrl: '',
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (formData.password !== formData.confirmPassword) {
            setError('Mật khẩu xác nhận không khớp!');
            return;
        }

        setLoading(true);
        try {
            const response = await axiosClient.post('/auth/register', {
                fullName: formData.fullName,
                email: formData.email,
                password: formData.password,
                studentId: formData.studentId,
                isFptStudent: formData.isFptStudent,
                universityName: formData.isFptStudent ? 'Đại học FPT' : formData.universityName,
                studentCardUrl: formData.studentCardUrl || ''
            });

            // Sử dụng chuẩn trường response.result từ ApiResponse
            alert(response.result || 'Đăng ký thành công! Đang chờ duyệt.');
            navigate('/login');
        } catch (err) {
            setError(err.message || 'Có lỗi xảy ra khi đăng ký!');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen py-10 bg-slate-100">
            <div className="w-full max-w-xl p-8 space-y-6 bg-white rounded-xl shadow-lg border border-slate-200">
                <div className="text-center">
                    <h1 className="text-3xl font-extrabold text-indigo-600">Đăng Ký SEAL Hackathon</h1>
                    <p className="mt-2 text-sm text-gray-500">Tạo tài khoản thí sinh để tham gia giải đấu</p>
                </div>

                {error && <div className="p-3 text-sm text-red-600 bg-red-100 rounded-lg">{error}</div>}

                <form className="space-y-4" onSubmit={handleSubmit}>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="col-span-2">
                            <label className="block text-sm font-medium text-gray-700">Họ và Tên</label>
                            <input required type="text" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50 focus:ring-indigo-500 focus:border-indigo-500"
                                   value={formData.fullName} onChange={e => setFormData({...formData, fullName: e.target.value})} />
                        </div>

                        <div className="col-span-2 sm:col-span-1">
                            <label className="block text-sm font-medium text-gray-700">Email</label>
                            <input required type="email" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50"
                                   value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} />
                        </div>

                        <div className="col-span-2 sm:col-span-1">
                            <label className="block text-sm font-medium text-gray-700">Mã Số Sinh Viên</label>
                            <input required type="text" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50"
                                   value={formData.studentId} onChange={e => setFormData({...formData, studentId: e.target.value})} />
                        </div>

                        <div className="col-span-2 sm:col-span-1">
                            <label className="block text-sm font-medium text-gray-700">Mật khẩu</label>
                            <input required type="password" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50"
                                   value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} />
                        </div>

                        <div className="col-span-2 sm:col-span-1">
                            <label className="block text-sm font-medium text-gray-700">Xác nhận mật khẩu</label>
                            <input required type="password" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50"
                                   value={formData.confirmPassword} onChange={e => setFormData({...formData, confirmPassword: e.target.value})} />
                        </div>

                        {/* Toggle Sinh viên FPT hay trường ngoài */}
                        <div className="col-span-2 flex items-center space-x-3 mt-2">
                            <input type="checkbox" id="isFpt" className="w-5 h-5 text-indigo-600 rounded"
                                   checked={formData.isFptStudent} onChange={e => setFormData({...formData, isFptStudent: e.target.checked})} />
                            <label htmlFor="isFpt" className="font-medium text-gray-700">Tôi là sinh viên Đại học FPT</label>
                        </div>

                        {!formData.isFptStudent && (
                            <div className="col-span-2">
                                <label className="block text-sm font-medium text-gray-700">Tên Trường Đại Học</label>
                                <input required type="text" className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50"
                                       value={formData.universityName} onChange={e => setFormData({...formData, universityName: e.target.value})} />
                            </div>
                        )}

                        <div className="col-span-2">
                            <label className="block text-sm font-medium text-gray-700">Link Ảnh thẻ Sinh Viên (Xác thực)</label>
                            <input type="url" placeholder="https://..." className="w-full px-4 py-2 mt-1 border rounded-lg bg-slate-50 text-sm"
                                   value={formData.studentCardUrl} onChange={e => setFormData({...formData, studentCardUrl: e.target.value})} />
                            <p className="text-xs text-gray-500 mt-1">Ảnh cần thấy rõ Tên, MSSV và Logo trường.</p>
                        </div>
                    </div>

                    <button type="submit" disabled={loading} className="w-full px-4 py-2 mt-4 font-bold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50">
                        {loading ? 'Đang xử lý...' : 'Đăng Ký Tài Khoản'}
                    </button>
                </form>

                <p className="text-sm text-center text-gray-600">
                    Đã có tài khoản? <Link to="/login" className="font-medium text-indigo-600 hover:underline">Đăng nhập</Link>
                </p>
            </div>
        </div>
    );
}
