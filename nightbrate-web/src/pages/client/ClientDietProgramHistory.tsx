import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { Loader2, History, UtensilsCrossed, CalendarOff } from "lucide-react";

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
};

function startOfDay(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function ymdToLocalNoon(ymd: string) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd.trim());
  if (!m) return null;
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const day = Number(m[3]);
  return new Date(y, mo - 1, day, 12, 0, 0, 0);
}

function isYmdBeforeToday(ymd: string) {
  const t = ymdToLocalNoon(ymd);
  if (!t) return false;
  return startOfDay(t).getTime() < startOfDay(new Date()).getTime();
}

const MEAL_LABELS: { key: keyof Pick<ProgramDay, "breakfast" | "lunch" | "dinner" | "snack">; label: string }[] = [
  { key: "breakfast", label: "Kahvaltı" },
  { key: "lunch", label: "Öğle" },
  { key: "dinner", label: "Akşam" },
  { key: "snack", label: "Ara öğün" },
];

function mealKcalFor(p: ProgramDay, key: (typeof MEAL_LABELS)[number]["key"]) {
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
}

export function ClientDietProgramHistory() {
  const userName = useAuthProfileDisplayName();
  const [loading, setLoading] = useState(true);
  const [programs, setPrograms] = useState<ProgramDay[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ProgramDay[]>("/api/client/diet-programs");
      setPrograms(Array.isArray(data) ? data : []);
    } catch {
      setPrograms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const pastPrograms = useMemo(() => {
    return programs
      .filter((p) => p.programDate && isYmdBeforeToday(p.programDate))
      .sort((a, b) => b.programDate.localeCompare(a.programDate));
  }, [programs]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-full bg-[#F8FAF7] dark:bg-slate-950 px-4 py-6 pb-28 lg:pb-8 transition-colors">
        <div className="mx-auto max-w-3xl">
          <div className="flex items-start justify-between gap-3 flex-wrap">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <History className="w-7 h-7 sm:w-8 sm:h-8 text-amber-600 dark:text-amber-400" />
                Geçmiş diyet programlarım
              </h1>
              <p className="text-slate-500 dark:text-[#9CA3AF] text-sm mt-1">
                Bugünün <strong>tarihinden önce</strong> atanan günlere ait kayıtlar. Güncel ve ileri tarihleri
                görmek için{" "}
                <Link to="/client/diet-program" className="text-emerald-600 dark:text-[#2ECC71] font-medium underline">
                  Diyet Programım
                </Link>{" "}
                sayfasını kullanın.
              </p>
            </div>
          </div>

          {loading && (
            <p className="mt-8 flex items-center gap-2 text-slate-500">
              <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
            </p>
          )}

          {!loading && pastPrograms.length === 0 && (
            <div className="mt-8 rounded-2xl border border-dashed border-slate-300 dark:border-slate-600 bg-white/60 dark:bg-[#1F2937]/50 p-10 text-center">
              <CalendarOff className="w-10 h-10 mx-auto text-slate-400 mb-2" />
              <p className="text-slate-600 dark:text-slate-300">Geçmişe ait diyet programı kaydı yok.</p>
              <p className="text-sm text-slate-500 mt-2">Diyetisyeniniz yeni tarihler atadıkça burada birikir.</p>
            </div>
          )}

          {!loading && pastPrograms.length > 0 && (
            <ul className="mt-6 space-y-4">
              {pastPrograms.map((p) => {
                const ymd = p.programDate;
                const d = ymdToLocalNoon(ymd);
                const head = d
                  ? d.toLocaleDateString("tr-TR", { weekday: "long", day: "numeric", month: "long", year: "numeric" })
                  : ymd;
                return (
                  <li
                    key={ymd}
                    className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm"
                  >
                    <div className="flex items-start justify-between gap-2 flex-wrap border-b border-slate-200 dark:border-slate-700 pb-3 mb-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide text-amber-700 dark:text-amber-500">
                          Tarih
                        </p>
                        <p className="text-lg font-bold text-slate-900 dark:text-white">{head}</p>
                        <p className="text-xs text-slate-500 font-mono">{ymd}</p>
                        {p.dietitianName && (
                          <p className="text-sm text-slate-600 dark:text-[#9CA3AF] mt-1">{p.dietitianName}</p>
                        )}
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-slate-500">Günlük toplam</p>
                        <p className="text-xl font-bold text-emerald-600 dark:text-[#2ECC71]">{p.totalCalories} kcal</p>
                      </div>
                    </div>
                    <div className="space-y-2">
                      {MEAL_LABELS.map(({ key, label }) => {
                        const t = (p[key] as string) || "";
                        const k = mealKcalFor(p, key);
                        if (!t.trim() && k === 0) return null;
                        return (
                          <div key={key} className="flex gap-2 text-sm">
                            <UtensilsCrossed className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                            <div>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {label} {k > 0 ? <span className="text-emerald-600">({k} kcal)</span> : null}:
                              </span>{" "}
                              {t.trim() ? (
                                <span className="text-slate-600 dark:text-[#9CA3AF] whitespace-pre-wrap">{t}</span>
                              ) : (
                                <span className="text-slate-500 italic">Sadece kalori tanimi</span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    {p.updatedAt && (
                      <p className="text-xs text-slate-500 mt-3 pt-2 border-t border-slate-100 dark:border-slate-800">
                        Son güncelleme: {new Date(p.updatedAt).toLocaleString("tr-TR")}
                      </p>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
