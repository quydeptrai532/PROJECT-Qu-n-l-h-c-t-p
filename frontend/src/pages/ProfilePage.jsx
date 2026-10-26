import { useEffect, useState } from "react";
import { KeyRound, Mail, Phone, Save, UserRound } from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { Alert, Badge, Field, PageHeader, Spinner } from "../components/ui";
import { ROLE_LABEL, useAuth } from "../context/AuthContext";
import { useApi } from "../lib/useApi";

export default function ProfilePage() {
  const { updateUser } = useAuth();
  const { data, loading, error, reload } = useApi("/auth/me");
  const [hoTen, setHoTen] = useState("");
  const [email, setEmail] = useState("");
  const [soDienThoai, setSoDienThoai] = useState("");
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const [matKhauCu, setMatKhauCu] = useState("");
  const [matKhauMoi, setMatKhauMoi] = useState("");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    if (data) {
      setHoTen(data.hoTen || "");
      setEmail(data.email || "");
      setSoDienThoai(data.soDienThoai || "");
    }
  }, [data]);

  const saveProfile = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    setFormError("");
    try {
      await api.put("/auth/me", { hoTen, email, soDienThoai });
      updateUser({ hoTen });
      setMessage("Cập nhật hồ sơ thành công");
      reload();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (event) => {
    event.preventDefault();
    setChangingPassword(true);
    setPasswordMessage("");
    setPasswordError("");
    try {
      await api.put("/auth/doi-mat-khau", { matKhauCu, matKhauMoi });
      setPasswordMessage("Đổi mật khẩu thành công");
      setMatKhauCu("");
      setMatKhauMoi("");
    } catch (err) {
      setPasswordError(getErrorMessage(err));
    } finally {
      setChangingPassword(false);
    }
  };

  if (loading) return <Spinner />;
  if (error) return <Alert tone="error">{error}</Alert>;

  return (
    <div>
      <PageHeader
        icon={UserRound}
        title="Hồ sơ cá nhân"
        description="Xem thông tin tài khoản và cập nhật thông tin liên hệ."
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="card p-5 lg:col-span-1">
          <div className="flex items-center gap-3">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-indigo-100 text-lg font-semibold text-indigo-700">
              {(data.hoTen || data.username || "?").slice(0, 1).toUpperCase()}
            </span>
            <div>
              <p className="font-semibold text-slate-900">{data.hoTen || "Quản trị viên"}</p>
              <p className="text-sm text-slate-500">{data.ma}</p>
            </div>
          </div>
          <dl className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Tên đăng nhập</dt>
              <dd className="font-medium text-slate-800">{data.username}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Vai trò</dt>
              <dd>
                <Badge tone="indigo">{ROLE_LABEL[data.role] || data.role}</Badge>
              </dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Email</dt>
              <dd className="max-w-[200px] truncate text-slate-800">{data.email || "—"}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Điện thoại</dt>
              <dd className="text-slate-800">{data.soDienThoai || "—"}</dd>
            </div>
          </dl>
        </div>

        <div className="space-y-6 lg:col-span-2">
          <form className="card p-5" onSubmit={saveProfile}>
            <div className="mb-4 flex items-center gap-2">
              <UserRound size={18} className="text-indigo-600" />
              <h2 className="text-sm font-semibold text-slate-800">Thông tin liên hệ</h2>
            </div>
            {message && <Alert tone="success" className="mb-4" onClose={() => setMessage("")}>{message}</Alert>}
            {formError && <Alert tone="error" className="mb-4">{formError}</Alert>}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <Field label="Họ tên" required>
                  <input
                    className="input"
                    value={hoTen}
                    onChange={(event) => setHoTen(event.target.value)}
                    placeholder="Họ và tên"
                    disabled={data.role === "QUAN_TRI"}
                    required
                  />
                </Field>
              </div>
              <Field label="Email">
                <div className="relative">
                  <Mail size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    className="input pl-9"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="email@example.com"
                    disabled={data.role === "QUAN_TRI"}
                  />
                </div>
              </Field>
              <Field label="Số điện thoại">
                <div className="relative">
                  <Phone size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    className="input pl-9"
                    value={soDienThoai}
                    onChange={(event) => setSoDienThoai(event.target.value)}
                    placeholder="09xx xxx xxx"
                    disabled={data.role === "QUAN_TRI"}
                  />
                </div>
              </Field>
            </div>
            {data.role === "QUAN_TRI" && (
              <p className="mt-3 text-xs text-slate-500">
                Tài khoản quản trị không có hồ sơ liên hệ riêng.
              </p>
            )}
            <div className="mt-5 flex justify-end">
              <button type="submit" className="btn-primary" disabled={saving || data.role === "QUAN_TRI"}>
                <Save size={16} />
                {saving ? "Đang lưu..." : "Lưu thay đổi"}
              </button>
            </div>
          </form>

          <form className="card p-5" onSubmit={changePassword}>
            <div className="mb-4 flex items-center gap-2">
              <KeyRound size={18} className="text-indigo-600" />
              <h2 className="text-sm font-semibold text-slate-800">Đổi mật khẩu</h2>
            </div>
            {passwordMessage && (
              <Alert tone="success" className="mb-4" onClose={() => setPasswordMessage("")}>{passwordMessage}</Alert>
            )}
            {passwordError && <Alert tone="error" className="mb-4">{passwordError}</Alert>}
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Mật khẩu hiện tại" required>
                <input
                  className="input"
                  type="password"
                  value={matKhauCu}
                  onChange={(event) => setMatKhauCu(event.target.value)}
                  required
                />
              </Field>
              <Field label="Mật khẩu mới" required hint="Tối thiểu 6 ký tự">
                <input
                  className="input"
                  type="password"
                  value={matKhauMoi}
                  onChange={(event) => setMatKhauMoi(event.target.value)}
                  minLength={6}
                  required
                />
              </Field>
            </div>
            <div className="mt-5 flex justify-end">
              <button type="submit" className="btn-secondary" disabled={changingPassword}>
                {changingPassword ? "Đang cập nhật..." : "Đổi mật khẩu"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
