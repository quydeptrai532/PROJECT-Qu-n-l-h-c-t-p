import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Calculator, ClipboardList, Save, Users } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDiem, lichHoc } from "../../lib/format";
import { useApi } from "../../lib/useApi";

function tinhTongKet(quaTrinh, cuoiKy) {
  const qt = Number(quaTrinh);
  const ck = Number(cuoiKy);
  if (quaTrinh === "" || cuoiKy === "" || Number.isNaN(qt) || Number.isNaN(ck)) return null;
  return Math.round((0.4 * qt + 0.6 * ck) * 10) / 10;
}

export default function GradeEntryPage() {
  const { id } = useParams();
  const { data: lop, loading: loadingLop, error: lopError } = useApi(`/lop-hoc-phan/${id}`);
  const {
    data: sinhVienList,
    loading: loadingSv,
    error: svError,
  } = useApi(`/giang-vien/lop-hoc-phan/${id}/sinh-vien`);

  const [rows, setRows] = useState([]);
  const [edits, setEdits] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    setRows(sinhVienList || []);
  }, [sinhVienList]);

  const soDaNhap = useMemo(
    () => rows.filter((row) => row.diemTongKet !== null && row.diemTongKet !== undefined).length,
    [rows]
  );

  const onChange = (dangKyId, field, value) => {
    setEdits((current) => ({
      ...current,
      [dangKyId]: {
        ...(current[dangKyId] || {
          diemQuaTrinh: rows.find((row) => row.dangKyId === dangKyId)?.diemQuaTrinh ?? "",
          diemCuoiKy: rows.find((row) => row.dangKyId === dangKyId)?.diemCuoiKy ?? "",
        }),
        [field]: value,
      },
    }));
  };

  const getValue = (row, field) => {
    if (edits[row.dangKyId]?.[field] !== undefined) {
      return edits[row.dangKyId][field];
    }
    const value = row[field];
    return value === null || value === undefined ? "" : value;
  };

  const luuDiem = async (row) => {
    const quaTrinh = getValue(row, "diemQuaTrinh");
    const cuoiKy = getValue(row, "diemCuoiKy");
    if (quaTrinh === "" || cuoiKy === "") {
      setError(`Vui lòng nhập đủ điểm quá trình và cuối kỳ cho ${row.maSinhVien}`);
      return;
    }
    setBusyId(row.dangKyId);
    setError("");
    setMessage("");
    try {
      const { data } = await api.put(`/diem/${row.dangKyId}`, {
        diemQuaTrinh: Number(quaTrinh),
        diemCuoiKy: Number(cuoiKy),
      });
      setRows((current) =>
        current.map((item) => (item.dangKyId === row.dangKyId ? { ...item, ...data } : item))
      );
      setEdits((current) => {
        const next = { ...current };
        delete next[row.dangKyId];
        return next;
      });
      setMessage(`Đã lưu điểm cho ${row.maSinhVien} - ${row.hoTen}`);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <div className="mb-4">
        <Link to="/lop-cua-toi" className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800">
          <ArrowLeft size={16} /> Quay lại danh sách lớp
        </Link>
      </div>

      <PageHeader
        icon={ClipboardList}
        title={loadingLop ? "Nhập điểm" : `${lop?.tenMon} - ${lop?.maLop}`}
        description={
          lop
            ? `${lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · Phòng ${lop.phong} · ${lop.tenHocKy}`
            : "Nhập điểm quá trình và điểm cuối kỳ cho sinh viên trong lớp."
        }
        actions={
          lop && (
            <div className="flex items-center gap-2 text-sm text-slate-600">
              <Users size={16} className="text-slate-400" />
              {lop.siSoHienTai}/{lop.siSoToiDa} sinh viên
            </div>
          )
        }
      />

      {(lopError || svError) && <Alert tone="error" className="mb-4">{lopError || svError}</Alert>}
      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm font-medium text-slate-700">Danh sách sinh viên</p>
          <p className="text-xs text-slate-500">
            Đã nhập điểm: {soDaNhap}/{rows.length} · Điểm tổng kết = 40% quá trình + 60% cuối kỳ
          </p>
        </div>

        {loadingSv ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState
            icon={Users}
            title="Lớp chưa có sinh viên đăng ký"
            description="Sinh viên cần đăng ký lớp trước khi giảng viên nhập điểm."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[860px]">
              <thead className="bg-white">
                <tr>
                  <th className="th w-12 text-center">STT</th>
                  <th className="th">Mã SV</th>
                  <th className="th">Họ tên</th>
                  <th className="th">Email</th>
                  <th className="th w-32 text-center">Quá trình</th>
                  <th className="th w-32 text-center">Cuối kỳ</th>
                  <th className="th w-28 text-center">Tổng kết</th>
                  <th className="th w-28 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row, index) => {
                  const quaTrinh = getValue(row, "diemQuaTrinh");
                  const cuoiKy = getValue(row, "diemCuoiKy");
                  const tongKet = tinhTongKet(quaTrinh, cuoiKy);
                  const daThayDoi =
                    String(quaTrinh) !== String(row.diemQuaTrinh ?? "") ||
                    String(cuoiKy) !== String(row.diemCuoiKy ?? "");
                  return (
                    <tr key={row.dangKyId} className="hover:bg-slate-50/60">
                      <td className="td text-center text-slate-500">{index + 1}</td>
                      <td className="td font-medium text-slate-900">{row.maSinhVien}</td>
                      <td className="td">{row.hoTen}</td>
                      <td className="td text-slate-500">{row.email || "—"}</td>
                      <td className="td">
                        <input
                          className="input w-24 text-center"
                          type="number"
                          min="0"
                          max="10"
                          step="0.1"
                          value={quaTrinh}
                          onChange={(event) => onChange(row.dangKyId, "diemQuaTrinh", event.target.value)}
                        />
                      </td>
                      <td className="td">
                        <input
                          className="input w-24 text-center"
                          type="number"
                          min="0"
                          max="10"
                          step="0.1"
                          value={cuoiKy}
                          onChange={(event) => onChange(row.dangKyId, "diemCuoiKy", event.target.value)}
                        />
                      </td>
                      <td className="td text-center">
                        <span className="inline-flex items-center gap-1 font-semibold text-slate-900">
                          <Calculator size={14} className="text-slate-400" />
                          {tongKet !== null ? formatDiem(tongKet) : formatDiem(row.diemTongKet)}
                        </span>
                      </td>
                      <td className="td text-right">
                        <button
                          type="button"
                          className={daThayDoi ? "btn-primary px-3 py-1.5 text-xs" : "btn-secondary px-3 py-1.5 text-xs"}
                          disabled={busyId === row.dangKyId || !daThayDoi}
                          onClick={() => luuDiem(row)}
                        >
                          <Save size={14} />
                          {busyId === row.dangKyId ? "Đang lưu..." : "Lưu"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="mt-3 text-xs text-slate-500">
        Điểm hợp lệ từ 0 đến 10. Sinh viên đạt môn khi điểm tổng kết từ 5.0 trở lên.
      </p>
    </div>
  );
}
