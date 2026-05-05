import { useEffect } from "react";

/**
 * Uygulama yalnızca açık tema kullanır; eski kayıtları temizler.
 */
export function ThemeBootstrap() {
  useEffect(() => {
    localStorage.setItem("theme", "light");
    document.documentElement.classList.remove("dark");
  }, []);
  return null;
}
