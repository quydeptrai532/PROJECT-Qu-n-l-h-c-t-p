import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Giảng viên", () => {
  test("xem tổng quan, lớp phụ trách và nhập điểm", async ({ page }) => {
    await login(page, "gv01", "123456");
    await expect(page).toHaveURL(/\/tong-quan$/);
    await expect(page.getByRole("main").getByText("Không gian giảng dạy")).toBeVisible();
    await expect(page.getByText("Lớp phụ trách")).toBeVisible();

    await page.goto("/lop-cua-toi");
    await expect(page.getByRole("heading", { name: "Lớp học phần phụ trách" })).toBeVisible();
    await expect(page.getByText("INT303.01")).toBeVisible();

    await page.getByRole("link", { name: /Nhập điểm & xem danh sách/ }).first().click();
    await expect(page.getByText("Danh sách sinh viên")).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Mã SV" })).toBeVisible();

    await page.goto("/lop-cua-toi/thoi-khoa-bieu");
    await expect(page.getByRole("heading", { name: "Thời khóa biểu giảng dạy" })).toBeVisible();
  });
});
