import { expect, test } from "@playwright/test";
import { login } from "./helpers";

async function goToMyCourses(page) {
  const dangKyLoaded = page.waitForResponse(
    (response) =>
      response.url().includes("/api/dang-ky/me") &&
      response.url().includes("hocKyId=") &&
      response.request().method() === "GET" &&
      response.ok()
  );
  await page.goto("/hoc-phan-cua-toi");
  await dangKyLoaded;
  await expect(page.locator(".animate-spin")).toHaveCount(0);
}

async function huyNeuDaDangKy(page) {
  await goToMyCourses(page);
  const row = page.locator("tr").filter({ hasText: "INT201.01" });
  if ((await row.count()) > 0) {
    await row.getByRole("button", { name: "Hủy", exact: true }).click();
    await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
    await expect(page.getByText(/Đã hủy đăng ký/)).toBeVisible();
    await expect(row).toHaveCount(0);
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

    await goToMyCourses(page);
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

  test("tra cứu lịch sử học tập, môn chưa đạt và lọc theo trạng thái", async ({ page }) => {
    await login(page, "sv03", "123456");
    await page.goto("/lich-su-hoc-tap");
    await expect(page.getByRole("heading", { name: "Lịch sử học tập & môn chưa đạt" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Môn chưa đạt", exact: true })).toBeVisible();
    await expect(page.getByText("INT101.01-HK2").first()).toBeVisible();
    await expect(page.getByText("1/2 lần").first()).toBeVisible();
    await expect(page.getByText("2/2 lần").first()).toBeVisible();

    const statusSelect = page.getByLabel("Lọc theo trạng thái");
    await statusSelect.selectOption("CHUA_DAT");
    await expect(page.locator("tbody tr")).toHaveCount(1);
    await expect(page.locator("tbody tr").first()).toContainText("Chưa đạt");

    await statusSelect.selectOption("CHUA_CO_DIEM");
    await expect(page.locator("tbody tr").first()).toContainText("Chưa có điểm");
  });
});
