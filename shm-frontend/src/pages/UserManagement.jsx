import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function UserManagement() {
    const [users, setUsers] = useState([]);
    const [staffForm, setStaffForm] = useState({ fullName: '', email: '', password: '1', role: 'JUDGE' });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const fetchUsers = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/users');
            setUsers(response.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải danh sách người dùng.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleUpdateStatus = async (userId, status) => {
        try {
            await axiosClient.put(`/users/${userId}/status`, { status });
            await fetchUsers();
        } catch (err) {
            setError(err.message || 'Không thể cập nhật trạng thái.');
        }
    };

    const handleCreateStaff = async (e) => {
        e.preventDefault();
        try {
            setSaving(true);
            await axiosClient.post('/users/staff', staffForm);
            setStaffForm({ fullName: '', email: '', password: '1', role: 'JUDGE' });
            await fetchUsers();
        } catch (err) {
            setError(err.message || 'Không thể tạo tài khoản staff.');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="mx-auto max-w-7xl space-y-6">
            {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <h2 className="text-2xl font-black uppercase tracking-wide text-slate-900">Tạo tài khoản staff</h2>
                <p className="mt-2 text-sm text-slate-600">Coordinator tạo Mentor/Judge bằng tài khoản thật, sau đó phân công vào matrix.</p>

                <form onSubmit={handleCreateStaff} className="mt-6 grid gap-4 md:grid-cols-[1fr_1fr_160px_140px_auto]">
                    <input required className="input-custom" value={staffForm.fullName} onChange={(e) => setStaffForm({ ...staffForm, fullName: e.target.value })} placeholder="Họ tên" />
                    <input required type="email" className="input-custom" value={staffForm.email} onChange={(e) => setStaffForm({ ...staffForm, email: e.target.value })} placeholder="email@fpt.edu.vn" />
                    <input required className="input-custom" value={staffForm.password} onChange={(e) => setStaffForm({ ...staffForm, password: e.target.value })} placeholder="Mật khẩu" />
                    <select className="input-custom" value={staffForm.role} onChange={(e) => setStaffForm({ ...staffForm, role: e.target.value })}>
                        <option value="JUDGE">Judge</option>
                        <option value="MENTOR">Mentor</option>
                        <option value="COORDINATOR">Coordinator</option>
                    </select>
                    <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Đang tạo...' : 'Tạo'}</button>
                </form>
            </section>

            <section className="rounded-lg border border-blue-100 bg-white shadow-sm">
                <div className="flex items-center justify-between border-b border-blue-100 bg-blue-50 px-6 py-4">
                    <div>
                        <h2 className="text-xl font-black uppercase tracking-wide text-slate-900">Quản lý tài khoản</h2>
                        <p className="mt-1 text-sm text-slate-600">Duyệt, từ chối hoặc theo dõi role người dùng.</p>
                    </div>
                    <button type="button" onClick={fetchUsers} className="btn-secondary">Làm mới</button>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead className="border-b border-blue-100 bg-white text-xs font-black uppercase tracking-wide text-slate-500">
                        <tr>
                            <th className="px-6 py-4">Người dùng</th>
                            <th className="px-6 py-4">Email</th>
                            <th className="px-6 py-4">Role</th>
                            <th className="px-6 py-4">Trạng thái</th>
                            <th className="px-6 py-4 text-right">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-blue-50">
                        {loading ? (
                            <tr><td colSpan="5" className="px-6 py-8 text-center text-slate-500">Đang tải...</td></tr>
                        ) : users.length === 0 ? (
                            <tr><td colSpan="5" className="px-6 py-8 text-center text-slate-500">Chưa có người dùng.</td></tr>
                        ) : users.map((user) => (
                            <tr key={user.id} className="hover:bg-blue-50/40">
                                <td className="px-6 py-4">
                                    <p className="font-bold text-slate-900">{user.fullName}</p>
                                    <p className="text-xs text-slate-500">{user.studentId || user.universityName || 'Staff'}</p>
                                </td>
                                <td className="px-6 py-4 text-sm text-slate-600">{user.email}</td>
                                <td className="px-6 py-4"><span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-black text-blue-700">{user.role}</span></td>
                                <td className="px-6 py-4"><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-black text-slate-700">{user.status}</span></td>
                                <td className="px-6 py-4 text-right">
                                    {user.status === 'PENDING' && (
                                        <div className="flex justify-end gap-2">
                                            <button type="button" onClick={() => handleUpdateStatus(user.id, 'APPROVED')} className="btn-primary">Duyệt</button>
                                            <button type="button" onClick={() => handleUpdateStatus(user.id, 'REJECTED')} className="btn-secondary">Từ chối</button>
                                        </div>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
