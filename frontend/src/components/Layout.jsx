import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  BookMarked,
  BookOpen,
  Building2,
  CalendarDays,
  CalendarRange,
  ClipboardList,
  GraduationCap,
  Layers,
  LayoutDashboard,
  LogOut,
  Menu,
  Network,
  UserCog,
  UserRound,
  Users,
  X,
} from "lucide-react";
import { ROLE_LABEL, useAuth } from "../context/AuthContext";

const NAV = {
  SINH_VIEN: [
    { to: "/tong-quan", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/dang-ky", label: "Đăng ký học phần", icon: BookOpen },
    { to: "/ke-hoach-hoc-tap", label: "Kế hoạch học tập", icon: CalendarRange },
    { to: "/hoc-phan-cua-toi", label: "Học phần của tôi", icon: ClipboardList },
    { to: "/thoi-khoa-bieu", label: "Thời khóa biểu", icon: CalendarDays },
    { to: "/diem", label: "Điểm & kết quả", icon: GraduationCap },
  ],
  GIANG_VIEN: [
    { to: "/tong-quan", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/lop-cua-toi", label: "Lớp học phần", icon: Layers },
    { to: "/lop-cua-toi/thoi-khoa-bieu", label: "Thời khóa biểu", icon: CalendarDays },
  ],
  QUAN_TRI: [
    { to: "/quan-tri", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/quan-tri/hoc-ky", label: "Học kỳ", icon: CalendarRange },
    { to: "/quan-tri/khoa", label: "Khoa", icon: Building2 },
    { to: "/quan-tri/nganh", label: "Ngành", icon: Network },
    { to: "/quan-tri/lop-hanh-chinh", label: "Lớp hành chính", icon: Users },
    { to: "/quan-tri/mon-hoc", label: "Môn học", icon: BookMarked },
    { to: "/quan-tri/lop-hoc-phan", label: "Lớp học phần", icon: Layers },
    { to: "/quan-tri/sinh-vien", label: "Sinh viên", icon: UserRound },
    { to: "/quan-tri/giang-vien", label: "Giảng viên", icon: UserCog },
  ],
};

const SECTION_LABEL = {
  SINH_VIEN: "Không gian học tập",
  GIANG_VIEN: "Không gian giảng dạy",
  QUAN_TRI: "Quản trị hệ thống",
};

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  const items = NAV[user?.role] || [];
  const allItems = Object.values(NAV).flat();
  const exact = allItems.find((item) => item.to === location.pathname);
  const title = exact
    ? exact.label
    : location.pathname.endsWith("/nhap-diem")
    ? "Nhập điểm"
    : "Hồ sơ cá nhân";

  const homNay = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (
    <div className="min-h-screen">
      {open && (
        <div
          className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-sm lg:hidden"
          onClick={() => setOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col overflow-hidden bg-slate-950 text-slate-300 transition-transform duration-300 lg:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="pointer-events-none absolute -left-24 top-16 h-64 w-64 rounded-full bg-indigo-600/20 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 bottom-0 h-64 w-64 rounded-full bg-violet-600/15 blur-3xl" />

        <div className="relative flex h-16 items-center gap-3 border-b border-white/10 px-5">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white shadow-lg shadow-indigo-900/40">
            <GraduationCap size={20} />
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold tracking-wide text-white">EduReg</p>
            <p className="truncate text-[11px] text-slate-400">Quản lý đào tạo</p>
          </div>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="ml-auto rounded-lg p-1.5 text-slate-400 hover:bg-white/10 hover:text-white lg:hidden"
            aria-label="Đóng menu"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="relative flex-1 space-y-1 overflow-y-auto px-3 py-4">
          <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500">
            {SECTION_LABEL[user?.role] || "Menu"}
          </p>
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/quan-tri" || item.to === "/lop-cua-toi"}
              className={({ isActive }) =>
                `group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-all duration-200 ${
                  isActive
                    ? "bg-gradient-to-r from-indigo-500/90 to-violet-500/80 font-medium text-white shadow-lg shadow-indigo-950/40"
                    : "text-slate-300/90 hover:bg-white/5 hover:text-white"
                }`
              }
            >
              <item.icon size={18} className="shrink-0" />
              <span className="truncate">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="relative space-y-2 border-t border-white/10 p-3">
          <NavLink
            to="/ho-so"
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2 text-sm transition-colors ${
                isActive ? "bg-white/10 text-white" : "text-slate-300 hover:bg-white/5 hover:text-white"
              }`
            }
          >
            <UserRound size={18} className="shrink-0" />
            Hồ sơ cá nhân
          </NavLink>
          <div className="rounded-xl bg-white/5 p-3 ring-1 ring-white/10">
            <div className="flex items-center gap-2.5">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-indigo-400 to-violet-500 text-sm font-semibold text-white">
                {(user?.hoTen || user?.username || "?").slice(0, 1).toUpperCase()}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-white">{user?.hoTen || user?.username}</p>
                <p className="truncate text-[11px] text-slate-400">
                  {user?.ma} · {ROLE_LABEL[user?.role]}
                </p>
              </div>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-white"
                title="Đăng xuất"
              >
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-slate-200/70 bg-white/80 backdrop-blur-xl">
          <div className="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
            <button
              type="button"
              onClick={() => setOpen(true)}
              className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 lg:hidden"
              aria-label="Mở menu"
            >
              <Menu size={20} />
            </button>
            <div className="min-w-0 flex-1">
              <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-indigo-500">
                EduReg / {ROLE_LABEL[user?.role]}
              </p>
              <h1 className="truncate text-base font-semibold text-slate-900">{title}</h1>
            </div>

            <span className="hidden items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-500 shadow-sm xl:inline-flex">
              <CalendarDays size={14} className="text-indigo-500" />
              <span className="capitalize">{homNay}</span>
            </span>

            <div className="flex items-center gap-3">
              <div className="hidden text-right sm:block">
                <p className="max-w-[220px] truncate text-sm font-medium text-slate-800">
                  {user?.hoTen || user?.username}
                </p>
                <p className="text-[11px] text-slate-500">
                  {user?.ma} · {ROLE_LABEL[user?.role]}
                </p>
              </div>
              <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 text-sm font-semibold text-white shadow-md shadow-indigo-600/20">
                {(user?.hoTen || user?.username || "?").slice(0, 1).toUpperCase()}
              </span>
            </div>
          </div>
        </header>

        <main className="relative mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-72 bg-gradient-to-b from-indigo-50/80 via-slate-50/40 to-transparent" />
          <div key={location.pathname} className="page-enter">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
