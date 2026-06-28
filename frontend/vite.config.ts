import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The frontend talks ONLY to the Spring Boot backend (VITE_API_BASE_URL),
// never to Supabase directly.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
