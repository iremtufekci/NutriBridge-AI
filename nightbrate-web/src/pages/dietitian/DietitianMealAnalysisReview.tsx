// Diyetisyen yemek analizi inceleme sayfası — danışanların fotoğraf tabanlı kalori tahminleri
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Flame, Loader2, ScanSearch, Search } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage, resolveMediaUrl } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

// Tek bir yemek analizi kaydı
type MealAnalysisLog = {
  id?: string;
  clientId?: string;
  clientFirstName?: string;
  clientLastName?: string;
  clientDisplayName?: string;
  photoUrl?: string;
  calories?: number;
  detectedFoods?: string[];
  timestampUtc?: string;
  protein?: number;
  carb?: number;
  fat?: number;
};

// Danışan seçim listesi öğesi
type ClientOption = { id: string; label: string };

// Danışan listesi API yanıtı
type MyClientsResponse = {
  clients?: { id: string; displayName?: string; firstName?: string; lastName?: string }[];
};

// Belirli bir danışanın yemek analizi sayfasına yönlendirme yolu üretir
export function dietitianMealAnalysisPath(clientId?: string): string {
  if (!clientId) return "/dietitian/meal-analysis";
  return `/dietitian/meal-analysis?clientId=${encodeURIComponent(clientId)}`;
}

export function DietitianMealAnalysisReview() {
  const dietitianName = useAuthProfileDisplayName();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedClientId = searchParams.get("clientId")?.trim() || "";
  const showAll = searchParams.get("view") === "all";

  // Danışan listesi ve analiz kayıtları durumu
  const [clients, setClients] = useState<ClientOption[]>([]);
  const [loadingClients, setLoadingClients] = useState(true);
  const [logs, setLogs] = useState<MealAnalysisLog[]>([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [clientQuery, setClientQuery] = useState("");

  // Bağlı danışanları API'den yükler
  const loadClients = useCallback(async () => {
    setLoadingClients(true);
    try {
      const { data } = await api.get<MyClientsResponse>("/api/dietitian/my-clients", {
        params: { tab: "all", sort: "nameAsc" },
      });
      const mapped = (data.clients ?? [])
        .filter((c) => c.id)
        .map((c) => ({
          id: c.id,
          label:
            c.displayName?.trim() ||
            `${c.firstName || ""} ${c.lastName || ""}`.trim() ||
            "Danışan",
        }));
      setClients(mapped);
    } catch {
      setClients([]);
    } finally {
      setLoadingClients(false);
    }
  }, []);

  // Seçili danışan veya "tümü" moduna göre analiz kayıtlarını yükler
  const loadLogs = useCallback(async () => {
    if (!showAll && !selectedClientId) {
      setLogs([]);
      setError(null);
      return;
    }
    setLoadingLogs(true);
    setError(null);
    try {
      const { data } = await api.get<MealAnalysisLog[]>("/api/dietitian/meal-analysis-logs", {
        params: {
          take: showAll ? 80 : 50,
          ...(selectedClientId && !showAll ? { clientId: selectedClientId } : {}),
        },
      });
      setLogs(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(getApiErrorMessage(e));
      setLogs([]);
    } finally {
      setLoadingLogs(false);
    }
  }, [selectedClientId, showAll]);

  useEffect(() => {
    void loadClients();
  }, [loadClients]);

  useEffect(() => {
    void loadLogs();
  }, [loadLogs]);

  // Arama kutusuna göre danışan listesini filtreler
  const filteredClients = useMemo(() => {
    const q = clientQuery.trim().toLowerCase();
    if (!q) return clients;
    return clients.filter((c) => c.label.toLowerCase().includes(q));
  }, [clients, clientQuery]);

  // Seçili danışanın görünen adını bulur
  const selectedClientLabel = useMemo(() => {
    if (!selectedClientId) return "";
    return clients.find((c) => c.id === selectedClientId)?.label ?? "Seçili danışan";
  }, [clients, selectedClientId]);

  // URL parametresini güncelleyerek danışan seçer
  const selectClient = (clientId: string) => {
    setSearchParams({ clientId });
  };

  // Tüm danışanların kayıtlarını göstermek için URL parametresini ayarlar
  const showAllClients = () => {
    setSearchParams({ view: "all" });
  };

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-slate-50 min-h-screen text-slate-900 transition-colors pb-24 lg:pb-8">
        {/* Sayfa başlığı ve açıklama */}
        <div>
          <h1 className="text-4xl font-bold flex items-center gap-2">
            <ScanSearch className="w-9 h-9 text-emerald-500 shrink-0" />
            Yemek analizi
          </h1>
          <p className="text-slate-500 mt-1 max-w-2xl">
            Danışanınızı seçin; <strong>fotoğraf yükleyerek yaptığı kalori tahmini</strong> kayıtlarını
            (görsel, tahmini kkal, tespit edilen besinler) burada inceleyin.
          </p>
        </div>

        {/* Danışan seçimi ve filtre paneli */}
        <div className="rounded-2xl border border-slate-200 bg-white p-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={showAllClients}
              className={[
                "rounded-full px-4 py-2 text-sm font-semibold transition-colors",
                showAll
                  ? "bg-emerald-500 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200",
              ].join(" ")}
            >
              Tüm kayıtlar
            </button>
            {selectedClientId && !showAll && (
              <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-sm font-medium text-emerald-800">
                Seçili: {selectedClientLabel}
              </span>
            )}
          </div>

          <div className="flex flex-col sm:flex-row gap-2 sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="search"
                placeholder="Danışan adı ile ara…"
                className="w-full rounded-xl border border-slate-300 bg-slate-50 pl-10 pr-3 py-3"
                value={clientQuery}
                onChange={(e) => setClientQuery(e.target.value)}
              />
            </div>
            {loadingClients && (
              <Loader2 className="w-5 h-5 animate-spin text-emerald-500 self-center" />
            )}
          </div>

          <div className="max-h-52 overflow-y-auto rounded-xl border border-slate-200 divide-y divide-slate-100">
            {loadingClients ? (
              <p className="p-3 text-sm text-slate-500">Danışan listesi yükleniyor…</p>
            ) : filteredClients.length === 0 ? (
              <p className="p-3 text-sm text-slate-500">
                {clients.length === 0
                  ? "Henüz bağlı danışan yok."
                  : "Arama sonucu yok; aramayı sadeleştirin."}
              </p>
            ) : (
              filteredClients.map((client) => (
                <button
                  key={client.id}
                  type="button"
                  onClick={() => selectClient(client.id)}
                  className={[
                    "w-full text-left px-3 py-2.5 text-sm font-medium transition-colors",
                    selectedClientId === client.id && !showAll
                      ? "bg-emerald-500/20 text-emerald-800 ring-1 ring-inset ring-emerald-500/30"
                      : "hover:bg-slate-50",
                  ].join(" ")}
                >
                  {client.label}
                </button>
              ))
            )}
          </div>
        </div>

        {/* İçerik alanı — duruma göre boş, yükleme, hata veya kayıt listesi */}
        {!showAll && !selectedClientId ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-14 text-center">
            <UserIcon />
            <p className="mt-3 font-medium text-slate-700">Danışan seçin</p>
            <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
              Yukarıdaki listeden bir danışan seçtiğinizde, o danışanın görsel ile yaptığı kalori tahmini
              kayıtları burada listelenir.
            </p>
          </div>
        ) : loadingLogs ? (
          <div className="flex items-center gap-2 text-slate-600 py-8">
            <Loader2 className="w-5 h-5 animate-spin text-emerald-500" />
            Kalori tahmini kayıtları yükleniyor…
          </div>
        ) : error ? (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
            {error}
          </div>
        ) : logs.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-14 text-center">
            <ScanSearch className="mx-auto h-10 w-10 text-slate-300" />
            <p className="mt-3 font-medium text-slate-700">
              {showAll ? "Henüz kayıt yok" : `${selectedClientLabel} için kayıt yok`}
            </p>
            <p className="mt-1 text-sm text-slate-500">
              Danışan yemek analizi ekranından fotoğraf yüklediğinde veriler burada görünür.
            </p>
          </div>
        ) : (
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-xl font-bold text-slate-900">
                {showAll ? "Tüm danışanların kayıtları" : `${selectedClientLabel} — kalori tahminleri`}
              </h2>
              <span className="text-sm text-slate-500">{logs.length} kayıt</span>
            </div>

            {/* Analiz kayıtları kart grid'i */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {logs.map((log) => {
                const photoSrc = log.photoUrl ? resolveMediaUrl(log.photoUrl) : "";
                const foods = (log.detectedFoods || []).filter(Boolean);
                const when = log.timestampUtc
                  ? new Date(log.timestampUtc).toLocaleString("tr-TR", {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })
                  : "—";
                const name =
                  log.clientDisplayName?.trim() ||
                  `${log.clientFirstName || ""} ${log.clientLastName || ""}`.trim() ||
                  "Danışan";

                return (
                  <article
                    key={log.id || `${log.clientId}-${log.timestampUtc}`}
                    className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                  >
                    {photoSrc ? (
                      <img
                        src={photoSrc}
                        alt={`${name} öğün fotoğrafı`}
                        className="h-48 w-full object-cover bg-slate-100"
                      />
                    ) : (
                      <div className="flex h-48 items-center justify-center bg-slate-100 text-slate-400">
                        <ScanSearch className="h-10 w-10" />
                      </div>
                    )}
                    <div className="p-4 space-y-2">
                      {showAll && (
                        <p className="text-sm font-semibold text-slate-800">{name}</p>
                      )}
                      <p className="text-xs text-slate-500">{when}</p>
                      <div className="flex items-center gap-2">
                        <Flame className="h-5 w-5 text-amber-500 shrink-0" />
                        <p className="text-2xl font-bold text-emerald-700">
                          {typeof log.calories === "number" && log.calories > 0
                            ? `${log.calories} kkal`
                            : "—"}
                        </p>
                        <span className="text-xs font-medium text-slate-500">tahmini</span>
                      </div>
                      {foods.length > 0 ? (
                        <p className="text-sm text-slate-600">
                          <span className="font-medium text-slate-800">Tespit edilen: </span>
                          {foods.join(", ")}
                        </p>
                      ) : (
                        <p className="text-sm text-slate-500">Besin tespiti yok.</p>
                      )}
                      {log.protein || log.carb || log.fat ? (
                        <p className="text-xs text-slate-500 tabular-nums rounded-lg bg-slate-50 px-2 py-1.5">
                          Protein {Math.round(log.protein || 0)}g · Karbonhidrat {Math.round(log.carb || 0)}g ·
                          Yağ {Math.round(log.fat || 0)}g
                        </p>
                      ) : null}
                    </div>
                  </article>
                );
              })}
            </div>
          </section>
        )}
      </div>
    </SidebarLayout>
  );
}

// Danışan seçilmediğinde gösterilen yer tutucu ikon
function UserIcon() {
  return (
    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
      <ScanSearch className="h-6 w-6" />
    </div>
  );
}
