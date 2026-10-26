import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Quản trị", () => {
  test("xem dashboard thống kê và biểu đồ", async ({ page }) => {
    await login(page, "admin", "admin123");
    await expect(page).toHaveURL(/\/quan-tri$/);
    await expect(page.getByRole("heading", { name: "Tổng quan đào tạo" })).toBeVisible();
    await expect(page.getByText("Đăng ký theo môn học")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Tỷ lệ lấp đầy" })).toBeVisible();
    await expect(page.getByText("Thao tác nhanh")).toBeVisible();
  });

  test("thêm và xóa môn học bằng modal xác nhận", async ({ page }) => {
    await login(page, "admin", "admin123");
    await page.goto("/quan-tri/mon-hoc");

    const maMon = `E2E${Date.now().toString().slice(-6)}`;
    await page.getByRole("button", { name: "Thêm môn học" }).click();
    await page.getByLabel("Mã môn học").fill(maMon);
    await page.getByLabel("Tên môn học").fill("Môn kiểm thử E2E");
    await page.getByLabel("Số tín chỉ").fill("2");
    await page.getByLabel("Khoa").selectOption({ index: 1 });
    await page.getByRole("button", { name: "Lưu", exact: true }).click();
    await expect(page.getByText("Thêm môn học thành công")).toBeVisible();

    await page.getByPlaceholder("Tìm theo mã, tên môn, khoa...").fill(maMon);
    const row = page.locator("tr").filter({ hasText: maMon });
    await expect(row).toBeVisible();
    await row.getByTitle("Xóa").click();
    await expect(page.getByRole("heading", { name: "Xóa môn học" })).toBeVisible();
    await page.getByRole("button", { name: "Xóa môn học", exact: true }).click();
    await expect(page.getByText("Đã xóa môn học")).toBeVisible();
    await expect(page.locator("tr").filter({ hasText: maMon })).toHaveCount(0);
  });

  test("khóa và mở khóa tài khoản sinh viên", async ({ page }) => {
    await login(page, "admin", "admin123");
    await page.goto("/quan-tri/sinh-vien");

    let row = page.locator("tr").filter({ hasText: "SV005" });
    await expect(row).toBeVisible();

    if ((await row.getByText("Đã khóa").count()) > 0) {
      await row.getByTitle("Mở khóa tài khoản").click();
      const dialog = page.getByRole("dialog");
      await dialog.getByRole("button", { name: "Mở khóa", exact: true }).click();
      await expect(row.getByText("Hoạt động")).toBeVisible();
    }

    await row.getByTitle("Khóa tài khoản").click();
    let dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading", { name: "Khóa tài khoản" })).toBeVisible();
    await dialog.getByRole("button", { name: "Khóa tài khoản", exact: true }).click();
    await expect(row.getByText("Đã khóa")).toBeVisible();

    row = page.locator("tr").filter({ hasText: "SV005" });
    await row.getByTitle("Mở khóa tài khoản").click();
    dialog = page.getByRole("dialog");
    await dialog.getByRole("button", { name: "Mở khóa", exact: true }).click();
    await expect(row.getByText("Hoạt động")).toBeVisible();
  });
});
