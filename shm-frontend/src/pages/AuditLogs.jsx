import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function AuditLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchLogs = async () => {
        try {
            setLoading(true);
            const response = await axiosClient.get('/audit-logs');
            setLogs(response.result || []);
            setError('');
        } catch (err) {
            setError(err.message || 'Không thể tải audit log.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs();
    }, []);

    return (
        <div className="mx-auto max-w-6xl rounded-lg border border-blue-100 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-blue-100 bg-blue-50 px-6 py-4">
                <div>
                    <h2 className="text-xl font-black uppercase tracking-wide text-slate-900">Audit log điểm số</h2>
                    <p className="mt-1 text-sm text-slate-600">Truy vết mọi lần judge sửa điểm đã chấm.</p>
                </div>
                <button type="button" onClick={fetchLogs} className="btn-secondary">Làm mới</button>
            </div>

            {error && <div className="m-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead className="border-b border-blue-100 bg-white text-xs font-black uppercase tracking-wide text-slate-500">
                    <tr>
                        <th className="px-6 py-4">Thời gian</th>
                        <th className="px-6 py-4">Judge</th>
                        <th className="px-6 py-4">Đội</th>
                        <th className="px-6 py-4">Điểm cũ</th>
                        <th className="px-6 py-4">Điểm mới</th>
                        <th className="px-6 py-4">Lý do</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-blue-50">
                    {loading ? (
                        <tr><td colSpan="6" className="px-6 py-8 text-center text-slate-500">Đang tải...</td></tr>
                    ) : logs.length === 0 ? (
                        <tr><td colSpan="6" className="px-6 py-8 text-center text-slate-500">Chưa có lần sửa điểm nào.</td></tr>
                    ) : logs.map((log) => (
                        <tr key={log.id} className="hover:bg-blue-50/40">
                            <td className="px-6 py-4 text-sm text-slate-600">{log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : ''}</td>
                            <td className="px-6 py-4">
                                <p className="font-bold text-slate-900">{log.judgeName}</p>
                                <p className="text-xs text-slate-500">{log.judgeEmail}</p>
                            </td>
                            <td className="px-6 py-4 font-bold text-slate-800">{log.teamName || 'Không rõ đội'}</td>
                            <td className="px-6 py-4 text-amber-700">{log.oldScore}</td>
                            <td className="px-6 py-4 text-green-700">{log.newScore}</td>
                            <td className="px-6 py-4 text-sm text-slate-600">{log.reason}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
