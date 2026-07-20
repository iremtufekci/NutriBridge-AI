// React kancaları
import { useCallback, useEffect, useMemo, useState } from "react";
// İç sayfa linki
import { Link } from "react-router-dom";
// Kenar çubuğu düzeni
import { SidebarLayout } from "../../components/SidebarLayout";
// HTTP istemcisi
import { api } from "../../api/http";
// Kullanıcı görünen adı
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
// İkonlar
import { Loader2, History, UtensilsCrossed, CalendarOff, Check, Filter } from "lucide-react";

// Geçmiş program kaydı tipi
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

type MealKey = "breakfast" | "lunch" | "dinner" | "snack";

// Öğün meta bilgisi (etiket ve tamamlanma alanı)
const MEAL_ITEMS: {
  key: MealKey;
  label: string;
  completedKey: keyof Pick<
    ProgramDay,
    "breakfastCompleted" | "lunchCompleted" | "dinnerCompleted" | "snackCompleted"
  >;
}[] = [
  { key: "breakfast", label: "Kahvaltı", completedKey: "breakfastCompleted" },
  { key: "lunch", label: "Öğle", completedKey: "lunchCompleted" },
  { key: "dinner", label: "Akşam", completedKey: "dinnerCompleted" },
  { key: "snack", label: "Ara öğün", completedKey: "snackCompleted" },
];

// Gün başlangıcına indirir
function startOfDay(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

// YYYY-MM-DD metnini yerel öğlen saatine çevirir (timezone kayması önlenir)
function ymdToLocalNoon(ymd: string) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd.trim());
  if (!m) return null;
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const day = Number(m[3]);
  return new Date(y, mo - 1, day, 12, 0, 0, 0);
}

// Verilen tarih bugünden önce mi?
function isYmdBeforeToday(ymd: string) {
  const t = ymdToLocalNoon(ymd);
  if (!t) return false;
  return startOfDay(t).getTime() < startOfDay(new Date()).getTime();
}

// YYYY-MM ay anahtarından Türkçe ay etiketi üretir
function monthLabelTr(ym: string) {
  const [y, m] = ym.split("-").map(Number);
  if (!y || !m) return ym;
  const d = new Date(y, m - 1, 1);
  return d.toLocaleDateString("tr-TR", { month: "long", year: "numeric" });
}

// Öğün kalorisi (ayrı alan yoksa toplam/4)
function mealKcalFor(p: ProgramDay, key: MealKey) {
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

// Geçmiş diyet programları listesi sayfası
export function ClientDietProgramHistory() {
  const userName = useAuthProfileDisplayName();
  const [loading, setLoading] = useState(true);
  const [programs, setPrograms] = useState<ProgramDay[]>([]);
  const [monthFilter, setMonthFilter] = useState<string>("all");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  // Tüm program kayıtlarını API'den çeker
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

  // Yalnızca bugünden önceki kayıtlar, yeniden eskiye sıralı
  const pastPrograms = useMemo(() => {
    return programs
      .filter((p) => p.programDate && isYmdBeforeToday(p.programDate))
      .sort((a, b) => b.programDate.localeCompare(a.programDate));
  }, [programs]);

  // Filtre için ay seçenekleri
  const monthOptions = useMemo(() => {
    const months = new Set<string>();
    for (const p of pastPrograms) {
      if (p.programDate.length >= 7) months.add(p.programDate.slice(0, 7));
    }
    return Array.from(months).sort().reverse();
  }, [pastPrograms]);

  // Ay ve tarih aralığı filtrelerini uygular
  const filteredPrograms = useMemo(() => {
    return pastPrograms.filter((p) => {
      const ymd = p.programDate;
      if (monthFilter !== "all" && !ymd.startsWith(monthFilter)) return false;
      if (dateFrom && ymd < dateFrom) return false;
      if (dateTo && ymd > dateTo) return false;
      return true;
    });
  }, [pastPrograms, monthFilter, dateFrom, dateTo]);

  const hasActiveFilter = monthFilter !== "all" || dateFrom !== "" || dateTo !== "";

  const clearFilters = () => {
    setMonthFilter("all");
    setDateFrom("");
    setDateTo("");
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-full bg-slate-50 px-4 py-6 pb-28 lg:pb-8 transition-colors">
        <div className="mx-auto max-w-3xl">
          {/* Başlık ve açıklama */}
          <div className="flex items-start justify-between gap-3 flex-wrap">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 flex items-center gap-2">
                <History className="w-7 h-7 sm:w-8 sm:h-8 text-amber-600" />
                Geçmiş diyet programlarım
              </h1>
              <p className="text-slate-500 text-sm mt-1">
                Bugünün <strong>tarihinden önce</strong> atanan günlere ait kayıtlar. Güncel ve ileri tarihleri
                görmek için{" "}
                <Link to="/client/diet-program" className="text-emerald-600 font-medium underline">
                  Diyet Programım
                </Link>{" "}
                sayfasını kullanın.
              </p>
            </div>
          </div>

          {/* Tarih filtre paneli */}
          {!loading && pastPrograms.length > 0 && (
            <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-sm font-bold text-slate-800 flex items-center gap-2">
                  <Filter className="w-4 h-4 text-emerald-600" />
                  Tarihe göre filtrele
                </h2>
                {hasActiveFilter && (
                  <button
                    type="button"
                    onClick={clearFilters}
                    className="text-xs font-semibold text-emerald-600 hover:underline"
                  >
                    Filtreyi temizle
                  </button>
                )}
              </div>

              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setMonthFilter("all")}
                  className={[
                    "rounded-full px-3 py-1.5 text-xs font-semibold border transition-colors",
                    monthFilter === "all"
                      ? "bg-emerald-500 text-white border-emerald-500"
                      : "bg-slate-50 text-slate-600 border-slate-200 hover:border-emerald-300",
                  ].join(" ")}
                >
                  Tümü
                </button>
                {monthOptions.map((ym) => (
                  <button
                    key={ym}
                    type="button"
                    onClick={() => setMonthFilter(ym)}
                    className={[
                      "rounded-full px-3 py-1.5 text-xs font-semibold border transition-colors",
                      monthFilter === ym
                        ? "bg-emerald-500 text-white border-emerald-500"
                        : "bg-slate-50 text-slate-600 border-slate-200 hover:border-emerald-300",
                    ].join(" ")}
                  >
                    {monthLabelTr(ym)}
                  </button>
                ))}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <label className="flex flex-col text-xs text-slate-500">
                  Başlangıç tarihi
                  <input
                    type="date"
                    value={dateFrom}
                    onChange={(e) => setDateFrom(e.target.value)}
                    className="mt-1 rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 text-sm text-slate-900"
                  />
                </label>
                <label className="flex flex-col text-xs text-slate-500">
                  Bitiş tarihi
                  <input
                    type="date"
                    value={dateTo}
                    onChange={(e) => setDateTo(e.target.value)}
                    className="mt-1 rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 text-sm text-slate-900"
                  />
                </label>
              </div>

              <p className="text-xs text-slate-500">
                {filteredPrograms.length} kayıt gösteriliyor
                {hasActiveFilter ? ` (${pastPrograms.length} geçmiş kayıt içinden)` : ""}
              </p>
            </section>
          )}

          {loading && (
            <p className="mt-8 flex items-center gap-2 text-slate-500">
              <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
            </p>
          )}

          {/* Hiç geçmiş kayıt yok */}
          {!loading && pastPrograms.length === 0 && (
            <div className="mt-8 rounded-2xl border border-dashed border-slate-300 bg-white/60 p-10 text-center">
              <CalendarOff className="w-10 h-10 mx-auto text-slate-400 mb-2" />
              <p className="text-slate-600">Geçmişe ait diyet programı kaydı yok.</p>
              <p className="text-sm text-slate-500 mt-2">Diyetisyeniniz yeni tarihler atadıkça burada birikir.</p>
            </div>
          )}

          {/* Filtre sonucu boş */}
          {!loading && pastPrograms.length > 0 && filteredPrograms.length === 0 && (
            <div className="mt-6 rounded-2xl border border-dashed border-amber-200 bg-amber-50/60 p-8 text-center">
              <p className="text-slate-700">Seçtiğiniz tarih aralığında kayıt bulunamadı.</p>
              <button
                type="button"
                onClick={clearFilters}
                className="mt-3 text-sm font-semibold text-emerald-600 hover:underline"
              >
                Filtreyi temizle
              </button>
            </div>
          )}

          {/* Geçmiş program kartları */}
          {!loading && filteredPrograms.length > 0 && (
            <ul className="mt-6 space-y-4">
              {filteredPrograms.map((p) => {
                const ymd = p.programDate;
                const d = ymdToLocalNoon(ymd);
                const head = d
                  ? d.toLocaleDateString("tr-TR", { weekday: "long", day: "numeric", month: "long", year: "numeric" })
                  : ymd;
                const completedCount = MEAL_ITEMS.filter((m) => p[m.completedKey] === true).length;
                return (
                  <li
                    key={ymd}
                    className="rounded-2xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm"
                  >
                    <div className="flex items-start justify-between gap-2 flex-wrap border-b border-slate-200 pb-3 mb-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">Tarih</p>
                        <p className="text-lg font-bold text-slate-900">{head}</p>
                        <p className="text-xs text-slate-500 font-mono">{ymd}</p>
                        {completedCount > 0 && (
                          <p className="text-xs text-emerald-700 mt-1 font-medium">
                            {completedCount} öğün tamamlandı olarak işaretlendi
                          </p>
                        )}
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-slate-500">Günlük toplam</p>
                        <p className="text-xl font-bold text-emerald-600">{p.totalCalories} kkal</p>
                      </div>
                    </div>
                    <div className="space-y-2">
                      {MEAL_ITEMS.map(({ key, label, completedKey }) => {
                        const t = (p[key] as string) || "";
                        const k = mealKcalFor(p, key);
                        const done = p[completedKey] === true;
                        if (!t.trim() && k === 0) return null;
                        return (
                          <div
                            key={key}
                            className={[
                              "flex gap-2 text-sm rounded-xl px-3 py-2",
                              done ? "bg-emerald-50/80 border border-emerald-100" : "bg-slate-50/50",
                            ].join(" ")}
                          >
                            <UtensilsCrossed className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                            <div className="min-w-0 flex-1">
                              <div className="flex flex-wrap items-center gap-2">
                                <span className="font-semibold text-slate-800">
                                  {label} {k > 0 ? <span className="text-emerald-600">({k} kkal)</span> : null}
                                </span>
                                {done && (
                                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-emerald-800">
                                    <Check className="w-3 h-3" aria-hidden />
                                    Tamamlandı
                                  </span>
                                )}
                              </div>
                              <p className="mt-0.5 text-slate-600 whitespace-pre-wrap">
                                {t.trim() ? t : <span className="italic text-slate-500">Sadece kalori tanımı</span>}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    {p.updatedAt && (
                      <p className="text-xs text-slate-500 mt-3 pt-2 border-t border-slate-100">
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
