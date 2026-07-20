import { useEffect, useState } from "react"; // Durum ve profil yenileme kancaları
import { api } from "../api/http"; // Kimlik doğrulama API istemcisi

const FALLBACK = "Kullanici"; // Profil adı yoksa gösterilecek yedek metin

/**
 * Giriş sonrası ve sayfa açılışında /api/Auth/profile ile gosterim adi (veritabani) alinir.
 */
export function useAuthProfileDisplayName() {
  // İlk render: localStorage'daki kayıtlı ad veya yedek
  const [displayName, setDisplayName] = useState(
    () => localStorage.getItem("userName")?.trim() || FALLBACK
  );

  useEffect(() => {
    if (!localStorage.getItem("token")) return; // Oturum yoksa API çağrısı yapma
    void (async () => {
      try {
        const { data } = await api.get<{ displayName?: string }>("/api/Auth/profile");
        // Sunucu adı > localStorage > yedek sırasıyla birleştir
        const d = (data?.displayName || "").trim() || localStorage.getItem("userName") || FALLBACK;
        setDisplayName(d); // Bileşen durumunu güncelle
        localStorage.setItem("userName", d); // Sonraki sayfa yüklemeleri için önbelleğe al
      } catch {
        // token gecersiz / ag hatasi: mevcut localStorage ile devam
      }
    })();
  }, []); // Bileşen mount olduğunda bir kez profil çek

  return displayName; // Kenar çubuğu ve başlıklarda kullanılacak ad
}
