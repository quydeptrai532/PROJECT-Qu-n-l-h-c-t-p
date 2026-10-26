export default async function globalSetup() {
  const url = process.env.API_HEALTH_URL || "http://127.0.0.1:8080/api/health";
  const loginUrl = process.env.API_LOGIN_URL || "http://127.0.0.1:8080/api/auth/login";

  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
  } catch (error) {
    throw new Error(
      `Không kết nối được backend tại ${url}. Hãy chạy backend trước khi test E2E ` +
        `(ví dụ: mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=h2). Chi tiết: ${error.message}`
    );
  }

  for (let attempt = 1; attempt <= 30; attempt += 1) {
    const response = await fetch(loginUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "sv01", password: "123456" }),
    });
    if (response.ok) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error("Dữ liệu mẫu chưa sẵn sàng: không đăng nhập được bằng sv01/123456");
}
