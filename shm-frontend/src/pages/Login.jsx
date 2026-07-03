import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

export default function Login() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const response = await axiosClient.post('/auth/login', formData);
            const { token, role, email } = response.result || response;

            localStorage.setItem('token', token);
            localStorage.setItem('role', role);
            localStorage.setItem('email', email);

            navigate('/dashboard');
        } catch (err) {
            setError(err?.message || 'Error message required.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-[#F1F5F9] px-4 py-8 antialiased selection:bg-[#1E5BB8]/10 selection:text-[#1E5BB8]">
            {/* Main Container: Thêm hiệu ứng xuất hiện mượt mà khi load trang (animate-fade-in-up) */}
            <div className="w-full max-w-[1100px] bg-white rounded-[32px] shadow-xl border border-slate-100 overflow-hidden flex min-h-[600px] transition-all duration-500 ease-out transform opacity-0 translate-y-4 animate-[fadeInUp_0.6s_ease-out_forwards]">

                {/* BÊN TRÁI: Banner hình ảnh lớn */}
                <div
                    className="hidden lg:block lg:w-[95%] relative bg-no-repeat bg-cover bg-center transition-transform duration-700 ease-out hover:scale-[1.01]"
                    style={{
                        backgroundImage: "url('/a.png')",
                    }}
                >
                    {/* Lớp overlay chuyển màu siêu nhẹ tạo chiều sâu cho ảnh */}
                    <div className="absolute inset-0 bg-gradient-to-r from-black/5 to-transparent pointer-events-none"></div>
                </div>

                {/* BÊN PHẢI: Form Đăng nhập */}
                <div className="w-full lg:w-[45%] p-8 sm:p-12 md:p-14 flex flex-col justify-center bg-white">
                    <div className="w-full max-w-sm mx-auto">

                        {/* Tiêu đề */}
                        <div className="mb-6">
                            <h3 className="text-3xl font-bold text-[#1E293B] tracking-tight mb-3">Login</h3>
                            <p className="text-[13px] text-slate-600 leading-relaxed">
                                Đăng nhập tài khoản dữ liệu của bạn, tiếp tục hành trình chinh phục Hackathon cùng SEAL.
                            </p>
                        </div>

                        {/* Form chính */}
                        <form onSubmit={handleSubmit} className="space-y-4">

                            {/* Ô nhập Username */}
                            <div>
                                <label className="block text-xs font-bold text-slate-800 mb-1.5 transition-colors duration-200">Username</label>
                                <input
                                    type="email"
                                    required
                                    value={formData.email}
                                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                    placeholder="Username"
                                    className={`w-full px-4 py-2.5 rounded-lg border text-slate-800 bg-white placeholder-slate-400 focus:outline-none transition-all duration-200 font-medium text-sm ${
                                        error
                                            ? 'border-red-400 focus:border-red-500 bg-red-50/10'
                                            : 'border-slate-300 focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5'
                                    }`}
                                />

                                {/* Thông báo lỗi hiển thị mượt mà với hiệu ứng slideDown */}
                                {error && (
                                    <div className="mt-2 text-xs text-red-600 flex items-center gap-1.5 transition-all duration-300 transform translate-y-0 opacity-0 animate-[slideDown_0.25s_ease-out_forwards]">
                                        <svg className="w-3.5 h-3.5 shrink-0" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                        </svg>
                                        <span className="font-medium">{error}</span>
                                    </div>
                                )}
                            </div>

                            {/* Ô nhập Mật khẩu */}
                            <div>
                                <div className="flex justify-between items-center mb-1.5">
                                    <label className="block text-xs font-bold text-slate-800">Password</label>
                                    <Link to="/forgot" className="text-xs font-bold text-[#1E5BB8] hover:text-[#164384] underline-offset-4 hover:underline transition-all duration-200">
                                        Quên mật khẩu?
                                    </Link>
                                </div>
                                <div className="relative group">
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        required
                                        value={formData.password}
                                        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                        placeholder="Password"
                                        className="w-full pl-4 pr-10 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] focus:ring-4 focus:ring-[#1E5BB8]/5 transition-all duration-200 font-medium text-sm"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1.5 rounded-md hover:bg-slate-50 transition-colors duration-200"
                                    >
                                        {showPassword ? (
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                                            </svg>
                                        ) : (
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            </svg>
                                        )}
                                    </button>
                                </div>
                            </div>

                            {/* Ghi nhớ tài khoản */}
                            <div className="flex items-center pt-1">
                                <label className="inline-flex items-center gap-2.5 cursor-pointer group">
                                    <input
                                        type="checkbox"
                                        checked={rememberMe}
                                        onChange={(e) => setRememberMe(e.target.checked)}
                                        className="h-4 w-4 rounded border-slate-300 text-[#1E5BB8] focus:ring-0 cursor-pointer transition-colors"
                                    />
                                    <span className="text-xs text-slate-600 font-medium select-none group-hover:text-slate-900 transition-colors duration-200">
                                        Duy trì đăng nhập
                                    </span>
                                </label>
                            </div>

                            {/* Nút Đăng nhập Flat hoàn toàn nhưng mượt mà */}
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
                                <span>{loading ? 'Logging in...' : 'Log in'}</span>
                            </button>
                        </form>

                        {/* Chuyển hướng đăng ký */}
                        <p className="text-center text-xs text-slate-600 mt-6 font-medium">
                            Bạn là thành viên mới?{' '}
                            <Link to="/register" className="text-[#1E5BB8] font-bold underline-offset-4 hover:underline transition-all duration-200">
                                Tạo tài khoản ngay
                            </Link>
                        </p>

                    </div>
                </div>

            </div>
        </div>
    );
}