import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Em desenvolvimento, /api é encaminhado ao Spring Boot (sem CORS)
    proxy: { "/api": "http://localhost:8080" },
  },
  test: {
    include: ["src/**/*.test.ts"],
  },
});
