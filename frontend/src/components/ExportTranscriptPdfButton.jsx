import { useState } from "react";
import { FileDown } from "lucide-react";
import api, { getErrorMessage } from "../api/client";

async function docThongBaoLoi(err) {
  const data = err?.response?.data;
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text());
      if (parsed?.message) return parsed.message;
    } catch {
      // Phản hồi lỗi không phải JSON, dùng thông báo mặc định bên dưới.
    }
  }
  return getErrorMessage(err);
}

function layTenTepTuHeader(headers, macDinh) {
  const disposition = headers?.["content-disposition"] || headers?.["Content-Disposition"];
  if (!disposition) return macDinh;

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;\s]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i);
  if (asciiMatch?.[1]) {
    return asciiMatch[1].trim();
  }
  return macDinh;
}

/** Nút xuất bảng điểm cá nhân của sinh viên đang đăng nhập dưới dạng PDF (QLSV-62 / TASK-ST-06). */
export default function ExportTranscriptPdfButton({
  hocKyId,
  disabled = false,
  label = "Xuất bảng điểm PDF",
  onSuccess,
  onError,
}) {
  const [exporting, setExporting] = useState(false);

  const xuatBangDiem = async () => {
    setExporting(true);
    onError?.("");
    onSuccess?.("");
    try {
      const params = {};
      if (hocKyId) {
        params.hocKyId = hocKyId;
      }
      const response = await api.get("/sinh-vien/me/bang-diem/pdf", {
        params,
        responseType: "blob",
      });
      const tenTep = layTenTepTuHeader(
        response.headers,
        hocKyId ? `bang-diem-hk${hocKyId}.pdf` : "bang-diem-ca-nhan.pdf"
      );
      const blob = new Blob([response.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = tenTep;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      onSuccess?.(`Đã tải xuống bảng điểm PDF (${tenTep})`);
    } catch (err) {
      onError?.(await docThongBaoLoi(err));
    } finally {
      setExporting(false);
    }
  };

  return (
    <button
      type="button"
      className="btn-primary"
      disabled={disabled || exporting}
      onClick={xuatBangDiem}
    >
      <FileDown size={16} />
      {exporting ? "Đang xuất PDF..." : label}
    </button>
  );
}
