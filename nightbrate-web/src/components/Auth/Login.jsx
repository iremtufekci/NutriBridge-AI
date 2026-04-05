import { useState } from "react";
import { Link } from "react-router-dom"; // useNavigate şu an kullanılmadığı için kaldırıldı

export function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = (e) => {
    e.preventDefault();
    console.log("Giriş denemesi:", email);
    alert("Giriş özelliği yakında eklenecek! Şimdilik kayıt sayfalarını test edebilirsiniz.");
  };

  return (
    <div className="min-h-screen bg-[#0F172A] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-[#1E293B] rounded-2xl p-8 border border-[#334155] shadow-2xl">
          
          {/* Logo Bölümü */}
          <div className="text-center mb-10">
            <h1 className="text-4xl font-extrabold text-[#22C55E] mb-2">NightBrate</h1>
            <p className="text-[#94A3B8] text-sm">Akıllı Beslenme Platformu</p>
          </div>

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

            <p className="text-[#22C55E] text-sm hover:underline cursor-pointer inline-block">Şifremi Unuttum</p>

            <button type="submit" className="w-full py-4 bg-[#22C55E] text-white font-bold rounded-xl text-lg hover:bg-[#16A34A] transition-all transform hover:scale-[1.01] active:scale-[0.98]">
              Giriş Yap
            </button>
          </form>

          <p className="text-[#94A3B8] text-xs text-center mt-4 italic">Sisteme rolünüze göre otomatik yönlendirilirsiniz.</p>

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

        {/* Demo Kutusu */}
        <div className="mt-4 p-4 bg-[#1E293B]/50 rounded-xl border border-[#334155]/50 text-center">
          <p className="text-[#94A3B8] text-[10px]">
            <strong>Demo:</strong> admin@nightbrate.com | dietitian@nightbrate.com | client@nightbrate.com
          </p>
        </div>
      </div>
    </div>
  );
}