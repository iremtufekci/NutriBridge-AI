import { useCallback, useEffect, useMemo, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { Apple, Bell, Camera, ChefHat, ChevronRight, Loader2, Salad, Soup, UtensilsCrossed } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type ClientProfile = {
  firstName?: string;
  lastName?: string;
  targetCalories: number;
  goalText: string;
  dietitianName: string;
};

type ProgramDay = {
  programDate: string;
  breakfast: string;
  lunch: string;
  dinner: string;
  snack: string;
  breakfastCalories?: number;
  lunchCalories?: number;
  dinnerCalories?: number;
  snackCalories?: number;
  totalCalories: number;
  dietitianName?: string | null;
};

const DAY_SHORT = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"] as const;

function toYmd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function startOfDay(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function isSameDay(a: Date, b: Date) {
  return startOfDay(a).getTime() === startOfDay(b).getTime();
}

function getUpcomingDaysFromToday(count: number): Date[] {
  const t = startOfDay(new Date());
  return Array.from({ length: count }, (_, i) => {
    const x = new Date(t);
    x.setDate(t.getDate() + i);
    return x;
  });
}

function initialsFromDietitianName(raw: string) {
  if (!raw || /atanmadi/i.test(raw)) return "?";
  const s = raw.replace(/^Dr\.\s*/i, "").trim();
  if (!s) return "?";
  const parts = s.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return s.slice(0, 2).toUpperCase();
}

export function ClientHome() {
  const navigate = useNavigate();
  const userName = useAuthProfileDisplayName();
  const [profileLoad, setProfileLoad] = useState(true);
  const [programLoad, setProgramLoad] = useState(true);
  const [profile, setProfile] = useState<ClientProfile | null>(null);
  const [dayProgram, setDayProgram] = useState<ProgramDay | null>(null);
  const [selected, setSelected] = useState(() => startOfDay(new Date()));
  const upcomingDays = useMemo(() => getUpcomingDaysFromToday(14), []);

  const loadProfile = useCallback(async () => {
    setProfileLoad(true);
    try {
      const pr = await api.get("/api/client/profile");
      setProfile({
        firstName: pr.data.firstName,
        lastName: pr.data.lastName,
        targetCalories: Number(pr.data.targetCalories) || 0,
        goalText: (pr.data.goalText as string) || "—",
        dietitianName: (pr.data.dietitianName as string) || "Atanmadi",
      });
    } catch {
      setProfile(null);
    } finally {
      setProfileLoad(false);
    }
  }, []);

  const loadProgramForDate = useCallback(async (ymd: string) => {
    setProgramLoad(true);
    try {
      const { data } = await api.get<ProgramDay | null>("/api/client/diet-program", { params: { programDate: ymd } });
      setDayProgram(data && typeof data === "object" ? data : null);
    } catch {
      setDayProgram(null);
    } finally {
      setProgramLoad(false);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  const selectedYmd = useMemo(() => toYmd(selected), [selected]);
  useEffect(() => {
    void loadProgramForDate(selectedYmd);
  }, [loadProgramForDate, selectedYmd]);

  const selectedDateLabel = useMemo(() => {
    return selected.toLocaleDateString("tr-TR", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
  }, [selected]);

  const isSelectedToday = isSameDay(selected, new Date());
  const pageLoading = profileLoad;

  const greetingName =
    (profile && `${profile.firstName || ""} ${profile.lastName || ""}`.trim()) || userName;

  const targetKcal = profile?.targetCalories ?? 0;
  const planTotal = useMemo(() => {
    const p = dayProgram;
    if (!p) return 0;
    const s =
      (p.breakfastCalories ?? 0) + (p.lunchCalories ?? 0) + (p.dinnerCalories ?? 0) + (p.snackCalories ?? 0);
    if (s > 0) return s;
    return p.totalCalories ?? 0;
  }, [dayProgram]);

  const kcalForSlot = (p: ProgramDay, slot: "b" | "l" | "d" | "s") => {
    const a = p.breakfastCalories ?? 0;
    const b = p.lunchCalories ?? 0;
    const c = p.dinnerCalories ?? 0;
    const d = p.snackCalories ?? 0;
    if (a + b + c + d > 0) {
      if (slot === "b") return Math.max(0, a);
      if (slot === "l") return Math.max(0, b);
      if (slot === "d") return Math.max(0, c);
      return Math.max(0, d);
    }
    if (p.totalCalories > 0) return Math.round(p.totalCalories / 4);
    return 0;
  };
  const hasPerMealKcal = (p: ProgramDay) =>
    (p.breakfastCalories ?? 0) + (p.lunchCalories ?? 0) + (p.dinnerCalories ?? 0) + (p.snackCalories ?? 0) > 0;

  /** Profil hedefine göre bugünkü atanan planın payı (görsel halka). */
  const planFillRatio = targetKcal > 0 ? Math.min(1, planTotal / targetKcal) : 0;
  const ringDashOffset = 390 - 390 * planFillRatio;

  const displayDietitianName = useMemo(() => {
    const fromProgram = dayProgram?.dietitianName?.trim();
    if (fromProgram) return fromProgram;
    const d = profile?.dietitianName?.trim();
    if (d && d !== "Atanmadi") return d;
    return "Diyetisyen atanmadi";
  }, [profile?.dietitianName, dayProgram?.dietitianName]);

  const hasLiveDietitian = displayDietitianName !== "Diyetisyen atanmadi";

  const mealRows: {
    key: string;
    name: string;
    text: string;
    kcal: number;
    status: string;
    statusColor: string;
    borderColor: string;
    icon: typeof Soup;
  }[] = useMemo(() => {
    if (!dayProgram) {
      return [
        { key: "b", name: "Kahvaltı", text: "", kcal: 0, status: "Plan yok", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: Soup },
        { key: "l", name: "Öğle", text: "", kcal: 0, status: "Plan yok", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: UtensilsCrossed },
        { key: "d", name: "Akşam", text: "", kcal: 0, status: "Plan yok", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: Salad },
        { key: "s", name: "Ara Öğün", text: "", kcal: 0, status: "Plan yok", statusColor: "text-slate-500", borderColor: "border-slate-200", icon: Apple },
      ];
    }
    const t = {
      b: (dayProgram.breakfast || "").trim(),
      l: (dayProgram.lunch || "").trim(),
      d: (dayProgram.dinner || "").trim(),
      s: (dayProgram.snack || "").trim(),
    };
    const row = (key: "b" | "l" | "d" | "s", name: string, icon: typeof Soup) => {
      const te = t[key];
      const has = te.length > 0;
      return {
        key,
        name,
        text: te,
        kcal: kcalForSlot(dayProgram, key),
        status: has ? "Planda" : "Bekleniyor",
        statusColor: has ? "text-emerald-600 dark:text-[#2ECC71]" : "text-amber-600 dark:text-amber-500",
        borderColor: has ? "border-emerald-500" : "border-amber-300",
        icon,
      };
    };
    return [
      row("b", "Kahvaltı", Soup),
      row("l", "Öğle", UtensilsCrossed),
      row("d", "Akşam", Salad),
      row("s", "Ara Öğün", Apple),
    ];
  }, [dayProgram]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-full bg-[#F8FAF7] dark:bg-slate-950 px-4 py-6 pb-4 transition-colors sm:px-6 sm:pb-6 lg:px-8 md:pb-8">
        <div className="mx-auto max-w-6xl space-y-6">
          {pageLoading && (
            <p className="flex items-center gap-2 text-slate-500 text-sm">
              <Loader2 className="w-4 h-4 animate-spin" /> Profil yükleniyor…
            </p>
          )}
          <div className="flex items-start justify-between gap-3">
            <header>
              <h1 className="text-2xl sm:text-4xl font-bold text-slate-900 dark:text-slate-100">Günaydın, {greetingName} 🌞</h1>
              <p className="text-slate-500 dark:text-[#9CA3AF] mt-1 text-sm sm:text-base">
                {hasLiveDietitian
                  ? `${displayDietitianName} — ${isSelectedToday ? "Bugün" : "Seçili gün"} için öğünler ve günlük plan aşağıda.`
                  : "Diyetisyeninizle eşleştiğinizde planınız burada görünür."}
              </p>
            </header>
            <button className="relative p-2.5 bg-white dark:bg-[#1F2937] border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-[#2D3748] transition-all">
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
                  strokeDashoffset={ringDashOffset}
                  strokeLinecap="round"
                  className="transition-all duration-1000"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-[#0D1117] dark:text-slate-100 text-center px-1">
                <span className="text-2xl sm:text-4xl font-black leading-tight">
                  {programLoad ? "…" : dayProgram && planTotal > 0 ? planTotal : "—"}
                </span>
                <span className="text-xs sm:text-sm opacity-70 mt-1">
                  {isSelectedToday ? "Bugün" : "Seçili gün"} / {targetKcal > 0 ? `${targetKcal} kcal hedef` : "hedef tanımlı değil"}
                </span>
              </div>
            </div>

            <p className="text-center text-sm text-slate-600 dark:text-[#9CA3AF] max-w-sm mt-2">
              {targetKcal > 0
                ? "Halka, profil hedefinize göre seçili günün diyetisyen planı toplamının payını gösterir."
                : "Profilinizde kalori hedefi tanımlayın; karşılaştırma buna göre hesaplanır."}
            </p>

            <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3 mt-4">
              <span className="px-3 py-1 bg-white/70 dark:bg-slate-100/80 rounded-full text-xs font-semibold text-slate-700 dark:text-slate-800">
                {profile?.goalText || "Hedef yok"}
              </span>
              {targetKcal > 0 && (
                <span className="px-3 py-1 bg-white/70 dark:bg-slate-100/80 rounded-full text-xs font-semibold text-slate-700 dark:text-slate-800">
                  {targetKcal} kcal / gun
                </span>
              )}
            </div>
          </section>

          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">Öğünler (diyetisyen planı)</h2>
              <button
                type="button"
                onClick={() => navigate("/client/diet-program")}
                className="text-sm text-emerald-600 font-semibold hover:text-emerald-700"
              >
                Ayrıntı
              </button>
            </div>

            {!dayProgram && !programLoad && (
              <p className="text-sm text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-800/50 rounded-xl px-3 py-2">
                {selectedYmd} için henüz program kaydı yok. Diyetisyeniniz atadığında bu liste dolar.{" "}
                <button type="button" onClick={() => void loadProgramForDate(selectedYmd)} className="underline font-medium">
                  Yenile
                </button>
              </p>
            )}

            <div className="space-y-3">
              {mealRows.map((meal) => {
                const MealIcon = meal.icon;
                return (
                  <div
                    key={meal.key}
                    className={`bg-white dark:bg-[#1F2937] rounded-2xl border ${meal.borderColor} dark:border-slate-700 border-l-4 p-4 sm:p-5 shadow-sm`}
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="w-9 h-9 shrink-0 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                        <MealIcon size={18} className="text-slate-600 dark:text-slate-300" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="font-semibold text-slate-900 dark:text-slate-100">
                          {meal.name}{" "}
                          <span className={`text-sm font-medium ${meal.statusColor}`}>{meal.status}</span>
                        </p>
                        <p className="text-slate-500 dark:text-[#9CA3AF] text-sm">
                          {meal.kcal > 0
                            ? dayProgram && hasPerMealKcal(dayProgram)
                              ? `${meal.kcal} kcal (diyetisyen)`
                              : `~${meal.kcal} kcal (toplam/4, eski kayit)`
                            : "0 kcal"}
                        </p>
                        {meal.text ? (
                          <p className="text-slate-600 dark:text-slate-300 text-sm mt-1 line-clamp-2">{meal.text}</p>
                        ) : null}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="rounded-2xl border border-emerald-200/80 dark:border-emerald-800/50 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700 dark:text-[#2ECC71] mb-2">
              Günlük plan (diyetisyen)
            </p>
            <div className="flex items-baseline flex-wrap gap-2 gap-y-1">
              {programLoad ? (
                <span className="flex items-center gap-2 text-slate-500 text-lg">
                  <Loader2 className="w-5 h-5 animate-spin" /> Hesaplanıyor…
                </span>
              ) : (
                <span className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tabular-nums">
                  {dayProgram && planTotal > 0 ? `${planTotal.toLocaleString("tr-TR")} kcal` : "—"}
                </span>
              )}
            </div>
            <p className="text-sm text-slate-500 dark:text-[#9CA3AF] mt-1">{selectedDateLabel}</p>
            <p className="text-xs text-slate-500 dark:text-[#9CA3AF] mt-3 mb-2">Günü seçin (bugün ve sonraki 14 gün)</p>
            <div className="flex gap-1 sm:gap-2 overflow-x-auto pb-1 -mx-0.5 px-0.5 scrollbar-thin">
              {upcomingDays.map((d) => {
                const idx = d.getDay() === 0 ? 6 : d.getDay() - 1;
                const short = DAY_SHORT[idx];
                const n = d.getDate();
                const active = isSameDay(d, selected);
                const now = new Date();
                const today = isSameDay(d, now);
                const tmr = new Date(now);
                tmr.setDate(tmr.getDate() + 1);
                const tomorrow = isSameDay(d, tmr);
                const dayLabel = today ? "Bugün" : tomorrow ? "Yarın" : short;
                return (
                  <button
                    key={toYmd(d)}
                    type="button"
                    onClick={() => setSelected(startOfDay(d))}
                    className={`flex flex-col items-center min-w-[2.75rem] sm:min-w-[3.25rem] py-2 rounded-2xl border-2 text-xs sm:text-sm transition-all shrink-0 ${
                      active
                        ? "border-emerald-500 bg-emerald-500 text-white shadow"
                        : today
                          ? "border-emerald-500/50 bg-slate-50 dark:bg-slate-800/80 text-slate-900 dark:text-white"
                          : "border-slate-200 dark:border-slate-600 bg-slate-50/80 dark:bg-slate-800/50 text-slate-600 dark:text-slate-300"
                    }`}
                  >
                    <span className="text-[9px] sm:text-[10px] font-medium opacity-90 leading-tight text-center px-0.5">{dayLabel}</span>
                    <span className="text-base sm:text-lg font-bold leading-tight">{n}</span>
                  </button>
                );
              })}
            </div>
          </section>

          <button
            type="button"
            onClick={() => navigate("/client/diet-program")}
            className="w-full text-left bg-white dark:bg-[#1F2937] border border-slate-200 dark:border-slate-700 rounded-2xl p-4 sm:p-5 flex items-center justify-between gap-2 shadow-sm hover:border-emerald-300 dark:hover:border-emerald-600 transition-colors"
          >
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-11 h-11 shrink-0 rounded-full bg-emerald-500 text-white flex items-center justify-center text-sm font-bold">
                {initialsFromDietitianName(displayDietitianName)}
              </div>
              <div className="min-w-0">
                <p className="font-semibold text-slate-900 dark:text-slate-100 truncate">{displayDietitianName}</p>
                <p className="text-sm text-slate-500 dark:text-[#9CA3AF]">
                  {hasLiveDietitian
                    ? "Öğün listenin altında gün seçimi ve günlük toplam kcal. Tam takvim: Diyet Programım."
                    : "Takip kodu ile diyetisyeninize bağlanın; profil sayfasından eşleştirme yapabilirsiniz."}
                </p>
              </div>
            </div>
            <ChevronRight className="text-slate-400 dark:text-[#9CA3AF] shrink-0" />
          </button>

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