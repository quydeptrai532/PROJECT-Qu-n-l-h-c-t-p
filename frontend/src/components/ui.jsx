import { useEffect } from "react";
import { createPortal } from "react-dom";
import { AlertCircle, CheckCircle2, Info, Loader2, X } from "lucide-react";

const BADGE_TONES = {
  slate: "bg-slate-100 text-slate-700 ring-slate-200",
  indigo: "bg-indigo-50 text-indigo-700 ring-indigo-200",
  green: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  red: "bg-rose-50 text-rose-700 ring-rose-200",
  amber: "bg-amber-50 text-amber-800 ring-amber-200",
  sky: "bg-sky-50 text-sky-700 ring-sky-200",
};

export function Badge({ tone = "slate", children, className = "", dot = true }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${BADGE_TONES[tone]} ${className}`}
    >
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />}
      {children}
    </span>
  );
}

const ALERT_TONES = {
  info: {
    shell: "border-sky-200/80 bg-sky-50/80 text-sky-900",
    icon: "bg-sky-100 text-sky-600",
    Icon: Info,
  },
  success: {
    shell: "border-emerald-200/80 bg-emerald-50/80 text-emerald-900",
    icon: "bg-emerald-100 text-emerald-600",
    Icon: CheckCircle2,
  },
  error: {
    shell: "border-rose-200/80 bg-rose-50/80 text-rose-900",
    icon: "bg-rose-100 text-rose-600",
    Icon: AlertCircle,
  },
  warning: {
    shell: "border-amber-200/80 bg-amber-50/80 text-amber-900",
    icon: "bg-amber-100 text-amber-600",
    Icon: AlertCircle,
  },
};

export function Alert({ tone = "info", children, onClose, className = "" }) {
  if (!children) return null;
  const config = ALERT_TONES[tone] || ALERT_TONES.info;
  const Icon = config.Icon;
  return (
    <div
      className={`flex items-start gap-3 rounded-xl border px-4 py-3.5 text-sm shadow-sm ${config.shell} ${className}`}
    >
      <span className={`grid h-7 w-7 shrink-0 place-items-center rounded-lg ${config.icon}`}>
        <Icon size={16} />
      </span>
      <div className="flex-1 whitespace-pre-line pt-0.5 leading-relaxed">{children}</div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="shrink-0 rounded-lg p-1 opacity-50 transition hover:bg-white/60 hover:opacity-100"
          aria-label="Đóng"
        >
          <X size={15} />
        </button>
      )}
    </div>
  );
}

export function Spinner({ label = "Đang tải dữ liệu..." }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-sm text-slate-500">
      <span className="grid h-10 w-10 place-items-center rounded-full bg-[#edf3e4] text-[#58734f]">
        <Loader2 size={20} className="animate-spin" />
      </span>
      {label}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 px-6 py-14 text-center">
      {Icon && (
        <span className="grid h-14 w-14 place-items-center rounded-2xl bg-[#edf3e4] text-[#58734f] ring-1 ring-[#dfe8d4]">
          <Icon size={26} />
        </span>
      )}
      <div>
        <p className="font-semibold text-slate-800">{title}</p>
        {description && <p className="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function Modal({ open, onClose, title, description, children, footer, size = "md" }) {
  useEffect(() => {
    if (!open) return undefined;
    const handleKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  const sizes = {
    sm: "max-w-md",
    md: "max-w-xl",
    lg: "max-w-3xl",
    xl: "max-w-5xl",
  };

  return createPortal(
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/50 p-4 backdrop-blur-sm">
      <div className="absolute inset-0" onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`modal-panel relative mx-auto my-8 w-full ${sizes[size]} overflow-hidden rounded-2xl border border-white/60 bg-white shadow-2xl`}
      >
        <div className="h-1 w-full bg-[#d9ed8a]" />
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-4">
          <div>
            <h2 className="text-base font-semibold text-slate-900">{title}</h2>
            {description && <p className="mt-1 text-sm leading-relaxed text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="Đóng"
          >
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-slate-100 bg-slate-50/70 px-6 py-4">{footer}</div>
        )}
      </div>
    </div>,
    document.body
  );
}

export function PageHeader({ title, description, actions, icon: Icon, eyebrow }) {
  return (
    <div className="page-enter mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div className="flex items-start gap-3.5">
        {Icon ? (
          <span className="mt-0.5 grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-[#e8efe0] text-[#54734d] ring-1 ring-[#dce7d4]">
            <Icon size={22} />
          </span>
        ) : (
          <span className="mt-1.5 hidden h-9 w-1 rounded-full bg-gradient-to-b from-indigo-500 to-violet-500 sm:block" />
        )}
        <div>
          {eyebrow && (
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#718c68]">{eyebrow}</p>
          )}
          <h1 className="page-heading text-xl font-semibold tracking-tight sm:text-2xl">{title}</h1>
          {description && (
            <p className="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">{description}</p>
          )}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2 lg:justify-end">{actions}</div>}
    </div>
  );
}

const STAT_TONES = {
  indigo: "text-[#52704d] bg-[#edf3e4]",
  emerald: "text-[#43766a] bg-[#e9f2eb]",
  amber: "text-[#9a7048] bg-[#f7eee2]",
  sky: "text-[#567b89] bg-[#eaf1f2]",
  rose: "text-[#a4675c] bg-[#f7eae5]",
};

export function StatCard({ label, value, hint, icon: Icon, tone = "indigo" }) {
  return (
    <div
      className="stat-card relative overflow-hidden rounded-[20px] p-5"
    >
      <div className="pointer-events-none absolute -right-8 -top-10 h-28 w-28 rounded-full bg-white/10" />
      <div className="pointer-events-none absolute -bottom-12 -right-2 h-28 w-28 rounded-full bg-white/5" />
      <div className="relative flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="stat-label text-[11px] font-semibold uppercase tracking-wider">{label}</p>
          <p className="mt-2 truncate text-3xl font-semibold tracking-tight">{value}</p>
          {hint && <p className="mt-1.5 text-xs leading-snug text-slate-500">{hint}</p>}
        </div>
        {Icon && (
          <span className={`grid h-11 w-11 shrink-0 place-items-center rounded-xl ${STAT_TONES[tone] || STAT_TONES.indigo}`}>
            <Icon size={22} />
          </span>
        )}
      </div>
    </div>
  );
}

export function Field({ label, required, hint, children }) {
  return (
    <label className="block">
      <span className="label">
        {label} {required && <span className="text-rose-500">*</span>}
      </span>
      {children}
      {hint && <span className="mt-1.5 block text-xs leading-relaxed text-slate-400">{hint}</span>}
    </label>
  );
}
