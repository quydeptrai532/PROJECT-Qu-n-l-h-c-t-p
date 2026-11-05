import { useEffect, useMemo, useState } from "react";
import { BookOpenCheck, CheckCircle2, GraduationCap } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";

export default function RetakeRegistrationPage() {
  const { data: semesters } = useApi("/hoc-ky");
  const [semesterId, setSemesterId] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    if (!semesterId && semesters?.length) setSemesterId(String(semesters[0].id));
  }, [semesters, semesterId]);

  useEffect(() => {
    if (!semesterId) return;
    setLoading(true);
    setError("");
    api.get("/sinh-vien/me/dang-ky-hoc-lai-cai-thien", { params: { hocKyId: semesterId } })
      .then(({ data }) => setItems(data))
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [semesterId]);

  const semester = useMemo(() => semesters?.find((item) => String(item.id) === semesterId), [semesters, semesterId]);

  const register = async (item) => {
    setBusyId(item.lopHocPhanId);
    setError("");
    setMessage("");
    try {
      await api.post("/dang-ky", { lopHocPhanId: item.lopHocPhanId, loaiDangKy: item.loaiDangKy });
      setMessage(`Đã đăng ký lớp ${item.maLop} · ${item.tenMon}. Điểm các lần học cũ vẫn được lưu riêng.`);
      setItems((current) => current.filter((candidate) => candidate.monHocId !== item.monHocId));
      window.dispatchEvent(new Event("notifications:updated"));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <PageHeader icon={BookOpenCheck} title="Đăng ký học lại / học cải thiện"
        description="Danh sách được xác định từ kết quả học tập của tài khoản hiện tại. Máy chủ kiểm tra lại điều kiện khi đăng ký."
        actions={<select className="input w-auto min-w-[220px]" value={semesterId} onChange={(event) => setSemesterId(event.target.value)}>
          {(semesters || []).map((item) => <option key={item.id} value={item.id}>{item.ten} - {item.namHoc}</option>)}
        </select>} />

      <Alert tone="warning">
        Quy chế thay điểm và tính GPA/tín chỉ cho lần học lại, học cải thiện chưa được cấu hình. Điểm cũ được giữ nguyên; kết quả GPA hiện hành chưa phải kết quả chính thức cho các môn có nhiều lần học.
      </Alert>
      {semester && <div className="my-4"><Badge tone={semester.dangMoDangKy ? "green" : "red"}>{semester.dangMoDangKy ? "Đang mở đăng ký" : "Đã đóng đăng ký"}</Badge></div>}
      {message && <div className="my-4"><Alert tone="success">{message}</Alert></div>}
      {error && <div className="my-4"><Alert tone="error">{error}</Alert></div>}

      {loading ? <Spinner /> : items.length === 0 ? (
        <div className="card mt-4"><EmptyState icon={GraduationCap} title="Không có môn đủ điều kiện trong học kỳ này"
          description="Chỉ những môn đã có điểm tổng kết mới xuất hiện. Hãy chọn học kỳ khác hoặc xem lại khi lớp được mở." /></div>
      ) : <div className="mt-4 space-y-4">
        {items.map((item) => {
          const full = item.siSoHienTai >= item.siSoToiDa;
          const disabled = !item.dangMoDangKy || full || busyId === item.lopHocPhanId;
          return <section className="card p-5" key={item.lopHocPhanId}>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div><div className="flex flex-wrap items-center gap-2"><h2 className="font-semibold">{item.maMon} · {item.tenMon}</h2>
                <Badge tone={item.loaiDangKy === "HOC_LAI" ? "red" : "amber"}>{item.loaiDangKy === "HOC_LAI" ? "Học lại" : "Học cải thiện"}</Badge></div>
                <p className="mt-1 text-sm text-slate-600">Lớp {item.maLop} · {item.soTinChi} tín chỉ · {item.tenGiangVien} · Thứ {item.thu}, tiết {item.tietBatDau}-{item.tietBatDau + item.soTiet - 1}</p>
                <p className="mt-1 text-xs text-slate-500">Sĩ số {item.siSoHienTai}/{item.siSoToiDa}{full ? " · Lớp đã đủ sĩ số" : ""}</p>
              </div>
              <button type="button" className="btn-primary px-3 py-2 text-sm" disabled={disabled} onClick={() => register(item)}>
                <CheckCircle2 size={15} />{busyId === item.lopHocPhanId ? "Đang xử lý..." : !item.dangMoDangKy ? "Đã đóng" : full ? "Hết chỗ" : "Đăng ký"}
              </button>
            </div>
            <div className="mt-4 border-t border-slate-100 pt-3"><h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Các lần học trước</h3>
              <div className="mt-2 flex flex-wrap gap-2">{item.cacLanHocTruoc.map((attempt) => <Badge key={attempt.dangKyId} tone={attempt.diemTongKet >= 5 ? "green" : "red"}>
                {attempt.tenHocKy} · {attempt.maLop} · {attempt.diemTongKet.toFixed(1)} điểm
              </Badge>)}</div>
            </div>
          </section>;
        })}
      </div>}
    </div>
  );
}
