import { useEffect, useMemo, useState } from "react";
import { Download, Layers, Pencil, Plus, Search, Trash2 } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { lichHoc, soBuoiHoc, tietLyThuyet } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const EMPTY_FORM = {
  ma: "",
  monHocId: "",
  hocKyId: "",
  giangVienId: "",
  phong: "",
  thu: "2",
  tietBatDau: "1",
  soTiet: "3",
  siSoToiDa: "40",
};

export default function CourseClassPage() {
  const confirm = useConfirm();
  const { data: hocKyList } = useApi("/admin/hoc-ky");
  const { data: monHocList } = useApi("/admin/mon-hoc");
  const { data: giangVienList } = useApi("/admin/giang-vien");

  const [hocKyId, setHocKyId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [message, setMessage] = useState("");
  const [warning, setWarning] = useState("");
  const [actionError, setActionError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data: lopList, loading, error, reload } = useApi("/admin/lop-hoc-phan", params);

  const rows = useMemo(() => {
    const list = lopList || [];
    const term = keyword.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (lop) =>
        lop.ma.toLowerCase().includes(term) ||
        lop.tenMon.toLowerCase().includes(term) ||
        lop.maMon.toLowerCase().includes(term) ||
        lop.tenGiangVien.toLowerCase().includes(term)
    );
  }, [lopList, keyword]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM, hocKyId: hocKyId || "" });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    setForm({
      ma: row.ma,
      monHocId: String(row.monHocId),
      hocKyId: String(row.hocKyId),
      giangVienId: String(row.giangVienId),
      phong: row.phong,
      thu: String(row.thu),
      tietBatDau: String(row.tietBatDau),
      soTiet: String(row.soTiet),
      siSoToiDa: String(row.siSoToiDa),
    });
    setFormError("");
    setModalOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    setWarning("");
    setActionError("");
    try {
      const tietBatDau = Number(form.tietBatDau);
      const soTiet = Number(form.soTiet);
      if (tietBatDau + soTiet - 1 > 12) {
        throw new Error("Lịch học vượt quá tiết 12");
      }
      const payload = {
        ma: form.ma.trim(),
        monHocId: Number(form.monHocId),
        hocKyId: Number(form.hocKyId),
        giangVienId: Number(form.giangVienId),
        phong: form.phong.trim(),
        thu: Number(form.thu),
        tietBatDau,
        soTiet,
        siSoToiDa: Number(form.siSoToiDa),
      };
      const { data } = editing
        ? await api.put(`/admin/lop-hoc-phan/${editing.id}`, payload)
        : await api.post("/admin/lop-hoc-phan", payload);

      setModalOpen(false);
      reload();
      if (data.canhBao?.length) {
        setWarning(`${editing ? "Đã cập nhật" : "Đã tạo"} lớp ${data.lopHocPhan.ma}. Cảnh báo:\n- ${data.canhBao.join("\n- ")}`);
      } else {
        setMessage(`${editing ? "Cập nhật" : "Tạo"} lớp học phần ${data.lopHocPhan.ma} thành công`);
      }
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa lớp học phần",
      message: `Bạn có chắc muốn xóa lớp ${row.ma} - ${row.tenMon}?`,
      detail: "Chỉ có thể xóa lớp chưa có sinh viên đăng ký. Thao tác này không thể hoàn tác.",
      confirmLabel: "Xóa lớp",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    setWarning("");
    try {
      await api.delete(`/admin/lop-hoc-phan/${row.id}`);
      setMessage(`Đã xóa lớp ${row.ma}`);
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  const taiCsv = async (row) => {
    setActionError("");
    try {
      const response = await api.get(`/admin/lop-hoc-phan/${row.id}/csv`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `lop-${row.ma}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        icon={Layers}
        title="Lớp học phần"
        description="Mở lớp theo học kỳ, phân công giảng viên, phòng học, lịch học và sĩ số tối đa."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <select
              className="input w-auto min-w-[220px]"
              value={hocKyId}
              onChange={(event) => setHocKyId(event.target.value)}
            >
              {(hocKyList || []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} - {item.namHoc}
                </option>
              ))}
            </select>
            <button type="button" className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> Mở lớp
            </button>
          </div>
        }
      />

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {warning && <Alert tone="warning" onClose={() => setWarning("")}>{warning}</Alert>}
        {actionError && <Alert tone="error" onClose={() => setActionError("")}>{actionError}</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-sm">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Tìm lớp, môn học, giảng viên..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>
          <p className="text-sm text-slate-500">{rows.length} lớp học phần</p>
        </div>

        {loading ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState icon={Layers} title="Chưa có lớp học phần" description="Bấm Mở lớp để tạo lớp cho học kỳ này." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã lớp</th>
                  <th className="th">Môn học</th>
                  <th className="th">Giảng viên</th>
                  <th className="th">Lịch học</th>
                  <th className="th">Phòng</th>
                  <th className="th text-center">Sĩ số</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-slate-50/60">
                    <td className="td font-medium text-slate-900">{row.ma}</td>
                    <td className="td">
                      <p className="font-medium text-slate-800">{row.tenMon}</p>
                      <p className="text-xs text-slate-500">
                        {row.maMon} · {row.soTinChi} TC · {tietLyThuyet(row.soTinChi)} tiết LT ·{" "}
                        {soBuoiHoc(row.soTinChi)} buổi
                      </p>
                    </td>
                    <td className="td">{row.tenGiangVien}</td>
                    <td className="td">{lichHoc(row.thu, row.tietBatDau, row.soTiet)}</td>
                    <td className="td">{row.phong}</td>
                    <td className="td text-center">
                      {row.siSoHienTai}/{row.siSoToiDa}
                    </td>
                    <td className="td">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                          onClick={() => taiCsv(row)}
                          title="Xuất danh sách CSV"
                        >
                          <Download size={15} />
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
        title={editing ? "Cập nhật lớp học phần" : "Mở lớp học phần"}
        size="lg"
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Hủy</button>
            <button type="submit" form="course-class-form" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="course-class-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2"><Alert tone="error">{formError}</Alert></div>
          )}
          <Field label="Mã lớp học phần" required>
            <input
              className="input"
              value={form.ma}
              onChange={(event) => setForm({ ...form, ma: event.target.value })}
              placeholder="VD: INT201.01"
              required
            />
          </Field>
          <Field label="Môn học" required>
            <select
              className="input"
              value={form.monHocId}
              onChange={(event) => setForm({ ...form, monHocId: event.target.value })}
              required
            >
              <option value="">-- Chọn môn --</option>
              {(monHocList || []).map((mon) => (
                <option key={mon.id} value={mon.id}>
                  {mon.ma} - {mon.ten} ({mon.soTinChi} TC · {tietLyThuyet(mon.soTinChi)} tiết)
                </option>
              ))}
            </select>
          </Field>
          <Field label="Học kỳ" required>
            <select
              className="input"
              value={form.hocKyId}
              onChange={(event) => setForm({ ...form, hocKyId: event.target.value })}
              required
            >
              <option value="">-- Chọn học kỳ --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} - {hk.namHoc}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Giảng viên" required>
            <select
              className="input"
              value={form.giangVienId}
              onChange={(event) => setForm({ ...form, giangVienId: event.target.value })}
              required
            >
              <option value="">-- Chọn giảng viên --</option>
              {(giangVienList || []).map((gv) => (
                <option key={gv.id} value={gv.id}>
                  {gv.ma} - {gv.hoTen}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Phòng học" required>
            <input
              className="input"
              value={form.phong}
              onChange={(event) => setForm({ ...form, phong: event.target.value })}
              placeholder="VD: A101"
              required
            />
          </Field>
          <Field label="Sĩ số tối đa" required>
            <input
              className="input"
              type="number"
              min="1"
              value={form.siSoToiDa}
              onChange={(event) => setForm({ ...form, siSoToiDa: event.target.value })}
              required
            />
          </Field>
          <Field label="Thứ học" required>
            <select
              className="input"
              value={form.thu}
              onChange={(event) => setForm({ ...form, thu: event.target.value })}
            >
              {[2, 3, 4, 5, 6, 7].map((thu) => (
                <option key={thu} value={thu}>Thứ {thu}</option>
              ))}
            </select>
          </Field>
          <Field label="Tiết bắt đầu" required>
            <input
              className="input"
              type="number"
              min="1"
              max="12"
              value={form.tietBatDau}
              onChange={(event) => setForm({ ...form, tietBatDau: event.target.value })}
              required
            />
          </Field>
          <Field label="Số tiết" required hint="Tiết bắt đầu + số tiết không vượt quá 12">
            <input
              className="input"
              type="number"
              min="1"
              max="6"
              value={form.soTiet}
              onChange={(event) => setForm({ ...form, soTiet: event.target.value })}
              required
            />
          </Field>
        </form>
      </Modal>
    </div>
  );
}
