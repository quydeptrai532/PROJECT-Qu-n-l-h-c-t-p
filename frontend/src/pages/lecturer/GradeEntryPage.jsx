import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Calculator, CheckCircle2, ClipboardList, Megaphone, RotateCcw, Save, Send, Users } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import ExportStudentsButton from "../../components/ExportStudentsButton";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDiem, lichHoc } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const GRADE_SHEET_STATUS_META = {
    NHAP: { label: "Bản nháp (Chưa công bố)", tone: "slate" },
    CHO_DUYET: { label: "Chờ phê duyệt", tone: "amber" },
    DA_DUYET: { label: "Đã phê duyệt (Sẵn sàng công bố)", tone: "blue" },
    DA_CONG_BO: { label: "Đã công bố", tone: "green" },
};

function tinhTongKet(quaTrinh, cuoiKy) {
    const qt = Number(quaTrinh);
    const ck = Number(cuoiKy);
    if (quaTrinh === "" || cuoiKy === "" || Number.isNaN(qt) || Number.isNaN(ck)) return null;
    return Math.round((0.4 * qt + 0.6 * ck) * 10) / 10;
}

export default function GradeEntryPage() {
    const { id } = useParams();
    const { data: lop, loading: loadingLop, error: lopError, reload: reloadLop } = useApi(`/lop-hoc-phan/${id}`);
    const {
        data: sinhVienList,
        loading: loadingSv,
        error: svError,
    } = useApi(`/giang-vien/lop-hoc-phan/${id}/sinh-vien`);

    const [rows, setRows] = useState([]);
    const [edits, setEdits] = useState({});
    const [busyId, setBusyId] = useState(null);
    const [workflowBusy, setWorkflowBusy] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    useEffect(() => {
        setRows(sinhVienList || []);
    }, [sinhVienList]);

    const soDaNhap = useMemo(
        () => rows.filter((row) => row.diemTongKet !== null && row.diemTongKet !== undefined).length,
        [rows]
    );

    const trangThaiBangDiem = lop?.trangThaiBangDiem || "NHAP";
    const statusMeta = GRADE_SHEET_STATUS_META[trangThaiBangDiem] || GRADE_SHEET_STATUS_META.NHAP;
    const daCongBo = trangThaiBangDiem === "DA_CONG_BO";
    const duDieuKienDuyet = rows.length > 0 && soDaNhap === rows.length && Object.keys(edits).length === 0;

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
            setMessage(`Đã lưu điểm nháp cho ${row.maSinhVien} - ${row.hoTen}`);
            reloadLop();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setBusyId(null);
        }
    };

    const thucHienQuyTrinh = async (endpoint, successMessage) => {
        setWorkflowBusy(true);
        setError("");
        setMessage("");
        try {
            await api.post(`/giang-vien/lop-hoc-phan/${id}/${endpoint}`);
            await reloadLop();
            setMessage(successMessage);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setWorkflowBusy(false);
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
                title={loadingLop ? "Nhập điểm" : `${lop?.tenMon} - ${lop?.ma}`}
                description={
                    lop
                        ? `${lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · Phòng ${lop.phong} · ${lop.tenHocKy}`
                        : "Nhập điểm quá trình và điểm cuối kỳ cho sinh viên trong lớp."
                }
                actions={
                    lop && (
                        <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
                            <Badge tone={statusMeta.tone}>{statusMeta.label}</Badge>
                            <span className="inline-flex items-center gap-2">
                                <Users size={16} className="text-slate-400" />
                                {lop.siSoHienTai}/{lop.siSoToiDa} sinh viên
                            </span>
                            <ExportStudentsButton
                                lopId={id}
                                maLop={lop.ma}
                                soSinhVien={rows.length}
                                loading={loadingSv}
                                onSuccess={(text) => {
                                    setError("");
                                    setMessage(text);
                                }}
                                onError={(text) => {
                                    setMessage("");
                                    setError(text);
                                }}
                            />
                        </div>
                    )
                }
            />

            {(lopError || svError) && <Alert tone="error" className="mb-4">{lopError || svError}</Alert>}
            <div className="mb-4 space-y-3">
                {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
                {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
            </div>

            {lop && (
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
                    <div className="text-xs text-slate-600">
                        <span className="font-semibold text-slate-800">Quy trình công bố điểm:</span>{" "}
                        Nhập điểm nháp → Gửi duyệt → Phê duyệt → Công bố chính thức (hệ thống chỉ gửi thông báo nội bộ cho sinh viên khi bảng điểm chuyển sang trạng thái Đã công bố).
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                        {trangThaiBangDiem === "NHAP" && (
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                disabled={workflowBusy || !duDieuKienDuyet}
                                onClick={() => thucHienQuyTrinh("gui-duyet-diem", "Đã gửi duyệt bảng điểm. Chưa phát thông báo tới sinh viên.")}
                            >
                                <Send size={14} /> Gửi duyệt
                            </button>
                        )}
                        {(trangThaiBangDiem === "NHAP" || trangThaiBangDiem === "CHO_DUYET") && (
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                disabled={workflowBusy || !duDieuKienDuyet}
                                onClick={() => thucHienQuyTrinh("phe-duyet-diem", "Đã phê duyệt bảng điểm. Bấm Công bố điểm để phát thông báo cho sinh viên.")}
                            >
                                <CheckCircle2 size={14} /> Phê duyệt bảng điểm
                            </button>
                        )}
                        {trangThaiBangDiem === "DA_DUYET" && (
                            <button
                                type="button"
                                className="btn-primary text-xs"
                                disabled={workflowBusy || !duDieuKienDuyet}
                                onClick={() => thucHienQuyTrinh("cong-bo-diem", "Đã công bố bảng điểm và phát thông báo nội bộ cho sinh viên đủ điều kiện của lớp.")}
                            >
                                <Megaphone size={14} /> Công bố điểm
                            </button>
                        )}
                        {daCongBo && (
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                disabled={workflowBusy}
                                onClick={() => thucHienQuyTrinh("thu-hoi-cong-bo-diem", "Đã thu hồi công bố bảng điểm để điều chỉnh.")}
                            >
                                <RotateCcw size={14} /> Thu hồi công bố
                            </button>
                        )}
                    </div>
                </div>
            )}

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
                                                disabled={daCongBo}
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
                                                disabled={daCongBo}
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
                                                disabled={daCongBo || busyId === row.dangKyId || !daThayDoi}
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
                Điểm hợp lệ từ 0 đến 10. Sinh viên đạt môn khi điểm tổng kết từ 5.0 trở lên. Điểm nháp/chờ duyệt chưa hiển thị cho sinh viên cho tới khi bảng điểm được phê duyệt và công bố.
            </p>
        </div>
    );
}
