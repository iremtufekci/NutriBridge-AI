import { SidebarLayout } from "../../components/SidebarLayout";
import { Droplet, Scale, Camera, ChefHat, Bell, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function ClientHome() {
  const navigate = useNavigate();
  // Giriş yapan kullanıcının ismini al, yoksa "Danışan" yaz
  const userName = localStorage.getItem("userName") || "Danışan";

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="p-8 space-y-8 max-w-5xl mx-auto bg-[#0F172A] min-h-screen text-white">
        
        {/* Üst Bar / Karşılama */}
        <div className="flex items-center justify-between">
          <header>
            <h1 className="text-3xl font-bold text-white">Günaydın, {userName} ☀️</h1>
            <p className="text-slate-400 mt-1">Hadi bugünkü hedeflerine bakalım.</p>
          </header>
          <button className="relative p-2 bg-[#1E293B] border border-slate-700 rounded-xl hover:bg-slate-800 transition-all">
            <Bell size={24} className="text-slate-300" />
            <span className="absolute top-2 right-2 w-2 h-2 bg-[#22C55E] rounded-full" />
          </button>
        </div>

        {/* Kalori Özeti (Modern SVG Progress ile) */}
        <div className="bg-gradient-to-br from-[#EAF3DE] to-[#D4E7C5] rounded-3xl p-8 flex flex-col items-center shadow-lg">
          <div className="relative w-44 h-44">
            {/* Arka Plan Çemberi */}
            <svg className="w-full h-full transform -rotate-90">
              <circle
                cx="88"
                cy="88"
                r="78"
                stroke="#C0D6AB"
                strokeWidth="12"
                fill="none"
              />
              {/* İlerleme Çemberi */}
              <circle
                cx="88"
                cy="88"
                r="78"
                stroke="#22C55E"
                strokeWidth="12"
                fill="none"
                strokeDasharray="490"
                strokeDashoffset={490 - (490 * 1240) / 1800} // Dinamik hesaplama
                strokeLinecap="round"
                className="transition-all duration-1000"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-[#0F172A]">
              <span className="text-4xl font-black">1.240</span>
              <span className="text-xs font-semibold opacity-70">/ 1.800 kcal</span>
            </div>
          </div>

          <div className="flex gap-3 mt-8">
            <span className="px-4 py-1.5 bg-white/50 backdrop-blur-md rounded-full text-[11px] font-bold text-[#0F172A] border border-white/20">
              PROTEİN %42
            </span>
            <span className="px-4 py-1.5 bg-white/50 backdrop-blur-md rounded-full text-[11px] font-bold text-[#0F172A] border border-white/20">
              KARB %35
            </span>
            <span className="px-4 py-1.5 bg-white/50 backdrop-blur-md rounded-full text-[11px] font-bold text-[#0F172A] border border-white/20">
              YAĞ %23
            </span>
          </div>
        </div>

        {/* Hızlı İşlemler */}
        <div className="grid grid-cols-3 gap-4">
          <button 
            onClick={() => navigate("/client/journal")}
            className="bg-[#1E293B] p-5 rounded-2xl border border-slate-700 flex flex-col items-center gap-3 hover:border-[#3B82F6] hover:bg-[#1E293B]/80 transition-all group"
          >
            <div className="p-3 bg-blue-500/10 rounded-xl group-hover:scale-110 transition-transform">
              <Droplet className="text-blue-500" size={28} />
            </div>
            <span className="text-sm font-semibold">Su Ekle</span>
          </button>

          <button 
            onClick={() => navigate("/client/food-scan")}
            className="bg-[#1E293B] p-5 rounded-2xl border border-slate-700 flex flex-col items-center gap-3 hover:border-[#22C55E] hover:bg-[#1E293B]/80 transition-all group"
          >
            <div className="p-3 bg-[#22C55E]/10 rounded-xl group-hover:scale-110 transition-transform">
              <Camera className="text-[#22C55E]" size={28} />
            </div>
            <span className="text-sm font-semibold">Fotoğrafla</span>
          </button>

          <button 
            onClick={() => navigate("/client/ai-chef")}
            className="bg-[#1E293B] p-5 rounded-2xl border border-slate-700 flex flex-col items-center gap-3 hover:border-orange-500 hover:bg-[#1E293B]/80 transition-all group"
          >
            <div className="p-3 bg-orange-500/10 rounded-xl group-hover:scale-110 transition-transform">
              <ChefHat className="text-orange-500" size={28} />
            </div>
            <span className="text-sm font-semibold">AI Şef</span>
          </button>
        </div>

        {/* Bilgilendirme Kartı */}
        <div className="bg-[#1E293B] border border-slate-700 rounded-2xl p-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-[#22C55E]/20 rounded-full flex items-center justify-center">
              <Scale className="text-[#22C55E]" size={24} />
            </div>
            <div>
              <p className="text-sm font-bold">Kilo Takibi</p>
              <p className="text-xs text-slate-400">Hedefine ulaşmana son 4.2 kg kaldı!</p>
            </div>
          </div>
          <ChevronRight className="text-slate-500" />
        </div>
      </div>
    </SidebarLayout>
  );
}