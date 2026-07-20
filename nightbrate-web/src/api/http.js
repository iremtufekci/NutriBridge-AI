import axios from "axios"; // HTTP istek kütüphanesi

// API kök adresi: .env varsa onu, yoksa dev'de proxy, prod'da localhost:5231
const resolvedBase =
  import.meta.env.VITE_API_BASE_URL || // Ortam değişkeni öncelikli
  (import.meta.env.DEV ? "" : "http://localhost:5231"); // Dev: boş = Vite proxy kullan

export const API_BASE_URL = resolvedBase; // Medya URL birleştirmede kullanılır

/** Göreli medya yollarını tam URL'ye çevirir (örn. /uploads/meals/x.png) */
export function resolveMediaUrl(path) {
  if (!path || typeof path !== "string") return ""; // Geçersiz girdi → boş
  const trimmed = path.trim(); // Baştaki/sondaki boşlukları sil
  if (!trimmed) return ""; // Boş string
  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed; // Zaten tam URL
  const base = API_BASE_URL.replace(/\/$/, ""); // Sondaki / kaldır
  const rel = trimmed.startsWith("/") ? trimmed : `/${trimmed}`; // Göreli yolu / ile başlat
  return base ? `${base}${rel}` : rel; // Base varsa birleştir, yoksa göreli döndür
}

export const api = axios.create({ // Tüm sayfaların kullandığı axios örneği
  baseURL: resolvedBase, // İsteklerin ön eki
  headers: { "Content-Type": "application/json" }, // Varsayılan JSON gövdesi
});

api.interceptors.request.use((config) => { // Her istekten önce çalışır
  const token = localStorage.getItem("token"); // Girişte kaydedilen JWT
  if (token) {
    config.headers.Authorization = `Bearer ${token}`; // Oturum başlığı ekle
  }
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    delete config.headers["Content-Type"]; // Dosya yüklerinde tarayıcı boundary seçsin
  }
  return config; // Değiştirilmiş isteği gönder
});

/** Axios hata nesnesinden Türkçe kullanıcı mesajı üretir */
export function getApiErrorMessage(err) {
  if (!err || typeof err !== "object") return "Beklenmeyen hata."; // Tanımsız hata
  if (err.code === "ECONNABORTED") return "İstek zaman aşımına uğradı. Bağlantıyı kontrol edin."; // Timeout
  if (err.message === "Network Error")
    return "Sunucuya bağlanılamadı. Sunucu çalışıyor mu (örneğin 5231)? Geliştirme ortamında /api yönlendirmesinin açık olduğunu kontrol edin."; // Ağ kopuk
  const r = err.response; // Sunucudan gelen cevap
  if (!r) return err.message || "Bilinmeyen hata."; // Cevap yok
  const s = r.status; // HTTP durum kodu (401, 404 vb.)
  const d = r.data; // Hata gövdesi (JSON)
  if (d && typeof d === "object") {
    if (typeof d.message === "string" && d.message.trim()) return d.message; // Backend message alanı
    if (typeof d.detail === "string" && d.detail.trim()) return d.detail; // ASP.NET detail
    if (typeof d.title === "string" && d.title.trim()) return d.title; // ProblemDetails title
    if (d.errors && typeof d.errors === "object") {
      const parts = Object.values(d.errors).flat().filter((x) => typeof x === "string"); // Validasyon hataları
      if (parts.length) return parts.join(" "); // Birleştirilmiş mesaj
    }
  }
  if (s === 401) return "Oturum süresi doldu veya giriş yok. Lütfen tekrar giriş yapın."; // Yetkisiz
  if (s === 403) return "Bu işlem için yetkiniz yok."; // Yasak
  if (s === 404) return "Uç nokta bulunamadı. Sunucuyu yeniden başlatıp güncel sürümü çalıştırdığınızdan emin olun."; // Bulunamadı
  if (s === 405) return "İstek yöntemi uygun değil. Gönderilen işlemle sunucu eşlemesini kontrol edin."; // Yanlış HTTP metodu
  if (s >= 500) return "Sunucu hatası. Durum kodu: " + s; // Sunucu tarafı hata
  return "İstek başarısız (durum kodu: " + s + ")."; // Diğer 4xx/5xx
}
