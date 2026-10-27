import { useEffect, useMemo, useState } from "react";
import { BellRing, Trash2 } from "lucide-react";
import { Alert, Field } from "./ui";
import { formatDate, sessionDates, THU_LABEL, toDateKey } from "../lib/format";

export default function AnnouncementPanel({
  classes = [],
  announcements = [],
  hocKy,
  editable = false,
  busy = false,
  error = "",
  success = "",
  onDismissError,
  onDismissSuccess,
  onCreate,
  onDelete,
}) {
  const [lopHocPhanId, setLopHocPhanId] = useState("");
  const [ngayNghi, setNgayNghi] = useState("");
  const [lyDo, setLyDo] = useState("");
  const [formError, setFormError] = useState("");

  useEffect(() => {
    const conTonTai = classes.some((lop) => String(lop.lopHocPhanId) === String(lopHocPhanId));
    if (!conTonTai) {
      setLopHocPhanId(classes.length ? String(classes[0].lopHocPhanId) : "");
    }
  }, [classes, lopHocPhanId]);

  const lopDangChon = classes.find((lop) => String(lop.lopHocPhanId) === String(lopHocPhanId));

  const cacBuoiHoc = useMemo(
    () =>
      lopDangChon
        ? sessionDates(hocKy?.ngayBatDau, hocKy?.ngayKetThuc, lopDangChon.thu, new Date())
        : [],
    [
      hocKy?.ngayBatDau,
      hocKy?.ngayKetThuc,
      lopDangChon?.lopHocPhanId,
      lopDangChon?.thu,
    ]
  );

  useEffect(() => {
    const hopLe = cacBuoiHoc.map((date) => toDateKey(date));
    if (hopLe.length === 0) {
      if (ngayNghi) setNgayNghi("");
    } else if (!hopLe.includes(ngayNghi)) {
      setNgayNghi(hopLe[0]);
    }
  }, [cacBuoiHoc, ngayNghi]);

  const submit = async (event) => {
    event.preventDefault();
    setFormError("");
    if (!lopHocPhanId) {
      setFormError("Vui lòng chọn lớp học phần.");
      return;
    }
    if (!ngayNghi) {
      setFormError("Vui lòng chọn buổi học cần nghỉ.");
      return;
    }

    const thanhCong = await onCreate({
      lopHocPhanId: Number(lopHocPhanId),
      ngayNghi,
      lyDo: lyDo.trim() || null,
    });
    if (thanhCong) {
      setLyDo("");
    }
  };

  return (
    <aside className="card flex flex-col gap-4 p-4 xl:self-start">
      <div className="flex items-start gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-rose-500 to-pink-500 text-white shadow-lg shadow-rose-500/25">
          <BellRing size={19} />
        </span>
        <div>
          <h2 className="text-sm font-semibold text-slate-800">Thông báo nghỉ học</h2>
          <p className="mt-0.5 text-xs leading-relaxed text-slate-500">
            {editable
              ? "Đăng thông báo để sinh viên biết buổi học bị nghỉ."
              : "Các buổi học được giáo viên thông báo nghỉ."}
          </p>
        </div>
      </div>

      {error && <Alert tone="error" onClose={onDismissError}>{error}</Alert>}
      {success && <Alert tone="success" onClose={onDismissSuccess}>{success}</Alert>}

      {editable && (
        <form className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/60 p-3" onSubmit={submit}>
          <Field label="Lớp học phần" required>
            <select
              className="input"
              value={lopHocPhanId}
              onChange={(event) => setLopHocPhanId(event.target.value)}
              disabled={classes.length === 0}
            >
              {classes.length === 0 ? (
                <option value="">Chưa có lớp học phần</option>
              ) : (
                classes.map((lop) => (
                  <option key={lop.lopHocPhanId} value={lop.lopHocPhanId}>
                    {lop.maLop} - {lop.tenMon}
                  </option>
                ))
              )}
            </select>
          </Field>

          <Field
            label="Buổi học nghỉ"
            required
            hint={
              lopDangChon
                ? `Chỉ chọn được các buổi còn lại của lớp: ${THU_LABEL[lopDangChon.thu]}, tiết ${lopDangChon.tietBatDau}-${lopDangChon.tietBatDau + lopDangChon.soTiet - 1}`
                : ""
            }
          >
            <select
              className="input"
              value={ngayNghi}
              onChange={(event) => setNgayNghi(event.target.value)}
              disabled={cacBuoiHoc.length === 0}
              required
            >
              {cacBuoiHoc.length === 0 ? (
                <option value="">Không còn buổi học nào</option>
              ) : (
                cacBuoiHoc.map((date) => {
                  const key = toDateKey(date);
                  return (
                    <option key={key} value={key}>
                      {THU_LABEL[lopDangChon.thu]}, {formatDate(key)}
                    </option>
                  );
                })
              )}
            </select>
          </Field>

          <Field label="Lý do (tuỳ chọn)">
            <input
              className="input"
              value={lyDo}
              maxLength={255}
              onChange={(event) => setLyDo(event.target.value)}
              placeholder="VD: Giảng viên bận công tác"
            />
          </Field>

          {formError && <p className="text-xs text-rose-600">{formError}</p>}

          <button
            type="submit"
            className="btn-primary w-full"
            disabled={busy || classes.length === 0 || cacBuoiHoc.length === 0}
          >
            <BellRing size={15} /> {busy ? "Đang đăng..." : "Đăng thông báo nghỉ"}
          </button>
        </form>
      )}

      <div className="space-y-2">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
          Danh sách thông báo ({announcements.length})
        </p>
        {announcements.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-xs text-slate-400">
            Chưa có thông báo nghỉ học.
          </p>
        ) : (
          announcements.map((thongBao) => (
            <div key={thongBao.id} className="rounded-xl border border-rose-200 bg-rose-50/70 p-3">
              <div className="flex items-start justify-between gap-2">
                <p className="text-sm font-semibold text-rose-700">{formatDate(thongBao.ngayNghi)}</p>
                {editable && onDelete && (
                  <button
                    type="button"
                    className="rounded-md p-1 text-rose-500 transition hover:bg-rose-100"
                    onClick={() => onDelete(thongBao)}
                    title="Xoá thông báo"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
              <p className="mt-0.5 text-sm text-slate-700">
                {thongBao.maLop} · {thongBao.tenMon}
              </p>
              {thongBao.lyDo && <p className="mt-0.5 text-xs text-slate-500">Lý do: {thongBao.lyDo}</p>}
              <p className="mt-1 text-[11px] text-slate-400">{thongBao.tenGiangVien}</p>
            </div>
          ))
        )}
      </div>

      <p className="text-[11px] leading-relaxed text-slate-400">
        Buổi nghỉ sẽ hiển thị màu đỏ với dòng chữ “Nghỉ buổi hôm đó” trên thời khóa biểu. Chuyển đến đúng tuần để xem.
      </p>
    </aside>
  );
}
