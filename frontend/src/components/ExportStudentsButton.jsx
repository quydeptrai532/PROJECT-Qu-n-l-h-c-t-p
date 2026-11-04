import { useState } from "react";
import { Download } from "lucide-react";
import api, { getErrorMessage } from "../api/client";

async function docThongBaoLoi(err) {
    // Khi tải file (responseType blob), thân phản hồi lỗi cũng là Blob nên cần đọc ra JSON.
    const data = err?.response?.data;
    if (data instanceof Blob) {
        try {
            const parsed = JSON.parse(await data.text());
            if (parsed?.message) return parsed.message;
        } catch {
            // Không phải JSON, dùng thông báo mặc định bên dưới.
        }
    }
    return getErrorMessage(err);
}

/** Nút xuất danh sách sinh viên của lớp học phần ra CSV (TASK-LC-04). */
export default function ExportStudentsButton({ lopId, maLop, soSinhVien, loading, onSuccess, onError }) {
    const [exporting, setExporting] = useState(false);

    const xuatDanhSach = async () => {
        setExporting(true);
        try {
            const response = await api.get(`/giang-vien/lop-hoc-phan/${lopId}/sinh-vien/csv`, { responseType: "blob" });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
            const link = document.createElement("a");
            link.href = url;
            link.download = `danh-sach-sinh-vien-${maLop || lopId}.csv`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            onSuccess?.(`Đã xuất danh sách ${soSinhVien} sinh viên ra file CSV`);
        } catch (err) {
            onError?.(await docThongBaoLoi(err));
        } finally {
            setExporting(false);
        }
    };

    return (
        <button
            type="button"
            className="btn-secondary px-3 py-1.5 text-xs"
            disabled={loading || soSinhVien === 0 || exporting}
            title={soSinhVien === 0 ? "Lớp chưa có sinh viên để xuất" : "Tải danh sách sinh viên (CSV, UTF-8)"}
            onClick={xuatDanhSach}
        >
            <Download size={14} />
            {exporting ? "Đang xuất..." : "Xuất CSV"}
        </button>
    );
}