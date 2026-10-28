import { expect, test } from "@playwright/test";
import { login } from "./helpers";

async function addClass(page, classCode) {
  await page.getByLabel("Tìm lớp học phần").fill(classCode);
  const row = page.locator("tr").filter({ hasText: classCode });
  await expect(row).toBeVisible();
  await row.getByRole("button", { name: "Thêm", exact: false }).click();
  await page.getByLabel("Tìm lớp học phần").fill("");
}

test.describe("Course Planner", () => {
  test("lưu giỏ theo học kỳ và nêu rõ xung đột lớp dự kiến", async ({ page }) => {
    let confirmationRequests = 0;
    page.on("request", (request) => {
      if (request.url().includes("/api/dang-ky/xac-nhan-ke-hoach")) confirmationRequests += 1;
    });

    await login(page, "sv02", "123456");
    await page.route(/\/api\/lop-hoc-phan(?:\?|$)/, async (route) => {
      const response = await route.fetch();
      const classes = await response.json();
      await route.fulfill({
        response,
        json: classes.map((item) => item.ma === "INT301.01" ? { ...item, thu: 2 } : item),
      });
    });
    await page.goto("/ke-hoach-hoc-tap");
    await expect(page.getByRole("main").getByRole("heading", { name: "Kế hoạch học tập" })).toBeVisible();
    await expect(page.getByText("Giỏ đang trống")).toBeVisible();

    await addClass(page, "INT201.01");
    await addClass(page, "INT301.01");
    await expect(page.getByText(/Trùng lịch: INT201\.01 và INT301\.01 \(Thứ 2, tiết 1-3/)).toBeVisible();
    await addClass(page, "INT201.02");
    await expect(page.getByText(/Trùng môn: INT201\.01 và INT201\.02/)).toBeVisible();
    await expect(page.getByRole("button", { name: /Xác nhận đăng ký/ })).toBeDisabled();

    await page.reload();
    await expect(page.getByText("3 lớp · 9 tín chỉ")).toBeVisible();
    await expect(page.getByText(/Trùng lịch: INT201\.01 và INT301\.01/)).toBeVisible();
    await page.getByRole("button", { name: "Bỏ INT301.01 khỏi giỏ" }).click();
    await page.getByRole("button", { name: "Bỏ INT201.02 khỏi giỏ" }).click();
    await page.getByRole("button", { name: "Bỏ INT201.01 khỏi giỏ" }).click();
    await expect(page.getByText("Giỏ đang trống")).toBeVisible();
    expect(confirmationRequests).toBe(0);
  });

  test("chỉ ghi danh sau xác nhận và giữ nguyên giỏ khi backend từ chối tiên quyết", async ({ page }) => {
    await login(page, "sv02", "123456");
    await page.goto("/ke-hoach-hoc-tap");
    await addClass(page, "INT301.01");
    await addClass(page, "INT302.01");

    await expect(page.getByText(/2 lớp · 6 tín chỉ/)).toBeVisible();
    await expect(page.getByText(/Trùng lịch:/)).toHaveCount(0);
    const confirmation = page.waitForResponse((response) =>
      response.url().includes("/api/dang-ky/xac-nhan-ke-hoach")
    );
    await page.getByRole("button", { name: "Xác nhận đăng ký (2)" }).click();
    await expect(page.getByRole("heading", { name: "Xác nhận đăng ký học phần" })).toBeVisible();
    await page.getByRole("button", { name: "Đăng ký các lớp này" }).click();
    const response = await confirmation;

    expect(response.status()).toBe(400);
    await expect(page.getByText(/Chưa đạt môn tiên quyết: INT201/)).toBeVisible();
    await expect(page.getByText("2 lớp · 6 tín chỉ")).toBeVisible();

    await page.goto("/hoc-phan-cua-toi");
    await expect(page.locator("tr").filter({ hasText: "INT301.01" })).toHaveCount(0);
    await expect(page.locator("tr").filter({ hasText: "INT302.01" })).toHaveCount(0);
  });

  test("cảnh báo lớp vừa hết chỗ trước khi gửi xác nhận", async ({ page }) => {
    await login(page, "sv02", "123456");
    await page.route(/\/api\/lop-hoc-phan(?:\?|$)/, async (route) => {
      const response = await route.fetch();
      const classes = await response.json();
      await route.fulfill({
        response,
        json: classes.map((item) => item.ma === "INT301.01"
          ? { ...item, siSoHienTai: item.siSoToiDa, conLai: 0 }
          : item),
      });
    });
    await page.goto("/ke-hoach-hoc-tap");
    await addClass(page, "INT301.01");

    await expect(page.getByText(/Lớp INT301\.01 đã hết chỗ/)).toBeVisible();
    await expect(page.getByRole("button", { name: "Xác nhận đăng ký (1)" })).toBeDisabled();
  });
});
