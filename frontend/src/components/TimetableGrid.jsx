import { Fragment, useMemo } from "react";
import { SUBJECT_COLORS, THU_LABEL } from "../lib/format";

const DAYS = [2, 3, 4, 5, 6, 7];
const PERIODS = Array.from({ length: 12 }, (_, index) => index + 1);

export default function TimetableGrid({ items }) {
  const jsDay = new Date().getDay();
  const today = jsDay === 0 ? 8 : jsDay + 1;
  const monHoc = useMemo(() => [...new Set((items || []).map((item) => item.tenMon))], [items]);
  const mauTheoMon = (tenMon) =>
    SUBJECT_COLORS[Math.max(0, monHoc.indexOf(tenMon)) % SUBJECT_COLORS.length];

  return (
    <>
      <div className="overflow-x-auto">
        <div className="min-w-[760px]">
          <div
            className="grid"
            style={{
              gridTemplateColumns: "64px repeat(6, minmax(0, 1fr))",
              gridTemplateRows: "auto repeat(12, minmax(54px, auto))",
            }}
          >
            <div className="border-b border-slate-200 px-2 py-2 text-center text-[11px] font-semibold uppercase text-slate-400">
              Tiết
            </div>
            {DAYS.map((day) => (
              <div
                key={day}
                className={`border-b border-l border-slate-200 px-2 py-2 text-center text-xs font-semibold ${
                  day === today ? "bg-indigo-50 text-indigo-700" : "text-slate-600"
                }`}
                style={{ gridColumn: day, gridRow: 1 }}
              >
                {THU_LABEL[day]}
                {day === today && (
                  <span className="ml-1 inline-block h-1.5 w-1.5 rounded-full bg-indigo-500 align-middle" />
                )}
              </div>
            ))}

            {PERIODS.map((period) => (
              <Fragment key={period}>
                <div
                  className="border-b border-slate-100 py-1 text-center text-[11px] text-slate-400"
                  style={{ gridColumn: 1, gridRow: period + 1 }}
                >
                  {period}
                </div>
                {DAYS.map((day) => (
                  <div
                    key={`${day}-${period}`}
                    className={`border-b border-l border-slate-100 ${day === today ? "bg-indigo-50/40" : ""}`}
                    style={{ gridColumn: day, gridRow: period + 1 }}
                  />
                ))}
              </Fragment>
            ))}

            {(items || []).map((item) => (
              <div
                key={item.lopHocPhanId}
                className={`z-10 m-0.5 overflow-hidden rounded-lg border border-l-4 px-2 py-1.5 text-xs shadow-sm transition-shadow duration-200 hover:shadow-md ${mauTheoMon(item.tenMon)}`}
                style={{
                  gridColumn: item.thu,
                  gridRow: `${item.tietBatDau + 1} / span ${item.soTiet}`,
                }}
              >
                <p className="line-clamp-2 font-semibold leading-snug">{item.tenMon}</p>
                <p className="mt-0.5 opacity-80">Phòng {item.phong}</p>
                <p className="opacity-70">
                  Tiết {item.tietBatDau}-{item.tietBatDau + item.soTiet - 1}
                </p>
                <p className="opacity-70">{item.maLop}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
        {monHoc.map((tenMon) => (
          <span
            key={tenMon}
            className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs ${mauTheoMon(tenMon)}`}
          >
            <span className="h-1.5 w-1.5 rounded-full bg-current opacity-60" />
            {tenMon}
          </span>
        ))}
      </div>
    </>
  );
}
