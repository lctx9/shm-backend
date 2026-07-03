import React from 'react';

export default function Footer() {
    return (
        <footer className="w-full bg-slate-900 text-slate-400 py-12 px-6 border-t border-slate-800">
            <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">

                {/* Cụm thông tin bản quyền */}
                <div className="text-center md:text-left">
                    <div className="flex items-center justify-center md:justify-start gap-2 mb-2">
                        <span className="text-white font-black tracking-wider text-sm">SAIGON REF TEAM</span>
                        <span className="text-[10px] font-bold bg-slate-800 text-slate-300 px-2 py-0.5 rounded">PLATFORM</span>
                    </div>
                    <p className="text-xs text-slate-500 font-medium">
                        &copy; {new Date().getFullYear()} Hệ thống quản lý và vận hành giải đấu Pickleball tự động.
                    </p>
                </div>

                {/* Khối liên kết/Liên hệ nhanh */}
                <div className="flex flex-col sm:flex-row items-center gap-4 sm:gap-6 text-xs font-semibold text-slate-400">
                    <a href="mailto:contact@saigonref.vn" className="hover:text-white transition-colors">📧 Hỗ trợ: contact@saigonref.vn</a>
                    <span className="hidden sm:inline text-slate-800">|</span>
                    <span className="text-slate-500">📍 Ho Chi Minh City, Vietnam</span>
                </div>

            </div>
        </footer>
    );
}