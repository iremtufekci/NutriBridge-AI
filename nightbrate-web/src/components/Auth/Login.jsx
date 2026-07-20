import { useState } from "react"; // Form alanları için yerel state
import { useNavigate, useLocation } from "react-router-dom"; // Sayfa yönlendirme + önceki sayfa state'i
import { Loader2 } from "lucide-react"; // Yükleniyor ikonu (dönen)
import { api } from "../../api/http"; // Axios istemcisi
import {
  AuthError, // Hata mesajı kutusu
  AuthField, // Label + input sarmalayıcı
  AuthFooterLinks, // Kayıt sayfalarına linkler
  AuthLayout, // Ortak giriş sayfası çerçevesi
  AuthPageHeader, // Başlık + alt başlık
  AuthSuccess, // Başarı mesajı (kayıttan sonra)
  authBtnPrimaryClass, // Yeşil buton Tailwind sınıfları
  authInputClass, // Input Tailwind sınıfları
} from "./AuthLayout";
import { useAppFeedback } from "../feedback/AppFeedback"; // Modal alert için

export function Login() { // Giriş sayfası bileşeni
  const [email, setEmail] = useState(""); // E-posta input değeri
  const [password, setPassword] = useState(""); // Şifre input değeri
  const [error, setError] = useState(""); // Gösterilecek hata metni
  const [loading, setLoading] = useState(false); // İstek sürüyor mu
  const navigate = useNavigate(); // Programatik yönlendirme
  const location = useLocation(); // URL ve state (kayıt sonrası mesaj)
  const successMessage = location.state?.message; // Register'dan gelen başarı yazısı
  const { alert: uiAlert } = useAppFeedback(); // "Şifremi unuttum" popup

  const handleLogin = async (e) => { // Form gönderildiğinde
    e.preventDefault(); // Sayfa yenilemeyi engelle
    setError(""); // Önceki hatayı temizle
    setLoading(true); // Butonu kilitle

    try {
      const response = await api.post("/api/auth/login", { // Backend login
        email: email.trim(), // Boşluksuz e-posta
        password: password, // Şifre
      });

      const { token, role } = response.data; // JWT ve rol
      const userRole = typeof role === "string" ? role.toLowerCase() : "client"; // Küçük harf normalize

      localStorage.setItem("token", token); // http.js interceptor bunu okur
      localStorage.setItem("userRole", userRole); // Sidebar menü seçimi için

      try {
        const me = await api.get("/api/Auth/profile"); // Görünen adı al
        const d = (me.data?.displayName || "").trim();
        if (d) localStorage.setItem("userName", d); // Tam ad
        else localStorage.setItem("userName", email.split("@")[0]); // E-posta ön eki
        localStorage.setItem("theme", "light"); // Varsayılan açık tema
        document.documentElement.classList.remove("dark"); // Koyu sınıfı kaldır
      } catch (meErr) {
        console.error("Oturum profili alınamadı", meErr); // Profil opsiyonel
        localStorage.setItem("userName", email.split("@")[0]); // Yedek isim
      }

      if (userRole === "admin") navigate("/admin"); // Admin paneli
      else if (userRole === "dietitian") navigate("/dietitian"); // Diyetisyen paneli
      else navigate("/client"); // Danışan paneli (varsayılan)
    } catch (err) {
      if (err.response) { // Sunucu hata cevabı verdi
        setError(err.response.data.message || "Giriş başarısız.");
      } else { // Ağ hatası
        setError("Sunucuya bağlanılamadı. Sunucunun çalıştığından (ör. 5231) emin olun.");
      }
    } finally {
      setLoading(false); // Butonu tekrar aktif et
    }
  };

  return (
    <AuthLayout // Ortak auth sayfa düzeni
      footer={
        <>
          <span className="font-medium text-slate-600">Demo hesaplar: </span>
          admin@nutribridge.ai · dietitian@nutribridge.ai · client@nutribridge.ai
        </>
      }
    >
      <AuthPageHeader title="Hoş geldiniz" subtitle="NutriBridge hesabınıza giriş yapın" />

      <AuthSuccess message={successMessage} /> {/* Kayıt sonrası yeşil kutu */}
      <AuthError message={error} /> {/* Kırmızı hata kutusu */}

      <form className="space-y-6" onSubmit={handleLogin}> {/* Enter ile submit */}
        <AuthField id="login-email" label="E-posta adresi">
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            placeholder="ornek@nutribridge.ai"
            className={authInputClass}
            value={email}
            onChange={(e) => setEmail(e.target.value)} // Controlled input
            required
          />
        </AuthField>

        <AuthField id="login-password" label="Şifre">
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            className={authInputClass}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </AuthField>

        <div className="flex justify-end">
          <button
            type="button" // Form submit etmesin
            className="shrink-0 border-0 bg-transparent p-0 text-sm font-medium text-[#2ECC71] hover:underline"
            onClick={() => uiAlert({ title: "Yakında", message: "Şifre sıfırlama yakında eklenecek." })}
          >
            Şifremi unuttum
          </button>
        </div>

        <button type="submit" disabled={loading} className={authBtnPrimaryClass}>
          {loading ? ( // İstek sürerken spinner
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Giriş yapılıyor…
            </>
          ) : (
            "Giriş yap"
          )}
        </button>
      </form>

      <AuthFooterLinks /> {/* Danışan/diyetisyen kayıt linkleri */}
    </AuthLayout>
  );
}
