import React, { useState } from 'react';
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
        universityName: '', // Để trống mặc định để bắt buộc nhập khi chọn trường ngoài
        studentCardUrl: '',
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        // Kiểm tra logic mật khẩu khớp nhau
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
                studentCardUrl: formData.studentCardUrl
            });

            alert(response.result || 'Đăng ký thành công! Đang chờ duyệt.');
            navigate('/login');
        } catch (err) {
            setError(err.message || 'Có lỗi xảy ra khi đăng ký!');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-[#F1F5F9] px-4 py-8 antialiased selection:bg-[#1E5BB8]/10 selection:text-[#1E5BB8]">
            {/* Main Container */}
            <div className="w-full max-w-[600px] bg-white rounded-[32px] shadow-xl border border-slate-100 overflow-hidden p-8 sm:p-12 flex flex-col justify-center transition-all duration-500 ease-out transform opacity-0 translate-y-4 animate-[fadeInUp_0.6s_ease-out_forwards]">
                <div className="w-full mx-auto">

                    {/* Tiêu đề */}
                    <div className="mb-6">
                        <h3 className="text-3xl font-bold text-[#1E293B] tracking-tight mb-3">Register</h3>
                        <p className="text-[13px] text-slate-600 leading-relaxed">
                            Tạo tài khoản thí sinh để tham gia giải đấu, tiếp tục hành trình chinh phục Hackathon cùng SEAL.
                        </p>
                    </div>

                    {/* Form chính */}
                    <form onSubmit={handleSubmit} className="space-y-4">

                        {/* Trường: Họ và Tên (Bắt buộc) */}
                        <div>
                            <label className="block text-xs font-bold text-slate-800 mb-1.5 transition-colors duration-200">
                                Họ và Tên <span className="text-red-500">*</span>
                            </label>
                            <input
                                required
                                type="text"
                                value={formData.fullName}
                                onChange={e => setFormData({...formData, fullName: e.target.value})}
                                placeholder="Nhập họ và tên..."
                                className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                            />
                        </div>

                        {/* Khối Grid 2 cột */}
                        <div className="grid grid-cols-2 gap-4">
                            {/* Trường: Email (Bắt buộc) */}
                            <div className="col-span-2 sm:col-span-1">
                                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                    Email <span className="text-red-500">*</span>
                                </label>
                                <input
                                    required
                                    type="email"
                                    value={formData.email}
                                    onChange={e => setFormData({...formData, email: e.target.value})}
                                    placeholder="Email"
                                    className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                />
                            </div>

                            {/* Trường: Mã Số Sinh Viên (Bắt buộc) */}
                            <div className="col-span-2 sm:col-span-1">
                                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                    Mã số sinh viên <span className="text-red-500">*</span>
                                </label>
                                <input
                                    required
                                    type="text"
                                    value={formData.studentId}
                                    onChange={e => setFormData({...formData, studentId: e.target.value})}
                                    placeholder="MSSV"
                                    className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                />
                            </div>

                            {/* Trường: Mật khẩu (Bắt buộc) */}
                            <div className="col-span-2 sm:col-span-1">
                                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                    Mật khẩu <span className="text-red-500">*</span>
                                </label>
                                <input
                                    required
                                    type="password"
                                    value={formData.password}
                                    onChange={e => setFormData({...formData, password: e.target.value})}
                                    placeholder="Mật khẩu"
                                    className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                />
                            </div>

                            {/* Trường: Xác nhận mật khẩu (Bắt buộc) */}
                            <div className="col-span-2 sm:col-span-1">
                                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                    Xác nhận mật khẩu <span className="text-red-500">*</span>
                                </label>
                                <input
                                    required
                                    type="password"
                                    value={formData.confirmPassword}
                                    onChange={e => setFormData({...formData, confirmPassword: e.target.value})}
                                    placeholder="Nhập lại..."
                                    className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                />
                            </div>
                        </div>

                        {/* Toggle Sinh viên FPT (Không bắt buộc tích) */}
                        <div className="flex items-center pt-1">
                            <label className="inline-flex items-center gap-2.5 cursor-pointer group">
                                <input
                                    type="checkbox"
                                    id="isFpt"
                                    checked={formData.isFptStudent}
                                    onChange={e => setFormData({...formData, isFptStudent: e.target.checked})}
                                    className="h-4 w-4 rounded border-slate-300 text-[#1E5BB8] focus:ring-0 cursor-pointer transition-colors"
                                />
                                <span className="text-xs text-slate-600 font-medium select-none group-hover:text-slate-900 transition-colors duration-200">
                                    Tôi là sinh viên Đại học FPT
                                </span>
                            </label>
                        </div>

                        {/* Tên Trường Đại Học (Chỉ bắt buộc nhập khi KHÔNG PHẢI sinh viên FPT) */}
                        {!formData.isFptStudent && (
                            <div className="transition-all duration-300 transform translate-y-0 opacity-0 animate-[slideDown_0.25s_ease-out_forwards]">
                                <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                    Tên Trường Đại Học <span className="text-red-500">*</span>
                                </label>
                                <input
                                    required={!formData.isFptStudent}
                                    type="text"
                                    value={formData.universityName}
                                    onChange={e => setFormData({...formData, universityName: e.target.value})}
                                    placeholder="Nhập tên trường đại học của bạn..."
                                    className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                />
                            </div>
                        )}

                        {/* Link Ảnh thẻ Sinh Viên (Bắt buộc) */}
                        <div>
                            <label className="block text-xs font-bold text-slate-800 mb-1.5">
                                Link Ảnh thẻ Sinh Viên (Xác thực) <span className="text-red-500">*</span>
                            </label>
                            <input
                                required
                                type="url"
                                value={formData.studentCardUrl}
                                onChange={e => setFormData({...formData, studentCardUrl: e.target.value})}
                                placeholder="https://..."
                                className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                            />
                            <p className="text-[11px] text-slate-400 mt-1">Ảnh cần thấy rõ Tên, MSSV và Logo trường.</p>
                        </div>

                        {/* Thông báo lỗi */}
                        {error && (
                            <div className="mt-2 text-xs text-red-600 flex items-center gap-1.5 transition-all duration-300 transform translate-y-0 opacity-0 animate-[slideDown_0.25s_ease-out_forwards]">
                                <svg className="w-3.5 h-3.5 shrink-0" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                </svg>
                                <span className="font-medium">{error}</span>
                            </div>
                        )}

                        {/* Nút Đăng Ký */}
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full mt-2 inline-flex items-center justify-center gap-2 rounded-lg bg-[#1E5BB8] text-white py-2.5 text-sm font-semibold hover:bg-[#164384] active:scale-[0.99] disabled:opacity-60 disabled:pointer-events-none transition-all duration-200 ease-in-out"
                        >
                            {loading && (
                                <svg className="w-4 h-4 animate-spin text-white" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.2" />
                                    <path d="M22 12a10 10 0 00-10-10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
                                </svg>
                            )}
                            <span>{loading ? 'Đang xử lý...' : 'Đăng Ký Tài Khoản'}</span>
                        </button>
                    </form>

                    {/* Quay lại đăng nhập */}
                    <p className="text-center text-xs text-slate-600 mt-6 font-medium">
                        Đã có tài khoản?{' '}
                        <Link to="/login" className="text-[#1E5BB8] font-bold underline-offset-4 hover:underline transition-all duration-200">
                            Đăng nhập ngay
                        </Link>
                    </p>

                </div>
            </div>
        </div>
    );
}