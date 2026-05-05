import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Loader2,
  Search,
  ArrowDownAZ,
  ArrowUpAZ,
  FileText,
  ChefHat,
  Check,
  X,
  CalendarDays,
} from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { API_BASE_URL, api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type TabKey = "all" | "active" | "critical" | "passive";

type TabCounts = {
  all: number;
  active: number;
  critical: number;
  passive: number;
};

type ClientCard = {
  id: string;
  firstName?: string;
  lastName?: string;
  displayName: string;
  startedAtUtc: string;
  lastActivityUtc?: string | null;
  compliancePercent: number;
  segment: string;
  isCritical: boolean;
};

type MyClientsResponse = {
  tabCounts: TabCounts;
  clients: ClientCard[];
};

type KitchenRecipeDto = {
  title?: string;
  estimatedCalories?: number;
};

type KitchenLog = {
  id?: string;
  createdAtUtc: string;
  selectedRecipes?: KitchenRecipeDto[];
  preference?: string;
};

type PdfItem = {
  id: string;
  pdfUrl: string;
  originalFileName: string;
  summary?: string;
  createdAtUtc: string;
};

type ProgramMeal = {
  mealKey: string;
  label: string;
  description: string;
  calories: number;
  completed: boolean;
};

type ProgramDay = {
  programDate: string;
  weekdayLabel: string;
  meals: ProgramMeal[];
};

type OverviewResponse = {
  client: {
    clientId: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    targetCalories?: number;
  };
  compliancePercent: number;
  complianceReferenceDate?: string | null;
  weeklyProgramDays?: ProgramDay[];
  kitchenRecipeLogs: KitchenLog[];
  pdfAnalyses: PdfItem[];
};

function resolvePdfHref(path: string): string {
  if (!path) return "#";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const base = API_BASE_URL.replace(/\/$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  if (!base) return p;
  return `${base}${p}`;
}

function relativeActivity(iso?: string | null): string {
  if (!iso) return "Henüz aktivite yok";
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return "—";
  const diff = Math.max(0, Date.now() - t);
  const mins = Math.floor(diff / 60000);
  const h = Math.floor(diff / 3600000);
  const d = Math.floor(diff / 86400000);
  if (mins < 2) return "Az önce";
  if (mins < 60) return `${mins} dakika önce`;
  if (h < 24) return `${h} saat önce`;
  if (d < 14) return `${d} gün önce`;
  return `${d} gün önce`;
}

function formatStart(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString("tr-TR", { day: "numeric", month: "short", year: "numeric" });
  } catch {
    return iso;
  }
}

export function DietitianClients() {
  const dietitianName = useAuthProfileDisplayName();
  const navigate = useNavigate();
  const { clientId } = useParams<{ clientId?: string }>();

  const [loadingList, setLoadingList] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [tabCounts, setTabCounts] = useState<TabCounts>({ all: 0, active: 0, critical: 0, passive: 0 });
  const [clients, setClients] = useState<ClientCard[]>([]);

  const [tab, setTab] = useState<TabKey>("all");
  const [sort, setSort] = useState<"nameAsc" | "nameDesc">("nameAsc");
  const [query, setQuery] = useState("");

  const [overview, setOverview] = useState<OverviewResponse | null>(null);
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [overviewError, setOverviewError] = useState<string | null>(null);

  const loadList = useCallback(async () => {
    setLoadingList(true);
    setListError(null);
    try {
      const { data } = await api.get<MyClientsResponse>("/api/dietitian/my-clients", {
        params: { sort, tab },
      });
      const tc = data.tabCounts || ({} as TabCounts);
      setTabCounts({
        all: tc.all ?? 0,
        active: tc.active ?? 0,
        critical: tc.critical ?? 0,
        passive: tc.passive ?? 0,
      });
      setClients(Array.isArray(data.clients) ? data.clients : []);
    } catch (e) {
      setListError(getApiErrorMessage(e));
      setClients([]);
    } finally {
      setLoadingList(false);
    }
  }, [sort, tab]);

  useEffect(() => {
    if (clientId) return;
    void loadList();
  }, [clientId, loadList]);

  const loadOverview = useCallback(async (id: string) => {
    setLoadingOverview(true);
    setOverviewError(null);
    try {
      const { data } = await api.get<OverviewResponse>("/api/dietitian/client-overview", {
        params: { clientId: id },
      });
      setOverview(data);
    } catch (e) {
      setOverviewError(getApiErrorMessage(e));
      setOverview(null);
    } finally {
      setLoadingOverview(false);
    }
  }, []);

  useEffect(() => {
    if (!clientId) {
      setOverview(null);
      return;
    }
    void loadOverview(clientId);
  }, [clientId, loadOverview]);

  const filteredClients = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return clients;
    return clients.filter((c) => {
      const n = `${c.displayName || ""}`.toLowerCase();
      return n.includes(q);
    });
  }, [clients, query]);

  const toggleSort = () => {
    setSort((s) => (s === "nameAsc" ? "nameDesc" : "nameAsc"));
  };

  /* ——— Detay görünümü ——— */
  if (clientId) {
    const display =
      overview?.client?.firstName || overview?.client?.lastName
        ? `${overview?.client?.firstName || ""} ${overview?.client?.lastName || ""}`.trim()
        : overview?.client?.email || "Danışan";

    return (
      <SidebarLayout userRole="dietitian" userName={dietitianName}>
        <div className="mx-auto max-w-4xl px-4 py-6 space-y-6">
          <button
            type="button"
            onClick={() => navigate("/dietitian/clients")}
            className="inline-flex items-center gap-2 text-sm font-medium text-emerald-600 hover:text-emerald-700"
          >
            <ArrowLeft className="w-4 h-4" />
            Danışanlarıma dön
          </button>

          {loadingOverview && (
            <div className="flex items-center gap-2 text-slate-600">
              <Loader2 className="w-5 h-5 animate-spin" /> Yükleniyor…
            </div>
          )}
          {overviewError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
              {overviewError}
            </div>
          )}

          {!loadingOverview && overview && (
            <>
              <div>
                <h1 className="text-2xl font-bold text-slate-900">{display}</h1>
                <p className="text-sm text-slate-500">
                  Hedef kalori: {overview.client.targetCalories ?? "—"} kkal
                </p>
              </div>

              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <CalendarDays className="w-5 h-5 text-emerald-600" />
                  <h2 className="text-lg font-bold text-slate-900">Diyet programı</h2>
                </div>
                <p className="text-xs text-slate-500 mb-4">
                  Bu haftanın öğünleri; işaret tamamlanan, çarpı eksik veya işaretlenmemiş öğünleri gösterir.
                </p>
                {!overview.weeklyProgramDays?.length ? (
                  <p className="text-sm text-slate-500">Haftalık program verisi yüklenemedi.</p>
                ) : (
                  <div className="flex gap-4 overflow-x-auto pb-2 snap-x snap-mandatory scrollbar-thin">
                    {overview.weeklyProgramDays.map((day) => (
                      <div
                        key={day.programDate}
                        className="min-w-[220px] max-w-[260px] shrink-0 snap-start space-y-3 border border-slate-100 rounded-xl p-3 bg-slate-50/50"
                      >
                        <div>
                          <p className="font-bold text-slate-900">{day.weekdayLabel}</p>
                          <p className="text-xs text-slate-500">{day.programDate}</p>
                        </div>
                        {!day.meals?.length ? (
                          <p className="text-sm text-slate-500">Bu gün için program kaydı yok.</p>
                        ) : (
                          <div className="space-y-2">
                            {day.meals.map((m) => (
                              <div
                                key={`${day.programDate}-${m.mealKey}`}
                                className={[
                                  "rounded-xl border p-3 text-left transition-colors",
                                  m.completed
                                    ? "border-emerald-300 bg-emerald-50/90"
                                    : "border-slate-200 bg-white",
                                ].join(" ")}
                              >
                                <div className="flex justify-between items-start gap-2">
                                  <span className="text-xs font-medium text-slate-500">{m.label}</span>
                                  {m.completed ? (
                                    <Check className="w-4 h-4 text-emerald-600 shrink-0" aria-hidden />
                                  ) : (
                                    <X className="w-4 h-4 text-slate-400 shrink-0" aria-hidden />
                                  )}
                                </div>
                                <p className="font-semibold text-sm mt-1 text-slate-900 leading-snug">
                                  {(m.description || "").trim() || "—"}
                                </p>
                                <p className="text-xs text-slate-500 mt-1 tabular-nums">
                                  {typeof m.calories === "number" && m.calories > 0 ? `${m.calories} kkal` : "—"}
                                </p>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </section>

              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <ChefHat className="w-5 h-5 text-emerald-600" />
                  <h2 className="text-lg font-bold text-slate-900">Yapay zeka ile üretilen tarifler</h2>
                </div>
                {!overview.kitchenRecipeLogs?.length ? (
                  <p className="text-sm text-slate-500">Henüz kayıtlı yapay zeka tarif üretimi yok.</p>
                ) : (
                  <ul className="space-y-4">
                    {overview.kitchenRecipeLogs.map((log) => (
                      <li
                        key={log.id || log.createdAtUtc}
                        className="rounded-xl border border-slate-100 bg-slate-50/80 p-4"
                      >
                        <p className="text-xs text-slate-500 mb-2">
                          {new Date(log.createdAtUtc).toLocaleString("tr-TR")}{" "}
                          {log.preference ? `· ${log.preference}` : ""}
                        </p>
                        <div className="space-y-2">
                          {(log.selectedRecipes || []).map((r, i) => (
                            <div key={`${log.id}-${i}`} className="font-medium text-slate-900">
                              {r.title || "Tarif"}
                              {typeof r.estimatedCalories === "number" ? (
                                <span className="text-sm font-normal text-slate-500 ml-2">~{r.estimatedCalories} kkal</span>
                              ) : null}
                            </div>
                          ))}
                          {!log.selectedRecipes?.length && <span className="text-sm text-slate-600">Tarif detayı yok.</span>}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <FileText className="w-5 h-5 text-emerald-600" />
                  <h2 className="text-lg font-bold text-slate-900">Yüklenen PDF analizleri</h2>
                </div>
                {!overview.pdfAnalyses?.length ? (
                  <p className="text-sm text-slate-500">Henüz PDF yükleme yok.</p>
                ) : (
                  <ul className="space-y-3">
                    {overview.pdfAnalyses.map((p) => (
                      <li
                        key={p.id}
                        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 px-4 py-3"
                      >
                        <div>
                          <p className="font-medium text-slate-900">{p.originalFileName}</p>
                          <p className="text-xs text-slate-500">
                            {new Date(p.createdAtUtc).toLocaleString("tr-TR")}
                            {p.summary ? ` · ${p.summary.slice(0, 120)}${p.summary.length > 120 ? "…" : ""}` : ""}
                          </p>
                        </div>
                        <a
                          href={resolvePdfHref(p.pdfUrl)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="shrink-0 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
                        >
                          PDF’yi aç
                        </a>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </>
          )}
        </div>
      </SidebarLayout>
    );
  }

  /* ——— Liste görünümü ——— */
  const tabs: { key: TabKey; label: string; count: number }[] = [
    { key: "all", label: "Tümü", count: tabCounts.all },
    { key: "active", label: "Aktif", count: tabCounts.active },
    { key: "critical", label: "Kritik", count: tabCounts.critical },
    { key: "passive", label: "Pasif", count: tabCounts.passive },
  ];

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="mx-auto max-w-6xl px-4 py-6 space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Danışanlarım</h1>
          <p className="text-slate-600 mt-1">Aktif danışanlarınızı takip edin ve yönetin</p>
        </div>

        <div className="flex flex-wrap gap-2">
          {tabs.map((x) => (
            <button
              key={x.key}
              type="button"
              onClick={() => setTab(x.key)}
              className={[
                "rounded-full px-4 py-2 text-sm font-semibold transition-colors",
                tab === x.key
                  ? "bg-emerald-600 text-white shadow"
                  : "bg-slate-100 text-slate-700 hover:bg-slate-200",
              ].join(" ")}
            >
              {x.label}{" "}
              <span className={tab === x.key ? "opacity-90" : "opacity-70"}>({x.count})</span>
            </button>
          ))}
        </div>

        <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="search"
              placeholder="Danışan ara (isim, soyisim...)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm"
            />
          </div>
          <button
            type="button"
            onClick={toggleSort}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 hover:bg-slate-50"
          >
            {sort === "nameAsc" ? <ArrowDownAZ className="w-4 h-4" /> : <ArrowUpAZ className="w-4 h-4" />}
            İsme göre sırala
          </button>
        </div>

        {listError && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
            {listError}
          </div>
        )}

        {loadingList ? (
          <div className="flex items-center gap-2 text-slate-600 py-12 justify-center">
            <Loader2 className="w-6 h-6 animate-spin" />
            Liste yükleniyor…
          </div>
        ) : filteredClients.length === 0 ? (
          <p className="text-center text-slate-500 py-12">
            {clients.length === 0 ? "Henüz bağlı danışan yok veya filtreye uygun kayıt yok." : "Aramanızla eşleşen danışan yok."}
          </p>
        ) : (
          <>
            <p className="text-sm text-slate-500">{filteredClients.length} danışan</p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {filteredClients.map((c) => {
                const initial = (c.displayName || "?").trim().charAt(0).toUpperCase();
                const pct = Math.min(100, Math.max(0, c.compliancePercent));
                const barOk = pct >= 70;
                const barWarn = pct >= 40 && pct < 70;
                const criticalCard = c.isCritical || c.segment === "critical";
                return (
                  <div
                    key={c.id}
                    className={[
                      "rounded-2xl border bg-white p-4 shadow-sm flex flex-col",
                      criticalCard ? "border-rose-400 ring-1 ring-rose-200" : "border-slate-200",
                    ].join(" ")}
                  >
                    <div className="flex items-start gap-3 mb-3">
                      <div
                        className={[
                          "flex h-12 w-12 shrink-0 items-center justify-center rounded-full text-lg font-bold text-white",
                          criticalCard ? "bg-rose-500" : "bg-emerald-500",
                        ].join(" ")}
                      >
                        {initial}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="font-bold text-slate-900 truncate">{c.displayName}</p>
                        <p className="text-xs text-slate-500">Başlangıç: {formatStart(c.startedAtUtc)}</p>
                        <p className="text-xs text-slate-500 mt-0.5">Son aktivite: {relativeActivity(c.lastActivityUtc)}</p>
                      </div>
                    </div>
                    <div className="mb-4">
                      <div className="flex justify-between text-xs font-medium text-slate-600 mb-1">
                        <span>Uyum oranı</span>
                        <span>%{pct}</span>
                      </div>
                      <div className="h-2 rounded-full bg-slate-100 overflow-hidden">
                        <div
                          className={[
                            "h-full rounded-full",
                            barOk ? "bg-emerald-500" : barWarn ? "bg-amber-500" : "bg-rose-500",
                          ].join(" ")}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                    <Link
                      to={`/dietitian/clients/${encodeURIComponent(c.id)}`}
                      className="mt-auto text-center rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700"
                    >
                      Görüntüle
                    </Link>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>
    </SidebarLayout>
  );
}
