import { useEffect, useState } from "react";
import { api } from "../api/http";

const FALLBACK = "Kullanici";

/**
 * Giriş sonrası ve sayfa açılışında /api/Auth/profile ile gosterim adi (veritabani) alinir.
 */
export function useAuthProfileDisplayName() {
  const [displayName, setDisplayName] = useState(
    () => localStorage.getItem("userName")?.trim() || FALLBACK
  );

  useEffect(() => {
    if (!localStorage.getItem("token")) return;
    void (async () => {
      try {
        const { data } = await api.get<{ displayName?: string }>("/api/Auth/profile");
        const d = (data?.displayName || "").trim() || localStorage.getItem("userName") || FALLBACK;
        setDisplayName(d);
        localStorage.setItem("userName", d);
      } catch {
        // token gecersiz / ag hatasi: mevcut localStorage ile devam
      }
    })();
  }, []);

  return displayName;
}
