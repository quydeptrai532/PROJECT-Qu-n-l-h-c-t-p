import { useEffect, useState } from "react";
import { CalendarDays } from "lucide-react";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import TimetableGrid from "../../components/TimetableGrid";
import { useApi } from "../../lib/useApi";

export default function TimetablePage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data, loading, error } = useApi("/sinh-vien/me/thoi-khoa-bieu", params);

  return (
    <div>
      <PageHeader
        icon={CalendarDays}
        title="Thời khóa biểu"
        description="Thời khóa biểu theo tuần của các học phần đã đăng ký."
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
            title="Chưa có lịch học"
            description="Đăng ký học phần để thời khóa biểu hiển thị tại đây."
          />
        ) : (
          <TimetableGrid items={data} />
        )}
      </div>
    </div>
  );
}
