import axios from "axios";

/** Geliştirme: Vite proxy (vite.config.js) /api -> backend; aynı origin, CORS sorunu yok. */
const resolvedBase =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.DEV ? "" : "http://localhost:5231");

export const API_BASE_URL = resolvedBase;

export const api = axios.create({
  baseURL: resolvedBase,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    delete config.headers["Content-Type"];
  }
  return config;
});

/** Axios/HTTP hata nesnesinden kullanıcıya gösterilecek metin. */
export function getApiErrorMessage(err) {
  if (!err || typeof err !== "object") return "Beklenmeyen hata.";
  if (err.code === "ECONNABORTED") return "İstek zaman aşımına uğradı. Bağlantıyı kontrol edin.";
  if (err.message === "Network Error")
    return "Sunucuya bağlanılamadı. Sunucu çalışıyor mu (örneğin 5231)? Geliştirme ortamında /api yönlendirmesinin açık olduğunu kontrol edin.";
  const r = err.response;
  if (!r) return err.message || "Bilinmeyen hata.";
  const s = r.status;
  const d = r.data;
  if (d && typeof d === "object") {
    if (typeof d.message === "string" && d.message.trim()) return d.message;
    if (typeof d.detail === "string" && d.detail.trim()) return d.detail;
    if (typeof d.title === "string" && d.title.trim()) return d.title;
    if (d.errors && typeof d.errors === "object") {
      const parts = Object.values(d.errors).flat().filter((x) => typeof x === "string");
      if (parts.length) return parts.join(" ");
    }
  }
  if (s === 401) return "Oturum süresi doldu veya giriş yok. Lütfen tekrar giriş yapın.";
  if (s === 403) return "Bu işlem için yetkiniz yok.";
  if (s === 404) return "Uç nokta bulunamadı. Sunucuyu yeniden başlatıp güncel sürümü çalıştırdığınızdan emin olun.";
  if (s === 405) return "İstek yöntemi uygun değil. Gönderilen işlemle sunucu eşlemesini kontrol edin.";
  if (s >= 500) return "Sunucu hatası. Durum kodu: " + s;
  return "İstek başarısız (durum kodu: " + s + ").";
}
