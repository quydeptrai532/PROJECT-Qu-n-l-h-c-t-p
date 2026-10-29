import { useMemo } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, CalendarDays, Clock, Layers, MapPin, Users } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Badge, EmptyState, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { lichHoc, THU_LABEL } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const CHART_TOOLTIP = {
  borderRadius: 12,
  border: "1px solid #e2e8f0",
  fontSize: 12,
  boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
};

export default function LecturerDashboard() {
  const { user } = useAuth();
  const { data: hocKyList } = useApi("/hoc-ky");
  const { data: lopList, loading: loadingLop } = useApi("/giang-vien/me/lop-hoc-phan");
  const { data: thoiKhoaBieu, loading: loadingTkb } = useApi("/giang-vien/me/thoi-khoa-bieu");

  const hocKy = hocKyList?.[0];
  const lopHienTai = useMemo(() => {
    const list = lopList || [];
    if (!hocKy) return list;
    const filtered = list.filter((item) => item.hocKyId === hocKy.id);
    return filtered.length ? filtered : list;
  }, [lopList, hocKy]);

  const tongSinhVien = lopHienTai.reduce((sum, item) => sum + item.siSoHienTai, 0);

  const homNay = (() => {
    const day = new Date().getDay();
    return day === 0 ? 8 : day + 1;
  })();

  const lichHomNay = useMemo(
    () =>
      (thoiKhoaBieu || [])
        .filter((item) => item.thu === homNay)
        .sort((a, b) => a.tietBatDau - b.tietBatDau),
    [thoiKhoaBieu, homNay]
  );

  const chartData = useMemo(
    () =>
      lopHienTai.map((item) => ({
        name: item.ma,
        "Đã đăng ký": item.siSoHienTai,
        "Sĩ số tối đa": item.siSoToiDa,
      })),
    [lopHienTai]
  );

  return (
    <div>
      <section className="dashboard-hero relative overflow-hidden rounded-[24px] border border-[#dce6d5] p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-10 -top-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-24 left-24 h-56 w-56 rounded-full bg-emerald-300/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="hero-muted text-[11px] font-semibold uppercase tracking-[0.2em]">
              Không gian giảng dạy
            </p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">
              Xin chào, {user?.hoTen || user?.username}
            </h2>
            <p className="hero-muted mt-2 max-w-xl text-sm leading-relaxed">
              Bạn đang phụ trách {lopHienTai.length} lớp học phần với {tongSinhVien} sinh viên trong{" "}
              {hocKy?.ten || "học kỳ hiện tại"}.
            </p>
            <div className="mt-5 flex flex-wrap gap-2">
              <Link
                to="/lop-cua-toi"
                className="hero-action inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold shadow-sm transition"
              >
                <Layers size={16} /> Lớp học phần
              </Link>
              <Link
                to="/lop-cua-toi/thoi-khoa-bieu"
                className="inline-flex items-center gap-2 rounded-xl border border-[#cbd8c5] bg-white/60 px-4 py-2.5 text-sm font-medium text-[#34564c] transition hover:bg-white"
              >
                <CalendarDays size={16} /> Lịch giảng dạy
              </Link>
            </div>
          </div>

          <div className="w-full max-w-xs rounded-xl border border-white/80 bg-white/70 p-4">
            <p className="hero-muted text-[10px] font-semibold uppercase tracking-wider">
              Lịch dạy hôm nay
            </p>
            <p className="mt-1 text-lg font-semibold">
              {lichHomNay.length > 0 ? `${lichHomNay.length} buổi dạy` : "Không có lịch"}
            </p>
            <p className="hero-muted mt-2 text-[11px]">{THU_LABEL[homNay] || "Hôm nay"}</p>
          </div>
        </div>
      </section>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Lớp phụ trách" value={lopHienTai.length} icon={Layers} />
        <StatCard label="Sinh viên" value={tongSinhVien} icon={Users} tone="emerald" />
        <StatCard
          label="Buổi dạy / tuần"
          value={(thoiKhoaBieu || []).length}
          icon={CalendarDays}
          tone="amber"
        />
        <StatCard
          label="Buổi dạy hôm nay"
          value={lichHomNay.length}
          hint={THU_LABEL[homNay] || ""}
          icon={Clock}
          tone="sky"
        />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <section className="card p-5 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Sĩ số lớp học phần</h2>
              <p className="mt-0.5 text-xs text-slate-500">So sánh số sinh viên đã đăng ký và sĩ số tối đa</p>
            </div>
            <Badge tone="emerald">{lopHienTai.length} lớp</Badge>
          </div>

          {loadingLop ? (
            <Spinner />
          ) : chartData.length === 0 ? (
            <EmptyState icon={Layers} title="Chưa có lớp học phần" />
          ) : (
            <div className="mt-4 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <RechartsTooltip contentStyle={CHART_TOOLTIP} cursor={{ fill: "rgba(99, 102, 241, 0.06)" }} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="Đã đăng ký" fill="#10b981" radius={[6, 6, 0, 0]} maxBarSize={34} isAnimationActive={false} />
                  <Bar dataKey="Sĩ số tối đa" fill="#cbd5e1" radius={[6, 6, 0, 0]} maxBarSize={34} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="card p-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Lịch dạy hôm nay</h2>
              <p className="mt-0.5 text-xs text-slate-500">{THU_LABEL[homNay] || "Hôm nay"}</p>
            </div>
            <Badge tone="sky">{lichHomNay.length} buổi</Badge>
          </div>

          {loadingTkb ? (
            <Spinner />
          ) : lichHomNay.length === 0 ? (
            <div className="mt-6 flex flex-col items-center gap-2 text-center">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-400 ring-1 ring-slate-100">
                <CalendarDays size={22} />
              </span>
              <p className="text-sm font-medium text-slate-600">Hôm nay không có lịch dạy</p>
              <p className="text-xs text-slate-400">Xem lịch cả tuần trong mục Thời khóa biểu.</p>
            </div>
          ) : (
            <div className="mt-4 space-y-3">
              {lichHomNay.map((item) => (
                <div key={item.lopHocPhanId} className="rounded-xl border border-slate-200/80 p-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-medium text-slate-800">{item.tenMon}</p>
                    <span className="shrink-0 rounded-lg bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700">
                      Tiết {item.tietBatDau}
                    </span>
                  </div>
                  <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
                    <Clock size={13} /> Tiết {item.tietBatDau}-{item.tietBatDau + item.soTiet - 1}
                    <span className="text-slate-300">·</span>
                    <MapPin size={13} /> {item.phong}
                  </p>
                  <p className="mt-0.5 text-[11px] text-slate-400">{item.maLop}</p>
                </div>
              ))}
            </div>
          )}

          <Link
            to="/lop-cua-toi/thoi-khoa-bieu"
            className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-emerald-600 hover:text-emerald-700"
          >
            Xem cả tuần <ArrowRight size={13} />
          </Link>
        </section>
      </div>

      <section className="card mt-6 p-5">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-semibold text-slate-800">Lớp đang phụ trách</h2>
            <p className="mt-0.5 text-xs text-slate-500">Chọn lớp để xem danh sách và nhập điểm</p>
          </div>
          <Link to="/lop-cua-toi" className="text-xs font-medium text-indigo-600 hover:text-indigo-700">
            Xem tất cả
          </Link>
        </div>

        {loadingLop ? (
          <Spinner />
        ) : lopHienTai.length === 0 ? (
          <EmptyState icon={Layers} title="Chưa được phân công lớp" />
        ) : (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {lopHienTai.slice(0, 4).map((lop) => {
              const percent = lop.siSoToiDa ? Math.min(100, (lop.siSoHienTai / lop.siSoToiDa) * 100) : 0;
              return (
                <Link
                  key={lop.id}
                  to={`/lop-cua-toi/${lop.id}/nhap-diem`}
                  className="group rounded-xl border border-slate-200/80 p-4 transition hover:border-indigo-200 hover:bg-indigo-50/30"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-800 group-hover:text-indigo-700">{lop.tenMon}</p>
                      <p className="text-xs text-slate-500">{lop.ma} · {lop.soTinChi} TC</p>
                    </div>
                    <Badge tone="slate">{lop.tenHocKy}</Badge>
                  </div>
                  <p className="mt-2 text-xs text-slate-500">{lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · {lop.phong}</p>
                  <div className="mt-3 flex items-center gap-2">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-200">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-teal-500"
                        style={{ width: `${percent}%` }}
                      />
                    </div>
                    <span className="text-xs text-slate-600">{lop.siSoHienTai}/{lop.siSoToiDa}</span>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
