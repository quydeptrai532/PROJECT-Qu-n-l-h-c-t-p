import { useCallback, useEffect, useState } from "react";
import {
  CheckCircle2,
  Clock,
  Download,
  Eye,
  History,
  LifeBuoy,
  MessageSquare,
  Paperclip,
  Search,
  Send,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem } from "../../lib/format";
import {
  downloadAttachmentFile,
  formatFileSize,
  STATUS_TONE,
} from "../student/SupportRequestsPage";

const PAGE_SIZE = 15;

export default function AdminSupportRequestsPage() {
  const [catalog, setCatalog] = useState(null);
  const [filters, setFilters] = useState({ trangThai: "", loaiYeuCau: "", keyword: "" });
  const [appliedFilters, setAppliedFilters] = useState({ trangThai: "", loaiYeuCau: "", keyword: "" });
  const [pageNumber, setPageNumber] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Detail modal state
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [selectedTicket, setSelectedTicket] = useState(null);

  // Status update form state
  const [nextStatus, setNextStatus] = useState("");
  const [statusNote, setStatusNote] = useState("");
  const [statusUpdating, setStatusUpdating] = useState(false);

  // Reply form state
  const [replyText, setReplyText] = useState("");
  const [replySubmitting, setReplySubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .get("/yeu-cau-ho-tro/danh-muc")
      .then(({ data }) => {
        if (active) setCatalog(data);
      })
      .catch((err) => {
        if (active) setError(getErrorMessage(err));
      });
    return () => {
      active = false;
    };
  }, []);

  const loadList = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = { page: pageNumber, size: PAGE_SIZE };
      if (appliedFilters.trangThai) params.trangThai = appliedFilters.trangThai;
      if (appliedFilters.loaiYeuCau) params.loaiYeuCau = appliedFilters.loaiYeuCau;
      if (appliedFilters.keyword) params.keyword = appliedFilters.keyword;
      const { data } = await api.get("/admin/yeu-cau-ho-tro", { params });
      setPageData(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [pageNumber, appliedFilters]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const statusNameMap = (catalog?.trangThai || []).reduce((acc, cur) => {
    acc[cur.ma] = cur.ten;
    return acc;
  }, {});

  const openTicketDetail = async (ticketId) => {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError("");
    setReplyText("");
    setStatusNote("");
    try {
      const { data } = await api.get(`/admin/yeu-cau-ho-tro/${ticketId}`);
      setSelectedTicket(data);
      setNextStatus(data.trangThaiTiepTheoHopLe?.[0] || "");
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setDetailLoading(false);
    }
  };

  const handleStatusUpdate = async (event) => {
    event.preventDefault();
    if (!selectedTicket || !nextStatus) return;
    setStatusUpdating(true);
    setDetailError("");
    setSuccessMsg("");
    try {
      const { data } = await api.patch(`/admin/yeu-cau-ho-tro/${selectedTicket.id}/trang-thai`, {
        trangThai: nextStatus,
        ghiChu: statusNote.trim() || null,
      });
      setSelectedTicket(data);
      setNextStatus(data.trangThaiTiepTheoHopLe?.[0] || "");
      setStatusNote("");
      setSuccessMsg(`Đã cập nhật trạng thái phiếu ${data.maPhieu} sang "${data.tenTrangThai}".`);
      loadList();
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleReplySubmit = async (event) => {
    event.preventDefault();
    if (!selectedTicket) return;
    const trimmed = replyText.trim();
    if (!trimmed) {
      setDetailError("Vui lòng nhập nội dung phản hồi.");
      return;
    }
    setReplySubmitting(true);
    setDetailError("");
    try {
      await api.post(`/yeu-cau-ho-tro/${selectedTicket.id}/trao-doi`, {
        noiDung: trimmed,
      });
      setReplyText("");
      const { data } = await api.get(`/admin/yeu-cau-ho-tro/${selectedTicket.id}`);
      setSelectedTicket(data);
      loadList();
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setReplySubmitting(false);
    }
  };

  const applyFilterSubmit = (event) => {
    event.preventDefault();
    setPageNumber(0);
    setAppliedFilters({ ...filters });
  };

  const resetFilters = () => {
    const empty = { trangThai: "", loaiYeuCau: "", keyword: "" };
    setFilters(empty);
    setAppliedFilters(empty);
    setPageNumber(0);
  };

  const items = pageData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        icon={LifeBuoy}
        eyebrow="Quản trị học vụ"
        title="Tiếp nhận & xử lý phiếu hỗ trợ sinh viên"
        description="Tra cứu, phản hồi trao đổi, kiểm tra tệp minh chứng và cập nhật trạng thái xử lý yêu cầu hỗ trợ / khiếu nại."
      />

      {successMsg && (
        <Alert tone="success" onClose={() => setSuccessMsg("")}>
          {successMsg}
        </Alert>
      )}
      {error && (
        <Alert tone="error" onClose={() => setError("")}>
          {error}
        </Alert>
      )}

      <form
        onSubmit={applyFilterSubmit}
        className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"
      >
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-medium text-slate-600">
            Loại yêu cầu
            <select
              className="input mt-1 w-full"
              value={filters.loaiYeuCau}
              onChange={(e) => setFilters((prev) => ({ ...prev, loaiYeuCau: e.target.value }))}
            >
              <option value="">Tất cả loại yêu cầu</option>
              {(catalog?.loaiYeuCau || []).map((item) => (
                <option key={item.ma} value={item.ma}>
                  {item.ten}
                </option>
              ))}
            </select>
          </label>

          <label className="text-xs font-medium text-slate-600">
            Trạng thái
            <select
              className="input mt-1 w-full"
              value={filters.trangThai}
              onChange={(e) => setFilters((prev) => ({ ...prev, trangThai: e.target.value }))}
            >
              <option value="">Tất cả trạng thái</option>
              {(catalog?.trangThai || []).map((item) => (
                <option key={item.ma} value={item.ma}>
                  {item.ten}
                </option>
              ))}
            </select>
          </label>

          <label className="text-xs font-medium text-slate-600 sm:col-span-2">
            Tìm theo mã phiếu, tiêu đề, mã SV hoặc họ tên SV
            <div className="mt-1 flex gap-2">
              <input
                type="text"
                className="input flex-1"
                placeholder="Nhập mã phiếu, mã SV, tên SV hoặc tiêu đề..."
                value={filters.keyword}
                onChange={(e) => setFilters((prev) => ({ ...prev, keyword: e.target.value }))}
              />
              <button type="submit" className="btn-primary shrink-0">
                <Search size={16} /> Lọc
              </button>
              <button type="button" className="btn-secondary shrink-0" onClick={resetFilters}>
                Đặt lại
              </button>
            </div>
          </label>
        </div>
      </form>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Đang tải danh sách phiếu hỗ trợ..." />
        ) : items.length === 0 ? (
          <EmptyState
            icon={LifeBuoy}
            title="Chưa có phiếu hỗ trợ phù hợp"
            description="Khi sinh viên gửi yêu cầu hỗ trợ hoặc khiếu nại, danh sách sẽ hiển thị tại đây."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] text-left text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã phiếu</th>
                  <th className="th">Sinh viên</th>
                  <th className="th">Loại yêu cầu</th>
                  <th className="th">Tiêu đề</th>
                  <th className="th">Trạng thái</th>
                  <th className="th">Người xử lý</th>
                  <th className="th">Ngày gửi</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/70">
                    <td className="td font-mono text-xs font-semibold text-slate-700">
                      {item.maPhieu}
                    </td>
                    <td className="td">
                      <p className="font-medium text-slate-900">{item.hoTenSinhVien}</p>
                      <p className="text-xs text-slate-500">{item.maSinhVien}</p>
                    </td>
                    <td className="td">
                      <Badge tone="indigo" dot={false}>
                        {item.tenLoaiYeuCau}
                      </Badge>
                    </td>
                    <td className="td max-w-sm">
                      <p className="font-medium text-slate-900">{item.tieuDe}</p>
                      <div className="mt-1 flex flex-wrap gap-2 text-xs text-slate-500">
                        {item.monHoc && <span>Môn: {item.monHoc.ma}</span>}
                        {item.hocKy && <span>· {item.hocKy.ten}</span>}
                        {item.diem && <span>· Lớp {item.diem.maLop}</span>}
                      </div>
                    </td>
                    <td className="td">
                      <Badge tone={STATUS_TONE[item.trangThai] || "slate"}>
                        {item.tenTrangThai}
                      </Badge>
                    </td>
                    <td className="td text-xs text-slate-600">
                      {item.nguoiXuLyUsername || "—"}
                    </td>
                    <td className="td whitespace-nowrap text-xs text-slate-600">
                      {formatDateTime(item.createdAt)}
                    </td>
                    <td className="td text-right">
                      <button
                        type="button"
                        className="btn-secondary py-1.5 text-xs"
                        onClick={() => openTicketDetail(item.id)}
                      >
                        <Eye size={14} /> Xử lý
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {pageData && pageData.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <span className="text-xs text-slate-500">
              Trang {pageData.number + 1} / {pageData.totalPages} · Tổng {pageData.totalElements} phiếu
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-secondary"
                disabled={pageData.first}
                onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
              >
                Trước
              </button>
              <button
                type="button"
                className="btn-secondary"
                disabled={pageData.last}
                onClick={() => setPageNumber((p) => p + 1)}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Admin Ticket Detail & Processing Modal */}
      <Modal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={
          selectedTicket
            ? `Xử lý phiếu ${selectedTicket.maPhieu} — ${selectedTicket.hoTenSinhVien} (${selectedTicket.maSinhVien})`
            : "Xử lý phiếu hỗ trợ"
        }
        description={
          selectedTicket
            ? `${selectedTicket.tenLoaiYeuCau} · Gửi lúc ${formatDateTime(selectedTicket.createdAt)}`
            : ""
        }
        size="xl"
      >
        {detailLoading ? (
          <Spinner label="Đang tải chi tiết phiếu hỗ trợ..." />
        ) : detailError && !selectedTicket ? (
          <Alert tone="error">{detailError}</Alert>
        ) : (
          selectedTicket && (
            <div className="space-y-6">
              {detailError && (
                <Alert tone="error" onClose={() => setDetailError("")}>
                  {detailError}
                </Alert>
              )}

              {/* Status Update Panel */}
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-slate-600">Trạng thái hiện tại:</span>
                    <Badge tone={STATUS_TONE[selectedTicket.trangThai] || "slate"}>
                      {selectedTicket.tenTrangThai}
                    </Badge>
                  </div>
                  <span className="text-xs text-slate-500">
                    Người xử lý: <strong>{selectedTicket.nguoiXuLyUsername || "Chưa phân công"}</strong>
                  </span>
                </div>

                {selectedTicket.trangThaiTiepTheoHopLe?.length > 0 && (
                  <form
                    onSubmit={handleStatusUpdate}
                    className="mt-3 grid gap-3 border-t border-slate-200 pt-3 sm:grid-cols-12"
                  >
                    <div className="sm:col-span-4">
                      <Field label="Chuyển sang trạng thái" required>
                        <select
                          className="input w-full text-sm"
                          value={nextStatus}
                          onChange={(e) => setNextStatus(e.target.value)}
                          required
                        >
                          {selectedTicket.trangThaiTiepTheoHopLe.map((code) => (
                            <option key={code} value={code}>
                              {statusNameMap[code] || code}
                            </option>
                          ))}
                        </select>
                      </Field>
                    </div>
                    <div className="sm:col-span-6">
                      <Field label="Ghi chú chuyển trạng thái (tùy chọn)">
                        <input
                          type="text"
                          maxLength={500}
                          className="input w-full text-sm"
                          placeholder="Nhập lý do hoặc kết quả xử lý..."
                          value={statusNote}
                          onChange={(e) => setStatusNote(e.target.value)}
                        />
                      </Field>
                    </div>
                    <div className="flex items-end sm:col-span-2">
                      <button
                        type="submit"
                        className="btn-primary w-full justify-center py-2 text-xs"
                        disabled={statusUpdating}
                      >
                        <CheckCircle2 size={14} />{" "}
                        {statusUpdating ? "Đang lưu..." : "Cập nhật"}
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {/* Original Immutable Content */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                  <h3 className="text-sm font-semibold text-slate-900">
                    {selectedTicket.tieuDe}
                  </h3>
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">
                    Nội dung gốc sinh viên gửi
                  </span>
                </div>
                <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">
                  {selectedTicket.noiDungGoc}
                </p>

                {(selectedTicket.monHoc || selectedTicket.hocKy || selectedTicket.diem) && (
                  <div className="mt-4 grid gap-2 rounded-lg bg-slate-50 p-3 text-xs text-slate-700 sm:grid-cols-3">
                    {selectedTicket.monHoc && (
                      <div>
                        <span className="font-semibold text-slate-500">Môn học: </span>
                        {selectedTicket.monHoc.ma} - {selectedTicket.monHoc.ten}
                      </div>
                    )}
                    {selectedTicket.hocKy && (
                      <div>
                        <span className="font-semibold text-slate-500">Học kỳ: </span>
                        {selectedTicket.hocKy.ten} ({selectedTicket.hocKy.namHoc})
                      </div>
                    )}
                    {selectedTicket.diem && (
                      <div>
                        <span className="font-semibold text-slate-500">Bản ghi điểm: </span>
                        Lớp {selectedTicket.diem.maLop} · QT:{" "}
                        {formatDiem(selectedTicket.diem.diemQuaTrinh)} · CK:{" "}
                        {formatDiem(selectedTicket.diem.diemCuoiKy)} · TK:{" "}
                        {formatDiem(selectedTicket.diem.diemTongKet)}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Attachments */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <Paperclip size={16} className="text-slate-500" />
                  Tệp minh chứng đính kèm ({selectedTicket.tepDinhKem?.length || 0})
                </h4>
                {!selectedTicket.tepDinhKem?.length ? (
                  <p className="mt-2 text-xs text-slate-500">Không có tệp đính kèm.</p>
                ) : (
                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                    {selectedTicket.tepDinhKem.map((att) => (
                      <div
                        key={att.id}
                        className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2 text-xs"
                      >
                        <div className="min-w-0">
                          <p className="truncate font-medium text-slate-800">{att.tenTepGoc}</p>
                          <p className="text-slate-400">
                            {formatFileSize(att.kichThuoc)} · {att.contentType}
                          </p>
                        </div>
                        <button
                          type="button"
                          className="btn-secondary shrink-0 py-1 text-xs"
                          onClick={() =>
                            downloadAttachmentFile(selectedTicket.id, att, setDetailError)
                          }
                        >
                          <Download size={13} /> Tải xuống
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Two columns: Discussion Thread & Status History */}
              <div className="grid gap-6 lg:grid-cols-5">
                <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 lg:col-span-3">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <MessageSquare size={16} className="text-slate-500" />
                    Trao đổi trên phiếu ({selectedTicket.traoDoi?.length || 0})
                  </h4>

                  {!selectedTicket.traoDoi?.length ? (
                    <p className="py-4 text-center text-xs text-slate-500">
                      Chưa có trao đổi nào trên phiếu này.
                    </p>
                  ) : (
                    <div className="max-h-72 space-y-3 overflow-y-auto pr-1">
                      {selectedTicket.traoDoi.map((msg) => {
                        const isAdmin = msg.vaiTroNguoiGui === "QUAN_TRI";
                        return (
                          <div
                            key={msg.id}
                            className={`rounded-xl border p-3 text-sm ${
                              isAdmin
                                ? "border-indigo-100 bg-indigo-50/60"
                                : "border-slate-200 bg-slate-50/70"
                            }`}
                          >
                            <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                              <span className="font-semibold text-slate-800">
                                {msg.tenHienThiNguoiGui}
                              </span>
                              <div className="flex items-center gap-2">
                                <Badge tone={isAdmin ? "indigo" : "green"} dot={false}>
                                  {isAdmin ? "Phòng Đào tạo" : "Sinh viên"}
                                </Badge>
                                <span className="text-slate-400">
                                  {formatDateTime(msg.createdAt)}
                                </span>
                              </div>
                            </div>
                            <p className="mt-2 whitespace-pre-line text-slate-700">
                              {msg.noiDung}
                            </p>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {selectedTicket.coThePhanHoi ? (
                    <form onSubmit={handleReplySubmit} className="space-y-2 border-t border-slate-100 pt-3">
                      <textarea
                        rows={3}
                        maxLength={2000}
                        className="input w-full text-sm"
                        placeholder="Nhập phản hồi cho sinh viên..."
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        required
                      />
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-400">
                          {replyText.trim().length}/2000 ký tự
                        </span>
                        <button
                          type="submit"
                          className="btn-primary py-1.5 text-xs"
                          disabled={replySubmitting}
                        >
                          <Send size={14} />{" "}
                          {replySubmitting ? "Đang gửi..." : "Gửi phản hồi"}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <Alert tone="info">
                      Phiếu đang ở trạng thái <strong>{selectedTicket.tenTrangThai}</strong> nên đã
                      khóa tính năng gửi thêm phản hồi.
                    </Alert>
                  )}
                </div>

                <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 lg:col-span-2">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <History size={16} className="text-slate-500" />
                    Lịch sử trạng thái ({selectedTicket.lichSuTrangThai?.length || 0})
                  </h4>

                  <ol className="relative ml-2 border-l border-slate-200 space-y-4">
                    {(selectedTicket.lichSuTrangThai || []).map((hist) => (
                      <li key={hist.id} className="ml-4 text-xs">
                        <span className="absolute -left-1.5 mt-1 h-3 w-3 rounded-full border border-white bg-emerald-600" />
                        <div className="flex flex-wrap items-center gap-1.5">
                          {hist.tenTrangThaiCu && (
                            <>
                              <span className="text-slate-500">{hist.tenTrangThaiCu}</span>
                              <span className="text-slate-400">→</span>
                            </>
                          )}
                          <Badge tone={STATUS_TONE[hist.trangThaiMoi] || "slate"} dot={false}>
                            {hist.tenTrangThaiMoi}
                          </Badge>
                        </div>
                        <p className="mt-1 text-slate-600">
                          Thực hiện bởi: <strong>{hist.tenNguoiThucHien}</strong> (
                          {hist.vaiTroNguoiThucHien === "QUAN_TRI" ? "Quản trị" : "Sinh viên"})
                        </p>
                        <p className="mt-0.5 flex items-center gap-1 text-slate-400">
                          <Clock size={12} /> {formatDateTime(hist.thoiDiem)}
                        </p>
                        {hist.ghiChu && (
                          <p className="mt-1 rounded bg-slate-50 p-2 text-slate-700">
                            {hist.ghiChu}
                          </p>
                        )}
                      </li>
                    ))}
                  </ol>
                </div>
              </div>
            </div>
          )
        )}
      </Modal>
    </div>
  );
}
