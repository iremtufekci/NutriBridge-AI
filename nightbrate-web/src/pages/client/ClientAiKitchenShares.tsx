import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChefHat, Filter, Loader2, Share2, ChevronDown, ChevronUp } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

const PREF_LABEL: Record<string, string> = {
  practical: "Pratik",
  low_calorie: "Düşük Kalori",
  vegetarian: "Vejetaryen",
  high_protein: "Protein",
  vegan: "Vegan",
  gluten_free: "Glütensiz",
};

type Recipe = {
  title: string;
  description?: string | null;
  estimatedCalories: number;
  prepTimeMinutes?: number | null;
  ingredients: string[];
  steps: string[];
};

type LogItem = {
  id?: string;
  createdAtUtc: string;
  ingredients: string;
  preference: string;
  targetCalories: number;
  source?: string;
  selectedRecipes: Recipe[];
};

function toYmd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function addDays(d: Date, n: number) {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}

export function ClientAiKitchenShares() {
  const userName = useAuthProfileDisplayName();
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [items, setItems] = useState<LogItem[]>([]);
  const [skip, setSkip] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set());
  const pageSize = 50;
  const [hasMore, setHasMore] = useState(false);

  const load = useCallback(
    async (nextSkip: number, append: boolean) => {
      if (append) setLoadingMore(true);
      else {
        setLoading(true);
        setError(null);
      }
      try {
        const { data } = await api.get<LogItem[]>("/api/KitchenChef/my-shares", {
          params: {
            ...(from.trim() ? { from: from.trim() } : {}),
            ...(to.trim() ? { to: to.trim() } : {}),
            skip: nextSkip,
            take: pageSize,
          },
          timeout: 60_000,
        });
        const list = Array.isArray(data) ? data : [];
        if (append) setItems((prev) => [...prev, ...list]);
        else setItems(list);
        setHasMore(list.length === pageSize);
        setSkip(nextSkip + list.length);
      } catch (e) {
        if (!append) {
          setItems([]);
          setError(getApiErrorMessage(e));
        } else {
          setError(getApiErrorMessage(e));
        }
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [from, to]
  );

  useEffect(() => {
    void load(0, false);
  }, [load]);

  const applyQuickRange = (days: number) => {
    const end = new Date();
    const start = addDays(end, -days);
    setFrom(toYmd(start));
    setTo(toYmd(end));
  };

  const clearFilters = () => {
    setFrom("");
    setTo("");
  };

  const toggleExpand = (id: string) => {
    setExpanded((prev) => {
      const n = new Set(prev);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="mx-auto max-w-2xl px-4 py-6 pb-28 lg:pb-8">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-emerald-200/60">
              <Share2 className="h-6 w-6 text-emerald-600" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-800">Diyetisyene paylaştıklarım</h1>
              <p className="text-sm text-slate-600">Yapay zeka mutfak’ta seçip gönderdiğiniz tarifler</p>
            </div>
          </div>
          <Link
            to="/client/ai-chef"
            className="inline-flex items-center justify-center gap-2 self-start rounded-2xl border border-emerald-200 bg-white px-4 py-2.5 text-sm font-semibold text-emerald-700 shadow-sm"
          >
            <ChefHat className="h-4 w-4" />
            Yeni tarif
          </Link>
        </div>

        <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-800">
            <Filter className="h-4 w-4 text-emerald-600" />
            Filtrele
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="flex flex-col text-xs text-slate-500">
              Başlangıç (tarih)
              <input
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className="mt-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-800"
              />
            </label>
            <label className="flex flex-col text-xs text-slate-500">
              Bitiş (tarih)
              <input
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className="mt-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-800"
              />
            </label>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <span className="text-xs text-slate-500 self-center w-full sm:w-auto">Hızlı:</span>
            <button
              type="button"
              onClick={() => applyQuickRange(7)}
              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700"
            >
              Son 7 gün
            </button>
            <button
              type="button"
              onClick={() => applyQuickRange(30)}
              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700"
            >
              Son 30 gün
            </button>
            <button
              type="button"
              onClick={clearFilters}
              className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1.5 text-xs font-medium text-amber-900"
            >
              Sıfırla
            </button>
          </div>
        </div>

        {loading && (
          <p className="text-sm text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
          </p>
        )}

        {error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </div>
        )}

        {!loading && items.length === 0 && !error && (
          <p className="text-center text-sm text-slate-500 py-8">
            Bu kriterlere uyan paylaşım yok. Tarih aralığını değiştirip tekrar deneyin.
          </p>
        )}

        <ul className="space-y-4">
          {items.map((log) => {
            const key = log.id || log.createdAtUtc + log.preference;
            const when = (() => {
              try {
                return new Date(log.createdAtUtc).toLocaleString("tr-TR", { dateStyle: "medium", timeStyle: "short" });
              } catch {
                return log.createdAtUtc;
              }
            })();
            const pref = PREF_LABEL[log.preference?.toLowerCase() ?? ""] ?? log.preference;
            return (
              <li
                key={key}
                className="overflow-hidden rounded-2xl border border-emerald-200/80 bg-white shadow-sm"
              >
                <div className="border-b border-emerald-100 bg-emerald-50/50 px-4 py-3">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="text-xs text-slate-500">{when}</p>
                      <p className="mt-0.5 text-sm font-semibold text-slate-800">
                        Hedef {log.targetCalories} kkal · {pref}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                        log.source === "gemini"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-amber-100 text-amber-800"
                      }`}
                    >
                      {log.source === "gemini" ? "Yapay zeka" : "Örnek"}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-slate-600 line-clamp-2">
                    <span className="font-medium">Malzemeler: </span>
                    {log.ingredients}
                  </p>
                </div>
                {log.selectedRecipes?.map((r, i) => (
                  <div key={i + r.title} className="px-4 py-3 border-t border-slate-100">
                    <p className="font-bold text-slate-900">{r.title}</p>
                    {r.description && (
                      <p className="text-sm text-slate-600 mt-1">{r.description}</p>
                    )}
                    <p className="text-xs text-slate-500 mt-1">
                      ~{r.estimatedCalories} kkal
                      {r.prepTimeMinutes != null && r.prepTimeMinutes > 0 ? ` · ${r.prepTimeMinutes} dk` : ""}
                    </p>
                    <button
                      type="button"
                      onClick={() => toggleExpand(key + "-" + i)}
                      className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-emerald-600"
                    >
                      {expanded.has(key + "-" + i) ? (
                        <>
                          <ChevronUp className="w-3 h-3" /> Detayı gizle
                        </>
                      ) : (
                        <>
                          <ChevronDown className="w-3 h-3" /> Malzeme &amp; adımlar
                        </>
                      )}
                    </button>
                    {expanded.has(key + "-" + i) && (
                      <div className="mt-2 space-y-2 text-sm text-slate-700">
                        {r.ingredients?.length > 0 && (
                          <ul className="list-inside list-disc text-xs">
                            {r.ingredients.map((x) => (
                              <li key={x}>{x}</li>
                            ))}
                          </ul>
                        )}
                        {r.steps?.length > 0 && (
                          <ol className="list-inside list-decimal text-xs space-y-1">
                            {r.steps.map((s, j) => (
                              <li key={j}>{s}</li>
                            ))}
                          </ol>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </li>
            );
          })}
        </ul>

        {hasMore && !loading && items.length > 0 && (
          <div className="mt-6 flex justify-center">
            <button
              type="button"
              disabled={loadingMore}
              onClick={() => void load(skip, true)}
              className="inline-flex items-center gap-2 rounded-2xl border-2 border-emerald-500 bg-white px-5 py-2.5 text-sm font-semibold text-emerald-700"
            >
              {loadingMore ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Daha fazla yükle
            </button>
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
