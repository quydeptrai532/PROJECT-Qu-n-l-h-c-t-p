import { Building2 } from "lucide-react";
import CrudPage from "../../components/CrudPage";

export default function FacultyPage() {
  return (
    <CrudPage
      icon={Building2}
      title="Khoa"
      description="Danh mục khoa của trường. Không thể xóa khoa đang được tham chiếu."
      endpoint="/admin/khoa"
      searchKeys={["ma", "ten"]}
      emptyValues={{ ma: "", ten: "" }}
      columns={[
        { key: "ma", label: "Mã khoa" },
        { key: "ten", label: "Tên khoa" },
      ]}
      fields={[
        { name: "ma", label: "Mã khoa", required: true, placeholder: "VD: CNTT" },
        { name: "ten", label: "Tên khoa", required: true, placeholder: "VD: Công nghệ thông tin" },
      ]}
    />
  );
}
