import { chromium } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

const APP_URL = process.env.APP_URL || "http://127.0.0.1:5173";
const API_URL = process.env.API_URL || "http://localhost:8080/api";
const OUTPUT_DIR = path.resolve(process.cwd(), "..", "docs", "screenshots");

fs.mkdirSync(OUTPUT_DIR, { recursive: true });

const browser = await chromium.launch({ channel: "chrome", headless: true });
const context = await browser.newContext({
  viewport: { width: 1440, height: 1000 },
  deviceScaleFactor: 1,
});
const page = await context.newPage();

async function capture(name, url, waitMs = 900) {
  await page.goto(`${APP_URL}${url}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(waitMs);
  await page.screenshot({ path: path.join(OUTPUT_DIR, name), fullPage: true });
  console.log(`saved ${name}`);
}

async function login(username, password) {
  const response = await context.request.post(`${API_URL}/auth/login`, {
    data: { username, password },
  });
  if (!response.ok()) {
    throw new Error(`Login failed for ${username}: HTTP ${response.status()}`);
  }
  const data = await response.json();
  await page.goto(`${APP_URL}/login`);
  await page.evaluate(
    ({ token, user }) => {
      localStorage.setItem("token", token);
      localStorage.setItem("user", JSON.stringify(user));
    },
    {
      token: data.token,
      user: {
        username: data.username,
        role: data.role,
        hoTen: data.hoTen,
        ma: data.ma,
      },
    }
  );
}

await page.goto(`${APP_URL}/login`, { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.screenshot({ path: path.join(OUTPUT_DIR, "login.png"), fullPage: true });
console.log("saved login.png");

await login("sv01", "123456");
await capture("student-dashboard.png", "/tong-quan", 1300);
await capture("student-registration.png", "/dang-ky");
await capture("student-grades.png", "/diem");

await login("gv01", "123456");
await capture("lecturer-dashboard.png", "/tong-quan", 1300);
await capture("lecturer-classes.png", "/lop-cua-toi");

await login("admin", "admin123");
await capture("admin-dashboard.png", "/quan-tri", 1500);
await capture("admin-classes.png", "/quan-tri/lop-hoc-phan");

await page.goto(`${APP_URL}/quan-tri/mon-hoc`, { waitUntil: "networkidle" });
await page.getByRole("button", { name: "Thêm môn học" }).click();
await page.waitForTimeout(500);
await page.screenshot({ path: path.join(OUTPUT_DIR, "admin-subject-modal.png"), fullPage: true });
console.log("saved admin-subject-modal.png");

await browser.close();
