import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function Profile() {
    const [profile, setProfile] = useState(null);
    const [avatarUrl, setAvatarUrl] = useState('');
    const [passwords, setPasswords] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
    const [loading, setLoading] = useState(true);
    const [savingProfile, setSavingProfile] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    const fetchProfile = async () => {
        const response = await axiosClient.get('/users/me');
        setProfile(response.result);
        setAvatarUrl(response.result?.avatarUrl || '');
    };

    useEffect(() => {
        fetchProfile()
            .catch((err) => setMessage({ text: err.message || 'Không thể tải profile.', type: 'error' }))
            .finally(() => setLoading(false));
    }, []);

    const handleAvatarSubmit = async (e) => {
        e.preventDefault();
        try {
            setSavingProfile(true);
            const response = await axiosClient.put('/users/me/profile', { avatarUrl });
            setProfile(response.result);
            setMessage({ text: 'Cập nhật avatar thành công.', type: 'success' });
        } catch (err) {
            setMessage({ text: err.message || 'Không thể cập nhật profile.', type: 'error' });
        } finally {
            setSavingProfile(false);
        }
    };

    const handlePasswordSubmit = async (e) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });

        if (passwords.newPassword !== passwords.confirmPassword) {
            setMessage({ text: 'Mật khẩu mới không khớp.', type: 'error' });
            return;
        }

        try {
            setSavingPassword(true);
            await axiosClient.put('/users/change-password', {
                oldPassword: passwords.oldPassword,
                newPassword: passwords.newPassword,
            });
            setPasswords({ oldPassword: '', newPassword: '', confirmPassword: '' });
            setMessage({ text: 'Đổi mật khẩu thành công.', type: 'success' });
        } catch (err) {
            setMessage({ text: err.message || 'Không thể đổi mật khẩu.', type: 'error' });
        } finally {
            setSavingPassword(false);
        }
    };

    if (loading) {
        return <div className="rounded-lg bg-white p-8 text-center text-gray-500">Đang tải profile...</div>;
    }

    return (
        <div className="mx-auto max-w-4xl space-y-6">
            {message.text && (
                <div className={`rounded-lg border p-4 text-sm font-semibold ${
                    message.type === 'success'
                        ? 'border-green-200 bg-green-50 text-green-700'
                        : 'border-red-200 bg-red-50 text-red-700'
                }`}>
                    {message.text}
                </div>
            )}

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
                    <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-blue-100 text-3xl font-black text-blue-700">
                        {profile?.avatarUrl ? (
                            <img src={profile.avatarUrl} alt={profile.fullName} className="h-full w-full object-cover" />
                        ) : (
                            profile?.fullName?.charAt(0)?.toUpperCase() || 'U'
                        )}
                    </div>
                    <div>
                        <p className="text-xs font-black uppercase tracking-[0.18em] text-blue-600">{profile?.role}</p>
                        <h2 className="mt-2 text-2xl font-black uppercase tracking-wide text-slate-900">{profile?.fullName}</h2>
                        <p className="mt-1 text-sm text-slate-600">{profile?.email}</p>
                        <p className="mt-1 text-sm text-slate-600">
                            {profile?.studentId || 'Chưa có MSSV'} - {profile?.universityName || 'Chưa có trường'}
                        </p>
                        <span className="mt-3 inline-flex rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-black text-blue-700">
                            {profile?.status}
                        </span>
                    </div>
                </div>
            </section>

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <h3 className="text-lg font-black uppercase tracking-wide text-slate-900">Cập nhật avatar</h3>
                <form onSubmit={handleAvatarSubmit} className="mt-5 space-y-4">
                    <input
                        type="url"
                        className="input-custom"
                        value={avatarUrl}
                        onChange={(e) => setAvatarUrl(e.target.value)}
                        placeholder="https://..."
                    />
                    <button type="submit" disabled={savingProfile} className="btn-primary">
                        {savingProfile ? 'Đang lưu...' : 'Lưu avatar'}
                    </button>
                </form>
            </section>

            <section className="rounded-lg border border-blue-100 bg-white p-8 shadow-sm">
                <h3 className="text-lg font-black uppercase tracking-wide text-slate-900">Đổi mật khẩu</h3>
                <form onSubmit={handlePasswordSubmit} className="mt-5 space-y-4">
                    <input
                        required
                        type="password"
                        className="input-custom"
                        placeholder="Mật khẩu hiện tại"
                        value={passwords.oldPassword}
                        onChange={(e) => setPasswords({ ...passwords, oldPassword: e.target.value })}
                    />
                    <input
                        required
                        minLength={6}
                        type="password"
                        className="input-custom"
                        placeholder="Mật khẩu mới"
                        value={passwords.newPassword}
                        onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
                    />
                    <input
                        required
                        minLength={6}
                        type="password"
                        className="input-custom"
                        placeholder="Xác nhận mật khẩu mới"
                        value={passwords.confirmPassword}
                        onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })}
                    />
                    <button type="submit" disabled={savingPassword} className="btn-primary">
                        {savingPassword ? 'Đang đổi...' : 'Đổi mật khẩu'}
                    </button>
                </form>
            </section>
        </div>
    );
}
