import { Network } from "lucide-react";
import CrudPage from "../../components/CrudPage";
import { useApi } from "../../lib/useApi";

export default function MajorPage() {
  const { data: khoaList } = useApi("/admin/khoa");

  return (
    <CrudPage
      icon={Network}
      title="Ngành đào tạo"
      description="Mỗi ngành thuộc một khoa. Dùng để phân loại sinh viên và chương trình đào tạo."
      endpoint="/admin/nganh"
      searchKeys={["ma", "ten", "khoaTen"]}
      emptyValues={{ ma: "", ten: "", khoaId: "" }}
      columns={[
        { key: "ma", label: "Mã ngành" },
        { key: "ten", label: "Tên ngành" },
        { key: "khoaTen", label: "Khoa" },
      ]}
      fields={[
        { name: "ma", label: "Mã ngành", required: true, placeholder: "VD: CNPM" },
        { name: "ten", label: "Tên ngành", required: true },
        {
          name: "khoaId",
          label: "Khoa",
          type: "select",
          numeric: true,
          required: true,
          options: (khoaList || []).map((khoa) => ({ value: khoa.id, label: `${khoa.ma} - ${khoa.ten}` })),
        },
      ]}
    />
  );
}
