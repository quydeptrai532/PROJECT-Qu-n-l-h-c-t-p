import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ClipboardList, Layers, MapPin, Users } from "lucide-react";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { lichHoc } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function LecturerClassesPage() {
  const { data, loading, error } = useApi("/giang-vien/me/lop-hoc-phan");
  const [hocKyId, setHocKyId] = useState("");

  const hocKyList = useMemo(() => {
    const map = new Map();
    (data || []).forEach((item) => {
      if (!map.has(item.hocKyId)) map.set(item.hocKyId, item.tenHocKy);
    });
    return [...map.entries()].map(([id, ten]) => ({ id, ten }));
  }, [data]);

  const danhSach = useMemo(() => {
    if (!hocKyId) return data || [];
    return (data || []).filter((item) => String(item.hocKyId) === String(hocKyId));
  }, [data, hocKyId]);

  return (
    <div>
      <PageHeader
        icon={Layers}
        title="Lớp học phần phụ trách"
        description="Các lớp được phân công và danh sách sinh viên để nhập điểm."
        actions={
          <select className="input w-auto min-w-[200px]" value={hocKyId} onChange={(e) => setHocKyId(e.target.value)}>
            <option value="">Tất cả học kỳ</option>
            {hocKyList.map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten}
              </option>
            ))}
          </select>
        }
      />

      {error && <Alert tone="error" className="mb-4">{error}</Alert>}

      {loading ? (
        <Spinner />
      ) : danhSach.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={Layers}
            title="Chưa được phân công lớp học phần"
            description="Liên hệ phòng đào tạo nếu cần bổ sung lịch giảng dạy."
          />
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {danhSach.map((lop) => (
            <div key={lop.id} className="card flex flex-col p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-indigo-600">{lop.ma}</p>
                  <h2 className="mt-1 font-semibold text-slate-900">{lop.tenMon}</h2>
                  <p className="text-xs text-slate-500">
                    {lop.maMon} · {lop.soTinChi} tín chỉ
                  </p>
                </div>
                <Badge tone="sky">{lop.tenHocKy}</Badge>
              </div>

              <dl className="mt-4 space-y-2 text-sm text-slate-600">
                <div className="flex items-center gap-2">
                  <ClipboardList size={15} className="text-slate-400" />
                  {lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)}
                </div>
                <div className="flex items-center gap-2">
                  <MapPin size={15} className="text-slate-400" />
                  Phòng {lop.phong}
                </div>
                <div className="flex items-center gap-2">
                  <Users size={15} className="text-slate-400" />
                  {lop.siSoHienTai}/{lop.siSoToiDa} sinh viên
                </div>
              </dl>

              <Link
                to={`/lop-cua-toi/${lop.id}/nhap-diem`}
                className="btn-primary mt-5 w-full py-2 text-sm"
              >
                Nhập điểm & xem danh sách
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
