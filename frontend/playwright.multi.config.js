import { defineConfig, devices } from "@playwright/test";
import base from "./playwright.config.js";

export default defineConfig({
    ...base,
    use: { ...base.use, channel: undefined },
    projects: [
        { name: "firefox", use: { ...devices["Desktop Firefox"] } },
        { name: "webkit", use: { ...devices["Desktop Safari"] } },
    ],
});