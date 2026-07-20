import { useEffect } from "react"; // React yan etki kancası

/**
 * Uygulama yalnızca açık tema kullanır; eski kayıtları temizler.
 */
export function ThemeBootstrap() {
  useEffect(() => {
    localStorage.setItem("theme", "light"); // Tema tercihini açık olarak kaydet
    document.documentElement.classList.remove("dark"); // HTML kökünden karanlık sınıfını kaldır
  }, []); // Yalnızca ilk montajda çalış
  return null; // Görsel çıktı üretmez
}
