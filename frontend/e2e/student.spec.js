import { expect, test } from "@playwright/test";
import { login } from "./helpers";

async function huyNeuDaDangKy(page) {
  await page.goto("/hoc-phan-cua-toi");
  const row = page.locator("tr").filter({ hasText: "INT201.01" });
  if ((await row.count()) > 0) {
    await row.getByRole("button", { name: "Hủy", exact: true }).click();
    await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
    await expect(page.getByText(/Đã hủy đăng ký/)).toBeVisible();
  }
}

test.describe("Sinh viên", () => {
  test("đăng ký, xem học phần và hủy qua modal xác nhận", async ({ page }) => {
    await login(page, "sv02", "123456");
    await huyNeuDaDangKy(page);

    await page.goto("/dang-ky");
    await page
      .getByPlaceholder("Tìm theo mã lớp, môn học, giảng viên...")
      .fill("INT201.01");
    const lop = page.locator("tr").filter({ hasText: "INT201.01" });
    await expect(lop).toBeVisible();
    await lop.getByRole("button", { name: "Đăng ký", exact: true }).click();
    await expect(page.getByText(/Đăng ký thành công lớp INT201.01/)).toBeVisible();

    await page.goto("/hoc-phan-cua-toi");
    const hocPhan = page.locator("tr").filter({ hasText: "INT201.01" });
    await expect(hocPhan).toBeVisible();
    await expect(page.getByText("INT201.01")).toBeVisible();

    await hocPhan.getByRole("button", { name: "Hủy", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Hủy đăng ký học phần" })).toBeVisible();
    await page.getByRole("button", { name: "Giữ lại", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Hủy đăng ký học phần" })).toBeHidden();
    await expect(hocPhan).toBeVisible();

    await hocPhan.getByRole("button", { name: "Hủy", exact: true }).click();
    await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
    await expect(page.getByText(/Đã hủy đăng ký lớp INT201.01/)).toBeVisible();
    await expect(page.locator("tr").filter({ hasText: "INT201.01" })).toHaveCount(0);
  });

  test("xem thời khóa biểu và kết quả học tập", async ({ page }) => {
    await login(page, "sv02", "123456");
    await page.goto("/thoi-khoa-bieu");
    await expect(page.getByRole("main").getByRole("heading", { name: "Thời khóa biểu" })).toBeVisible();
    await expect(
      page.getByText(/Chưa có lịch học|^Tiết$/).first()
    ).toBeVisible();

    await page.goto("/diem");
    await expect(page.getByRole("heading", { name: "Điểm & kết quả học tập" })).toBeVisible();
    await expect(page.getByText("GPA hệ 10")).toBeVisible();
    await expect(page.getByText("Xu hướng GPA")).toBeVisible();
  });
});
