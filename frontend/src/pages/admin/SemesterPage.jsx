import { CalendarRange } from "lucide-react";
import CrudPage from "../../components/CrudPage";
import { Badge } from "../../components/ui";
import { formatDate } from "../../lib/format";

export default function SemesterPage() {
  return (
    <CrudPage
      icon={CalendarRange}
      title="Học kỳ"
      description="Cấu hình thời gian học, thời gian mở đăng ký và giới hạn tín chỉ cho từng học kỳ."
      endpoint="/admin/hoc-ky"
      searchKeys={["ten", "namHoc"]}
      emptyValues={{
        ten: "",
        namHoc: "",
        ngayBatDau: "",
        ngayKetThuc: "",
        dangKyTu: "",
        dangKyDen: "",
        soTinChiToiThieu: "12",
        soTinChiToiDa: "24",
      }}
      columns={[
        {
          key: "ten",
          label: "Học kỳ",
          render: (row) => (
            <div>
              <p className="font-medium text-slate-800">{row.ten}</p>
              <p className="text-xs text-slate-500">{row.namHoc}</p>
            </div>
          ),
        },
        {
          key: "thoiGianHoc",
          label: "Thời gian học",
          render: (row) => `${formatDate(row.ngayBatDau)} - ${formatDate(row.ngayKetThuc)}`,
        },
        {
          key: "thoiGianDangKy",
          label: "Đăng ký",
          render: (row) => (
            <div>
              <p>{formatDate(row.dangKyTu)} - {formatDate(row.dangKyDen)}</p>
              <Badge tone={row.dangMoDangKy ? "green" : "slate"} className="mt-1">
                {row.dangMoDangKy ? "Đang mở" : "Đã đóng"}
              </Badge>
            </div>
          ),
        },
        {
          key: "tinChi",
          label: "Tín chỉ",
          render: (row) => `${row.soTinChiToiThieu} - ${row.soTinChiToiDa}`,
        },
      ]}
      fields={[
        { name: "ten", label: "Tên học kỳ", required: true, placeholder: "VD: Học kỳ 1" },
        { name: "namHoc", label: "Năm học", required: true, placeholder: "VD: 2026-2027" },
        { name: "ngayBatDau", label: "Ngày bắt đầu", type: "date", required: true },
        { name: "ngayKetThuc", label: "Ngày kết thúc", type: "date", required: true },
        { name: "dangKyTu", label: "Mở đăng ký", type: "date", required: true },
        { name: "dangKyDen", label: "Đóng đăng ký", type: "date", required: true },
        {
          name: "soTinChiToiThieu",
          label: "Tín chỉ tối thiểu",
          type: "number",
          required: true,
          min: 0,
        },
        {
          name: "soTinChiToiDa",
          label: "Tín chỉ tối đa",
          type: "number",
          required: true,
          min: 1,
        },
      ]}
    />
  );
}
