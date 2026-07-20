// Diyetisyen ana özet (dashboard) sayfası — danışanlar, görevler ve uyarıların genel görünümü
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { dietitianMealAnalysisPath } from "./DietitianMealAnalysisReview";
import { SidebarLayout } from "../../components/SidebarLayout";
import { Users, BookOpen, CheckSquare, AlertTriangle, ChevronRight, ScanSearch, UtensilsCrossed } from "lucide-react";
import { api, resolveMediaUrl } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

// Danışanın son öğün kaydı için tip tanımı
type LastMeal = {
  photoUrl?: string;
  calories?: number;
  detectedFoods?: string[];
  timestamp?: string;
};

// Son öğün bilgisiyle birlikte danışan kartı tipi
type ClientWithMeal = {
  id?: string;
  firstName?: string;
  lastName?: string;
  lastMeal?: LastMeal | null;
};

// ISO tarihini göreli Türkçe zaman metnine çevirir (örn. "5 dk önce")
function formatMealTime(iso?: string): string {
  if (!iso) return "Tarih yok";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "Tarih yok";
  const diffMs = Date.now() - d.getTime();
  const mins = Math.floor(diffMs / 60_000);
  if (mins < 1) return "Az önce";
  if (mins < 60) return `${mins} dk önce`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} saat önce`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} gün önce`;
  return d.toLocaleString("tr-TR", { dateStyle: "short", timeStyle: "short" });
}

// Günlük görev kalemi tipi
type TaskItem = {
  id: string;
  title: string;
  subtitle: string;
  isCompleted: boolean;
  dueLabel: string;
  category: string;
};

// API'den dönen günlük görev paketi
type TasksBundle = {
  taskDate: string;
  pendingCount: number;
  completedCount: number;
  totalCount: number;
  tasks: TaskItem[];
};

// Kritik uyarı kaydı tipi
type CritAlert = {
  id: string;
  clientId: string;
  clientName: string;
  alertType: string;
  severity: string;
  message: string;
  date: string;
  referenceDate: string;
};

// Uyarı türü kodunu kullanıcıya gösterilecek Türkçe etikete dönüştürür
function alertTypeLabel(t: string) {
  switch (t) {
    case "MissedMeals":
      return "Öğün tamamlama";
    case "HighCalories":
      return "Yüksek kalori";
    default:
      return t;
  }
}

export function DietitianDashboard() {
  const dietitianName = useAuthProfileDisplayName();
  // Sayfa verileri için durum değişkenleri
  const [clients, setClients] = useState<ClientWithMeal[]>([]);
  const [tasksBundle, setTasksBundle] = useState<TasksBundle | null>(null);
  const [criticalAlerts, setCriticalAlerts] = useState<CritAlert[]>([]);
  const [taskBusyId, setTaskBusyId] = useState<string | null>(null);

  // Danışan listesini ve son öğün bilgilerini API'den yükler
  const loadClients = useCallback(async () => {
    try {
      const { data } = await api.get("/api/dietitian/clients-with-last-meal");
      setClients(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Danışanlar alınamadı", error);
    }
  }, []);

  // Bugünkü günlük görevleri API'den yükler
  const loadTasks = useCallback(async () => {
    try {
      const { data } = await api.get<TasksBundle>("/api/dietitian/daily-tasks/today");
      setTasksBundle(data);
    } catch (error) {
      console.error("Görevler alınamadı", error);
      setTasksBundle(null);
    }
  }, []);

  // Kritik uyarıları API'den yükler
  const loadCriticalAlerts = useCallback(async () => {
    try {
      const { data } = await api.get<CritAlert[]>("/api/dietitian/critical-alerts");
      setCriticalAlerts(Array.isArray(data) ? data : []);
    } catch {
      setCriticalAlerts([]);
    }
  }, []);

  // Sayfa açıldığında tüm veri kaynaklarını paralel olarak çeker
  useEffect(() => {
    void loadClients();
    void loadTasks();
    void loadCriticalAlerts();
  }, [loadClients, loadTasks, loadCriticalAlerts]);

  // Özet panelde gösterilecek en fazla 3 kritik uyarı
  const criticalPreview = useMemo(() => criticalAlerts.slice(0, 3), [criticalAlerts]);

  // Danışanların son öğün kayıtlarını zamana göre sıralayıp en yenilerini seçer
  const recentMeals = useMemo(
    () =>
      clients
        .filter((x) => x.lastMeal?.timestamp)
        .map((x) => ({
          clientId: x.id,
          clientName: `${x.firstName || ""} ${x.lastName || ""}`.trim() || "Danışan",
          initial: (x.firstName || "D").charAt(0).toUpperCase(),
          meal: x.lastMeal!,
          photoSrc: x.lastMeal?.photoUrl ? resolveMediaUrl(x.lastMeal.photoUrl) : "",
          hasPhoto: Boolean(x.lastMeal?.photoUrl?.trim()),
        }))
        .sort(
          (a, b) =>
            new Date(b.meal.timestamp || 0).getTime() - new Date(a.meal.timestamp || 0).getTime()
        )
        .slice(0, 4),
    [clients]
  );

  // Özet panelde gösterilecek tamamlanmamış görevler (en fazla 4)
  const dashboardTasks = useMemo(() => {
    if (!tasksBundle?.tasks?.length) return [];
    return tasksBundle.tasks.filter((t) => !t.isCompleted).slice(0, 4);
  }, [tasksBundle]);

  const todayTasksTotal = tasksBundle?.totalCount ?? 0;

  // Görev tamamlama durumunu API üzerinden değiştirir ve listeyi yeniler
  const toggleTask = async (task: TaskItem) => {
    setTaskBusyId(task.id);
    try {
      await api.patch(`/api/dietitian/daily-tasks/${task.id}/complete`, {
        isCompleted: !task.isCompleted,
      });
      await loadTasks();
    } catch (e) {
      console.error(e);
    } finally {
      setTaskBusyId(null);
    }
  };

  // Üst istatistik kartları için yapılandırma
  const statCards = [
    { title: "Toplam Danışan", value: String(clients.length), icon: Users, iconColor: "text-emerald-500" },
    { title: "Aktif Program", value: String(clients.length), icon: BookOpen, iconColor: "text-emerald-500" },
    { title: "Bugünkü Görevler", value: String(todayTasksTotal), icon: CheckSquare, iconColor: "text-amber-500" },
    { title: "Kritik Uyarı", value: String(criticalAlerts.length), icon: AlertTriangle, iconColor: "text-rose-500" },
  ];

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-slate-50 min-h-screen text-slate-900 transition-colors pb-24 lg:pb-8">
        {/* Karşılama başlığı ve kritik uyarı özeti */}
        <div className="bg-[#DFF1EA] rounded-3xl p-5 sm:p-8 border border-[#CCE6DC]">
          <h1 className="text-3xl sm:text-5xl font-bold mb-2 text-slate-900">Merhaba, {dietitianName} 👋</h1>
          <p className="text-slate-600">
            {criticalAlerts.length > 0 ? (
              <>
                <span className="font-bold">{criticalAlerts.length} kritik uyarınız</span> var. Öğün uyumu ve kalori
                eşiklerini inceleyin.
              </>
            ) : (
              <>Şu an kritik uyarı yok. Güncel aktivitelere göz atın.</>
            )}
          </p>
        </div>

        {/* Dört sütunlu özet istatistik kartları */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.title} className="rounded-2xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm">
                <div className="flex items-start justify-between">
                  <p className="text-sm text-slate-500">{card.title}</p>
                  <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
                    <Icon size={18} className={card.iconColor} />
                  </div>
                </div>
                <p className="text-3xl font-bold mt-2">{card.value}</p>
                {card.title === "Bugünkü Görevler" && tasksBundle != null && (
                  <p className="text-xs text-slate-500 mt-1">
                    {tasksBundle.pendingCount} bekleyen · {tasksBundle.completedCount} tamamlandı
                  </p>
                )}
              </div>
            );
          })}
        </div>

        {/* Kritik uyarılar ve son öğün kayıtları — iki sütunlu grid */}
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
          {/* Kritik uyarı önizleme paneli */}
          <div className="rounded-3xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-bold">Kritik Uyarılar</h3>
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-rose-100 text-rose-500">
                {criticalAlerts.length} uyarı
              </span>
            </div>

            {criticalPreview.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center">
                <p className="text-sm text-slate-600">Şu an listelenecek kritik uyarı yok.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {criticalPreview.map((alert) => {
                  const isHigh = alert.severity === "High";
                  return (
                    <Link
                      key={alert.id}
                      to="/dietitian/alerts"
                      className={[
                        "block rounded-2xl border bg-slate-50 p-4 transition-colors hover:bg-rose-50/40",
                        isHigh
                          ? "border-rose-200 border-l-4 border-l-rose-500"
                          : "border-amber-200 border-l-4 border-l-amber-500",
                      ].join(" ")}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-start gap-3 min-w-0">
                          <div
                            className={[
                              "w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold shrink-0",
                              isHigh ? "bg-rose-100 text-rose-600" : "bg-amber-100 text-amber-800",
                            ].join(" ")}
                          >
                            {(alert.clientName || "?").charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-lg truncate">{alert.clientName}</p>
                            <p className="text-xs text-slate-500 mt-0.5">
                              {new Date(alert.date).toLocaleDateString("tr-TR", {
                                day: "numeric",
                                month: "long",
                                year: "numeric",
                              })}
                            </p>
                            <p className="inline-flex mt-2 text-xs px-3 py-1 rounded-full bg-rose-100 text-rose-600 font-medium">
                              {alertTypeLabel(alert.alertType)}
                            </p>
                            <p className="mt-2 text-sm text-slate-600 line-clamp-2">{alert.message}</p>
                          </div>
                        </div>
                        <ChevronRight className="text-slate-400 mt-1 shrink-0" size={18} />
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
            <Link
              to="/dietitian/alerts"
              className="mt-4 w-full block text-center py-2 text-emerald-500 font-semibold hover:text-emerald-600"
            >
              Tümünü görüntüle →
            </Link>
          </div>

          {/* Son öğün kayıtları önizleme paneli */}
          <div className="rounded-3xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-bold">Son öğün kayıtları</h3>
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-100 text-emerald-700">
                {recentMeals.length} kayıt
              </span>
            </div>

            {recentMeals.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center">
                <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-white text-slate-400 shadow-sm">
                  <ScanSearch size={22} />
                </div>
                <p className="font-medium text-slate-700">Henüz öğün kaydı yok</p>
                <p className="mt-1 text-sm text-slate-500">
                  Danışanlarınız yemek analizi yaptığında kayıtlar burada listelenir.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {recentMeals.map((item) => {
                  const foods = (item.meal.detectedFoods || []).filter(Boolean).slice(0, 3);
                  const kcal = item.meal.calories;
                  const href = dietitianMealAnalysisPath(item.clientId);
                  return (
                    <Link
                      key={`${item.clientId}-${item.meal.timestamp}`}
                      to={href}
                      className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50 p-3 transition-colors hover:border-emerald-200 hover:bg-emerald-50/40"
                    >
                      {item.hasPhoto && item.photoSrc ? (
                        <img
                          src={item.photoSrc}
                          alt={`${item.clientName} öğünü`}
                          className="h-16 w-16 shrink-0 rounded-xl object-cover bg-slate-200"
                          onError={(e) => {
                            e.currentTarget.style.display = "none";
                            e.currentTarget.nextElementSibling?.classList.remove("hidden");
                          }}
                        />
                      ) : null}
                      <div
                        className={[
                          "flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700",
                          item.hasPhoto && item.photoSrc ? "hidden" : "",
                        ].join(" ")}
                      >
                        {item.hasPhoto ? (
                          <UtensilsCrossed size={22} />
                        ) : (
                          <span className="text-lg font-bold">{item.initial}</span>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-start justify-between gap-2">
                          <p className="truncate font-semibold text-slate-900">{item.clientName}</p>
                          <span className="shrink-0 text-xs text-slate-500">
                            {formatMealTime(item.meal.timestamp)}
                          </span>
                        </div>
                        <p className="mt-0.5 text-sm text-slate-600">
                          {typeof kcal === "number" && kcal > 0 ? (
                            <span className="font-medium text-emerald-700">{kcal} kkal</span>
                          ) : (
                            <span>Kalori hesaplanmadı</span>
                          )}
                          {foods.length > 0 && (
                            <span className="text-slate-500"> · {foods.join(", ")}</span>
                          )}
                        </p>
                        <span className="mt-1.5 inline-flex text-[11px] font-semibold uppercase tracking-wide text-emerald-600">
                          Analizi gör →
                        </span>
                      </div>
                      <ChevronRight className="shrink-0 text-slate-400" size={18} aria-hidden />
                    </Link>
                  );
                })}
              </div>
            )}
            <Link
              to="/dietitian/meal-analysis"
              className="mt-4 w-full block text-center py-2 text-emerald-500 font-semibold hover:text-emerald-600"
            >
              Tüm yemek analizlerini incele →
            </Link>
          </div>
        </div>

        {/* Bugünkü görevler özeti — hızlı tamamlama onay kutuları */}
        <section className="rounded-3xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
            <h3 className="text-3xl font-bold">Bugünkü Görevler</h3>
            <Link
              to="/dietitian/tasks"
              className="text-sm font-semibold text-emerald-600 hover:underline"
            >
              Tümünü gör →
            </Link>
          </div>
          {dashboardTasks.length === 0 ? (
            <p className="text-slate-500 py-4">Bugün için bekleyen görev yok veya liste henüz yüklenemedi.</p>
          ) : (
            <div className="space-y-3">
              {dashboardTasks.map((task) => (
                <div key={task.id} className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 border border-slate-100 p-4">
                  <div className="flex items-start gap-3 min-w-0">
                    <input
                      type="checkbox"
                      disabled={taskBusyId === task.id}
                      checked={task.isCompleted}
                      onChange={() => void toggleTask(task)}
                      className="mt-1 h-5 w-5 shrink-0 rounded border-slate-300 disabled:opacity-50 accent-emerald-600"
                      aria-label="Tamamlandı"
                    />
                    <div className="min-w-0">
                      <p className="font-semibold text-lg truncate">{task.title}</p>
                      <p className="text-slate-500 text-sm line-clamp-2">{task.subtitle}</p>
                    </div>
                  </div>
                  <span className="text-sm font-semibold whitespace-nowrap text-amber-500">{task.dueLabel}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </SidebarLayout>
  );
}
