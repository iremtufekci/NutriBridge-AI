import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { api } from "../api/http";

function applyDocTheme(isDark) {
  const t = isDark ? "dark" : "light";
  localStorage.setItem("theme", t);
  document.documentElement.classList.toggle("dark", isDark);
  window.dispatchEvent(new CustomEvent("nightbrate-theme", { detail: { isDark: !!isDark } }));
}

/**
 * Token varsa sunucudaki hesap tercihini ceker (tum roller), html.dark ile esitler.
 * Sayfa yuklenirken once localStorage, sonra bu API ile guncelleme.
 */
export function ThemeBootstrap() {
  const location = useLocation();

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;
    const path = location.pathname;
    if (path === "/login" || path === "/" || path.startsWith("/register")) return;

    let cancelled = false;
    (async () => {
      try {
        const { data } = await api.get("/api/Auth/profile");
        if (cancelled) return;
        const isDark = data?.themePreference === "dark";
        applyDocTheme(isDark);
      } catch {
        // Profil cekilemese de localStorage + dark class kalsin
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [location.pathname]);

  return null;
}
