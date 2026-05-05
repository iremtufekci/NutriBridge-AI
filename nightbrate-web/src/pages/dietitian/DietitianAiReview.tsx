import { useCallback, useEffect, useMemo, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { BarChart3, Loader2, Search } from "lucide-react";

type ClientItem = { id?: string; firstName?: string; lastName?: string };

type KitchenRecipeSnapshot = {
  title: string;
  description?: string | null;
  estimatedCalories: number;
  prepTimeMinutes?: number | null;
  ingredients: string[];
  steps: string[];
};

type KitchenChefLogItem = {
  id?: string;
  createdAtUtc: string;
  ingredients: string;
  preference: string;
  targetCalories: number;
  source?: string;
  selectedRecipes: KitchenRecipeSnapshot[];
};

export function DietitianAiReview() {
  const dietitianName = useAuthProfileDisplayName();
  const [clients, setClients] = useState<ClientItem[]>([]);
  const [clientQuery, setClientQuery] = useState("");
  const [selectedClientId, setSelectedClientId] = useState("");
  const [loadingList, setLoadingList] = useState(true);
  const [kitchenLogs, setKitchenLogs] = useState<KitchenChefLogItem[]>([]);
  const [loadingKitchen, setLoadingKitchen] = useState(false);

  const filteredClients = useMemo(() => {
    const q = clientQuery.trim().toLowerCase();
    if (!q) return clients;
    return clients.filter((c) => {
      const n = `${c.firstName || ""} ${c.lastName || ""}`.trim().toLowerCase();
      return n.includes(q);
    });
  }, [clients, clientQuery]);

  const loadClients = useCallback(async () => {
    setLoadingList(true);
    try {
      const { data } = await api.get("/api/dietitian/clients-with-last-meal");
      const mapped = Array.isArray(data) ? data : [];
      setClients(mapped);
    } catch (error) {
      console.error("Danışanlar alınamadı", error);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    loadClients();
  }, [loadClients]);

  useEffect(() => {
    if (!selectedClientId) {
      setKitchenLogs([]);
      return;
    }
    let cancelled = false;
    setLoadingKitchen(true);
    (async () => {
      try {
        const { data } = await api.get<KitchenChefLogItem[]>("/api/dietitian/client-kitchen-recipe-logs", {
          params: { clientId: selectedClientId, take: 30 },
        });
        if (!cancelled) setKitchenLogs(Array.isArray(data) ? data : []);
      } catch {
        if (!cancelled) setKitchenLogs([]);
      } finally {
        if (!cancelled) setLoadingKitchen(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedClientId]);

  const selectedClientName = useMemo(() => {
    if (!selectedClientId) return "";
    const c = clients.find((x) => x.id === selectedClientId);
    return `${c?.firstName || ""} ${c?.lastName || ""}`.trim();
  }, [clients, selectedClientId]);

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-slate-50 min-h-screen text-slate-900 transition-colors pb-24 lg:pb-8">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div>
            <h1 className="text-4xl font-bold flex items-center gap-2">
              <BarChart3 className="w-9 h-9 text-emerald-500 shrink-0" />
              Yapay zeka denetimi
            </h1>
            <p className="text-slate-500 mt-1 max-w-2xl">
              Danışanların <strong>yapay zeka mutfak</strong> ekranında seçtikleri ve sizinle paylaştıkları tarifler burada listelenir.
              Kayıtlar uygulama üzerinden veritabanına alınır; yalnızca kendi danışanlarınızı görebilirsiniz.
            </p>
          </div>
        </div>

        {selectedClientId && selectedClientName && (
          <div className="rounded-2xl border-2 border-emerald-500/40 bg-emerald-50/90 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">Seçili danışan</p>
            <p className="text-2xl font-bold text-slate-900 mt-0.5">{selectedClientName}</p>
          </div>
        )}

        <div className="rounded-2xl border border-slate-200 bg-white p-4 space-y-3">
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
            {loadingList && <Loader2 className="w-5 h-5 animate-spin text-emerald-500 self-center" />}
          </div>
          <div className="max-h-48 overflow-y-auto rounded-xl border border-slate-200 divide-y divide-slate-200">
            {filteredClients.length === 0 ? (
              <p className="p-3 text-sm text-slate-500">
                {loadingList
                  ? "Liste yükleniyor…"
                  : clients.length === 0
                    ? "Henüz bağlı danışan yok."
                    : "Arama sonucu yok; aramayı sadeleştirin."}
              </p>
            ) : (
              filteredClients.map((client) => {
                const label = `${client.firstName || ""} ${client.lastName || ""}`.trim() || "İsimsiz";
                return (
                  <button
                    type="button"
                    key={client.id}
                    onClick={() => {
                      if (client.id) {
                        setSelectedClientId(client.id);
                      }
                    }}
                    className={`w-full text-left px-3 py-2.5 text-sm font-medium transition-colors ${
                      selectedClientId === client.id
                        ? "bg-emerald-500/20 text-emerald-800 ring-1 ring-inset ring-emerald-500/30"
                        : "hover:bg-slate-100"
                    }`}
                  >
                    {label}
                  </button>
                );
              })
            )}
          </div>
        </div>

        {selectedClientId && (
          <div className="rounded-2xl border border-slate-200 bg-white p-4">
            <h2 className="text-lg font-bold text-slate-900">Yapay zeka mutfak paylaşımları</h2>
            <p className="text-sm text-slate-500 mt-1">
              Danışan uygulamada seçer ve paylaşır; burada sadece okunur. Son kayıtlar (en çok 30) listelenir.
            </p>
            {loadingKitchen ? (
              <p className="text-sm text-slate-500 mt-3 flex items-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
              </p>
            ) : kitchenLogs.length === 0 ? (
              <p className="text-sm text-slate-500 mt-3">Henüz paylaşılmış tarif yok veya yükleme hatası.</p>
            ) : (
              <ul className="mt-4 space-y-4">
                {kitchenLogs.map((log) => {
                  const when = (() => {
                    try {
                      return new Date(log.createdAtUtc).toLocaleString("tr-TR", {
                        dateStyle: "medium",
                        timeStyle: "short",
                      });
                    } catch {
                      return log.createdAtUtc;
                    }
                  })();
                  return (
                    <li
                      key={log.id || log.createdAtUtc + (log.preference || "")}
                      className="rounded-xl border border-slate-200 p-3 bg-slate-50/80"
                    >
                      <div className="flex flex-wrap items-baseline justify-between gap-2 text-xs text-slate-500">
                        <span>{when}</span>
                        <span>
                          Hedef {log.targetCalories} kkal
                          {log.source ? ` · ${log.source}` : ""}
                        </span>
                      </div>
                      <p className="text-sm mt-1 text-slate-600">
                        <span className="font-medium text-slate-800">Tercih:</span> {log.preference}
                      </p>
                      <p className="text-xs text-slate-500 mt-1">
                        <span className="font-medium">Malzemeler (sorgu):</span> {log.ingredients}
                      </p>
                      {log.selectedRecipes?.length > 0 && (
                        <div className="mt-3 space-y-3">
                          {log.selectedRecipes.map((r, i) => (
                            <div
                              key={i + (r.title || "")}
                              className="rounded-lg border border-emerald-200/60 p-2 bg-white/90"
                            >
                              <p className="font-semibold text-slate-900">{r.title}</p>
                              {r.description && (
                                <p className="text-sm text-slate-600 mt-1">{r.description}</p>
                              )}
                              <p className="text-xs text-slate-500 mt-1">
                                ~{r.estimatedCalories} kkal
                                {r.prepTimeMinutes != null ? ` · ${r.prepTimeMinutes} dk` : ""}
                              </p>
                              {r.ingredients?.length > 0 && (
                                <ul className="mt-1 text-xs list-disc list-inside text-slate-600">
                                  {r.ingredients.map((ing, j) => (
                                    <li key={j}>{ing}</li>
                                  ))}
                                </ul>
                              )}
                              {r.steps?.length > 0 && (
                                <ol className="mt-2 text-xs list-decimal list-inside text-slate-600 space-y-0.5">
                                  {r.steps.map((s, j) => (
                                    <li key={j}>{s}</li>
                                  ))}
                                </ol>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
