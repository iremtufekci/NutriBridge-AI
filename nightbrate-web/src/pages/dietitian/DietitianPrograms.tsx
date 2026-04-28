import { useCallback, useEffect, useMemo, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { Loader2, Search } from "lucide-react";

type ClientItem = { id?: string; firstName?: string; lastName?: string };

type DietProgramResponse = {
  clientId?: string;
  programDate?: string;
  breakfast?: string;
  lunch?: string;
  dinner?: string;
  snack?: string;
  breakfastCalories?: number;
  lunchCalories?: number;
  dinnerCalories?: number;
  snackCalories?: number;
  totalCalories?: number;
  hasSavedProgram?: boolean;
  updatedAt?: string;
};

/** Yerel saatle yyyy-MM-dd (sunucu ile aynı takvim günü). */
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

/** Bugünden itibaren n gün (bugün dahil) tarih listesi, her biri { ymd, label } */
function buildUpcomingOptions(from: Date, count: number) {
  const start = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  return Array.from({ length: count }, (_, i) => {
    const x = addDays(start, i);
    const ymd = toYmd(x);
    const w = x.toLocaleDateString("tr-TR", { weekday: "short" });
    const label = x.toLocaleDateString("tr-TR", { day: "numeric", month: "short" });
    return { ymd, label: `${w} ${label}` };
  });
}

export function DietitianPrograms() {
  const dietitianName = useAuthProfileDisplayName();
  const [clients, setClients] = useState<ClientItem[]>([]);
  const [clientQuery, setClientQuery] = useState("");
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedYmd, setSelectedYmd] = useState(() => toYmd(new Date()));
  const [breakfast, setBreakfast] = useState("");
  const [lunch, setLunch] = useState("");
  const [dinner, setDinner] = useState("");
  const [snack, setSnack] = useState("");
  const [breakfastKcal, setBreakfastKcal] = useState(0);
  const [lunchKcal, setLunchKcal] = useState(0);
  const [dinnerKcal, setDinnerKcal] = useState(0);
  const [snackKcal, setSnackKcal] = useState(0);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingProgram, setLoadingProgram] = useState(false);
  const [assignedDates, setAssignedDates] = useState<string[]>([]);
  const [loadingAssigned, setLoadingAssigned] = useState(false);

  const totalCalories = useMemo(
    () => breakfastKcal + lunchKcal + dinnerKcal + snackKcal,
    [breakfastKcal, lunchKcal, dinnerKcal, snackKcal]
  );

  const dateOptions = useMemo(() => buildUpcomingOptions(new Date(), 60), []);

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

  const loadAssignedDates = useCallback(async (clientId: string) => {
    if (!clientId) {
      setAssignedDates([]);
      return;
    }
    setLoadingAssigned(true);
    try {
      const { data } = await api.get<string[]>("/api/dietitian/diet-program-dates", {
        params: { clientId },
      });
      setAssignedDates(Array.isArray(data) ? data : []);
    } catch {
      setAssignedDates([]);
    } finally {
      setLoadingAssigned(false);
    }
  }, []);

  useEffect(() => {
    if (!selectedClientId) {
      setAssignedDates([]);
      return;
    }
    void loadAssignedDates(selectedClientId);
  }, [selectedClientId, loadAssignedDates]);

  useEffect(() => {
    if (!selectedClientId || !selectedYmd) return;
    let cancelled = false;
    setLoadingProgram(true);
    (async () => {
      try {
        const { data } = await api.get<DietProgramResponse>("/api/dietitian/diet-program", {
          params: { clientId: selectedClientId, programDate: selectedYmd },
        });
        if (cancelled) return;
        const d = data as DietProgramResponse;
        setBreakfast(d.breakfast ?? "");
        setLunch(d.lunch ?? "");
        setDinner(d.dinner ?? "");
        setSnack(d.snack ?? "");
        setBreakfastKcal(Number(d.breakfastCalories) || 0);
        setLunchKcal(Number(d.lunchCalories) || 0);
        setDinnerKcal(Number(d.dinnerCalories) || 0);
        setSnackKcal(Number(d.snackCalories) || 0);
      } catch (e) {
        if (!cancelled) {
          setBreakfast("");
          setLunch("");
          setDinner("");
          setSnack("");
          setBreakfastKcal(0);
          setLunchKcal(0);
          setDinnerKcal(0);
          setSnackKcal(0);
        }
      } finally {
        if (!cancelled) setLoadingProgram(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedClientId, selectedYmd]);

  const selectedClientName = useMemo(() => {
    if (!selectedClientId) return "";
    const c = clients.find((x) => x.id === selectedClientId);
    return `${c?.firstName || ""} ${c?.lastName || ""}`.trim();
  }, [clients, selectedClientId]);

  const saveProgram = async () => {
    if (!selectedClientId) {
      alert("Lutfen once bir danisan secin.");
      return;
    }
    const dateKey = selectedYmd?.trim() ?? "";
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dateKey)) {
      alert("Lutfen bir program tarihi secin (takvim veya hizli tarih dugmeleri).");
      return;
    }

    try {
      await api.post("/api/dietitian/diet-program", {
        clientId: selectedClientId,
        programDate: dateKey,
        breakfast,
        lunch,
        dinner,
        snack,
        breakfastCalories: breakfastKcal,
        lunchCalories: lunchKcal,
        dinnerCalories: dinnerKcal,
        snackCalories: snackKcal,
        totalCalories,
      });
      await loadAssignedDates(selectedClientId);
      alert("Program kaydedildi. Ayni danisan ve tarih icin tekrar duzenleyebilirsiniz.");
    } catch (error) {
      alert(
        "Kayit basarisiz: " +
          ((error as { response?: { data?: { message?: string } } })?.response?.data?.message || "Bilinmeyen hata")
      );
    }
  };

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 lg:pb-8">
        <div>
          <h1 className="text-4xl font-bold">Diyet Programi</h1>
          <p className="text-slate-500 dark:text-[#9CA3AF] mt-1">
            Danisani secin; <strong>yeni atama</strong> icin bugun veya ileri bir tarih secin. Daha once atadiginiz
            tarihleri alttan secerek <strong>duzenleyebilirsiniz</strong>; takvimle gecmis bir tarih de
            acilabilir (sadece mevcut kayit guncellemek icin).
          </p>
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
                {loadingList ? "Liste yukleniyor…" : clients.length === 0 ? "Henuz bagli danisan yok." : "Arama sonucu yok; aramayi sadelestirin."}
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

        <div>
          {selectedClientId && (
            <div className="mb-4 rounded-xl border border-amber-200/80 dark:border-amber-800/50 bg-amber-50/80 dark:bg-amber-950/30 p-3">
              <p className="text-sm font-semibold text-amber-900 dark:text-amber-200">Bu danisana atadiginiz tarihler</p>
              {loadingAssigned ? (
                <p className="text-xs text-amber-800/80 dark:text-amber-300/80 mt-1">Yukleniyor…</p>
              ) : assignedDates.length === 0 ? (
                <p className="text-xs text-amber-800/80 dark:text-amber-300/80 mt-1">Henuz kayit yok; asagidan tarih secip kaydedin.</p>
              ) : (
                <div className="mt-2 flex flex-wrap gap-2">
                  {assignedDates.map((ymd) => {
                    const d = new Date(ymd + "T12:00:00");
                    const w = d.toLocaleDateString("tr-TR", { weekday: "short" });
                    const label = d.toLocaleDateString("tr-TR", { day: "numeric", month: "short" });
                    return (
                      <button
                        type="button"
                        key={ymd}
                        onClick={() => setSelectedYmd(ymd)}
                        className={`px-2.5 py-1.5 rounded-lg text-left text-sm border ${
                          selectedYmd === ymd
                            ? "bg-amber-500 text-white border-amber-500"
                            : "bg-white dark:bg-amber-950/50 border-amber-300 dark:border-amber-700"
                        }`}
                      >
                        <span className="block font-semibold">
                          {w} {label}
                        </span>
                        <span className="text-xs opacity-80">{ymd}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          <p className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">Tarih secimi (yeni veya duzenleme)</p>
          <div className="flex flex-wrap items-end gap-3">
            <label className="flex flex-col text-xs text-slate-500">
              Takvim (gecmis: yalnizca atadiginiz gunleri duzenlemek icin)
              <input
                type="date"
                value={selectedYmd}
                onChange={(e) => e.target.value && setSelectedYmd(e.target.value)}
                className="mt-1 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-[#0D1117] px-3 py-2 text-slate-900 dark:text-white"
              />
            </label>
            <p className="text-xs text-slate-500 pb-1">Hizli: onumuzdeki 60 gun (yeni atama)</p>
          </div>
          <div className="mt-3 flex flex-wrap gap-2 max-h-40 overflow-y-auto p-1">
            {dateOptions.map((opt) => (
              <button
                type="button"
                key={opt.ymd}
                onClick={() => setSelectedYmd(opt.ymd)}
                className={`px-3 py-1.5 rounded-lg text-left text-sm border ${
                  selectedYmd === opt.ymd
                    ? "bg-emerald-500 text-white border-emerald-500"
                    : "bg-white dark:bg-[#1F2937] border-slate-200 dark:border-slate-700"
                }`}
              >
                <span className="block font-semibold">{opt.label}</span>
                <span className="text-xs opacity-80">{opt.ymd}</span>
              </button>
            ))}
          </div>
        </div>

        {loadingProgram && (
          <p className="text-sm text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" /> Program yukleniyor...
          </p>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <MealCard
            title="Kahvalti"
            value={breakfast}
            setValue={setBreakfast}
            kcal={breakfastKcal}
            setKcal={setBreakfastKcal}
          />
          <MealCard
            title="Ogle Yemegi"
            value={lunch}
            setValue={setLunch}
            kcal={lunchKcal}
            setKcal={setLunchKcal}
          />
          <MealCard
            title="Aksam Yemegi"
            value={dinner}
            setValue={setDinner}
            kcal={dinnerKcal}
            setKcal={setDinnerKcal}
          />
          <MealCard
            title="Ara Ogun"
            value={snack}
            setValue={setSnack}
            kcal={snackKcal}
            setKcal={setSnackKcal}
          />
        </div>

        <div className="rounded-3xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-100 dark:border-emerald-900 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <p className="text-slate-500 dark:text-[#9CA3AF]">Gunluk toplam (ogun toplamı)</p>
            <p className="text-3xl font-bold text-slate-900 dark:text-white mt-1">{totalCalories} kcal</p>
            <p className="text-xs text-slate-500 mt-1">Otomatik: dort ogunun kalori girdisinin toplami.</p>
          </div>
          <button
            type="button"
            onClick={saveProgram}
            disabled={loadingProgram}
            className="px-8 py-3 rounded-2xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600 disabled:opacity-50"
          >
            Programi Kaydet
          </button>
        </div>
      </div>
    </SidebarLayout>
  );
}

function MealCard({
  title,
  value,
  setValue,
  kcal,
  setKcal,
}: {
  title: string;
  value: string;
  setValue: (v: string) => void;
  kcal: number;
  setKcal: (n: number) => void;
}) {
  return (
    <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 min-h-[220px] flex flex-col">
      <div className="flex items-start justify-between gap-2">
        <h3 className="text-2xl font-bold leading-tight">{title}</h3>
        <label className="flex flex-col items-end text-xs text-slate-500 shrink-0">
          kcal
          <input
            type="number"
            min={0}
            value={kcal}
            onChange={(e) => setKcal(Math.max(0, Number(e.target.value) || 0))}
            className="mt-0.5 w-20 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-[#0D1117] px-2 py-1 text-slate-900 dark:text-white text-sm text-right"
          />
        </label>
      </div>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Ogun icerigini ve besin onerilerinizi yazin..."
        className="mt-3 flex-1 resize-none rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-[#0D1117] p-3 text-sm"
      />
    </div>
  );
}
