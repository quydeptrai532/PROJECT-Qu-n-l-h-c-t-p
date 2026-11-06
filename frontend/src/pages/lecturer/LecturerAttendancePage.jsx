import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarCheck,
  CalendarDays,
  CheckCircle,
  Clock,
  Edit2,
  Info,
  Lock,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Unlock,
  UserCheck,
  Users,
  XCircle,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner, StatCard } from "../../components/ui";
import { formatDate, formatDateTime, lichHoc } from "../../lib/format";
import { useAuth } from "../../context/AuthContext";

const ATTENDANCE_STATUSES = [
  { key: "CO_MAT", label: "Có mặt", color: "bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100", activeColor: "bg-emerald-600 text-white border-emerald-600" },
  { key: "VANG", label: "Vắng", color: "bg-rose-50 text-rose-700 border-rose-300 hover:bg-rose-100", activeColor: "bg-rose-600 text-white border-rose-600" },
  { key: "MUON", label: "Muộn", color: "bg-amber-50 text-amber-800 border-amber-300 hover:bg-amber-100", activeColor: "bg-amber-600 text-white border-amber-600" },
  { key: "CO_PHEP", label: "Có phép", color: "bg-sky-50 text-sky-700 border-sky-300 hover:bg-sky-100", activeColor: "bg-sky-600 text-white border-sky-600" },
];

export default function LecturerAttendancePage() {
  const { id } = useParams();
  const { user } = useAuth();
  const isAdmin = user?.role === "QUAN_TRI";

  const [activeTab, setActiveTab] = useState("diem-danh"); // "diem-danh" | "bao-cao"
  const [lop, setLop] = useState(null);
  const [buoiHocList, setBuoiHocList] = useState([]);
  const [selectedBuoiId, setSelectedBuoiId] = useState(null);
  const [chiTietDiemDanh, setChiTietDiemDanh] = useState(null);
  const [baoCaoLop, setBaoCaoLop] = useState(null);

  const [loading, setLoading] = useState(true);
  const [loadingSession, setLoadingSession] = useState(false);
  const [loadingReport, setLoadingReport] = useState(false);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [searchStudent, setSearchStudent] = useState("");

  // Attendance edits state: { [sinhVienId]: { trangThai, ghiChu } }
  const [attendanceEdits, setAttendanceEdits] = useState({});

  // Modal State
  const [sessionModalOpen, setSessionModalOpen] = useState(false);
  const [editingSession, setEditingSession] = useState(null);
  const [sessionForm, setSessionForm] = useState({
    ngayHoc: "",
    caHoc: 1,
    tietBatDau: 1,
    soTiet: 3,
    phongHoc: "",
    noiDung: "",
  });
  const [formError, setFormError] = useState("");

  // Load Class & Sessions list
  const loadClassAndSessions = useCallback(async () => {
    try {
      setError("");
      const [lopRes, buoiRes] = await Promise.all([
        api.get(`/lop-hoc-phan/${id}`),
        api.get(`/giang-vien/lop-hoc-phan/${id}/buoi-hoc`),
      ]);
      setLop(lopRes.data);
      const sessions = buoiRes.data || [];
      setBuoiHocList(sessions);

      // Auto-select session if none selected or current invalid
      if (sessions.length > 0) {
        setSelectedBuoiId((prev) => {
          const exists = sessions.some((s) => s.id === prev);
          return exists ? prev : sessions[sessions.length - 1].id;
        });
      } else {
        setSelectedBuoiId(null);
        setChiTietDiemDanh(null);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadClassAndSessions();
  }, [loadClassAndSessions]);

  // Load selected session's attendance detail
  const loadSessionDetail = useCallback(async (buoiId) => {
    if (!buoiId) {
      setChiTietDiemDanh(null);
      return;
    }
    setLoadingSession(true);
    try {
      const res = await api.get(`/giang-vien/buoi-hoc/${buoiId}/diem-danh`);
      setChiTietDiemDanh(res.data);
      // Initialize local edits with existing server values
      const initial = {};
      (res.data.danhSachDiemDanh || []).forEach((item) => {
        initial[item.sinhVienId] = {
          trangThai: item.trangThai || "",
          ghiChu: item.ghiChu || "",
        };
      });
      setAttendanceEdits(initial);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoadingSession(false);
    }
  }, []);

  useEffect(() => {
    if (selectedBuoiId) {
      loadSessionDetail(selectedBuoiId);
    }
  }, [selectedBuoiId, loadSessionDetail]);

  // Load Report
  const loadReport = useCallback(async () => {
    setLoadingReport(true);
    try {
      const res = await api.get(`/giang-vien/lop-hoc-phan/${id}/chuyen-can`);
      setBaoCaoLop(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoadingReport(false);
    }
  }, [id]);

  useEffect(() => {
    if (activeTab === "bao-cao") {
      loadReport();
    }
  }, [activeTab, loadReport]);

  const selectedSession = useMemo(() => {
    return buoiHocList.find((b) => b.id === selectedBuoiId) || chiTietDiemDanh?.buoiHoc;
  }, [buoiHocList, selectedBuoiId, chiTietDiemDanh]);

  const isSessionLocked = selectedSession?.daKhoa && !isAdmin;

  // Open Create/Edit Session Modal
  const openCreateSessionModal = () => {
    setEditingSession(null);
    setFormError("");
    setSessionForm({
      ngayHoc: new Date().toISOString().slice(0, 10),
      caHoc: 1,
      tietBatDau: lop?.tietBatDau || 1,
      soTiet: lop?.soTiet || 3,
      phongHoc: lop?.phong || "",
      noiDung: "",
    });
    setSessionModalOpen(true);
  };

  const openEditSessionModal = (session) => {
    setEditingSession(session);
    setFormError("");
    setSessionForm({
      ngayHoc: session.ngayHoc || "",
      caHoc: session.caHoc || 1,
      tietBatDau: session.tietBatDau || 1,
      soTiet: session.soTiet || 3,
      phongHoc: session.phongHoc || "",
      noiDung: session.noiDung || "",
    });
    setSessionModalOpen(true);
  };

  const handleSaveSessionModal = async (e) => {
    e.preventDefault();
    setFormError("");
    if (!sessionForm.ngayHoc) {
      setFormError("Vui lòng chọn ngày học");
      return;
    }
    if (!sessionForm.caHoc || Number(sessionForm.caHoc) < 1) {
      setFormError("Ca học phải từ 1 trở lên");
      return;
    }

    setSaving(true);
    try {
      if (editingSession) {
        await api.put(`/giang-vien/buoi-hoc/${editingSession.id}`, {
          ngayHoc: sessionForm.ngayHoc,
          caHoc: Number(sessionForm.caHoc),
          tietBatDau: Number(sessionForm.tietBatDau),
          soTiet: Number(sessionForm.soTiet),
          phongHoc: sessionForm.phongHoc,
          noiDung: sessionForm.noiDung,
        });
        setSuccess("Cập nhật thông tin buổi học thành công!");
      } else {
        const res = await api.post(`/giang-vien/lop-hoc-phan/${id}/buoi-hoc`, {
          lopHocPhanId: Number(id),
          ngayHoc: sessionForm.ngayHoc,
          caHoc: Number(sessionForm.caHoc),
          tietBatDau: Number(sessionForm.tietBatDau),
          soTiet: Number(sessionForm.soTiet),
          phongHoc: sessionForm.phongHoc,
          noiDung: sessionForm.noiDung,
        });
        setSelectedBuoiId(res.data.id);
        setSuccess("Tạo buổi học mới thành công!");
      }
      setSessionModalOpen(false);
      await loadClassAndSessions();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // Delete session
  const handleDeleteSession = async (buoiId) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa buổi học này và toàn bộ dữ liệu điểm danh kèm theo?")) {
      return;
    }
    setSaving(true);
    try {
      await api.delete(`/giang-vien/buoi-hoc/${buoiId}`);
      setSuccess("Đã xóa buổi học thành công.");
      await loadClassAndSessions();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // Lock / Unlock session
  const handleToggleLock = async (buoiId, currentLocked) => {
    const actionName = currentLocked ? "mở khóa" : "khóa";
    if (!window.confirm(`Bạn có chắc chắn muốn ${actionName} buổi học này?`)) {
      return;
    }
    setSaving(true);
    try {
      await api.post(`/giang-vien/buoi-hoc/${buoiId}/khoa`, { khoa: !currentLocked });
      setSuccess(`Đã ${actionName} buổi học thành công.`);
      await loadClassAndSessions();
      if (selectedBuoiId === buoiId) {
        await loadSessionDetail(buoiId);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // Mark all present
  const handleMarkAllPresent = () => {
    if (isSessionLocked) return;
    const next = { ...attendanceEdits };
    (chiTietDiemDanh?.danhSachDiemDanh || []).forEach((item) => {
      next[item.sinhVienId] = {
        ...next[item.sinhVienId],
        trangThai: "CO_MAT",
      };
    });
    setAttendanceEdits(next);
  };

  // Change individual status
  const handleStatusChange = (sinhVienId, statusKey) => {
    if (isSessionLocked) return;
    setAttendanceEdits((prev) => ({
      ...prev,
      [sinhVienId]: {
        ...prev[sinhVienId],
        trangThai: statusKey,
      },
    }));
  };

  // Change individual note
  const handleNoteChange = (sinhVienId, note) => {
    if (isSessionLocked) return;
    setAttendanceEdits((prev) => ({
      ...prev,
      [sinhVienId]: {
        ...prev[sinhVienId],
        ghiChu: note,
      },
    }));
  };

  // Save attendance
  const handleSaveAttendance = async (lockAfterSave = false) => {
    if (!selectedBuoiId || isSessionLocked) return;
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const danhSach = Object.entries(attendanceEdits)
        .filter(([, val]) => val?.trangThai)
        .map(([svId, val]) => ({
          sinhVienId: Number(svId),
          trangThai: val.trangThai,
          ghiChu: val.ghiChu || null,
        }));

      if (danhSach.length === 0) {
        setError("Chưa có sinh viên nào được chọn trạng thái điểm danh.");
        setSaving(false);
        return;
      }

      await api.put(`/giang-vien/buoi-hoc/${selectedBuoiId}/diem-danh`, {
        danhSach,
        khoaBuoi: lockAfterSave,
      });

      setSuccess("Lưu kết quả điểm danh thành công!");
      await loadClassAndSessions();
      await loadSessionDetail(selectedBuoiId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // Filtered student list for roll-call table
  const filteredStudents = useMemo(() => {
    const list = chiTietDiemDanh?.danhSachDiemDanh || [];
    if (!searchStudent.trim()) return list;
    const query = searchStudent.toLowerCase();
    return list.filter(
      (sv) =>
        sv.maSinhVien.toLowerCase().includes(query) ||
        sv.hoTenSinhVien.toLowerCase().includes(query) ||
        (sv.lopHanhChinh && sv.lopHanhChinh.toLowerCase().includes(query))
    );
  }, [chiTietDiemDanh?.danhSachDiemDanh, searchStudent]);

  if (loading) {
    return <Spinner label="Đang tải dữ liệu lớp và lịch điểm danh..." />;
  }

  if (!lop) {
    return (
      <div className="card">
        <EmptyState
          icon={CalendarCheck}
          title="Không tìm thấy lớp học phần"
          description="Lớp học phần không tồn tại hoặc bạn không được phân công phụ trách."
          action={
            <Link to="/lop-cua-toi" className="btn-secondary">
              <ArrowLeft size={16} /> Quay lại danh sách lớp
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <Link
          to="/lop-cua-toi"
          className="mb-3 inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-indigo-600 transition"
        >
          <ArrowLeft size={14} /> Quay lại danh sách lớp
        </Link>
        <PageHeader
          icon={CalendarCheck}
          eyebrow={lop.ma}
          title={`Điểm danh & Chuyên cần: ${lop.tenMon}`}
          description={`Phòng ${lop.phong} · ${lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · Sĩ số: ${lop.siSoHienTai}/${lop.siSoToiDa} · ${lop.tenHocKy}`}
          actions={
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={openCreateSessionModal}
                className="btn-primary"
              >
                <Plus size={16} /> Thêm buổi học
              </button>
              <button
                type="button"
                onClick={() => {
                  loadClassAndSessions();
                  if (activeTab === "bao-cao") loadReport();
                }}
                className="btn-secondary"
                title="Làm mới"
              >
                <RefreshCw size={15} />
              </button>
            </div>
          }
        />
      </div>

      {/* Notifications */}
      {error && (
        <Alert tone="error" onClose={() => setError("")}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert tone="success" onClose={() => setSuccess("")}>
          {success}
        </Alert>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab("diem-danh")}
          className={`flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-semibold transition ${
            activeTab === "diem-danh"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          <UserCheck size={16} /> Điểm danh buổi học
        </button>
        <button
          type="button"
          onClick={() => setActiveTab("bao-cao")}
          className={`flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-semibold transition ${
            activeTab === "bao-cao"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          <CalendarDays size={16} /> Báo cáo chuyên cần cả lớp
        </button>
      </div>

      {/* TAB 1: ĐIỂM DANH BUỔI HỌC */}
      {activeTab === "diem-danh" && (
        <div className="space-y-6">
          {/* Buổi học Selection Bar */}
          {buoiHocList.length === 0 ? (
            <div className="card">
              <EmptyState
                icon={CalendarDays}
                title="Chưa có buổi học nào được tạo"
                description="Bấm 'Thêm buổi học' ở trên để tạo buổi điểm danh gắn với ngày và ca học cho lớp."
                action={
                  <button type="button" onClick={openCreateSessionModal} className="btn-primary">
                    <Plus size={16} /> Tạo buổi học đầu tiên
                  </button>
                }
              />
            </div>
          ) : (
            <>
              {/* Session pills / selector */}
              <div className="card p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
                    <CalendarCheck size={16} className="text-indigo-600" />
                    <span>Danh sách buổi học ({buoiHocList.length})</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <select
                      className="input w-auto min-w-[240px] text-sm"
                      value={selectedBuoiId || ""}
                      onChange={(e) => setSelectedBuoiId(Number(e.target.value))}
                    >
                      {buoiHocList.map((buoi, idx) => (
                        <option key={buoi.id} value={buoi.id}>
                          Buổi {idx + 1}: {formatDate(buoi.ngayHoc)} (Ca {buoi.caHoc}) {buoi.daKhoa ? "[Đã khóa]" : ""}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Horizontal badge list */}
                <div className="mt-3 flex gap-2 overflow-x-auto pb-1 pt-1">
                  {buoiHocList.map((b, idx) => {
                    const isSelected = b.id === selectedBuoiId;
                    return (
                      <button
                        key={b.id}
                        type="button"
                        onClick={() => setSelectedBuoiId(b.id)}
                        className={`flex shrink-0 items-center gap-2 rounded-xl border px-3 py-2 text-xs font-medium transition ${
                          isSelected
                            ? "border-indigo-600 bg-indigo-50/80 text-indigo-900 shadow-sm ring-1 ring-indigo-600"
                            : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50"
                        }`}
                      >
                        <span className="font-semibold">Buổi {idx + 1}</span>
                        <span>{formatDate(b.ngayHoc)}</span>
                        <span className="text-slate-400">· Ca {b.caHoc}</span>
                        {b.daKhoa ? (
                          <Badge tone="red" className="py-0.5 text-[10px]">
                            Khóa
                          </Badge>
                        ) : (
                          <Badge tone="green" className="py-0.5 text-[10px]">
                            {b.soDaDiemDanh}/{b.tongSinhVien || lop.siSoHienTai}
                          </Badge>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Selected Session Detail Card */}
              {selectedSession && (
                <div className="card p-5 space-y-4">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between border-b border-slate-100 pb-4">
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-lg font-bold text-slate-900">
                          Buổi học ngày {formatDate(selectedSession.ngayHoc)}
                        </h2>
                        {selectedSession.daKhoa ? (
                          <Badge tone="red" className="px-2.5 py-1">
                            <Lock size={12} className="inline mr-1" /> Đã khóa
                          </Badge>
                        ) : (
                          <Badge tone="green" className="px-2.5 py-1">
                            Đang mở điểm danh
                          </Badge>
                        )}
                      </div>
                      <p className="mt-1 text-xs text-slate-500">
                        Ca {selectedSession.caHoc} · Tiết {selectedSession.tietBatDau || lop.tietBatDau}-
                        {(selectedSession.tietBatDau || lop.tietBatDau) + (selectedSession.soTiet || lop.soTiet) - 1} · Phòng{" "}
                        {selectedSession.phongHoc || lop.phong}
                        {selectedSession.noiDung ? ` · Ghi chú: ${selectedSession.noiDung}` : ""}
                      </p>
                      <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-slate-400">
                        {selectedSession.nguoiTao && (
                          <span>Người tạo: {selectedSession.nguoiTao} ({formatDateTime(selectedSession.thoiGianTao)})</span>
                        )}
                        {selectedSession.nguoiCapNhat && (
                          <span>Sửa lần cuối: {selectedSession.nguoiCapNhat} ({formatDateTime(selectedSession.thoiGianCapNhat)})</span>
                        )}
                        {selectedSession.daKhoa && selectedSession.nguoiKhoa && (
                          <span>Khóa bởi: {selectedSession.nguoiKhoa} ({formatDateTime(selectedSession.thoiGianKhoa)})</span>
                        )}
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="flex flex-wrap items-center gap-2">
                      {!isSessionLocked && (
                        <button
                          type="button"
                          onClick={handleMarkAllPresent}
                          className="btn-secondary text-xs"
                          title="Gán trạng thái Có mặt cho tất cả sinh viên"
                        >
                          <CheckCircle size={14} className="text-emerald-600" /> Tất cả có mặt
                        </button>
                      )}

                      {/* Lock / Unlock button */}
                      {(!selectedSession.daKhoa || isAdmin) && (
                        <button
                          type="button"
                          onClick={() => handleToggleLock(selectedSession.id, selectedSession.daKhoa)}
                          className={`btn-secondary text-xs ${
                            selectedSession.daKhoa ? "text-amber-700" : "text-rose-700"
                          }`}
                        >
                          {selectedSession.daKhoa ? (
                            <>
                              <Unlock size={14} /> Mở khóa
                            </>
                          ) : (
                            <>
                              <Lock size={14} /> Khóa buổi
                            </>
                          )}
                        </button>
                      )}

                      {!isSessionLocked && (
                        <>
                          <button
                            type="button"
                            onClick={() => openEditSessionModal(selectedSession)}
                            className="btn-secondary text-xs"
                            title="Sửa thông tin ngày/ca học"
                          >
                            <Edit2 size={13} /> Sửa
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteSession(selectedSession.id)}
                            className="btn-secondary text-xs text-rose-600 hover:bg-rose-50"
                            title="Xóa buổi học"
                          >
                            <Trash2 size={13} /> Xóa
                          </button>
                        </>
                      )}

                      {!isSessionLocked && (
                        <button
                          type="button"
                          onClick={() => handleSaveAttendance(false)}
                          disabled={saving}
                          className="btn-primary text-xs"
                        >
                          {saving ? "Đang lưu..." : "Lưu điểm danh"}
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Warning banner if locked */}
                  {selectedSession.daKhoa && (
                    <Alert tone="warning">
                      <div className="flex items-center gap-2 font-medium">
                        <Lock size={16} /> Buổi học này đã được khóa.
                      </div>
                      <p className="mt-0.5 text-xs text-amber-800">
                        Dữ liệu điểm danh không thể chỉnh sửa trừ khi có quyền Quản trị viên mở khóa.
                      </p>
                    </Alert>
                  )}

                  {/* Summary counts */}
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 p-3 text-emerald-900">
                      <p className="text-xs font-medium">Có mặt</p>
                      <p className="mt-1 text-2xl font-bold">{selectedSession.soCoMat || 0}</p>
                    </div>
                    <div className="rounded-xl border border-rose-200 bg-rose-50/60 p-3 text-rose-900">
                      <p className="text-xs font-medium">Vắng không phép</p>
                      <p className="mt-1 text-2xl font-bold">{selectedSession.soVang || 0}</p>
                    </div>
                    <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-3 text-amber-900">
                      <p className="text-xs font-medium">Đi muộn</p>
                      <p className="mt-1 text-2xl font-bold">{selectedSession.soMuon || 0}</p>
                    </div>
                    <div className="rounded-xl border border-sky-200 bg-sky-50/60 p-3 text-sky-900">
                      <p className="text-xs font-medium">Có phép</p>
                      <p className="mt-1 text-2xl font-bold">{selectedSession.soCoPhep || 0}</p>
                    </div>
                  </div>

                  {/* Search and Table */}
                  <div className="pt-2">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-3">
                      <div className="relative w-full sm:w-72">
                        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                        <input
                          type="text"
                          placeholder="Tìm sinh viên theo tên, mã..."
                          className="input pl-9 text-sm"
                          value={searchStudent}
                          onChange={(e) => setSearchStudent(e.target.value)}
                        />
                      </div>
                      <div className="text-xs text-slate-500">
                        Hiển thị {filteredStudents.length}/{chiTietDiemDanh?.danhSachDiemDanh?.length || 0} sinh viên
                      </div>
                    </div>

                    {loadingSession ? (
                      <Spinner label="Đang tải danh sách điểm danh..." />
                    ) : filteredStudents.length === 0 ? (
                      <EmptyState
                        icon={Users}
                        title="Không có sinh viên nào"
                        description="Không tìm thấy sinh viên phù hợp hoặc lớp chưa có sinh viên đăng ký hiệu lực."
                      />
                    ) : (
                      <div className="overflow-x-auto rounded-xl border border-slate-200">
                        <table className="w-full text-left text-sm">
                          <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-600">
                            <tr>
                              <th className="px-4 py-3 text-center w-12">STT</th>
                              <th className="px-4 py-3">Mã SV</th>
                              <th className="px-4 py-3">Họ và tên</th>
                              <th className="px-4 py-3">Lớp HC</th>
                              <th className="px-4 py-3 text-center">Trạng thái điểm danh</th>
                              <th className="px-4 py-3">Ghi chú</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100 bg-white">
                            {filteredStudents.map((sv, idx) => {
                              const curEdit = attendanceEdits[sv.sinhVienId] || {};
                              const activeStatus = curEdit.trangThai || "";
                              return (
                                <tr key={sv.sinhVienId} className="hover:bg-slate-50/70 transition">
                                  <td className="px-4 py-3 text-center text-xs text-slate-400">{idx + 1}</td>
                                  <td className="px-4 py-3 font-semibold text-slate-900">{sv.maSinhVien}</td>
                                  <td className="px-4 py-3 text-slate-800 font-medium">{sv.hoTenSinhVien}</td>
                                  <td className="px-4 py-3 text-xs text-slate-500">{sv.lopHanhChinh || "—"}</td>
                                  <td className="px-4 py-3">
                                    <div className="flex items-center justify-center gap-1.5">
                                      {ATTENDANCE_STATUSES.map((st) => {
                                        const isSelected = activeStatus === st.key;
                                        return (
                                          <button
                                            key={st.key}
                                            type="button"
                                            disabled={isSessionLocked}
                                            onClick={() => handleStatusChange(sv.sinhVienId, st.key)}
                                            className={`rounded-lg border px-2.5 py-1 text-xs font-semibold transition ${
                                              isSelected ? st.activeColor : st.color
                                            } ${isSessionLocked ? "opacity-60 cursor-not-allowed" : ""}`}
                                          >
                                            {st.label}
                                          </button>
                                        );
                                      })}
                                    </div>
                                  </td>
                                  <td className="px-4 py-3">
                                    <input
                                      type="text"
                                      disabled={isSessionLocked}
                                      placeholder="Ghi chú (nếu có)"
                                      value={curEdit.ghiChu || ""}
                                      onChange={(e) => handleNoteChange(sv.sinhVienId, e.target.value)}
                                      className="input text-xs py-1 px-2.5 disabled:bg-slate-50 disabled:text-slate-400"
                                    />
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {/* Bottom Save Bar */}
                    {!isSessionLocked && filteredStudents.length > 0 && (
                      <div className="mt-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-t border-slate-100 pt-4">
                        <p className="text-xs text-slate-500">
                          Lưu ý: Mọi thay đổi điểm danh đều được ghi nhận vào nhật ký hệ thống (Audit Log).
                        </p>
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => handleSaveAttendance(true)}
                            disabled={saving}
                            className="btn-secondary text-xs"
                            title="Lưu và khóa buổi học để tránh chỉnh sửa ngoài ý muốn"
                          >
                            <Lock size={13} /> Lưu & Khóa buổi
                          </button>
                          <button
                            type="button"
                            onClick={() => handleSaveAttendance(false)}
                            disabled={saving}
                            className="btn-primary text-xs"
                          >
                            {saving ? "Đang lưu..." : "Lưu điểm danh"}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* TAB 2: BÁO CÁO CHUYÊN CẦN CẢ LỚP */}
      {activeTab === "bao-cao" && (
        <div className="space-y-6">
          {loadingReport ? (
            <Spinner label="Đang tải báo cáo chuyên cần..." />
          ) : !baoCaoLop ? (
            <div className="card">
              <EmptyState
                icon={CalendarDays}
                title="Không có dữ liệu báo cáo"
                description="Lớp học phần chưa có buổi điểm danh nào hoặc không thể tải dữ liệu."
              />
            </div>
          ) : (
            <>
              {/* Overall stats */}
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <StatCard
                  label="Tổng buổi học đã tạo"
                  value={baoCaoLop.tongSoBuoi || 0}
                  hint="Tổng số buổi điểm danh của lớp"
                  icon={CalendarDays}
                  tone="indigo"
                />
                <StatCard
                  label="Sĩ số lớp"
                  value={baoCaoLop.siSoLop || 0}
                  hint="Sinh viên đăng ký hiệu lực"
                  icon={Users}
                  tone="sky"
                />
                <StatCard
                  label="Tỷ lệ chuyên cần TB"
                  value={
                    baoCaoLop.danhSachSinhVien?.length
                      ? `${(
                          baoCaoLop.danhSachSinhVien.reduce((sum, s) => sum + s.tyLeCoMat, 0) /
                          baoCaoLop.danhSachSinhVien.length
                        ).toFixed(1)}%`
                      : "100%"
                  }
                  hint="Trung bình cả lớp"
                  icon={UserCheck}
                  tone="emerald"
                />
                <StatCard
                  label="Sinh viên nguy cơ cấm thi"
                  value={
                    baoCaoLop.danhSachSinhVien?.filter((s) => s.tyLeCoMat < 80).length || 0
                  }
                  hint="Chuyên cần < 80%"
                  icon={XCircle}
                  tone="rose"
                />
              </div>

              {/* Policy & Formula Notice */}
              <Alert tone="info">
                <div className="font-semibold text-sky-950 flex items-center gap-1.5">
                  <Info size={16} /> Quy chế & công thức tính chuyên cần
                </div>
                <div className="mt-1 space-y-1 text-xs text-sky-900 leading-relaxed">
                  <p>
                    <strong>Công thức áp dụng:</strong> {baoCaoLop.congThucApDung}
                  </p>
                  <p>
                    <strong>Ghi chú quy chế (chờ duyệt):</strong> {baoCaoLop.ghiChuQuyChe}
                  </p>
                </div>
              </Alert>

              {/* Student Report Table */}
              <div className="card p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
                  <h3 className="font-semibold text-slate-800">
                    Bảng theo dõi chuyên cần sinh viên ({baoCaoLop.danhSachSinhVien?.length || 0})
                  </h3>
                  <div className="relative w-full sm:w-72">
                    <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Tìm sinh viên..."
                      className="input pl-9 text-xs"
                      value={searchStudent}
                      onChange={(e) => setSearchStudent(e.target.value)}
                    />
                  </div>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-200">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-600">
                      <tr>
                        <th className="px-4 py-3 text-center w-12">STT</th>
                        <th className="px-4 py-3">Mã SV</th>
                        <th className="px-4 py-3">Họ và tên</th>
                        <th className="px-4 py-3">Lớp HC</th>
                        <th className="px-3 py-3 text-center text-emerald-700">Có mặt</th>
                        <th className="px-3 py-3 text-center text-rose-700">Vắng</th>
                        <th className="px-3 py-3 text-center text-amber-700">Muộn</th>
                        <th className="px-3 py-3 text-center text-sky-700">Có phép</th>
                        <th className="px-4 py-3 text-center">Tỷ lệ có mặt</th>
                        <th className="px-4 py-3 text-center">Tỷ lệ vắng</th>
                        <th className="px-4 py-3 text-center">Đánh giá</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {(baoCaoLop.danhSachSinhVien || [])
                        .filter(
                          (sv) =>
                            !searchStudent.trim() ||
                            sv.maSinhVien.toLowerCase().includes(searchStudent.toLowerCase()) ||
                            sv.hoTenSinhVien.toLowerCase().includes(searchStudent.toLowerCase())
                        )
                        .map((sv, idx) => {
                          const isWarning = sv.tyLeCoMat < 80;
                          const isCritical = sv.tyLeCoMat < 70;
                          return (
                            <tr key={sv.sinhVienId} className="hover:bg-slate-50/70 transition">
                              <td className="px-4 py-3 text-center text-xs text-slate-400">{idx + 1}</td>
                              <td className="px-4 py-3 font-semibold text-slate-900">{sv.maSinhVien}</td>
                              <td className="px-4 py-3 font-medium text-slate-800">{sv.hoTenSinhVien}</td>
                              <td className="px-4 py-3 text-xs text-slate-500">{sv.lopHanhChinh || "—"}</td>
                              <td className="px-3 py-3 text-center font-bold text-emerald-600">{sv.soCoMat}</td>
                              <td className="px-3 py-3 text-center font-bold text-rose-600">{sv.soVang}</td>
                              <td className="px-3 py-3 text-center font-bold text-amber-600">{sv.soMuon}</td>
                              <td className="px-3 py-3 text-center font-bold text-sky-600">{sv.soCoPhep}</td>
                              <td className="px-4 py-3 text-center font-bold">
                                <span className={isCritical ? "text-rose-600" : isWarning ? "text-amber-600" : "text-emerald-700"}>
                                  {sv.tyLeCoMat.toFixed(1)}%
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center text-xs text-slate-600">{sv.tyLeVang.toFixed(1)}%</td>
                              <td className="px-4 py-3 text-center">
                                {isCritical ? (
                                  <Badge tone="red">Nguy cơ cấm thi</Badge>
                                ) : isWarning ? (
                                  <Badge tone="amber">Cảnh báo</Badge>
                                ) : (
                                  <Badge tone="green">Đạt</Badge>
                                )}
                              </td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* MODAL TẠO / SỬA BUỔI HỌC */}
      <Modal
        open={sessionModalOpen}
        onClose={() => setSessionModalOpen(false)}
        title={editingSession ? "Sửa thông tin buổi học" : "Tạo buổi học mới"}
        description={`Lớp ${lop.ma} - ${lop.tenMon}`}
        footer={
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setSessionModalOpen(false)}
              className="btn-secondary text-sm"
              disabled={saving}
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={handleSaveSessionModal}
              className="btn-primary text-sm"
              disabled={saving}
            >
              {saving ? "Đang lưu..." : editingSession ? "Cập nhật" : "Tạo buổi"}
            </button>
          </div>
        }
      >
        <form onSubmit={handleSaveSessionModal} className="space-y-4">
          {formError && <Alert tone="error">{formError}</Alert>}

          <Field label="Ngày học" required hint="Ngày diễn ra buổi học (phải nằm trong kỳ học)">
            <input
              type="date"
              required
              className="input text-sm"
              value={sessionForm.ngayHoc}
              onChange={(e) => setSessionForm({ ...sessionForm, ngayHoc: e.target.value })}
            />
          </Field>

          <div className="grid grid-cols-3 gap-3">
            <Field label="Ca học" required hint="Số thứ tự ca (1, 2...)">
              <input
                type="number"
                min="1"
                max="10"
                required
                className="input text-sm"
                value={sessionForm.caHoc}
                onChange={(e) => setSessionForm({ ...sessionForm, caHoc: Number(e.target.value) })}
              />
            </Field>

            <Field label="Tiết bắt đầu" hint="Tiết 1 - 12">
              <input
                type="number"
                min="1"
                max="12"
                className="input text-sm"
                value={sessionForm.tietBatDau}
                onChange={(e) => setSessionForm({ ...sessionForm, tietBatDau: Number(e.target.value) })}
              />
            </Field>

            <Field label="Số tiết" hint="1 - 6 tiết">
              <input
                type="number"
                min="1"
                max="6"
                className="input text-sm"
                value={sessionForm.soTiet}
                onChange={(e) => setSessionForm({ ...sessionForm, soTiet: Number(e.target.value) })}
              />
            </Field>
          </div>

          <Field label="Phòng học" hint="Mặc định phòng theo thời khóa biểu của lớp">
            <input
              type="text"
              placeholder="VD: A101, B202..."
              className="input text-sm"
              value={sessionForm.phongHoc}
              onChange={(e) => setSessionForm({ ...sessionForm, phongHoc: e.target.value })}
            />
          </Field>

          <Field label="Nội dung / Ghi chú buổi học" hint="Chủ đề bài giảng hoặc lưu ý">
            <input
              type="text"
              placeholder="VD: Chương 2: Thiết kế giao diện..."
              className="input text-sm"
              value={sessionForm.noiDung}
              onChange={(e) => setSessionForm({ ...sessionForm, noiDung: e.target.value })}
            />
          </Field>
        </form>
      </Modal>
    </div>
  );
}
