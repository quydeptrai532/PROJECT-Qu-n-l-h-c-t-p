import { useEffect, useState } from "react";
import { AlertCircle, CheckCircle2, History, Megaphone, RotateCw, Send } from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { Alert, Badge, EmptyState, Modal, Spinner } from "./ui";

const DATE_FORMATTER = new Intl.DateTimeFormat("vi-VN", {
  dateStyle: "medium",
  timeStyle: "short",
});

export default function ClassAnnouncementModal({ open, onClose, lop }) {
  const [activeTab, setActiveTab] = useState("compose");
  const [tieuDe, setTieuDe] = useState("");
  const [noiDung, setNoiDung] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [formSuccess, setFormSuccess] = useState("");

  const [historyList, setHistoryList] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");

  const lopId = lop?.id;

  const loadHistory = async () => {
    if (!lopId) return;
    setHistoryLoading(true);
    setHistoryError("");
    try {
      const { data } = await api.get(`/giang-vien/lop-hoc-phan/${lopId}/thong-bao`);
      setHistoryList(data || []);
    } catch (err) {
      setHistoryError(getErrorMessage(err));
    } finally {
      setHistoryLoading(false);
    }
  };

  useEffect(() => {
    if (open && lopId) {
      setTieuDe("");
      setNoiDung("");
      setFormError("");
      setFormSuccess("");
      setIdempotencyKey(typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `key-${Date.now()}`);
      loadHistory();
    }
  }, [open, lopId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    const cleanTitle = tieuDe.trim();
    if (!cleanTitle) {
      setFormError("Vui lòng nhập tiêu đề thông báo.");
      return;
    }
    if (cleanTitle.length > 120) {
      setFormError("Tiêu đề không được vượt quá 120 ký tự.");
      return;
    }

    const cleanContent = noiDung.trim();
    if (!cleanContent) {
      setFormError("Vui lòng nhập nội dung thông báo.");
      return;
    }
    if (cleanContent.length > 1000) {
      setFormError("Nội dung không được vượt quá 1000 ký tự.");
      return;
    }

    setSubmitting(true);
    try {
      const { data } = await api.post(
        `/giang-vien/lop-hoc-phan/${lopId}/thong-bao`,
        {
          tieuDe: cleanTitle,
          noiDung: cleanContent,
          idempotencyKey,
        },
        {
          headers: {
            "Idempotency-Key": idempotencyKey,
          },
        }
      );

      setFormSuccess(
        `Đã gửi thông báo thành công đến ${data.soNguoiNhan} sinh viên đang học hợp lệ trong lớp!`
      );
      setTieuDe("");
      setNoiDung("");
      setIdempotencyKey(typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `key-${Date.now()}`);
      await loadHistory();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const formatThoiGian = (isoString) => {
    if (!isoString) return "—";
    try {
      return DATE_FORMATTER.format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={
        lop
          ? `Gửi thông báo lớp: ${lop.ma} - ${lop.tenMon || lop.ten || ""}`
          : "Gửi thông báo cho lớp học phần"
      }
      description="Thông báo phát hành sẽ lập tức xuất hiện trong hộp thư thông báo của tất cả sinh viên đang có trạng thái đăng ký hợp lệ (DA_DANG_KY) trong lớp."
    >
      <div className="space-y-4">
        {/* Tab switcher */}
        <div className="flex border-b border-slate-200">
          <button
            type="button"
            className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-xs font-semibold transition ${
              activeTab === "compose"
                ? "border-emerald-600 text-emerald-700"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
            onClick={() => setActiveTab("compose")}
          >
            <Megaphone size={15} />
            Soạn thông báo
          </button>
          <button
            type="button"
            className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-xs font-semibold transition ${
              activeTab === "history"
                ? "border-emerald-600 text-emerald-700"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
            onClick={() => setActiveTab("history")}
          >
            <History size={15} />
            Lịch sử đã gửi ({historyList.length})
          </button>
        </div>

        {activeTab === "compose" && (
          <form onSubmit={handleSubmit} className="space-y-4">
            {formSuccess && (
              <Alert tone="success" onClose={() => setFormSuccess("")}>
                {formSuccess}
              </Alert>
            )}
            {formError && (
              <Alert tone="error" onClose={() => setFormError("")}>
                {formError}
              </Alert>
            )}

            {/* Recipient summary & Scope policy card */}
            <div className="rounded-xl border border-sky-100 bg-sky-50/70 p-3.5 text-xs text-sky-900">
              <div className="flex items-center justify-between">
                <span className="font-semibold">Phạm vi người nhận:</span>
                <Badge tone="sky">Sinh viên đang học (DA_DANG_KY)</Badge>
              </div>
              <p className="mt-1.5 leading-relaxed text-sky-800">
                • Danh sách người nhận do máy chủ xác minh tự động từ cơ sở dữ liệu phân công lớp và trạng thái đăng ký của sinh viên.
              </p>
              <p className="mt-1 text-slate-600">
                • <strong>Giới hạn chức năng:</strong> Hệ thống áp dụng cơ chế <em>Gửi ngay</em>. Dự án chưa có scheduler hoặc chính sách chỉnh sửa/xóa sau khi phát hành; vui lòng kiểm tra kỹ nội dung trước khi gửi.
              </p>
            </div>

            {/* Field: Tiêu đề */}
            <div>
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700">
                  Tiêu đề thông báo <span className="text-rose-500">*</span>
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    tieuDe.length > 120 ? "text-rose-600 font-bold" : "text-slate-400"
                  }`}
                >
                  {tieuDe.length}/120
                </span>
              </div>
              <input
                type="text"
                className="input mt-1 w-full text-xs"
                placeholder="Ví dụ: Nhắc nộp bài tập lớn / Dặn dò kiểm tra giữa kỳ"
                maxLength={120}
                value={tieuDe}
                onChange={(e) => setTieuDe(e.target.value)}
                disabled={submitting}
              />
            </div>

            {/* Field: Nội dung */}
            <div>
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700">
                  Nội dung chi tiết <span className="text-rose-500">*</span>
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    noiDung.length > 1000 ? "text-rose-600 font-bold" : "text-slate-400"
                  }`}
                >
                  {noiDung.length}/1000
                </span>
              </div>
              <textarea
                className="input mt-1 w-full text-xs leading-relaxed"
                rows={5}
                placeholder="Nhập nội dung thông báo gửi đến sinh viên trong lớp học phần..."
                maxLength={1000}
                value={noiDung}
                onChange={(e) => setNoiDung(e.target.value)}
                disabled={submitting}
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                className="btn-secondary text-xs"
                onClick={onClose}
                disabled={submitting}
              >
                Hủy / Đóng
              </button>
              <button
                type="submit"
                className="btn-primary text-xs"
                disabled={submitting || !tieuDe.trim() || !noiDung.trim() || tieuDe.length > 120 || noiDung.length > 1000}
              >
                <Send size={14} />
                {submitting ? "Đang gửi thông báo..." : "Gửi thông báo ngay"}
              </button>
            </div>
          </form>
        )}

        {activeTab === "history" && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs text-slate-500">
                Các thông báo đã được gửi cho lớp học phần này:
              </p>
              <button
                type="button"
                className="btn-secondary px-2 py-1 text-xs"
                onClick={loadHistory}
                disabled={historyLoading}
                title="Làm mới lịch sử"
              >
                <RotateCw size={13} className={historyLoading ? "animate-spin" : ""} />
                Làm mới
              </button>
            </div>

            {historyError && <Alert tone="error">{historyError}</Alert>}

            {historyLoading ? (
              <Spinner label="Đang tải lịch sử thông báo..." />
            ) : historyList.length === 0 ? (
              <EmptyState
                icon={Megaphone}
                title="Chưa có thông báo nào"
                description="Lớp học phần này chưa được gửi thông báo nào trước đây."
              />
            ) : (
              <div className="max-h-[380px] space-y-2.5 overflow-y-auto pr-1">
                {historyList.map((item) => (
                  <div
                    key={item.id}
                    className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-sm transition hover:border-slate-300"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h3 className="font-semibold text-slate-900 text-xs sm:text-sm">
                          {item.tieuDe}
                        </h3>
                        <p className="mt-0.5 text-[11px] text-slate-400">
                          {formatThoiGian(item.thoiGianGui)} · Người gửi: {item.giangVienTen || "Giảng viên"}
                        </p>
                      </div>
                      <Badge tone="green">
                        Đã gửi ({item.soNguoiNhan} SV)
                      </Badge>
                    </div>
                    <div className="mt-2.5 whitespace-pre-wrap rounded-lg bg-slate-50 p-2.5 text-xs text-slate-700 leading-relaxed border border-slate-100">
                      {item.noiDung}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
