import { Users } from "lucide-react";
import CrudPage from "../../components/CrudPage";
import { useApi } from "../../lib/useApi";

export default function AdminClassPage() {
  const { data: nganhList } = useApi("/admin/nganh");

  return (
    <CrudPage
      icon={Users}
      title="Lớp hành chính"
      description="Lớp hành chính dùng để quản lý sinh viên theo ngành và khóa."
      endpoint="/admin/lop-hanh-chinh"
      searchKeys={["ma", "ten", "nganhTen", "khoaTen"]}
      emptyValues={{ ma: "", ten: "", nganhId: "" }}
      columns={[
        { key: "ma", label: "Mã lớp" },
        { key: "ten", label: "Tên lớp" },
        { key: "nganhTen", label: "Ngành" },
        { key: "khoaTen", label: "Khoa" },
      ]}
      fields={[
        { name: "ma", label: "Mã lớp", required: true, placeholder: "VD: CNPM01" },
        { name: "ten", label: "Tên lớp", required: true },
        {
          name: "nganhId",
          label: "Ngành",
          type: "select",
          numeric: true,
          required: true,
          options: (nganhList || []).map((nganh) => ({ value: nganh.id, label: `${nganh.ma} - ${nganh.ten}` })),
        },
      ]}
    />
  );
}
