import { useEffect, useMemo, useState } from "react";
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  MapPin,
  Search,
  Users,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDate, lichHoc, soBuoiHoc, tietLyThuyet } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function RegisterPage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data: lopList, loading, error: loadError, reload: reloadLop } = useApi("/lop-hoc-phan", params);
  const { data: myRegs, reload: reloadRegs } = useApi("/dang-ky/me", params);

  const hocKy = hocKyList?.find((item) => String(item.id) === String(hocKyId));
  const registeredClassIds = useMemo(
    () => new Set((myRegs || []).map((item) => item.lopHocPhanId)),
    [myRegs]
  );
  const registeredSubjectIds = useMemo(
    () => new Set((myRegs || []).map((item) => item.monHocId)),
    [myRegs]
  );

  const filtered = useMemo(() => {
    const list = lopList || [];
    const term = keyword.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (lop) =>
        lop.ma.toLowerCase().includes(term) ||
        lop.maMon.toLowerCase().includes(term) ||
        lop.tenMon.toLowerCase().includes(term) ||
        lop.tenGiangVien.toLowerCase().includes(term)
    );
  }, [lopList, keyword]);

  const dangKy = async (lop) => {
    setBusyId(lop.id);
    setError("");
    setMessage("");
    try {
      const { data } = await api.post("/dang-ky", { lopHocPhanId: lop.id });
      setMessage(`Đăng ký thành công lớp ${data.maLop} - ${data.tenMon}`);
      await Promise.all([reloadLop(), reloadRegs()]);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <PageHeader
        icon={BookOpen}
        title="Đăng ký học phần"
        description="Tìm lớp đang mở và đăng ký trong thời gian cho phép. Hệ thống tự kiểm tra điều kiện."
        actions={
          <select
            className="input w-auto min-w-[220px]"
            value={hocKyId}
            onChange={(event) => {
              setHocKyId(event.target.value);
              setMessage("");
              setError("");
            }}
          >
            {(hocKyList || []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten} - {item.namHoc}
              </option>
            ))}
          </select>
        }
      />

      {hocKy && (
        <div className="mb-5 flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-soft">
          <Badge tone={hocKy.dangMoDangKy ? "green" : "red"}>
            {hocKy.dangMoDangKy ? "Đang mở đăng ký" : "Đã đóng đăng ký"}
          </Badge>
          <span className="text-slate-600">
            Thời gian: <strong>{formatDate(hocKy.dangKyTu)}</strong> - <strong>{formatDate(hocKy.dangKyDen)}</strong>
          </span>
          <span className="text-slate-600">
            Tín chỉ: {hocKy.soTinChiToiThieu} - {hocKy.soTinChiToiDa}
          </span>
          <span className="ml-auto text-slate-500">
            Đã đăng ký {(myRegs || []).reduce((sum, item) => sum + item.soTinChi, 0)} tín chỉ
          </span>
        </div>
      )}

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
        {loadError && <Alert tone="error">{loadError}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-sm">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Tìm theo mã lớp, môn học, giảng viên..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>
          <p className="text-sm text-slate-500">{filtered.length} lớp học phần</p>
        </div>

        {loading ? (
          <Spinner />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="Không có lớp học phần phù hợp"
            description="Thử chọn học kỳ khác hoặc thay đổi từ khóa tìm kiếm."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[920px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Lớp</th>
                  <th className="th">Môn học</th>
                  <th className="th">Giảng viên</th>
                  <th className="th">Lịch học</th>
                  <th className="th">Sĩ số</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((lop) => {
                  const isRegistered = registeredClassIds.has(lop.id);
                  const sameSubject = registeredSubjectIds.has(lop.monHocId);
                  const full = lop.conLai <= 0;
                  const disabled = isRegistered || sameSubject || full || !hocKy?.dangMoDangKy;
                  const percent = lop.siSoToiDa ? Math.min(100, (lop.siSoHienTai / lop.siSoToiDa) * 100) : 0;

                  let label = "Đăng ký";
                  let tone = "indigo";
                  if (isRegistered) {
                    label = "Đã đăng ký";
                    tone = "green";
                  } else if (sameSubject) {
                    label = "Đã có môn";
                    tone = "amber";
                  } else if (full) {
                    label = "Hết chỗ";
                    tone = "red";
                  } else if (!hocKy?.dangMoDangKy) {
                    label = "Đã đóng";
                    tone = "slate";
                  }

                  return (
                    <tr key={lop.id} className="hover:bg-slate-50/60">
                      <td className="td font-medium text-slate-900">{lop.ma}</td>
                      <td className="td">
                        <p className="font-medium text-slate-800">{lop.tenMon}</p>
                        <p className="text-xs text-slate-500">
                          {lop.maMon} · {lop.soTinChi} tín chỉ · {tietLyThuyet(lop.soTinChi)} tiết ·{" "}
                          {soBuoiHoc(lop.soTinChi)} buổi
                        </p>
                      </td>
                      <td className="td">{lop.tenGiangVien}</td>
                      <td className="td">
                        <p>{lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)}</p>
                        <p className="mt-0.5 flex items-center gap-1 text-xs text-slate-500">
                          <MapPin size={12} /> {lop.phong}
                        </p>
                      </td>
                      <td className="td">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-20 overflow-hidden rounded-full bg-slate-200">
                            <div
                              className={`h-full rounded-full transition-all duration-300 ${
                                full
                                  ? "bg-gradient-to-r from-rose-500 to-pink-500"
                                  : "bg-gradient-to-r from-emerald-400 to-teal-500"
                              }`}
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                          <span className="text-xs text-slate-600">
                            {lop.siSoHienTai}/{lop.siSoToiDa}
                          </span>
                        </div>
                        <p className="mt-1 flex items-center gap-1 text-xs text-slate-500">
                          <Users size={12} /> còn {lop.conLai} chỗ
                        </p>
                      </td>
                      <td className="td text-right">
                        {disabled ? (
                          <Badge tone={tone}>{label}</Badge>
                        ) : (
                          <button
                            type="button"
                            className="btn-primary px-3 py-1.5 text-xs"
                            disabled={busyId === lop.id}
                            onClick={() => dangKy(lop)}
                          >
                            <CheckCircle2 size={14} />
                            {busyId === lop.id ? "Đang xử lý..." : "Đăng ký"}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="mt-3 flex items-center gap-2 text-xs text-slate-500">
        <CalendarDays size={14} />
        Ghi chú: không thể đăng ký hai lớp cùng môn, trùng lịch, vượt sĩ số hoặc khi chưa đạt môn tiên quyết.
      </p>
    </div>
  );
}
