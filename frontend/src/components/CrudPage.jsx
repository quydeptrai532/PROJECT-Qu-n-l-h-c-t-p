import { useMemo, useState } from "react";
import { Inbox, Pencil, Plus, Search, Trash2 } from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { useConfirm } from "./ConfirmDialog";
import { Alert, EmptyState, Field, Modal, PageHeader, Spinner } from "./ui";
import { useApi } from "../lib/useApi";

export default function CrudPage({
  title,
  description,
  endpoint,
  columns,
  fields,
  emptyValues,
  searchKeys = [],
  addLabel = "Thêm mới",
  icon,
  deleteMessage = (row) => `Xóa bản ghi "${row.ma || row.ten || row.id}"?`,
}) {
  const confirm = useConfirm();
  const { data, loading, error, reload } = useApi(endpoint);
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyValues);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");

  const rows = useMemo(() => {
    const list = data || [];
    const term = search.trim().toLowerCase();
    if (!term) return list;
    return list.filter((row) =>
      searchKeys.some((key) => String(row[key] || "").toLowerCase().includes(term))
    );
  }, [data, search, searchKeys]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...emptyValues });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (row) => {
    const values = { ...emptyValues };
    fields.forEach((field) => {
      const value = row[field.name];
      values[field.name] = value === null || value === undefined ? "" : String(value);
    });
    setEditing(row);
    setForm(values);
    setFormError("");
    setModalOpen(true);
  };

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field.name]: value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    setActionError("");
    try {
      const payload = {};
      fields.forEach((field) => {
        const value = form[field.name];
        if (field.type === "number" || field.numeric) {
          payload[field.name] = value === "" || value === null ? null : Number(value);
        } else {
          payload[field.name] = typeof value === "string" ? value.trim() : value;
        }
      });
      if (editing) {
        await api.put(`${endpoint}/${editing.id}`, payload);
        setMessage("Cập nhật thành công");
      } else {
        await api.post(endpoint, payload);
        setMessage("Thêm mới thành công");
      }
      setModalOpen(false);
      reload();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa bản ghi",
      message: deleteMessage(row),
      detail: "Thao tác này không thể hoàn tác. Dữ liệu đang được tham chiếu sẽ không thể xóa.",
      confirmLabel: "Xóa",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    try {
      await api.delete(`${endpoint}/${row.id}`);
      setMessage("Đã xóa bản ghi");
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title={title}
        icon={icon}
        description={description}
        actions={
          <button type="button" className="btn-primary" onClick={openCreate}>
            <Plus size={16} />
            {addLabel}
          </button>
        }
      />

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {actionError && <Alert tone="error" onClose={() => setActionError("")}>{actionError}</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-xs">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Tìm kiếm..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <p className="text-sm text-slate-500">{rows.length} bản ghi</p>
        </div>

        {loading ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState icon={Inbox} title="Chưa có dữ liệu" description="Bấm Thêm mới để tạo bản ghi đầu tiên." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50">
                <tr>
                  {columns.map((column) => (
                    <th key={column.key} className={`th ${column.align === "right" ? "text-right" : ""}`}>
                      {column.label}
                    </th>
                  ))}
                  <th className="th w-28 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-slate-50/60">
                    {columns.map((column) => (
                      <td key={column.key} className={`td ${column.align === "right" ? "text-right" : ""}`}>
                        {column.render ? column.render(row) : row[column.key]}
                      </td>
                    ))}
                    <td className="td text-right">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
                          onClick={() => openEdit(row)}
                          title="Sửa"
                        >
                          <Pencil size={15} />
                        </button>
                        <button
                          type="button"
                          className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50 hover:text-rose-700"
                          onClick={() => remove(row)}
                          title="Xóa"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? `Cập nhật ${title.toLowerCase()}` : `Thêm ${title.toLowerCase()}`}
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>
              Hủy
            </button>
            <button type="submit" form="crud-form" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="crud-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2">
              <Alert tone="error">{formError}</Alert>
            </div>
          )}
          {fields.map((field) => (
            <div key={field.name} className={field.colSpan === 2 ? "sm:col-span-2" : ""}>
              <Field label={field.label} required={field.required} hint={field.hint}>
                {field.type === "select" ? (
                  <select
                    className="input"
                    value={form[field.name] ?? ""}
                    onChange={(event) => updateField(field, event.target.value)}
                    required={field.required}
                  >
                    <option value="">-- Chọn --</option>
                    {(field.options || []).map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    className="input"
                    type={field.type || "text"}
                    value={form[field.name] ?? ""}
                    onChange={(event) => updateField(field, event.target.value)}
                    placeholder={field.placeholder}
                    required={field.required}
                    min={field.min}
                    max={field.max}
                    step={field.step}
                  />
                )}
              </Field>
            </div>
          ))}
        </form>
      </Modal>
    </div>
  );
}
