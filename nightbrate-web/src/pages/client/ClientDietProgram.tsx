import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { Egg, Leaf, UtensilsCrossed, Apple, Loader2, Cookie } from "lucide-react";

const DAY_SHORT = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"] as const;

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
  hasProgram: boolean;
  updatedAt?: string;
  dietitianName?: string | null;
  breakfastCompleted?: boolean;
  lunchCompleted?: boolean;
  dinnerCompleted?: boolean;
  snackCompleted?: boolean;
};

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

function beforeCalendarDay(a: Date, b: Date) {
  return startOfDay(a).getTime() < startOfDay(b).getTime();
}

/** Bugünden (dahil) itibaren ardışık günler; geçmiş günler şeritte yok. */
function getUpcomingDaysFromToday(count: number): Date[] {
  const t = startOfDay(new Date());
  return Array.from({ length: count }, (_, i) => {
    const x = new Date(t);
    x.setDate(t.getDate() + i);
    return x;
  });
}

const MEALS: { key: "breakfast" | "lunch" | "dinner" | "snack"; title: string; time: string; icon: typeof Egg }[] = [
  { key: "breakfast", title: "Kahvaltı", time: "08:00", icon: Egg },
  { key: "lunch", title: "Öğle", time: "12:30", icon: Leaf },
  { key: "dinner", title: "Akşam", time: "19:00", icon: UtensilsCrossed },
  { key: "snack", title: "Ara Öğün", time: "16:00", icon: Apple },
];

export function ClientDietProgram() {
  const userName = useAuthProfileDisplayName();
  const [detailLoading, setDetailLoading] = useState(true);
  const [dayProgram, setDayProgram] = useState<ProgramDay | null>(null);
  const [selected, setSelected] = useState(() => startOfDay(new Date()));
  const [markingMeal, setMarkingMeal] = useState<string | null>(null);
  const fetchSeq = useRef(0);

  const upcomingDays = getUpcomingDaysFromToday(14);

  const loadDay = useCallback(async (ymd: string) => {
    const n = ++fetchSeq.current;
    setDetailLoading(true);
    try {
      const { data } = await api.get<ProgramDay | null>("/api/client/diet-program", { params: { programDate: ymd } });
      if (n !== fetchSeq.current) return;
      setDayProgram(data && typeof data === "object" ? data : null);
    } catch (e) {
      console.error(e);
      if (n !== fetchSeq.current) return;
      setDayProgram(null);
    } finally {
      if (n === fetchSeq.current) setDetailLoading(false);
    }
  }, []);

  const selectedYmd = useMemo(() => toYmd(selected), [selected]);

  useEffect(() => {
    void loadDay(selectedYmd);
  }, [loadDay, selectedYmd]);

  useEffect(() => {
    const onVis = () => {
      if (document.visibilityState === "visible") void loadDay(toYmd(selected));
    };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, [loadDay, selected]);

  useEffect(() => {
    const today0 = startOfDay(new Date());
    if (beforeCalendarDay(selected, today0)) {
      setSelected(today0);
    }
  }, [selected]);

  const kcalForMeal = (p: ProgramDay, key: (typeof MEALS)[number]["key"]) => {
    const a = p.breakfastCalories ?? 0;
    const b = p.lunchCalories ?? 0;
    const c = p.dinnerCalories ?? 0;
    const d = p.snackCalories ?? 0;
    if (a + b + c + d > 0) {
      if (key === "breakfast") return Math.max(0, a);
      if (key === "lunch") return Math.max(0, b);
      if (key === "dinner") return Math.max(0, c);
      return Math.max(0, d);
    }
    if (p.totalCalories > 0) return Math.round(p.totalCalories / 4);
    return 0;
  };

  const dietitianLabel = dayProgram?.dietitianName;

  const dayTotalKcal = useMemo(() => {
    const p = dayProgram;
    if (!p) return 0;
    const s =
      (p.breakfastCalories ?? 0) + (p.lunchCalories ?? 0) + (p.dinnerCalories ?? 0) + (p.snackCalories ?? 0);
    if (s > 0) return s;
    return p.totalCalories ?? 0;
  }, [dayProgram]);

  const mealCompleted = (p: ProgramDay, key: (typeof MEALS)[number]["key"]) => {
    if (key === "breakfast") return p.breakfastCompleted === true;
    if (key === "lunch") return p.lunchCompleted === true;
    if (key === "dinner") return p.dinnerCompleted === true;
    return p.snackCompleted === true;
  };

  const markMealComplete = async (key: (typeof MEALS)[number]["key"]) => {
    if (!window.confirm("Bu öğünü tamamlandı olarak kaydetmek istediğinize emin misiniz?")) return;
    setMarkingMeal(key);
    try {
      await api.post("/api/client/diet-program/meal-completed", {
        programDate: selectedYmd,
        meal: key,
      });
      await loadDay(selectedYmd);
    } catch (e) {
      window.alert(getApiErrorMessage(e));
    } finally {
      setMarkingMeal(null);
    }
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-full bg-slate-50 px-4 py-6 pb-28 lg:pb-8 transition-colors">
        <div className="mx-auto max-w-3xl">
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">Diyet Programım</h1>
          <p className="text-slate-500 text-sm mt-1">
            Tarih sekmesinde <strong>bugün ve ileri 14 gün</strong> gösteriliyor; güne tıklayın.
            {dietitianLabel ? ` ${dietitianLabel}` : ""}
          </p>

          <h2 className="text-lg font-bold text-slate-900 mt-8 mb-3">Diyet takvimi</h2>
          <p className="text-xs text-slate-500 -mt-1 mb-2">Bugün ve sonrası (14 gün) — soldan sağa kaydırın</p>
          <div className="flex gap-1 sm:gap-2 overflow-x-auto pb-2 -mx-1 px-1 scrollbar-thin">
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
                  className={`flex flex-col items-center min-w-[3rem] sm:min-w-[3.5rem] py-2.5 rounded-2xl border-2 transition-all shrink-0 ${
                    active
                      ? "border-emerald-500 bg-emerald-500 text-white shadow-md"
                      : today
                        ? "border-emerald-500/50 bg-white text-slate-900"
                        : "border-slate-200 bg-white text-slate-600"
                  }`}
                >
                  <span className="text-[10px] sm:text-xs font-medium opacity-90">{dayLabel}</span>
                  <span className="text-lg sm:text-xl font-bold leading-tight">{n}</span>
                </button>
              );
            })}
          </div>

          {detailLoading && (
            <p className="mt-6 flex items-center gap-2 text-slate-500">
              <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
            </p>
          )}

          {!detailLoading && !dayProgram && (
            <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white/60 p-8 text-center text-slate-500">
              {toYmd(selected)} tarihi için plan yok. Diyetisyeniniz bu tarihe program atadığında burada göreceksiniz.
            </div>
          )}

          {!detailLoading && dayProgram && (
            <div className="mt-4 space-y-4">
              {MEALS.map((m) => {
                const text = (dayProgram as Record<string, string>)[m.key] ?? "";
                const kcal = kcalForMeal(dayProgram, m.key);
                const isDone = mealCompleted(dayProgram, m.key);
                const isMarking = markingMeal === m.key;
                return (
                  <div
                    key={m.key}
                    className={`rounded-2xl border p-4 sm:p-5 transition-colors ${
                      isDone
                        ? "bg-emerald-50/90 border-emerald-200"
                        : "bg-white border-slate-200"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-3">
                        <div className="p-2 rounded-xl bg-slate-100 text-emerald-600">
                          {m.key === "snack" ? <Cookie className="w-5 h-5" /> : <m.icon className="w-5 h-5" />}
                        </div>
                        <div>
                          <h3 className="font-bold text-slate-900 text-lg">{m.title}</h3>
                          <p className="text-sm text-slate-500">{m.time}</p>
                        </div>
                      </div>
                      <span className="text-emerald-600 font-bold text-sm sm:text-base whitespace-nowrap">
                        {kcal} kkal
                      </span>
                    </div>
                    <p className="mt-3 text-slate-700 text-sm leading-relaxed min-h-[2.5rem]">
                      {text.trim() || "—"}
                    </p>
                    <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
                      <div className="inline-flex flex-wrap items-center gap-2 text-sm text-slate-600">
                        {isDone ? (
                          <span className="text-emerald-600 font-medium">Tamamlandı</span>
                        ) : (
                          <button
                            type="button"
                            disabled={isMarking}
                            onClick={() => void markMealComplete(m.key)}
                            className="inline-flex items-center gap-1.5 rounded-lg border-2 border-emerald-500/80 bg-emerald-50/80 text-emerald-800 font-semibold px-3 py-1.5 hover:bg-emerald-100/90 disabled:opacity-50"
                          >
                            {isMarking ? (
                              <>
                                <Loader2 className="w-3.5 h-3.5 animate-spin" /> Kaydediliyor…
                              </>
                            ) : (
                              "Tamamla"
                            )}
                          </button>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => window.alert("Alternatif öneriler yakında eklenecek.")}
                        className="text-sm font-semibold px-3 py-1.5 rounded-lg border-2 border-amber-400/80 text-amber-800 bg-amber-50/50 hover:bg-amber-100/80"
                      >
                        Evde yok
                      </button>
                    </div>
                  </div>
                );
              })}

              <p className="text-center text-sm text-slate-500 pt-1">
                Günlük toplam hedef: <strong className="text-slate-800">{dayTotalKcal} kkal</strong>
                {dayProgram.updatedAt && (
                  <span className="block text-xs mt-1 opacity-80">
                    Son güncelleme: {new Date(dayProgram.updatedAt).toLocaleString("tr-TR")}
                  </span>
                )}
              </p>
            </div>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
