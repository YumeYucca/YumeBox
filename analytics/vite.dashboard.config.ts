import { defineConfig } from "vite-plus";

export default defineConfig({
  root: "dashboard",
  build: {
    target: "es2022",
    outDir: "../dist/client",
    emptyOutDir: true,
  },
});
