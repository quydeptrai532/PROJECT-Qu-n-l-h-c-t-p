import { expect } from "@playwright/test";

export async function login(page, username, password) {
  await page.goto("/login");
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).not.toHaveURL(/\/login$/);
}

export async function logout(page) {
  await page.evaluate(() => {
    localStorage.clear();
  });
}

export async function confirmDialog(page, { title, confirmLabel }) {
  const dialog = page.getByRole("heading", { name: title });
  await expect(dialog).toBeVisible();
  await page.getByRole("button", { name: confirmLabel, exact: true }).click();
}

export async function cancelDialog(page, cancelLabel = "Hủy") {
  await page.getByRole("button", { name: cancelLabel, exact: true }).click();
}
