import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../../api/http";

const primary = "#2ECC71";
const label = "#333333";
const muted = "#777777";
const inputBorder = "#DDE1E4";

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
        const storedTheme = me.data?.themePreference === "dark" ? "dark" : "light";
        localStorage.setItem("theme", storedTheme);
        document.documentElement.classList.toggle("dark", storedTheme === "dark");
      } catch (meErr) {
        console.error("Oturum profili alinamadi", meErr);
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
        setError("Sunucuya bağlanılamadı. Lütfen Backend'in (5231) çalıştığından emin olun.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="min-h-svh w-full flex flex-col items-center justify-center px-4 py-8 sm:px-6"
      style={{
        background: "#F0F0F0",
        fontFamily: "'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
        paddingTop: "max(2rem, env(safe-area-inset-top, 0px))",
        paddingBottom: "max(2rem, env(safe-area-inset-bottom, 0px))"
      }}
    >
      <div className="w-full max-w-[450px] flex flex-col gap-4">
        <div
          className="bg-white rounded-[24px] p-8 sm:p-10"
          style={{
            boxShadow: "0 8px 32px rgba(0,0,0,0.08), 0 2px 8px rgba(0,0,0,0.04)"
          }}
        >
          <div className="text-center mb-8 sm:mb-10">
            <h1
              className="text-[1.75rem] sm:text-[2rem] font-bold leading-tight mb-2"
              style={{ color: primary }}
            >
              NutriBridge AI
            </h1>
            <p className="text-sm sm:text-base" style={{ color: muted }}>
              Akıllı Beslenme Platformu
            </p>
          </div>

          {error && (
            <div
              className="mb-6 p-3 rounded-xl text-sm text-center"
              style={{
                background: "rgba(231, 76, 60, 0.08)",
                border: "1px solid rgba(231, 76, 60, 0.35)",
                color: "#C0392B"
              }}
            >
              {error}
            </div>
          )}

          <form className="space-y-5 sm:space-y-6" onSubmit={handleLogin}>
            <div className="space-y-2">
              <label
                className="text-sm font-bold block"
                style={{ color: label }}
                htmlFor="login-email"
              >
                E-posta
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="ornek@email.com"
                className="w-full px-4 py-3.5 rounded-[14px] text-[#333] placeholder:text-[#999] outline-none transition-shadow focus:ring-2"
                style={{
                  background: "#fff",
                  border: `1px solid ${inputBorder}`,
                  boxShadow: "none"
                }}
                onFocus={(e) => (e.currentTarget.style.boxShadow = `0 0 0 3px ${primary}33`)}
                onBlur={(e) => (e.currentTarget.style.boxShadow = "none")}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-bold block"
                style={{ color: label }}
                htmlFor="login-password"
              >
                Şifre
              </label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                className="w-full px-4 py-3.5 rounded-[14px] text-[#333] placeholder:text-[#999] outline-none transition-shadow"
                style={{
                  background: "#fff",
                  border: `1px solid ${inputBorder}`
                }}
                onFocus={(e) => (e.currentTarget.style.boxShadow = `0 0 0 3px ${primary}33`)}
                onBlur={(e) => (e.currentTarget.style.boxShadow = "none")}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <div className="-mt-1">
              <button
                type="button"
                className="text-sm font-medium bg-transparent border-0 p-0 cursor-pointer hover:underline"
                style={{ color: primary }}
                onClick={() => window.alert("Şifre sıfırlama yakında eklenecek.")}
              >
                Şifremi Unuttum
              </button>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-4 text-white font-bold text-base sm:text-lg transition-opacity rounded-full"
              style={{
                background: primary,
                opacity: loading ? 0.65 : 1
              }}
            >
              {loading ? "Giriş Yapılıyor…" : "Giriş Yap"}
            </button>

            <p className="text-center text-xs sm:text-sm pt-1" style={{ color: muted }}>
              Sisteme rolünüze göre otomatik yönlendirilirsiniz.
            </p>
          </form>

          <div className="mt-8 sm:mt-10 pt-6 border-t border-[#ECECEC] text-center space-y-3">
            <p className="text-sm" style={{ color: muted }}>
              Hesabınız yok mu?{" "}
              <Link
                to="/register-client"
                className="font-bold hover:underline"
                style={{ color: primary }}
              >
                Danışan Kaydı
              </Link>
            </p>
            <p className="text-sm" style={{ color: muted }}>
              Diyetisyen misiniz?{" "}
              <Link
                to="/register-dietitian"
                className="font-bold hover:underline"
                style={{ color: primary }}
              >
                Kayıt Olun
              </Link>
            </p>
          </div>
        </div>

        <div
          className="rounded-xl px-4 py-3 text-center text-xs sm:text-sm"
          style={{
            background: "#F5F5F5",
            border: "1px solid #E0E0E0",
            color: "#666"
          }}
        >
          <span className="font-semibold" style={{ color: "#555" }}>
            Demo:{" "}
          </span>
          admin@nutribridge.ai | dietitian@nutribridge.ai | client@nutribridge.ai
        </div>
      </div>
    </div>
  );
}
