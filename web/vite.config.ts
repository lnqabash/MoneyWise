import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.svg"],
      manifest: {
        name: "MoneyMate — AI Financial Confidence Assistant",
        short_name: "MoneyMate",
        description:
          "Budget irregular income, save, learn, and stay safe from scams — in your language, even offline.",
        theme_color: "#0B6E4F",
        background_color: "#F7FAF8",
        display: "standalone",
        orientation: "portrait",
        start_url: "/",
        icons: [
          { src: "icon-192.png", sizes: "192x192", type: "image/png" },
          { src: "icon-512.png", sizes: "512x512", type: "image/png" },
          {
            src: "icon-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable",
          },
        ],
      },
      workbox: {
        // Cache the app shell; API calls are network-first so data stays fresh
        // but the shell still loads offline (offline-first per ADR-0004).
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.startsWith("/v1"),
            handler: "NetworkFirst",
            options: {
              cacheName: "moneymate-api",
              networkTimeoutSeconds: 5,
              expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 * 24 },
            },
          },
        ],
      },
    }),
  ],
  server: {
    port: 5173,
    // Proxy API + auth to the Spring Boot backend in dev (no CORS needed).
    proxy: {
      "/v1": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
});
