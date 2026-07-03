import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';

const coordinatorItems = [
    { to: '/dashboard/events', label: 'Quản lý giải đấu' },
    { to: '/dashboard/users', label: 'Tài khoản & staff' },
    { to: '/dashboard/notifications', label: 'Thông báo' },
    { to: '/dashboard/audit-logs', label: 'Audit log' },
];

const teamItems = [
    { to: '/dashboard/teams', label: 'Sảnh đội thi' },
    { to: '/dashboard/my-team', label: 'Đội của tôi' },
    { to: '/dashboard/submissions', label: 'Nộp bài' },
    { to: '/dashboard/chat', label: 'Chat mentor/team' },
    { to: '/dashboard/notifications', label: 'Thông báo' },
];

export default function DashboardLayout() {
    const navigate = useNavigate();
    const location = useLocation();
    const role = localStorage.getItem('role');
    const email = localStorage.getItem('email');

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('email');
        navigate('/login');
    };

    const navClass = (path) => (
        location.pathname === path
            ? 'bg-[#0f63c9] text-white shadow-sm'
            : 'text-slate-700 hover:bg-[#eaf3ff] hover:text-[#0f63c9]'
    );

    return (
        <div className="flex h-screen bg-[#f4f8ff] text-[#0b1f3f]">
            <aside className="flex w-72 flex-col border-r border-[#d7e6f8] bg-white">
                <div className="flex h-20 items-center gap-3 border-b border-[#d7e6f8] px-6">
                    <span className="brand-mark"><span className="brand-mark-text">SEAL</span></span>
                    <div>
                        <p className="text-sm font-black uppercase tracking-[0.16em] text-[#071936]">Dashboard</p>
                        <p className="text-xs font-semibold text-[#5c6d83]">{role}</p>
                    </div>
                </div>

                <nav className="flex-1 space-y-2 overflow-y-auto p-4">
                    <Link to="/dashboard" className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass('/dashboard')}`}>
                        Tổng quan
                    </Link>
                    <Link to="/dashboard/leaderboard" className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass('/dashboard/leaderboard')}`}>
                        Bảng xếp hạng
                    </Link>

                    {(role === 'COORDINATOR' || role === 'ADMIN') && (
                        <div className="pt-3">
                            <p className="mb-2 px-4 text-xs font-black uppercase tracking-[0.18em] text-[#5c6d83]">Coordinator</p>
                            {coordinatorItems.map((item) => (
                                <Link key={item.to} to={item.to} className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass(item.to)}`}>
                                    {item.label}
                                </Link>
                            ))}
                        </div>
                    )}

                    {(role === 'LEADER' || role === 'MEMBER') && (
                        <div className="pt-3">
                            <p className="mb-2 px-4 text-xs font-black uppercase tracking-[0.18em] text-[#5c6d83]">Đội thi</p>
                            {teamItems.map((item) => (
                                <Link key={item.to} to={item.to} className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass(item.to)}`}>
                                    {item.label}
                                </Link>
                            ))}
                        </div>
                    )}

                    {role === 'JUDGE' && (
                        <div className="pt-3">
                            <p className="mb-2 px-4 text-xs font-black uppercase tracking-[0.18em] text-[#5c6d83]">Judge</p>
                            <Link to="/dashboard/grading" className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass('/dashboard/grading')}`}>
                                Chấm bài
                            </Link>
                            <Link to="/dashboard/notifications" className={`block rounded-lg px-4 py-3 text-sm font-bold ${navClass('/dashboard/notifications')}`}>
                                Thông báo
                            </Link>
                        </div>
                    )}
                </nav>

                <div className="border-t border-[#d7e6f8] p-4">
                    <Link to="/dashboard/profile" className={`mb-3 block rounded-lg px-4 py-3 text-sm font-bold ${navClass('/dashboard/profile')}`}>
                        Profile
                    </Link>
                    <button type="button" onClick={logout} className="btn-secondary w-full">Đăng xuất</button>
                </div>
            </aside>

            <div className="flex min-w-0 flex-1 flex-col">
                <header className="flex h-20 items-center justify-between border-b border-[#d7e6f8] bg-white px-8">
                    <div>
                        <p className="text-xs font-black uppercase tracking-[0.18em] text-[#0f63c9]">SEAL Hackathon</p>
                        <h1 className="mt-1 text-xl font-black capitalize text-[#071936]">
                            {location.pathname.split('/').pop()?.replace('-', ' ') || 'Tổng quan'}
                        </h1>
                    </div>
                    <p className="text-sm font-semibold text-[#5c6d83]">{email}</p>
                </header>

                <main className="flex-1 overflow-y-auto p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
