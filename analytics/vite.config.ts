import { defineConfig } from "vite-plus";

export default defineConfig({
  build: {
    target: "es2022",
    outDir: "dist/worker",
    emptyOutDir: true,
    lib: {
      entry: "src/index.ts",
      formats: ["es"],
      fileName: () => "index.js",
    },
    rollupOptions: {
      output: {
        entryFileNames: "index.js",
      },
    },
    sourcemap: true,
  },
});
