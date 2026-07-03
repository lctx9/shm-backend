import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function TeamChat() {
    const [team, setTeam] = useState(null);
    const [messages, setMessages] = useState([]);
    const [content, setContent] = useState('');
    const [error, setError] = useState('');

    const fetchMessages = async (teamId) => {
        const response = await axiosClient.get(`/chat/teams/${teamId}`);
        setMessages(response.result || []);
    };

    useEffect(() => {
        const bootstrap = async () => {
            try {
                const teamRes = await axiosClient.get('/teams/my-team');
                setTeam(teamRes.result);
                if (teamRes.result?.id) await fetchMessages(teamRes.result.id);
            } catch (err) {
                setError(err.message || 'Không thể tải chat.');
            }
        };
        bootstrap();
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!team?.id || !content.trim()) return;
        try {
            await axiosClient.post(`/chat/teams/${team.id}`, { teamId: team.id, content });
            setContent('');
            await fetchMessages(team.id);
        } catch (err) {
            setError(err.message || 'Không thể gửi tin nhắn.');
        }
    };

    if (!team) {
        return <div className="rounded-lg border border-amber-200 bg-amber-50 p-6 text-amber-900">Bạn cần có đội trước khi chat.</div>;
    }

    return (
        <div className="mx-auto max-w-4xl rounded-lg border border-blue-100 bg-white shadow-sm">
            <div className="border-b border-blue-100 bg-blue-50 px-6 py-4">
                <h2 className="text-xl font-black uppercase tracking-wide text-slate-900">Chat đội: {team.name}</h2>
                <p className="mt-1 text-sm text-slate-600">Tin nhắn được lưu vào bảng chat_messages.</p>
            </div>
            {error && <div className="m-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}
            <div className="max-h-[520px] space-y-4 overflow-y-auto p-6">
                {messages.length === 0 ? (
                    <p className="text-center text-sm text-slate-500">Chưa có tin nhắn.</p>
                ) : messages.map((message) => (
                    <div key={message.id} className="rounded-lg border border-blue-100 bg-[#f8fbff] p-4">
                        <div className="mb-2 flex items-center justify-between gap-4">
                            <p className="font-bold text-slate-900">{message.senderName || message.senderEmail}</p>
                            <p className="text-xs text-slate-400">{message.createdAt ? new Date(message.createdAt).toLocaleString('vi-VN') : ''}</p>
                        </div>
                        <p className="text-sm leading-6 text-slate-700">{message.content}</p>
                    </div>
                ))}
            </div>
            <form onSubmit={handleSubmit} className="flex gap-3 border-t border-blue-100 p-4">
                <input className="input-custom" value={content} onChange={(e) => setContent(e.target.value)} placeholder="Nhập tin nhắn cho team/mentor..." />
                <button type="submit" className="btn-primary">Gửi</button>
            </form>
        </div>
    );
}
