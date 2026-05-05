import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../../api/http";

const primary = "#2ECC71";

export function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await api.post("/api/auth/login", {
        email: email.trim(),
        password: password
      });

      const { token, role } = response.data;
      const userRole = typeof role === "string" ? role.toLowerCase() : "client";

      localStorage.setItem("token", token);
      localStorage.setItem("userRole", userRole);

      try {
        const me = await api.get("/api/Auth/profile");
        const d = (me.data?.displayName || "").trim();
        if (d) localStorage.setItem("userName", d);
        else localStorage.setItem("userName", email.split("@")[0]);
        localStorage.setItem("theme", "light");
        document.documentElement.classList.remove("dark");
      } catch (meErr) {
        console.error("Oturum profili alınamadı", meErr);
        localStorage.setItem("userName", email.split("@")[0]);
      }

      if (userRole === "admin") {
        navigate("/admin");
      } else if (userRole === "dietitian") {
        navigate("/dietitian");
      } else {
        navigate("/client");
      }
    } catch (err) {
      if (err.response) {
        setError(err.response.data.message || "Giriş başarısız.");
      } else {
        setError("Sunucuya bağlanılamadı. Sunucunun çalıştığından (ör. 5231) emin olun.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="flex min-h-svh w-full flex-col items-center justify-center bg-slate-50 px-4 py-8 pt-[max(2rem,env(safe-area-inset-top,0px))] pb-[max(2rem,env(safe-area-inset-bottom,0px))] font-[family-name:var(--font-inter,Inter),system-ui,sans-serif] sm:px-6"
    >
      <div className="flex w-full max-w-[450px] flex-col gap-4">
        <div className="rounded-[24px] border border-slate-200/90 bg-white p-8 shadow-lg shadow-slate-300/35 sm:p-10">
          <div className="text-center mb-8 sm:mb-10">
            <h1
              className="text-[1.75rem] sm:text-[2rem] font-bold leading-tight mb-2 text-[#2ECC71]"
            >
              NutriBridge
            </h1>
            <p className="text-sm sm:text-base text-[#777777]">
              Akıllı Beslenme Platformu
            </p>
          </div>

          {error && (
            <div className="mb-6 p-3 rounded-xl text-sm text-center bg-[rgba(231,76,60,0.08)] border border-[rgba(231,76,60,0.35)] text-[#C0392B]">
              {error}
            </div>
          )}

          <form className="space-y-5 sm:space-y-6" onSubmit={handleLogin}>
            <div className="space-y-2">
              <label
                className="text-sm font-bold block text-[#333333]"
                htmlFor="login-email"
              >
                E-posta
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="örnek@ornek.com"
                className="w-full px-4 py-3.5 rounded-[14px] bg-white border border-[#DDE1E4] text-[#333] placeholder:text-[#999] outline-none transition-shadow focus:ring-2 focus:ring-[#2ECC71]/40"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-bold block text-[#333333]"
                htmlFor="login-password"
              >
                Şifre
              </label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                className="w-full px-4 py-3.5 rounded-[14px] bg-white border border-[#DDE1E4] text-[#333] placeholder:text-[#999] outline-none transition-shadow focus:ring-2 focus:ring-[#2ECC71]/40"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <div className="-mt-1">
              <button
                type="button"
                className="text-sm font-medium bg-transparent border-0 p-0 cursor-pointer hover:underline text-[#2ECC71]"
                onClick={() => window.alert("Şifre sıfırlama yakında eklenecek.")}
              >
                Şifremi Unuttum
              </button>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-4 text-white font-bold text-base sm:text-lg transition-opacity rounded-full disabled:opacity-65"
              style={{ background: primary }}
            >
              {loading ? "Giriş Yapılıyor…" : "Giriş Yap"}
            </button>

            <p className="text-center text-xs sm:text-sm pt-1 text-[#777777]">
              Sisteme rolünüze göre otomatik yönlendirilirsiniz.
            </p>
          </form>

          <div className="mt-8 sm:mt-10 pt-6 border-t border-[#ECECEC] text-center space-y-3">
            <p className="text-sm text-[#777777]">
              Hesabınız yok mu?{" "}
              <Link to="/register-client" className="font-bold hover:underline text-[#2ECC71]">
                Danışan Kaydı
              </Link>
            </p>
            <p className="text-sm text-[#777777]">
              Diyetisyen misiniz?{" "}
              <Link to="/register-dietitian" className="font-bold hover:underline text-[#2ECC71]">
                Kayıt Olun
              </Link>
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-slate-200/80 bg-slate-50 px-4 py-3 text-center text-xs text-slate-600 sm:text-sm">
          <span className="font-semibold text-[#555555]">
            Örnek hesaplar:{" "}
          </span>
          yönetici: admin@nutribridge.ai · diyetisyen: dietitian@nutribridge.ai · danışan: client@nutribridge.ai
        </div>
      </div>
    </div>
  );
}
