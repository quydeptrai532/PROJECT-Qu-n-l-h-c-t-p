import { useEffect, useState } from "react";
import { BookOpenCheck, CalendarPlus, RefreshCw, Save, Trash2 } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function ProgressPlannerPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [progress, setProgress] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [plans, setPlans] = useState([]);
  const [selected, setSelected] = useState("");
  const [name, setName] = useState("Kế hoạch học tập");
  const [ids, setIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const reload = async () => {
    setLoading(true); setError("");
    try {
      const [p, s, k] = await Promise.all([api.get("/sinh-vien/me/tien-do"), api.get("/sinh-vien/me/goi-y-ke-hoach"), api.get("/sinh-vien/me/ke-hoach")]);
      setProgress(p.data); setSuggestions(s.data); setPlans(k.data);
    } catch (e) { setError(getErrorMessage(e)); } finally { setLoading(false); }
  };
  useEffect(() => { reload(); }, []);
  useEffect(() => { const p = plans.find((x) => String(x.id) === selected); if (p) { setName(p.name); setIds(p.items.map((i) => i.classId)); } else setIds([]); }, [selected, plans]);
  const toggle = (id) => setIds((old) => old.includes(id) ? old.filter((x) => x !== id) : [...old, id]);
  const save = async () => { setSaving(true); setError(""); try { const { data } = selected ? await api.put(`/sinh-vien/me/ke-hoach/${selected}`, { name, classIds: ids }) : await api.post("/sinh-vien/me/ke-hoach", { name, classIds: ids }); await reload(); setSelected(String(data.id)); } catch (e) { setError(getErrorMessage(e)); } finally { setSaving(false); } };
  const remove = async () => { if (!selected) return; try { await api.delete(`/sinh-vien/me/ke-hoach/${selected}`); setSelected(""); await reload(); } catch (e) { setError(getErrorMessage(e)); } };
  const sendToCoursePlanner = () => { const plan=plans.find((p)=>String(p.id)===selected); if(!plan||!user?.username)return; const byTerm=new Map(); plan.items.forEach((item)=>{const key=`course-plan:${encodeURIComponent(user.username)}:${item.semesterId}`;byTerm.set(key,[...(byTerm.get(key)||[]),item.classId]);});try{for(const [key,classIds] of byTerm)localStorage.setItem(key,JSON.stringify([...new Set(classIds)]));const first=plan.items[0];if(first)navigate(`/ke-hoach-hoc-tap?hocKyId=${first.semesterId}`);}catch{setError("Không chuyển được kế hoạch sang giỏ Course Planner trên thiết bị này.");} };
  if (loading) return <Spinner label="Đang tải tiến độ học tập…" />;
  return <div className="space-y-6">
    <PageHeader title="Tiến độ & kế hoạch cá nhân" description="Tiến độ tính từ chương trình được gán, kết quả và đăng ký học phần hiện có." action={<button onClick={reload} className="btn-secondary"><RefreshCw size={16}/> Làm mới</button>} />
    {error && <Alert variant="error">{error}</Alert>}
    {progress && !progress.configured && <Alert>{progress.message} Nhờ giáo vụ cấu hình chương trình theo ngành và khóa tuyển sinh.</Alert>}
    {progress?.configured && <>
      <section className="card p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-sm text-slate-500">{progress.program.major} · Khóa {progress.program.cohort} · Phiên bản {progress.program.version}</p><h2 className="mt-1 text-xl font-semibold">{progress.earnedCredits}/{progress.requiredCredits} tín chỉ yêu cầu</h2></div><Badge>{progress.completionPercent ?? "—"}% hoàn thành</Badge></div><div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${progress.completionPercent ?? 0}%` }}/></div></section>
      {progress.groups.map((group) => <section key={group.name} className="card p-5"><div className="mb-4 flex justify-between"><h2 className="font-semibold">{group.name}</h2><span className="text-sm text-slate-500">{group.earnedCredits}/{group.requiredCredits} tín chỉ · {group.type === "BAT_BUOC" ? "Bắt buộc" : "Tự chọn"}</span></div><div className="divide-y">{group.courses.map((course) => <div key={course.id} className="flex flex-wrap items-center justify-between gap-2 py-3"><div><p className="font-medium">{course.code} · {course.name}</p><p className="text-xs text-slate-500">{course.reason}</p></div><Badge>{({ PASSED: "Đã đạt", IN_PROGRESS: "Đang học", RETAKE: "Cần học lại", NOT_STUDIED: "Chưa học" })[course.status]} · {course.credits} TC</Badge></div>)}</div></section>)}
    </>}
    <section className="card p-5"><div className="mb-4 flex items-center gap-2"><CalendarPlus size={18}/><h2 className="font-semibold">Kế hoạch dự kiến</h2></div><p className="mb-4 text-sm text-slate-500">Gợi ý chỉ gồm lớp đang mở đăng ký, còn chỗ và đủ tiên quyết. Lưu kế hoạch không tạo đăng ký chính thức.</p>
      <div className="mb-4 flex flex-wrap gap-2"><select className="input max-w-xs" value={selected} onChange={(e)=>setSelected(e.target.value)}><option value="">Kế hoạch mới…</option>{plans.map((p)=><option key={p.id} value={p.id}>{p.name}</option>)}</select><input className="input max-w-xs" value={name} onChange={(e)=>setName(e.target.value)} aria-label="Tên kế hoạch"/><button className="btn-primary" disabled={saving||!progress?.configured} onClick={save}><Save size={16}/>{saving?"Đang lưu…":"Lưu kế hoạch"}</button>{selected&&<><button className="btn-secondary" onClick={remove}><Trash2 size={16}/> Xóa</button><button className="btn-secondary" onClick={sendToCoursePlanner}>Mở trong Course Planner</button></>}</div>
      {!progress?.configured ? <EmptyState icon={BookOpenCheck} title="Chưa có chương trình đào tạo" description="Cần gán chương trình trước khi tạo gợi ý cá nhân."/> : suggestions.length===0 ? <EmptyState icon={BookOpenCheck} title="Chưa có lớp phù hợp đang mở" description="Gợi ý sẽ cập nhật khi tiến độ hoặc lớp mở đăng ký thay đổi."/> : <div className="divide-y">{suggestions.map((s)=><label key={s.classId} className="flex cursor-pointer items-start gap-3 py-3"><input type="checkbox" checked={ids.includes(s.classId)} onChange={()=>toggle(s.classId)} className="mt-1"/><span className="flex-1"><strong>{s.subjectCode} · {s.subjectName}</strong><span className="block text-sm text-slate-500">{s.classCode} · {s.semester} · {s.credits} TC · còn {s.remainingSeats} chỗ · Thứ {s.day}, tiết {s.startPeriod}–{s.startPeriod+s.periods-1}</span><span className="block text-xs text-emerald-700">{s.reason}</span></span></label>)}</div>}
    </section>
  </div>;
}
