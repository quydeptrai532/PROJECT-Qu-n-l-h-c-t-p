import { useEffect, useState } from "react";
import { CalendarDays } from "lucide-react";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import TimetableGrid from "../../components/TimetableGrid";
import { useApi } from "../../lib/useApi";

export default function LecturerTimetablePage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data, loading, error } = useApi("/giang-vien/me/thoi-khoa-bieu", params);

  return (
    <div>
      <PageHeader
        icon={CalendarDays}
        title="Thời khóa biểu giảng dạy"
        description="Lịch giảng dạy theo tuần của các lớp được phân công."
        actions={
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
        }
      />

      {error && <Alert tone="error" className="mb-4">{error}</Alert>}

      <div className="card p-4">
        {loading ? (
          <Spinner />
        ) : !data?.length ? (
          <EmptyState
            icon={CalendarDays}
            title="Chưa có lịch giảng dạy"
            description="Lịch sẽ hiển thị khi bạn được phân công lớp học phần."
          />
        ) : (
          <TimetableGrid items={data} />
        )}
      </div>
    </div>
  );
}
