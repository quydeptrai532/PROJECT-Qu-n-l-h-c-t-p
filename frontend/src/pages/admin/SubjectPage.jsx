import { useMemo, useState } from "react";
import { BookMarked, Pencil, Plus, Search, Trash2, X } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { soBuoiHoc, tietLyThuyet } from "../../lib/format";

const EMPTY_FORM = { ma: "", ten: "", soTinChi: "3", khoaId: "" };

export default function SubjectPage() {
  const confirm = useConfirm();
  const { data: monHocList, loading, error, reload } = useApi("/admin/mon-hoc");
  const { data: khoaList } = useApi("/admin/khoa");

  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const [prereqFor, setPrereqFor] = useState(null);
  const [prereqs, setPrereqs] = useState([]);
  const [prereqLoading, setPrereqLoading] = useState(false);
  const [prereqError, setPrereqError] = useState("");
  const [selectedPrereq, setSelectedPrereq] = useState("");

  const rows = useMemo(() => {
    const list = monHocList || [];
    const term = search.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (item) =>
        item.ma.toLowerCase().includes(term) ||
        item.ten.toLowerCase().includes(term) ||
        (item.khoaTen || "").toLowerCase().includes(term)
    );
  }, [monHocList, search]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    setForm({
      ma: row.ma,
      ten: row.ten,
      soTinChi: String(row.soTinChi),
      khoaId: String(row.khoaId || ""),
    });
    setFormError("");
    setModalOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    try {
      const payload = {
        ma: form.ma.trim(),
        ten: form.ten.trim(),
        soTinChi: Number(form.soTinChi),
        khoaId: form.khoaId ? Number(form.khoaId) : null,
      };
      if (editing) {
        await api.put(`/admin/mon-hoc/${editing.id}`, payload);
        setMessage("Cập nhật môn học thành công");
      } else {
        await api.post("/admin/mon-hoc", payload);
        setMessage("Thêm môn học thành công");
      }
      setModalOpen(false);
      reload();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa môn học",
      message: `Bạn có chắc muốn xóa môn ${row.ma} - ${row.ten}?`,
      detail: "Không thể xóa môn học đang được tham chiếu bởi lớp học phần hoặc môn tiên quyết.",
      confirmLabel: "Xóa môn học",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    try {
      await api.delete(`/admin/mon-hoc/${row.id}`);
      setMessage("Đã xóa môn học");
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  const moTienQuyet = async (row) => {
    setPrereqFor(row);
    setPrereqs([]);
    setSelectedPrereq("");
    setPrereqError("");
    setPrereqLoading(true);
    try {
      const { data } = await api.get(`/admin/mon-hoc/${row.id}/tien-quyet`);
      setPrereqs(data);
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    } finally {
      setPrereqLoading(false);
    }
  };

  const themTienQuyet = async () => {
    if (!selectedPrereq) return;
    setPrereqError("");
    try {
      await api.post(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`, {
        monTienQuyetId: Number(selectedPrereq),
      });
      setSelectedPrereq("");
      const { data } = await api.get(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`);
      setPrereqs(data);
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    }
  };

  const xoaTienQuyet = async (lienKet) => {
    const daXacNhan = await confirm({
      title: "Bỏ môn tiên quyết",
      message: `Bỏ môn ${lienKet.maMon} - ${lienKet.tenMon} khỏi danh sách tiên quyết của ${prereqFor.ma}?`,
      detail: "Sinh viên sẽ không còn bị chặn bởi môn này khi đăng ký học phần.",
      confirmLabel: "Bỏ môn",
      tone: "warning",
    });
    if (!daXacNhan) return;
    setPrereqError("");
    try {
      await api.delete(`/admin/mon-hoc/${prereqFor.id}/tien-quyet/${lienKet.id}`);
      const { data } = await api.get(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`);
      setPrereqs(data);
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    }
  };

  const luaChonTienQuyet = (monHocList || []).filter(
    (item) =>
      item.id !== prereqFor?.id && !prereqs.some((lienKet) => lienKet.monTienQuyetId === item.id)
  );

  return (
    <div>
      <PageHeader
        icon={BookMarked}
        title="Môn học"
        description="Danh mục môn học, số tín chỉ, số tiết lý thuyết (1 TC = 15 tiết = 5 buổi) và môn tiên quyết."
        actions={
          <button type="button" className="btn-primary" onClick={openCreate}>
            <Plus size={16} /> Thêm môn học
          </button>
        }
      />

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {actionError && <Alert tone="error" onClose={() => setActionError("")}>{actionError}</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-xs">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Tìm theo mã, tên môn, khoa..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <p className="text-sm text-slate-500">{rows.length} môn học</p>
        </div>

        {loading ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState icon={BookMarked} title="Chưa có môn học" description="Thêm môn học để mở lớp học phần." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã môn</th>
                  <th className="th">Tên môn học</th>
                  <th className="th text-center">Tín chỉ</th>
                  <th className="th text-center">Tiết lý thuyết</th>
                  <th className="th text-center">Số buổi (3 tiết/buổi)</th>
                  <th className="th">Khoa</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-slate-50/60">
                    <td className="td font-medium text-slate-900">{row.ma}</td>
                    <td className="td">{row.ten}</td>
                    <td className="td text-center">{row.soTinChi}</td>
                    <td className="td text-center">{tietLyThuyet(row.soTinChi)}</td>
                    <td className="td text-center">{soBuoiHoc(row.soTinChi)}</td>
                    <td className="td text-slate-500">{row.khoaTen}</td>
                    <td className="td">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          className="btn-secondary px-2.5 py-1.5 text-xs"
                          onClick={() => moTienQuyet(row)}
                        >
                          Tiên quyết
                        </button>
                        <button
                          type="button"
                          className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                          onClick={() => openEdit(row)}
                          title="Sửa"
                        >
                          <Pencil size={15} />
                        </button>
                        <button
                          type="button"
                          className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50"
                          onClick={() => remove(row)}
                          title="Xóa"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? "Cập nhật môn học" : "Thêm môn học"}
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Hủy</button>
            <button type="submit" form="subject-form" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="subject-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2"><Alert tone="error">{formError}</Alert></div>
          )}
          <Field label="Mã môn học" required>
            <input
              className="input"
              value={form.ma}
              onChange={(event) => setForm({ ...form, ma: event.target.value })}
              placeholder="VD: INT201"
              required
            />
          </Field>
          <Field
            label="Số tín chỉ"
            required
            hint={`1 tín chỉ = 15 tiết lý thuyết = 5 buổi (3 tiết/buổi) · ${tietLyThuyet(form.soTinChi)} tiết · ${soBuoiHoc(form.soTinChi)} buổi`}
          >
            <input
              className="input"
              type="number"
              min="1"
              max="10"
              value={form.soTinChi}
              onChange={(event) => setForm({ ...form, soTinChi: event.target.value })}
              required
            />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Tên môn học" required>
              <input
                className="input"
                value={form.ten}
                onChange={(event) => setForm({ ...form, ten: event.target.value })}
                required
              />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <Field label="Khoa" required>
              <select
                className="input"
                value={form.khoaId}
                onChange={(event) => setForm({ ...form, khoaId: event.target.value })}
                required
              >
                <option value="">-- Chọn khoa --</option>
                {(khoaList || []).map((khoa) => (
                  <option key={khoa.id} value={khoa.id}>
                    {khoa.ma} - {khoa.ten}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(prereqFor)}
        onClose={() => setPrereqFor(null)}
        title="Môn tiên quyết"
        description={
          prereqFor
            ? `${prereqFor.ma} - ${prereqFor.ten}. Sinh viên phải đạt các môn dưới đây trước khi đăng ký.`
            : ""
        }
        footer={
          <button type="button" className="btn-secondary" onClick={() => setPrereqFor(null)}>
            Đóng
          </button>
        }
      >
        {prereqError && <Alert tone="error" className="mb-3">{prereqError}</Alert>}
        {prereqLoading ? (
          <Spinner label="Đang tải môn tiên quyết..." />
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {prereqs.length === 0 ? (
                <p className="text-sm text-slate-500">Chưa cấu hình môn tiên quyết.</p>
              ) : (
                prereqs.map((lienKet) => (
                  <span
                    key={lienKet.id}
                    className="inline-flex items-center gap-2 rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700"
                  >
                    {lienKet.maMon} - {lienKet.tenMon}
                    <button
                      type="button"
                      onClick={() => xoaTienQuyet(lienKet)}
                      className="rounded-full p-0.5 hover:bg-indigo-200"
                      title="Bỏ môn tiên quyết"
                    >
                      <X size={13} />
                    </button>
                  </span>
                ))
              )}
            </div>

            <div className="mt-5 flex flex-col gap-2 sm:flex-row">
              <select
                className="input flex-1"
                value={selectedPrereq}
                onChange={(event) => setSelectedPrereq(event.target.value)}
              >
                <option value="">-- Chọn môn tiên quyết --</option>
                {luaChonTienQuyet.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.ma} - {item.ten}
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="btn-primary"
                onClick={themTienQuyet}
                disabled={!selectedPrereq}
              >
                <Plus size={16} /> Thêm
              </button>
            </div>
            <p className="mt-2 text-xs text-slate-500">
              Hệ thống tự động chặn đăng ký nếu sinh viên chưa đạt tất cả môn tiên quyết.
            </p>
          </>
        )}
      </Modal>
    </div>
  );
}
