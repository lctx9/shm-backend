import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function Notifications() {
    const role = localStorage.getItem('role');
    const [notifications, setNotifications] = useState([]);
    const [form, setForm] = useState({ title: '', body: '', targetRole: 'MEMBER' });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const canSend = role === 'COORDINATOR' || role === 'ADMIN';

    const fetchNotifications = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/notifications');
            setNotifications(response.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải thông báo.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await axiosClient.post('/notifications', form);
            setForm({ title: '', body: '', targetRole: 'MEMBER' });
            await fetchNotifications();
        } catch (err) {
            setError(err.message || 'Không thể gửi thông báo.');
        }
    };

    return (
        <div className="mx-auto max-w-5xl space-y-6">
            {canSend && (
                <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                    <h2 className="text-xl font-black uppercase tracking-wide text-slate-900">Gửi thông báo</h2>
                    <form onSubmit={handleSubmit} className="mt-5 space-y-4">
                        <div className="grid gap-4 md:grid-cols-[1fr_180px]">
                            <input required className="input-custom" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Tiêu đề" />
                            <select className="input-custom" value={form.targetRole} onChange={(e) => setForm({ ...form, targetRole: e.target.value })}>
                                <option value="MEMBER">Member</option>
                                <option value="LEADER">Leader</option>
                                <option value="MENTOR">Mentor</option>
                                <option value="JUDGE">Judge</option>
                            </select>
                        </div>
                        <textarea required rows="4" className="input-custom" value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} placeholder="Nội dung thông báo" />
                        <button type="submit" className="btn-primary">Gửi thông báo</button>
                    </form>
                </section>
            )}

            <section className="rounded-lg border border-blue-100 bg-white shadow-sm">
                <div className="flex items-center justify-between border-b border-blue-100 bg-blue-50 px-6 py-4">
                    <h2 className="text-xl font-black uppercase tracking-wide text-slate-900">Thông báo của tôi</h2>
                    <button type="button" onClick={fetchNotifications} className="btn-secondary">Làm mới</button>
                </div>
                {error && <div className="m-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}
                <div className="divide-y divide-blue-50">
                    {loading ? (
                        <div className="p-8 text-center text-slate-500">Đang tải...</div>
                    ) : notifications.length === 0 ? (
                        <div className="p-8 text-center text-slate-500">Chưa có thông báo.</div>
                    ) : notifications.map((item) => (
                        <article key={item.id} className="px-6 py-5">
                            <p className="text-xs font-black uppercase tracking-[0.16em] text-blue-600">{item.targetRole || 'Cá nhân'}</p>
                            <h3 className="mt-2 font-black text-slate-900">{item.title}</h3>
                            <p className="mt-2 text-sm leading-6 text-slate-600">{item.body}</p>
                            <p className="mt-3 text-xs text-slate-400">{item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : ''}</p>
                        </article>
                    ))}
                </div>
            </section>
        </div>
    );
}
