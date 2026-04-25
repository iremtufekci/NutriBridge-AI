import { SidebarLayout } from "../../components/SidebarLayout";
import {
  Apple,
  Bell,
  Camera,
  ChefHat,
  ChevronRight,
  Droplet,
  Salad,
  Scale,
  ShoppingBasket,
  Soup,
  UtensilsCrossed,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

export function ClientHome() {
  const navigate = useNavigate();
  const userName = useAuthProfileDisplayName();

  const addWater = async () => {
    const ml = Number(window.prompt("Kac ml su ictiniz?", "250"));
    if (!ml || ml <= 0) return;
    try {
      await api.post("/api/client/water", { ml });
      alert("Su takibi kaydedildi.");
    } catch (error) {
      alert("Su kaydi basarisiz: " + (error as any)?.response?.data?.message || "Hata");
    }
  };

  const addWeight = async () => {
    const weight = Number(window.prompt("Guncel kilonuz?", "70"));
    if (!weight || weight <= 0) return;
    try {
      await api.post("/api/client/weight", { weight });
      alert("Kilo kaydi eklendi.");
    } catch (error) {
      alert("Kilo kaydi basarisiz: " + (error as any)?.response?.data?.message || "Hata");
    }
  };

  const meals = [
    { name: "Kahvaltı", kcal: 420, status: "Tamamlandı", statusColor: "text-emerald-500", borderColor: "border-emerald-500", icon: Soup },
    { name: "Öğle", kcal: 540, status: "Bekliyor", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: UtensilsCrossed },
    { name: "Akşam", kcal: 480, status: "Bekliyor", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: Salad },
    { name: "Ara Öğün", kcal: 180, status: "Atlandı", statusColor: "text-rose-500", borderColor: "border-rose-400", icon: Apple },
  ];

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-full bg-[#F8FAF7] dark:bg-slate-950 px-4 py-6 pb-4 transition-colors sm:px-6 sm:pb-6 lg:px-8 md:pb-8">
        <div className="mx-auto max-w-6xl space-y-6">
          <div className="flex items-start justify-between gap-3">
            <header>
              <h1 className="text-2xl sm:text-4xl font-bold text-slate-900 dark:text-slate-100">Günaydın, {userName} 🌞</h1>
              <p className="text-slate-500 dark:text-slate-400 mt-1 text-sm sm:text-base">Günlük hedeflerinizi gözden geçirin</p>
            </header>
            <button className="relative p-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-all">
              <Bell size={22} className="text-slate-500 dark:text-slate-300" />
              <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-rose-500 rounded-full" />
            </button>
          </div>

          <section className="bg-[#EAF3DE] rounded-3xl p-5 sm:p-8 flex flex-col items-center shadow-sm border border-[#E1EBD3]">
            <div className="relative w-40 h-40 sm:w-44 sm:h-44">
              <svg className="w-full h-full -rotate-90">
                <circle cx="88" cy="88" r="62" stroke="#C0D6AB" strokeWidth="12" fill="none" />
                <circle
                  cx="88"
                  cy="88"
                  r="62"
                  stroke="#22C55E"
                  strokeWidth="12"
                  fill="none"
                  strokeDasharray="390"
                  strokeDashoffset={390 - (390 * 1240) / 1800}
                  strokeLinecap="round"
                  className="transition-all duration-1000"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-[#0F172A]">
                <span className="text-4xl font-black leading-none">1.240</span>
                <span className="text-sm opacity-70 mt-1">/ 1.800 kcal</span>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3 mt-6">
              <span className="px-3 py-1 bg-white/70 dark:bg-slate-100/80 rounded-full text-xs font-semibold text-slate-700">Protein %42</span>
              <span className="px-3 py-1 bg-white/70 dark:bg-slate-100/80 rounded-full text-xs font-semibold text-slate-700">Karb %35</span>
              <span className="px-3 py-1 bg-white/70 dark:bg-slate-100/80 rounded-full text-xs font-semibold text-slate-700">Yağ %23</span>
            </div>
          </section>

          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">Bugünkü Öğünler</h2>
              <button className="text-sm text-emerald-600 font-semibold hover:text-emerald-700">Tümünü Gör</button>
            </div>

            <div className="space-y-3">
              {meals.map((meal) => {
                const MealIcon = meal.icon;
                return (
                  <div
                    key={meal.name}
                    className={`bg-white dark:bg-slate-900 rounded-2xl border ${meal.borderColor} dark:border-slate-700 border-l-4 p-4 sm:p-5 shadow-sm flex items-center justify-between gap-3`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                        <MealIcon size={18} className="text-slate-600 dark:text-slate-300" />
                      </div>
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-slate-100">
                          {meal.name} <span className={`text-sm font-medium ${meal.statusColor}`}>{meal.status}</span>
                        </p>
                        <p className="text-slate-500 dark:text-slate-400 text-sm">{meal.kcal} kcal</p>
                      </div>
                    </div>
                    <button
                      onClick={() => navigate("/client/food-scan")}
                      className="text-emerald-500 hover:text-emerald-600 text-sm font-semibold"
                    >
                      Fotoğrafla
                    </button>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <button
              onClick={addWater}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-5 rounded-2xl flex flex-col items-center gap-2 hover:border-blue-300 transition-all"
            >
              <Droplet className="text-blue-500" size={22} />
              <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Su Ekle</span>
            </button>
            <button
              onClick={addWeight}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-5 rounded-2xl flex flex-col items-center gap-2 hover:border-amber-300 transition-all"
            >
              <Scale className="text-amber-500" size={22} />
              <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Kilo Gir</span>
            </button>
            <button className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-5 rounded-2xl flex flex-col items-center gap-2 hover:border-emerald-300 transition-all">
              <ShoppingBasket className="text-emerald-500" size={22} />
              <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Alışveriş</span>
            </button>
          </section>

          <section className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-2xl p-4 sm:p-5 flex items-center justify-between shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-emerald-500 text-white flex items-center justify-center font-bold">MK</div>
              <div>
                <p className="font-semibold text-slate-900 dark:text-slate-100">Dr. Merve K.</p>
                <p className="text-sm text-slate-500 dark:text-slate-400">Bugünkü programınızı kontrol ettim...</p>
              </div>
            </div>
            <ChevronRight className="text-slate-400 dark:text-slate-500" />
          </section>

          <section className="bg-[#DDF3E7] dark:bg-emerald-500/10 border border-[#CBEAD9] dark:border-emerald-500/20 rounded-2xl p-5 sm:p-6 flex items-start justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded-full bg-emerald-500 flex items-center justify-center text-white shadow-md">
                <ChefHat size={22} />
              </div>
              <div>
                <p className="font-bold text-slate-900 dark:text-slate-100">
                  AI Mutfak Şefi <span className="text-xs text-emerald-600 ml-1">YENİ</span>
                </p>
                <p className="text-slate-600 dark:text-slate-300 text-sm mt-1">Elinizdeki malzemelerle yapay zeka ile sağlıklı tarifler oluşturun</p>
                <button
                  onClick={() => navigate("/client/ai-chef")}
                  className="mt-2 text-emerald-700 text-sm font-semibold hover:text-emerald-800"
                >
                  Hemen Dene
                </button>
              </div>
            </div>
            <Camera className="text-emerald-600 hidden sm:block" size={20} />
          </section>
        </div>
      </div>
    </SidebarLayout>
  );
}