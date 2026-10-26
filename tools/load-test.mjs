#!/usr/bin/env node
/**
 * Load test đơn giản, không cần thư viện ngoài.
 *
 * Chạy: node tools/load-test.mjs
 * Biến môi trường:
 *   BASE_URL     (mặc định http://localhost:8080/api)
 *   CONCURRENCY  (mặc định 50)
 *   REQUESTS     (mặc định 200)
 *   LOAD_USER / LOAD_PASSWORD (mặc định sv01 / 123456)
 */

const BASE_URL = process.env.BASE_URL || "http://localhost:8080/api";
const CONCURRENCY = Number(process.env.CONCURRENCY || 50);
const REQUESTS = Number(process.env.REQUESTS || 200);
const WARMUP = Number(process.env.WARMUP || 30);
const USERNAME = process.env.LOAD_USER || "sv01";
const PASSWORD = process.env.LOAD_PASSWORD || "123456";
const TARGET_P95_MS = Number(process.env.TARGET_P95_MS || 500);

async function login() {
  for (let attempt = 1; attempt <= 30; attempt += 1) {
    const response = await fetch(`${BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
    });
    if (response.ok) {
      const data = await response.json();
      return data.token;
    }
    if (response.status !== 401) {
      throw new Error(`Đăng nhập thất bại: HTTP ${response.status}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error("Đăng nhập thất bại: tài khoản mẫu chưa sẵn sàng");
}

async function oneRequest(url, token) {
  const started = performance.now();
  try {
    const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    await response.text();
    return { ms: performance.now() - started, status: response.status, ok: response.ok };
  } catch (error) {
    return { ms: performance.now() - started, status: 0, ok: false, error: String(error) };
  }
}

function percentile(sorted, value) {
  if (sorted.length === 0) return 0;
  const index = Math.min(sorted.length - 1, Math.max(0, Math.ceil((value / 100) * sorted.length) - 1));
  return sorted[index];
}

function round(value) {
  return Math.round(value * 100) / 100;
}

async function main() {
  console.log(`Load test: ${REQUESTS} requests, concurrency ${CONCURRENCY}`);
  console.log(`Target: ${BASE_URL}`);
  const token = await login();
  const url = `${BASE_URL}/lop-hoc-phan`;

  if (WARMUP > 0) {
    console.log(`Warmup: ${WARMUP} requests...`);
    let warmIndex = 0;
    await Promise.all(
      Array.from({ length: Math.min(CONCURRENCY, 10) }, async () => {
        while (true) {
          const index = warmIndex++;
          if (index >= WARMUP) return;
          await oneRequest(url, token);
        }
      })
    );
  }

  const results = [];
  let next = 0;
  const startedAt = performance.now();

  async function worker() {
    while (true) {
      const index = next++;
      if (index >= REQUESTS) return;
      results.push(await oneRequest(url, token));
    }
  }

  await Promise.all(Array.from({ length: Math.min(CONCURRENCY, REQUESTS) }, () => worker()));

  const elapsed = performance.now() - startedAt;
  const times = results.map((item) => item.ms).sort((a, b) => a - b);
  const success = results.filter((item) => item.ok).length;
  const failed = results.length - success;
  const statusCounts = results.reduce((acc, item) => {
    acc[item.status] = (acc[item.status] || 0) + 1;
    return acc;
  }, {});
  const p95 = percentile(times, 95);

  console.log("");
  console.log("Kết quả");
  console.log(`- Tổng request     : ${results.length}`);
  console.log(`- Thành công       : ${success}`);
  console.log(`- Thất bại         : ${failed}`);
  console.log(`- Thời gian        : ${round(elapsed)} ms`);
  console.log(`- Throughput       : ${round((results.length / elapsed) * 1000)} req/s`);
  console.log(`- Min / P50        : ${round(times[0] || 0)} / ${round(percentile(times, 50))} ms`);
  console.log(`- P95 / P99 / Max  : ${round(p95)} / ${round(percentile(times, 99))} / ${round(times[times.length - 1] || 0)} ms`);
  console.log(`- Status codes     : ${JSON.stringify(statusCounts)}`);

  const passed = failed === 0 && p95 <= TARGET_P95_MS;
  const reasons = [];
  if (failed > 0) reasons.push(`${failed} request lỗi`);
  if (p95 > TARGET_P95_MS) reasons.push(`P95 ${round(p95)}ms > ${TARGET_P95_MS}ms`);
  console.log("");
  console.log(passed ? `PASS: không lỗi và P95 <= ${TARGET_P95_MS}ms` : `FAIL: ${reasons.join(", ")}`);
  process.exit(passed ? 0 : 1);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
