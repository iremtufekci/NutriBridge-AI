import { useEffect, useMemo, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";

const DAYS = ["Pazartesi", "Sali", "Carsamba", "Persembe", "Cuma", "Cumartesi", "Pazar"];

type ClientItem = { id?: string; firstName?: string; lastName?: string };

export function DietitianPrograms() {
  const dietitianName = localStorage.getItem("userName") || "Diyetisyen";
  const [clients, setClients] = useState<ClientItem[]>([]);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedDay, setSelectedDay] = useState(DAYS[0]);
  const [breakfast, setBreakfast] = useState("");
  const [lunch, setLunch] = useState("");
  const [dinner, setDinner] = useState("");
  const [snack, setSnack] = useState("");
  const [totalCalories, setTotalCalories] = useState(0);

  useEffect(() => {
    const loadClients = async () => {
      try {
        const { data } = await api.get("/api/dietitian/clients-with-last-meal");
        const mapped = Array.isArray(data) ? data : [];
        setClients(mapped);
        if (mapped.length > 0 && mapped[0].id) setSelectedClientId(mapped[0].id);
      } catch (error) {
        console.error("Danisanlar alinamadi", error);
      }
    };
    loadClients();
  }, []);

  const selectedClientName = useMemo(() => {
    const c = clients.find((x) => x.id === selectedClientId);
    return `${c?.firstName || ""} ${c?.lastName || ""}`.trim() || "Danisan seciniz";
  }, [clients, selectedClientId]);

  const saveProgram = async () => {
    if (!selectedClientId) {
      alert("Lutfen once bir danisan secin.");
      return;
    }

    try {
      await api.post("/api/dietitian/diet-program", {
        clientId: selectedClientId,
        dayOfWeek: selectedDay,
        breakfast,
        lunch,
        dinner,
        snack,
        totalCalories,
      });
      alert("Program kaydedildi.");
    } catch (error) {
      alert("Kayit basarisiz: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0F172A] min-h-screen text-slate-900 dark:text-white transition-colors">
        <div>
          <h1 className="text-4xl font-bold">Diyet Programi Olustur</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1">Danisaniniz icin haftalik diyet plani hazirlayin</p>
        </div>

        <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1E293B] p-4 flex flex-col sm:flex-row gap-3">
          <label className="text-sm font-semibold mt-2">Danisan</label>
          <select
            className="flex-1 rounded-xl border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-[#0F172A] p-3"
            value={selectedClientId}
            onChange={(e) => setSelectedClientId(e.target.value)}
          >
            {clients.map((client) => (
              <option key={client.id} value={client.id}>
                {`${client.firstName || ""} ${client.lastName || ""}`.trim()}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-wrap gap-3">
          {DAYS.map((day) => (
            <button
              key={day}
              onClick={() => setSelectedDay(day)}
              className={`px-5 py-2.5 rounded-full border ${
                selectedDay === day
                  ? "bg-emerald-500 text-white border-emerald-500"
                  : "bg-white dark:bg-[#1E293B] border-slate-200 dark:border-slate-700"
              }`}
            >
              {day}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <MealCard title="Kahvalti" value={breakfast} setValue={setBreakfast} />
          <MealCard title="Ogle Yemegi" value={lunch} setValue={setLunch} />
          <MealCard title="Aksam Yemegi" value={dinner} setValue={setDinner} />
          <MealCard title="Ara Ogun" value={snack} setValue={setSnack} />
        </div>

        <div className="rounded-3xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-100 dark:border-emerald-900 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <p className="text-slate-500 dark:text-slate-400">Gunluk Toplam Kalori</p>
            <div className="mt-2 flex items-center gap-3">
              <input
                type="number"
                value={totalCalories}
                onChange={(e) => setTotalCalories(Number(e.target.value || 0))}
                className="w-32 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-[#0F172A] px-3 py-2"
              />
              <span className="text-3xl font-bold">{totalCalories} kcal</span>
            </div>
            <p className="text-sm mt-2 text-slate-500">Secilen danisan: {selectedClientName}</p>
          </div>
          <button
            onClick={saveProgram}
            className="px-8 py-3 rounded-2xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600"
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
}: {
  title: string;
  value: string;
  setValue: (v: string) => void;
}) {
  return (
    <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1E293B] p-4 min-h-[220px] flex flex-col">
      <div className="flex items-center justify-between">
        <h3 className="text-2xl font-bold">{title}</h3>
        <span className="text-2xl text-slate-400">+</span>
      </div>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Besin eklemek icin notlarinizi yazin..."
        className="mt-4 flex-1 resize-none rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-[#0F172A] p-3 text-sm"
      />
    </div>
  );
}
