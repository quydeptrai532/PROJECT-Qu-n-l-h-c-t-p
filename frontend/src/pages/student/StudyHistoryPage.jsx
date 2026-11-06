import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Award,
  BookOpen,
  Clock,
  GraduationCap,
  RotateCcw,
  ScrollText,
  TrendingUp,
} from "lucide-react";
import ExportTranscriptPdfButton from "../../components/ExportTranscriptPdfButton";
import { Alert, Badge, EmptyState, PageHeader, Spinner, StatCard } from "../../components/ui";
import { formatDiem } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const STATUS_OPTIONS = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "DAT", label: "Đạt" },
  { value: "CHUA_DAT", label: "Chưa đạt" },
  { value: "CHUA_CO_DIEM", label: "Chưa có điểm" },
];

function trangThaiBadge(trangThaiKetQua, tenTrangThaiKetQua) {
  if (trangThaiKetQua === "DAT") {
    return <Badge tone="green">{tenTrangThaiKetQua || "Đạt"}</Badge>;
  }
  if (trangThaiKetQua === "CHUA_DAT") {
    return <Badge tone="red">{tenTrangThaiKetQua || "Chưa đạt"}</Badge>;
  }
  return <Badge tone="slate">{tenTrangThaiKetQua || "Chưa có điểm"}</Badge>;
}

export default function StudyHistoryPage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");
  const [trangThai, setTrangThai] = useState("");
  const [exportSuccess, setExportSuccess] = useState("");
  const [exportError, setExportError] = useState("");

  const queryParams = useMemo(() => {
    const params = {};
    if (hocKyId) params.hocKyId = hocKyId;
    if (trangThai) params.trangThai = trangThai;
    return params;
  }, [hocKyId, trangThai]);

  const { data, loading, error, reload } = useApi("/sinh-vien/me/lich-su-hoc-tap", queryParams);

  const groups = useMemo(() => {
    if (!data?.items?.length) return [];
    const map = new Map();
    data.items.forEach((item) => {
      if (!map.has(item.hocKyId)) {
        map.set(item.hocKyId, {
          hocKyId: item.hocKyId,
          tenHocKy: item.tenHocKy,
          namHoc: item.namHoc,
          items: [],
        });
      }
      map.get(item.hocKyId).items.push(item);
    });
    return [...map.values()];
  }, [data]);

  const tongKetTheoKy = useMemo(() => {
    const map = {};
    (data?.hocKy || []).forEach((item) => {
      map[item.hocKyId] = item;
    });
    return map;
  }, [data]);

  const monChuaDat = data?.monChuaDat || [];
  const dangLoc = Boolean(hocKyId || trangThai);

  return (
    <div>
      <PageHeader
        icon={ScrollText}
        title="Lịch sử học tập & môn chưa đạt"
        description="Tra cứu kết quả học tập theo học kỳ, môn chưa đạt và các lần học của tài khoản hiện tại."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <select
              aria-label="Lọc theo học kỳ"
              className="input w-auto min-w-[210px]"
              value={hocKyId}
              onChange={(event) => setHocKyId(event.target.value)}
            >
              <option value="">Tất cả học kỳ</option>
              {(hocKyList || []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} - {item.namHoc}
                </option>
              ))}
            </select>
            <select
              aria-label="Lọc theo trạng thái"
              className="input w-auto min-w-[180px]"
              value={trangThai}
              onChange={(event) => setTrangThai(event.target.value)}
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            {dangLoc && (
              <button
                type="button"
                className="btn-secondary px-3 py-2 text-xs"
                onClick={() => {
                  setHocKyId("");
                  setTrangThai("");
                }}
              >
                <RotateCcw size={14} />
                Đặt lại bộ lọc
              </button>
            )}
            <ExportTranscriptPdfButton
              hocKyId={hocKyId || undefined}
              disabled={loading}
              onSuccess={setExportSuccess}
              onError={setExportError}
            />
          </div>
        }
      />

      {exportSuccess && (
        <div className="mb-4">
          <Alert tone="success">{exportSuccess}</Alert>
        </div>
      )}
      {exportError && (
        <div className="mb-4">
          <Alert tone="error">{exportError}</Alert>
        </div>
      )}

      <div className="mb-4">
        <Alert tone="warning">
          Chỉ hiển thị điểm đã công bố (có điểm tổng kết); học phần chưa công bố điểm được giữ trạng thái &ldquo;Chưa có điểm&rdquo;. Các lần học lại, học cải thiện được lưu thành từng bản ghi riêng; quy chế thay điểm khi tính GPA chính thức cho môn học nhiều lần chưa được cấu hình.
        </Alert>
      </div>

      {loading ? (
        <Spinner label="Đang tải lịch sử học tập..." />
      ) : error ? (
        <div className="space-y-3">
          <Alert tone="error">{error}</Alert>
          <button type="button" className="btn-secondary" onClick={reload}>
            Thử tải lại
          </button>
        </div>
      ) : (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="GPA hệ 10"
              value={data?.gpa10 !== undefined ? data.gpa10.toFixed(2) : "0.00"}
              hint={`GPA hệ 4: ${data?.gpa4 !== undefined ? data.gpa4.toFixed(2) : "0.00"}`}
              icon={TrendingUp}
            />
            <StatCard
              label="Tín chỉ tích lũy"
              value={data?.tinChiTichLuy || 0}
              hint={`Đã đăng ký: ${data?.tongTinChi || 0} · Có điểm: ${data?.tinChiCoDiem || 0}`}
              icon={Award}
              tone="emerald"
            />
            <StatCard
              label="Đạt / Chưa đạt"
              value={`${data?.soMonDat || 0} / ${data?.soMonChuaDat || 0}`}
              hint="Ngưỡng đạt hiện hành: từ 5.0"
              icon={GraduationCap}
              tone="amber"
            />
            <StatCard
              label="Chưa có điểm"
              value={data?.soMonChuaCoDiem || 0}
              hint="Đang học hoặc chờ công bố điểm"
              icon={Clock}
              tone="sky"
            />
          </div>

          <section className="card mb-6 p-5" aria-labelledby="failed-courses-heading">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 id="failed-courses-heading" className="text-sm font-semibold text-slate-800">
                  Môn chưa đạt
                </h2>
                <p className="mt-0.5 text-xs text-slate-500">
                  Các lần học đã có điểm tổng kết dưới ngưỡng đạt 5.0 theo bộ lọc hiện tại
                </p>
              </div>
              <Badge tone={monChuaDat.length > 0 ? "red" : "green"}>
                {monChuaDat.length} bản ghi chưa đạt
              </Badge>
            </div>

            {monChuaDat.length === 0 ? (
              <p className="mt-4 rounded-xl bg-slate-50 px-4 py-4 text-sm text-slate-600">
                Không có môn chưa đạt trong phạm vi đang chọn.
              </p>
            ) : (
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                {monChuaDat.map((item) => (
                  <article
                    key={item.dangKyId}
                    className="rounded-xl border border-rose-200/80 bg-rose-50/50 p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <p className="font-semibold text-slate-900">
                          {item.maMon} · {item.tenMon}
                        </p>
                        <p className="mt-0.5 text-xs text-slate-600">
                          {item.tenHocKy} - {item.namHoc} · Lớp {item.maLop} · {item.soTinChi} tín chỉ
                        </p>
                      </div>
                      <Badge tone="red">Tổng kết: {formatDiem(item.diemTongKet)}</Badge>
                    </div>
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-600">
                      <span>
                        Lần học thứ {item.lanHoc}
                        {item.tongSoLanHoc > 1 ? ` / ${item.tongSoLanHoc} lần học` : ""} · QT:{" "}
                        {formatDiem(item.diemQuaTrinh)} · CK: {formatDiem(item.diemCuoiKy)}
                      </span>
                      <Link
                        to="/dang-ky-hoc-lai"
                        className="font-semibold text-indigo-600 hover:text-indigo-700"
                      >
                        Đăng ký học lại →
                      </Link>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          {!groups.length ? (
            <div className="card">
              <EmptyState
                icon={dangLoc ? AlertTriangle : BookOpen}
                title={dangLoc ? "Không tìm thấy kết quả phù hợp bộ lọc" : "Chưa có lịch sử học tập"}
                description={
                  dangLoc
                    ? "Hãy thử chọn học kỳ hoặc trạng thái kết quả khác."
                    : "Lịch sử học tập sẽ hiển thị khi bạn có học phần đã đăng ký."
                }
              />
            </div>
          ) : (
            <div className="space-y-6">
              {groups.map((group) => {
                const tongKet = tongKetTheoKy[group.hocKyId];
                return (
                  <section key={group.hocKyId} className="card overflow-hidden">
                    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
                      <div>
                        <h2 className="text-sm font-semibold text-slate-800">
                          {group.tenHocKy} - {group.namHoc}
                        </h2>
                        <p className="text-xs text-slate-500">{group.items.length} bản ghi học phần</p>
                      </div>
                      {tongKet && (
                        <div className="flex flex-wrap items-center gap-2 text-xs">
                          <Badge tone="indigo">{tongKet.tongTinChi} tín chỉ</Badge>
                          {tongKet.tinChiCoDiem > 0 ? (
                            <>
                              <Badge tone="sky">GPA 10: {tongKet.gpa10.toFixed(2)}</Badge>
                              <Badge tone="green">GPA 4: {tongKet.gpa4.toFixed(2)}</Badge>
                            </>
                          ) : (
                            <Badge tone="slate">Chưa có điểm</Badge>
                          )}
                        </div>
                      )}
                    </div>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[880px]">
                        <thead className="bg-white">
                          <tr>
                            <th className="th">Môn học</th>
                            <th className="th">Lớp học phần</th>
                            <th className="th text-center">Tín chỉ</th>
                            <th className="th text-center">Lần học</th>
                            <th className="th text-center">Quá trình</th>
                            <th className="th text-center">Cuối kỳ</th>
                            <th className="th text-center">Tổng kết</th>
                            <th className="th">Trạng thái</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {group.items.map((item) => (
                            <tr key={item.dangKyId} className="hover:bg-slate-50/60">
                              <td className="td">
                                <p className="font-medium text-slate-800">{item.tenMon}</p>
                                <p className="text-xs text-slate-500">{item.maMon}</p>
                              </td>
                              <td className="td">
                                <p className="font-medium text-slate-700">{item.maLop}</p>
                                {item.tenGiangVien && (
                                  <p className="text-xs text-slate-500">{item.tenGiangVien}</p>
                                )}
                              </td>
                              <td className="td text-center">{item.soTinChi}</td>
                              <td className="td text-center">
                                <div className="inline-flex flex-col items-center gap-1">
                                  <span className="font-medium text-slate-700">Lần {item.lanHoc}</span>
                                  {item.tongSoLanHoc > 1 && (
                                    <Badge tone="amber" dot={false}>
                                      {item.lanHoc}/{item.tongSoLanHoc} lần
                                    </Badge>
                                  )}
                                </div>
                              </td>
                              <td className="td text-center">{formatDiem(item.diemQuaTrinh)}</td>
                              <td className="td text-center">{formatDiem(item.diemCuoiKy)}</td>
                              <td className="td text-center font-semibold text-slate-900">
                                {formatDiem(item.diemTongKet)}
                              </td>
                              <td className="td">
                                {trangThaiBadge(item.trangThaiKetQua, item.tenTrangThaiKetQua)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
