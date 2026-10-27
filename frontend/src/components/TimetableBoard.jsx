import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import TimetableGrid, { TimetableLegend } from "./TimetableGrid";
import { addDays, diffInDays, formatDate, formatWeekRange, startOfWeek } from "../lib/format";

function clamp(value, min, max) {
  if (value < min) return min;
  if (value > max) return max;
  return value;
}

export default function TimetableBoard({ items, hocKy, announcements = [] }) {
  const firstWeek = startOfWeek(hocKy?.ngayBatDau) || startOfWeek(new Date());
  const semesterEndWeek = startOfWeek(hocKy?.ngayKetThuc);
  const lastWeek =
    semesterEndWeek && semesterEndWeek >= firstWeek ? semesterEndWeek : addDays(firstWeek, 7 * 17);

  const [selectedWeek, setSelectedWeek] = useState(null);

  const activeWeek = selectedWeek || clamp(startOfWeek(new Date()), firstWeek, lastWeek);
  const totalWeeks = Math.max(1, Math.round(diffInDays(firstWeek, lastWeek) / 7) + 1);
  const activeIndex = clamp(Math.round(diffInDays(firstWeek, activeWeek) / 7), 0, totalWeeks - 1);

  return (
    <div>
      <div className="flex flex-col gap-3 border-b border-slate-100 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="rounded-lg border border-slate-200 p-2 text-slate-500 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setSelectedWeek(addDays(activeWeek, -7))}
            disabled={activeIndex === 0}
            title="Tuần trước"
          >
            <ChevronLeft size={17} />
          </button>
          <div className="min-w-[210px] text-center">
            <p className="text-sm font-semibold text-slate-800">
              Tuần {activeIndex + 1}
              <span className="ml-1 font-normal text-slate-500">· {formatWeekRange(activeWeek)}</span>
            </p>
            <p className="text-xs text-slate-400">
              {hocKy ? `${hocKy.ten} - ${hocKy.namHoc}` : "Toàn bộ học kỳ"}
            </p>
          </div>
          <button
            type="button"
            className="rounded-lg border border-slate-200 p-2 text-slate-500 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setSelectedWeek(addDays(activeWeek, 7))}
            disabled={activeIndex >= totalWeeks - 1}
            title="Tuần sau"
          >
            <ChevronRight size={17} />
          </button>
          <button
            type="button"
            className="btn-secondary px-2.5 py-1.5 text-xs"
            onClick={() => setSelectedWeek(null)}
          >
            Hôm nay
          </button>
        </div>

        <div className="text-xs text-slate-500">
          Tuần {activeIndex + 1}/{totalWeeks}
          {hocKy?.ngayBatDau && hocKy?.ngayKetThuc && (
            <span className="ml-2 text-slate-400">
              ({formatDate(hocKy.ngayBatDau)} - {formatDate(hocKy.ngayKetThuc)})
            </span>
          )}
        </div>
      </div>

      <div className="mt-4">
        <TimetableLegend items={items} />
      </div>

      <div className="mt-4">
        <TimetableGrid items={items} weekStart={activeWeek} announcements={announcements} />
      </div>

      <p className="mt-4 text-xs text-slate-400">
        {hocKy?.ngayKetThuc
          ? `Lịch học lặp lại hằng tuần đến hết học kỳ (${formatDate(hocKy.ngayKetThuc)}).`
          : "Lịch học lặp lại hằng tuần trong học kỳ."}
      </p>
    </div>
  );
}
