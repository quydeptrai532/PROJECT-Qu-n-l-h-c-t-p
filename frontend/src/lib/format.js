export const THU_LABEL = {
  2: "Thứ 2",
  3: "Thứ 3",
  4: "Thứ 4",
  5: "Thứ 5",
  6: "Thứ 6",
  7: "Thứ 7",
  8: "Chủ nhật",
};

export function formatDate(value) {
  if (!value) return "—";
  const [year, month, day] = String(value).slice(0, 10).split("-");
  if (!year || !month || !day) return String(value);
  return `${day}/${month}/${year}`;
}

export function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function lichHoc(thu, tietBatDau, soTiet) {
  if (!thu || !tietBatDau || !soTiet) return "—";
  return `${THU_LABEL[thu] || `Thứ ${thu}`}, tiết ${tietBatDau}-${tietBatDau + soTiet - 1}`;
}

export function formatDiem(value) {
  if (value === null || value === undefined || value === "") return "—";
  return Number(value).toFixed(1);
}

export function formatNumber(value) {
  if (value === null || value === undefined) return "0";
  return new Intl.NumberFormat("vi-VN").format(value);
}

export const SUBJECT_COLORS = [
  "bg-indigo-100 border-indigo-300 text-indigo-900",
  "bg-emerald-100 border-emerald-300 text-emerald-900",
  "bg-amber-100 border-amber-300 text-amber-900",
  "bg-sky-100 border-sky-300 text-sky-900",
  "bg-rose-100 border-rose-300 text-rose-900",
  "bg-violet-100 border-violet-300 text-violet-900",
  "bg-teal-100 border-teal-300 text-teal-900",
  "bg-orange-100 border-orange-300 text-orange-900",
];
