import React, { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function EventManagement() {
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    // --- State danh sách từ Database ---
    const [staffList, setStaffList] = useState([]);

    // --- State Form Dữ Liệu ---
    const [formData, setFormData] = useState({
        name: '',
        season: 'SPRING',
        year: 2026,
        regStartDate: '',
        regEndDate: '',
        eventStartDate: '',
        eventEndDate: '',
        maxTeams: 30,
        totalRounds: 2,
        totalTracks: 2,

        selectedJudges: [],
        selectedMentors: [],

        // Cấu hình Rubrics
        rubrics: [
            { roundNumber: 1, criteria: 'Ý tưởng sáng tạo', weight: 40 },
            { roundNumber: 1, criteria: 'Tính khả thi & Kỹ thuật', weight: 60 }
        ],

        // Phân công Giám khảo phụ trách theo Vòng (Đã loại bỏ trường Đội thi)
        judgeAssignments: [
            { roundNumber: 1, judgeId: '' }
        ]
    });

    // --- Lấy dữ liệu danh sách Staff từ API ---
    useEffect(() => {
        const fetchData = async () => {
            try {
                // const staffRes = await axiosClient.get('/users/staffs');
                // setStaffList(staffRes.data);

                setStaffList([
                    { id: 'st01', fullName: 'Nguyễn Văn A (Staff)', email: 'anv@seal.com' },
                    { id: 'st02', fullName: 'Trần Thị B (Staff)', email: 'btt@seal.com' },
                    { id: 'st03', fullName: 'Lê Hoàng C (Staff)', email: 'cleh@seal.com' },
                    { id: 'st04', fullName: 'Phạm Minh D (Staff)', email: 'dpm@seal.com' },
                ]);
            } catch (err) {
                console.error("Lỗi tải danh sách cấu hình:", err);
            }
        };
        fetchData();
    }, []);

    // Xử lý Checkbox chọn Judge/Mentor
    const handleCheckboxChange = (id, type) => {
        const listName = type === 'judge' ? 'selectedJudges' : 'selectedMentors';
        setFormData(prev => {
            const currentList = prev[listName];
            const updatedList = currentList.includes(id)
                ? currentList.filter(item => item !== id)
                : [...currentList, id];
            return { ...prev, [listName]: updatedList };
        });
    };

    const addRubricRow = () => {
        setFormData({
            ...formData,
            rubrics: [...formData.rubrics, { roundNumber: 1, criteria: '', weight: 0 }]
        });
    };

    // Quản lý hàng phân công Judge theo vòng mới
    const addAssignmentRow = () => {
        setFormData({
            ...formData,
            judgeAssignments: [...formData.judgeAssignments, { roundNumber: 1, judgeId: '' }]
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ text: '', type: '' });

        // Kiểm tra tổng trọng số rubric mỗi vòng phải bằng 100%
        const weightsByRound = {};
        formData.rubrics.forEach(r => {
            weightsByRound[r.roundNumber] = (weightsByRound[r.roundNumber] || 0) + Number(r.weight);
        });

        const invalidRound = Object.keys(weightsByRound).find(round => weightsByRound[round] !== 100);
        if (invalidRound) {
            setMessage({ text: `Tổng trọng số tiêu chí (Rubric) của Vòng ${invalidRound} phải bằng 100%! Hiện tại là ${weightsByRound[invalidRound]}%`, type: 'error' });
            setLoading(false);
            return;
        }

        try {
            await axiosClient.post('/events', formData);
            setMessage({ text: 'Khởi tạo và phân công giám khảo hoàn tất thành công!', type: 'success' });
        } catch (err) {
            setMessage({ text: err.message || 'Có lỗi xảy ra khi tạo giải đấu!', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-[1000px] mx-auto bg-white rounded-[32px] shadow-xl border border-slate-100 p-6 sm:p-10 md:p-12 antialiased transition-all duration-500 ease-out transform opacity-0 translate-y-4 animate-[fadeInUp_0.6s_ease-out_forwards]">

            {/* Header */}
            <div className="mb-8 border-b border-slate-100 pb-6">
                <h2 className="text-3xl font-bold text-[#1E293B] tracking-tight mb-2">Cấu hình & Phân công Giải đấu</h2>
                <p className="text-[13px] text-slate-600">Thiết lập quy mô, tiêu chí chấm điểm (Rubric) và phân tách ban giám khảo phụ trách theo từng vòng.</p>
            </div>

            {/* Alert Message */}
            {message.text && (
                <div className={`mb-6 p-4 text-xs font-semibold rounded-lg border flex items-center gap-2 transition-all duration-300 transform translate-y-0 opacity-0 animate-[slideDown_0.25s_ease-out_forwards] ${
                    message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'
                }`}>
                    <span>{message.text}</span>
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-8">

                {/* PHẦN 1: THÔNG TIN CHUNG */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                    <div className="md:col-span-2">
                        <label className="block text-xs font-bold text-slate-800 mb-1.5">Tên Giải Đấu *</label>
                        <input
                            type="text" required placeholder="VD: SEAL Hackathon Toàn Quốc 2026..."
                            className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-[#1E5BB8] transition-all duration-200 text-sm font-medium"
                            value={formData.name}
                            onChange={(e) => setFormData({...formData, name: e.target.value})}
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-slate-800 mb-1.5">Mùa giải (Season) *</label>
                        <select
                            className="w-full px-4 py-2.5 rounded-lg border border-slate-300 text-slate-800 bg-white focus:outline-none focus:border-[#1E5BB8] transition-all duration-200 text-sm font-medium"
                            value={formData.season}
                            onChange={(e) => setFormData({...formData, season: e.target.value})}
                        >
                            <option value="SPRING">Spring (Mùa Xuân)</option>
                            <option value="SUMMER">Summer (Mùa Hè)</option>
                            <option value="FALL">Fall (Mùa Thu)</option>
                        </select>
                    </div>
                </div>

                {/* THÔNG SỐ SỐ LƯỢNG */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-5 bg-slate-50 p-5 rounded-2xl border border-slate-200/60">
                    <div>
                        <label className="block text-xs font-bold text-slate-800 mb-1.5">Số đội tối đa *</label>
                        <input type="number" required min="1" className="w-full px-4 py-2 rounded-lg border border-slate-300 text-slate-800 text-sm" value={formData.maxTeams} onChange={(e) => setFormData({...formData, maxTeams: e.target.value})} />
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-slate-800 mb-1.5">Số Vòng thi *</label>
                        <input type="number" required min="1" className="w-full px-4 py-2 rounded-lg border border-slate-300 text-slate-800 text-sm" value={formData.totalRounds} onChange={(e) => setFormData({...formData, totalRounds: e.target.value})} />
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-slate-800 mb-1.5">Số Bảng đấu *</label>
                        <input type="number" required min="1" className="w-full px-4 py-2 rounded-lg border border-slate-300 text-slate-800 text-sm" value={formData.totalTracks} onChange={(e) => setFormData({...formData, totalTracks: e.target.value})} />
                    </div>
                </div>

                {/* PHẦN 2: CHỌN NHÂN SỰ DẠNG KHUNG CUỘN */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Khung cuộn chọn Judge */}
                    <div className="border border-slate-200 rounded-2xl p-5 bg-white">
                        <label className="block text-xs font-bold text-slate-800 mb-1">Chọn Hội Đồng Giám Khảo (Judges)</label>
                        <p className="text-[11px] text-slate-400 mb-3">Tích chọn các Staff có sẵn để đưa vào hội đồng.</p>
                        <div className="h-40 overflow-y-auto border border-slate-200 rounded-lg p-3 space-y-2.5 bg-slate-50/50">
                            {staffList.map(staff => (
                                <label key={staff.id} className="flex items-center gap-3 cursor-pointer group select-none">
                                    <input
                                        type="checkbox"
                                        checked={formData.selectedJudges.includes(staff.id)}
                                        onChange={() => handleCheckboxChange(staff.id, 'judge')}
                                        className="h-4 w-4 rounded border-slate-300 text-[#1E5BB8] focus:ring-0"
                                    />
                                    <div className="text-xs">
                                        <p className="font-semibold text-slate-800 group-hover:text-[#1E5BB8] transition-colors">{staff.fullName}</p>
                                        <p className="text-slate-400 text-[11px]">{staff.email}</p>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Khung cuộn chọn Mentor */}
                    <div className="border border-slate-200 rounded-2xl p-5 bg-white">
                        <label className="block text-xs font-bold text-slate-800 mb-1">Chọn Cố Vấn Chuyên Môn (Mentors)</label>
                        <p className="text-[11px] text-slate-400 mb-3">Tích chọn các Staff có sẵn làm Mentor giải đấu.</p>
                        <div className="h-40 overflow-y-auto border border-slate-200 rounded-lg p-3 space-y-2.5 bg-slate-50/50">
                            {staffList.map(staff => (
                                <label key={staff.id} className="flex items-center gap-3 cursor-pointer group select-none">
                                    <input
                                        type="checkbox"
                                        checked={formData.selectedMentors.includes(staff.id)}
                                        onChange={() => handleCheckboxChange(staff.id, 'mentor')}
                                        className="h-4 w-4 rounded border-slate-300 text-[#1E5BB8] focus:ring-0"
                                    />
                                    <div className="text-xs">
                                        <p className="font-semibold text-slate-800 group-hover:text-[#1E5BB8] transition-colors">{staff.fullName}</p>
                                        <p className="text-slate-400 text-[11px]">{staff.email}</p>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>
                </div>

                {/* PHẦN 3: THIẾT LẬP TIÊU CHÍ CHẤM ĐIỂM (RUBRIC MULTI-ROUND) */}
                <div className="border border-slate-200 rounded-2xl p-5 bg-white space-y-4">
                    <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                        <div>
                            <h3 className="font-bold text-slate-800 text-sm">Cấu hình Tiêu chí chấm điểm (Rubric)</h3>
                            <p className="text-[11px] text-slate-400">Thiết lập tiêu chí và trọng số % cho mỗi vòng thi.</p>
                        </div>
                        <button type="button" onClick={addRubricRow} className="text-xs font-bold text-[#1E5BB8] hover:underline">+ Thêm tiêu chí</button>
                    </div>

                    <div className="space-y-3">
                        {formData.rubrics.map((rubric, idx) => (
                            <div key={idx} className="grid grid-cols-12 gap-3 items-center animate-[slideDown_0.2s_ease-out]">
                                <div className="col-span-3">
                                    <select
                                        className="w-full px-2 py-1.5 border border-slate-300 rounded-md text-xs font-medium"
                                        value={rubric.roundNumber}
                                        onChange={(e) => {
                                            const updated = [...formData.rubrics];
                                            updated[idx].roundNumber = Number(e.target.value);
                                            setFormData({...formData, rubrics: updated});
                                        }}
                                    >
                                        {[...Array(Number(formData.totalRounds || 1))].map((_, i) => (
                                            <option key={i+1} value={i+1}>Vòng {i+1}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-span-6">
                                    <input
                                        type="text" required placeholder="Tên tiêu chí (VD: Tính thực tiễn...)"
                                        className="w-full px-3 py-1.5 border border-slate-300 rounded-md text-xs font-medium"
                                        value={rubric.criteria}
                                        onChange={(e) => {
                                            const updated = [...formData.rubrics];
                                            updated[idx].criteria = e.target.value;
                                            setFormData({...formData, rubrics: updated});
                                        }}
                                    />
                                </div>
                                <div className="col-span-3 flex items-center gap-2">
                                    <input
                                        type="number" required min="1" max="100" placeholder="Trọng số"
                                        className="w-full px-3 py-1.5 border border-slate-300 rounded-md text-xs text-center font-medium"
                                        value={rubric.weight || ''}
                                        onChange={(e) => {
                                            const updated = [...formData.rubrics];
                                            updated[idx].weight = e.target.value;
                                            setFormData({...formData, rubrics: updated});
                                        }}
                                    />
                                    <span className="text-xs text-slate-500 font-bold">%</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* PHẦN 4: PHÂN CÔNG GIÁM KHẢO THEO VÒNG (Rút gọn chỉ chọn Vòng và Giám khảo) */}
                <div className="border border-slate-200 rounded-2xl p-5 bg-white space-y-4">
                    <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                        <div>
                            <h3 className="font-bold text-slate-800 text-sm">Phân công Hội đồng Giám khảo phụ trách Vòng thi</h3>
                            <p className="text-[11px] text-slate-400">Chỉ định cụ thể Giám khảo nào sẽ tham gia chấm thi cho từng Vòng tương ứng.</p>
                        </div>
                        <button type="button" onClick={addAssignmentRow} className="text-xs font-bold text-[#1E5BB8] hover:underline">+ Thêm phân công</button>
                    </div>

                    <div className="space-y-3">
                        {formData.judgeAssignments.map((assign, idx) => (
                            <div key={idx} className="grid grid-cols-12 gap-4 items-center animate-[slideDown_0.2s_ease-out]">
                                {/* Chọn Vòng thi */}
                                <div className="col-span-4">
                                    <label className="block text-[10px] font-bold text-slate-400 mb-1">VÒNG THI</label>
                                    <select
                                        className="w-full px-3 py-2 border border-slate-300 rounded-md text-xs font-medium focus:border-[#1E5BB8] focus:outline-none"
                                        value={assign.roundNumber}
                                        onChange={(e) => {
                                            const updated = [...formData.judgeAssignments];
                                            updated[idx].roundNumber = Number(e.target.value);
                                            setFormData({...formData, judgeAssignments: updated});
                                        }}
                                    >
                                        {[...Array(Number(formData.totalRounds || 1))].map((_, i) => (
                                            <option key={i+1} value={i+1}>Vòng {i+1}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Chọn Giám Khảo từ danh sách đã chọn ở trên */}
                                <div className="col-span-8">
                                    <label className="block text-[10px] font-bold text-slate-400 mb-1">GIÁM KHẢO PHỤ TRÁCH</label>
                                    <select
                                        required
                                        className="w-full px-3 py-2 border border-slate-300 rounded-md text-xs font-medium focus:border-[#1E5BB8] focus:outline-none"
                                        value={assign.judgeId}
                                        onChange={(e) => {
                                            const updated = [...formData.judgeAssignments];
                                            updated[idx].judgeId = e.target.value;
                                            setFormData({...formData, judgeAssignments: updated});
                                        }}
                                    >
                                        <option value="">-- Chọn Giám khảo trong hội đồng --</option>
                                        {staffList
                                            .filter(st => formData.selectedJudges.includes(st.id))
                                            .map(st => (
                                                <option key={st.id} value={st.id}>{st.fullName} ({st.email})</option>
                                            ))
                                        }
                                    </select>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* THỜI GIAN SỰ KIỆN */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                    <div className="p-4 rounded-xl bg-[#1E5BB8]/5 border border-[#1E5BB8]/10 space-y-3">
                        <h4 className="font-bold text-[#1E5BB8] text-xs uppercase tracking-wider">Thời gian Đăng ký</h4>
                        <div className="grid grid-cols-2 gap-2">
                            <input type="datetime-local" required className="w-full px-2 py-1.5 rounded border text-xs" value={formData.regStartDate} onChange={(e) => setFormData({...formData, regStartDate: e.target.value})} />
                            <input type="datetime-local" required className="w-full px-2 py-1.5 rounded border text-xs" value={formData.regEndDate} onChange={(e) => setFormData({...formData, regEndDate: e.target.value})} />
                        </div>
                    </div>
                    <div className="p-4 rounded-xl bg-slate-100/60 border border-slate-200 space-y-3">
                        <h4 className="font-bold text-slate-800 text-xs uppercase tracking-wider">Thời gian Thi đấu</h4>
                        <div className="grid grid-cols-2 gap-2">
                            <input type="datetime-local" required className="w-full px-2 py-1.5 rounded border text-xs" value={formData.eventStartDate} onChange={(e) => setFormData({...formData, eventStartDate: e.target.value})} />
                            <input type="datetime-local" required className="w-full px-2 py-1.5 rounded border text-xs" value={formData.eventEndDate} onChange={(e) => setFormData({...formData, eventEndDate: e.target.value})} />
                        </div>
                    </div>
                </div>

                {/* Button gửi */}
                <div className="pt-4 border-t border-slate-100 flex justify-end">
                    <button
                        type="submit" disabled={loading}
                        className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#1E5BB8] text-white px-8 py-2.5 text-sm font-semibold hover:bg-[#164384] active:scale-[0.99] disabled:opacity-60 transition-all duration-200 ease-in-out"
                    >
                        <span>{loading ? 'Đang khởi tạo...' : 'Tạo Giải Đấu Mới'}</span>
                    </button>
                </div>
            </form>
        </div>
    );
}