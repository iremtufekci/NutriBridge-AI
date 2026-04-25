import { useState } from "react";
import { Link, useNavigate } from "react-router-dom"; 
import { api } from "../../api/http";

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
        email: email.trim(), // Gereksiz boşlukları temizler
        password: password
      });

      // Başarılı giriş: Verileri tarayıcı hafızasına al
      const { token, role } = response.data;
      const userRole = typeof role === "string" ? role.toLowerCase() : "client";
      
      localStorage.setItem("token", token);
      localStorage.setItem("userRole", userRole);

      try {
        const me = await api.get("/api/Auth/profile");
        const d = (me.data?.displayName || "").trim();
        if (d) localStorage.setItem("userName", d);
        else localStorage.setItem("userName", email.split("@")[0]);
      } catch (meErr) {
        console.error("Oturum profili alinamadi", meErr);
        localStorage.setItem("userName", email.split("@")[0]);
      }

      // Role göre yönlendirme
      if (userRole === "admin") {
        navigate("/admin");
      } else if (userRole === "dietitian") {
        navigate("/dietitian");
      } else {
        try {
          const profileResponse = await api.get("/api/client/profile");
          const storedTheme = profileResponse.data?.themePreference === "dark" ? "dark" : "light";
          localStorage.setItem("theme", storedTheme);
          document.documentElement.classList.toggle("dark", storedTheme === "dark");
        } catch (profileError) {
          console.error("Client tema bilgisi alinamadi", profileError);
        }
        navigate("/client"); 
      }

    } catch (err) {
      if (err.response) {
        // Backend'den gelen hata mesajını göster (E-posta veya şifre hatalı vb.)
        setError(err.response.data.message || "Giriş başarısız.");
      } else {
        // Backend kapalıysa buraya düşer
        setError("Sunucuya bağlanılamadı. Lütfen Backend'in (5231) çalıştığından emin olun.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0F172A] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-[#1E293B] rounded-2xl p-8 border border-[#334155] shadow-2xl">
          
          <div className="text-center mb-10">
            <h1 className="text-4xl font-extrabold text-[#22C55E] mb-2">NutriBridge</h1>
            <p className="text-[#94A3B8] text-sm">Akıllı Beslenme Platformu</p>
          </div>

          {error && (
            <div className="mb-6 p-3 bg-red-500/10 border border-red-500/50 rounded-lg text-red-500 text-sm text-center">
              {error}
            </div>
          )}

          <form className="space-y-6" onSubmit={handleLogin}>
            <div className="space-y-2">
              <label className="text-white text-sm font-bold block ml-1">E-posta</label>
              <input 
                type="email" 
                placeholder="ornek@email.com"
                className="w-full p-3 rounded-xl bg-[#0F172A] border border-[#334155] text-white placeholder-[#475569] focus:outline-none focus:border-[#22C55E] transition-all"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-white text-sm font-bold block ml-1">Şifre</label>
              <input 
                type="password" 
                placeholder="••••••••"
                className="w-full p-3 rounded-xl bg-[#0F172A] border border-[#334155] text-white focus:outline-none focus:border-[#22C55E] transition-all"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button 
              type="submit" 
              disabled={loading}
              className={`w-full py-4 bg-[#22C55E] text-white font-bold rounded-xl text-lg transition-all transform hover:scale-[1.01] active:scale-[0.98] ${loading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-[#16A34A]'}`}
            >
              {loading ? "Giriş Yapılıyor..." : "Giriş Yap"}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-[#334155] text-center space-y-2">
             <p className="text-[#94A3B8] text-sm">
               Hesabınız yok mu?{" "}
               <Link to="/register-client" className="text-[#22C55E] font-bold cursor-pointer hover:underline">
                 Danışan Kaydı
               </Link>
             </p>
             <p className="text-[#94A3B8] text-sm">
               Diyetisyen misiniz?{" "}
               <Link to="/register-dietitian" className="text-[#22C55E] font-bold cursor-pointer hover:underline">
                 Kayıt Olun
               </Link>
             </p>
          </div>
        </div>
      </div>
    </div>
  );
}