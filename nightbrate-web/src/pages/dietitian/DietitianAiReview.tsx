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
      console.error("Danisanlar alinamadi", error);
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
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 lg:pb-8">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div>
            <h1 className="text-4xl font-bold flex items-center gap-2">
              <BarChart3 className="w-9 h-9 text-emerald-500 shrink-0" />
              AI Denetimi
            </h1>
            <p className="text-slate-500 dark:text-[#9CA3AF] mt-1 max-w-2xl">
              Danisanlarin <strong>AI Mutfak</strong> ekraninda secdikleri ve sizinle paylastiklari tarifler burada listelenir.
              Kayitlar uygulama uzerinden veritabanina alinir; yalnizca kendi danisanlarinizi gorebilirsiniz.
            </p>
          </div>
        </div>

        {selectedClientId && selectedClientName && (
          <div className="rounded-2xl border-2 border-emerald-500/40 bg-emerald-50/90 dark:bg-emerald-950/40 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700 dark:text-[#2ECC71]">Secili danisan</p>
            <p className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">{selectedClientName}</p>
          </div>
        )}

        <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 space-y-3">
          <div className="flex flex-col sm:flex-row gap-2 sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="search"
                placeholder="Danisan adi ile ara..."
                className="w-full rounded-xl border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-[#0D1117] pl-10 pr-3 py-3"
                value={clientQuery}
                onChange={(e) => setClientQuery(e.target.value)}
              />
            </div>
            {loadingList && <Loader2 className="w-5 h-5 animate-spin text-emerald-500 self-center" />}
          </div>
          <div className="max-h-48 overflow-y-auto rounded-xl border border-slate-200 dark:border-slate-600 divide-y divide-slate-200 dark:divide-slate-600">
            {filteredClients.length === 0 ? (
              <p className="p-3 text-sm text-slate-500">
                {loadingList
                  ? "Liste yukleniyor…"
                  : clients.length === 0
                    ? "Henuz bagli danisan yok."
                    : "Arama sonucu yok; aramayi sadelestirin."}
              </p>
            ) : (
              filteredClients.map((client) => {
                const label = `${client.firstName || ""} ${client.lastName || ""}`.trim() || "Isimsiz";
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
                        ? "bg-emerald-500/20 text-emerald-800 dark:text-emerald-200 ring-1 ring-inset ring-emerald-500/30"
                        : "hover:bg-slate-100 dark:hover:bg-[#2D3748]"
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
          <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white">AI Mutfak paylasimlari</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              Danisan uygulamada secer ve paylasir; burada sadece okunur. Son kayitlar (en cok 30) listelenir.
            </p>
            {loadingKitchen ? (
              <p className="text-sm text-slate-500 mt-3 flex items-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Yukleniyor…
              </p>
            ) : kitchenLogs.length === 0 ? (
              <p className="text-sm text-slate-500 mt-3">Henuz paylasilmis tarif yok veya yukleme hatasi.</p>
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
                      className="rounded-xl border border-slate-200 dark:border-slate-600 p-3 bg-slate-50/80 dark:bg-[#111827]/80"
                    >
                      <div className="flex flex-wrap items-baseline justify-between gap-2 text-xs text-slate-500 dark:text-slate-400">
                        <span>{when}</span>
                        <span>
                          Hedef {log.targetCalories} kcal
                          {log.source ? ` · ${log.source}` : ""}
                        </span>
                      </div>
                      <p className="text-sm mt-1 text-slate-600 dark:text-slate-300">
                        <span className="font-medium text-slate-800 dark:text-slate-200">Tercih:</span> {log.preference}
                      </p>
                      <p className="text-xs text-slate-500 mt-1">
                        <span className="font-medium">Malzemeler (sorgu):</span> {log.ingredients}
                      </p>
                      {log.selectedRecipes?.length > 0 && (
                        <div className="mt-3 space-y-3">
                          {log.selectedRecipes.map((r, i) => (
                            <div
                              key={i + (r.title || "")}
                              className="rounded-lg border border-emerald-200/60 dark:border-emerald-800/50 p-2 bg-white/90 dark:bg-[#0D1117]/90"
                            >
                              <p className="font-semibold text-slate-900 dark:text-white">{r.title}</p>
                              {r.description && (
                                <p className="text-sm text-slate-600 dark:text-slate-300 mt-1">{r.description}</p>
                              )}
                              <p className="text-xs text-slate-500 mt-1">
                                ~{r.estimatedCalories} kcal
                                {r.prepTimeMinutes != null ? ` · ${r.prepTimeMinutes} dk` : ""}
                              </p>
                              {r.ingredients?.length > 0 && (
                                <ul className="mt-1 text-xs list-disc list-inside text-slate-600 dark:text-slate-300">
                                  {r.ingredients.map((ing, j) => (
                                    <li key={j}>{ing}</li>
                                  ))}
                                </ul>
                              )}
                              {r.steps?.length > 0 && (
                                <ol className="mt-2 text-xs list-decimal list-inside text-slate-600 dark:text-slate-300 space-y-0.5">
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
