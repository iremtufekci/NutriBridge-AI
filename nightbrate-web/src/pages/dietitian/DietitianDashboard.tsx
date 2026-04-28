import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { SidebarLayout } from "../../components/SidebarLayout";
import { Users, BookOpen, CheckSquare, AlertTriangle, ChevronRight } from "lucide-react";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type TaskItem = {
  id: string;
  title: string;
  subtitle: string;
  isCompleted: boolean;
  dueLabel: string;
  category: string;
};

type TasksBundle = {
  taskDate: string;
  pendingCount: number;
  completedCount: number;
  totalCount: number;
  tasks: TaskItem[];
};

export function DietitianDashboard() {
  const dietitianName = useAuthProfileDisplayName();
  const [clients, setClients] = useState<any[]>([]);
  const [tasksBundle, setTasksBundle] = useState<TasksBundle | null>(null);
  const [criticalCount, setCriticalCount] = useState(0);
  const [taskBusyId, setTaskBusyId] = useState<string | null>(null);

  const loadClients = useCallback(async () => {
    try {
      const { data } = await api.get("/api/dietitian/clients-with-last-meal");
      setClients(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Danisanlar alinamadi", error);
    }
  }, []);

  const loadTasks = useCallback(async () => {
    try {
      const { data } = await api.get<TasksBundle>("/api/dietitian/daily-tasks/today");
      setTasksBundle(data);
    } catch (error) {
      console.error("Gorevler alinamadi", error);
      setTasksBundle(null);
    }
  }, []);

  const loadCriticalCount = useCallback(async () => {
    try {
      const { data } = await api.get<unknown[]>("/api/dietitian/critical-alerts");
      setCriticalCount(Array.isArray(data) ? data.length : 0);
    } catch {
      setCriticalCount(0);
    }
  }, []);

  useEffect(() => {
    void loadClients();
    void loadTasks();
    void loadCriticalCount();
  }, [loadClients, loadTasks, loadCriticalCount]);

  const criticalClients = useMemo(
    () =>
      clients.slice(0, 3).map((client, index) => ({
        id: client.id || index,
        name: `${client.firstName || ""} ${client.lastName || ""}`.trim(),
        reason: "Son ogun kontrolu gerekiyor",
        lastSeen: client.lastMeal?.timestamp ? "Yeni kayit var" : "Henuz ogun yok",
        avatar: (client.firstName || "D").charAt(0),
      })),
    [clients]
  );

  const lastMealPhotos = useMemo(
    () =>
      clients
        .filter((x) => x.lastMeal?.photoUrl)
        .slice(0, 3)
        .map((x, i) => ({
          id: x.id || i,
          label: x.lastMeal?.timestamp ? "Son kayit" : "Kayit yok",
          image: x.lastMeal.photoUrl,
        })),
    [clients]
  );

  const dashboardTasks = useMemo(() => {
    if (!tasksBundle?.tasks?.length) return [];
    return tasksBundle.tasks.filter((t) => !t.isCompleted).slice(0, 4);
  }, [tasksBundle]);

  const todayTasksTotal = tasksBundle?.totalCount ?? 0;

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

  const statCards = [
    { title: "Toplam Danışan", value: String(clients.length), icon: Users, iconColor: "text-emerald-500" },
    { title: "Aktif Program", value: String(clients.length), icon: BookOpen, iconColor: "text-emerald-500" },
    { title: "Bugünkü Görevler", value: String(todayTasksTotal), icon: CheckSquare, iconColor: "text-amber-500" },
    { title: "Kritik Uyarı", value: String(criticalCount), icon: AlertTriangle, iconColor: "text-rose-500" },
  ];

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 lg:pb-8">
        <div className="bg-[#DFF1EA] dark:bg-emerald-500/10 rounded-3xl p-5 sm:p-8 border border-[#CCE6DC] dark:border-emerald-500/20">
          <h1 className="text-3xl sm:text-5xl font-bold mb-2 text-slate-900 dark:text-white">Merhaba, {dietitianName} 👋</h1>
          <p className="text-slate-600 dark:text-[#9CA3AF]">
            Bugün <span className="font-bold">{criticalClients.length} danışanınızın kritik durumu</span> var. Güncel aktivitelere göz atın.
          </p>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.title} className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm">
                <div className="flex items-start justify-between">
                  <p className="text-sm text-slate-500 dark:text-[#9CA3AF]">{card.title}</p>
                  <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                    <Icon size={18} className={card.iconColor} />
                  </div>
                </div>
                <p className="text-3xl font-bold mt-2">{card.value}</p>
                {card.title === "Bugünkü Görevler" && tasksBundle != null && (
                  <p className="text-xs text-slate-500 dark:text-[#9CA3AF] mt-1">
                    {tasksBundle.pendingCount} bekleyen · {tasksBundle.completedCount} tamamlandı
                  </p>
                )}
              </div>
            );
          })}
        </div>

        <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
          <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-bold">Kritik Danışanlar</h3>
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-rose-100 text-rose-500">{criticalClients.length} Danışan</span>
            </div>

            <div className="space-y-3">
              {criticalClients.map((client) => (
                <div key={client.id} className="rounded-2xl border border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-[#0D1117] p-4 border-l-4 border-l-rose-500">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3">
                      <div className="w-9 h-9 rounded-full bg-rose-100 text-rose-500 flex items-center justify-center text-sm font-bold">
                        {client.avatar}
                      </div>
                      <div>
                        <p className="font-semibold text-lg">{client.name}</p>
                        <p className="text-sm text-slate-500 dark:text-[#9CA3AF]">{client.lastSeen}</p>
                        <p className="inline-flex mt-2 text-xs px-3 py-1 rounded-full bg-rose-100 text-rose-500">{client.reason}</p>
                      </div>
                    </div>
                    <ChevronRight className="text-slate-400 mt-1" size={18} />
                  </div>
                </div>
              ))}
            </div>
            <button className="mt-4 w-full py-2 text-emerald-500 font-semibold hover:text-emerald-600">Tümünü Görüntüle →</button>
          </div>

          <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-bold">Son Öğün Fotoğrafları</h3>
              <span className="text-sm text-slate-500 dark:text-[#9CA3AF]">İnceleme Bekliyor</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {lastMealPhotos.map((photo) => (
                <div key={photo.id} className={photo.id === 3 ? "sm:col-span-2" : ""}>
                  <img
                    src={photo.image}
                    alt="Öğün fotoğrafı"
                    className="w-full h-40 object-cover rounded-2xl"
                  />
                  <p className="text-sm text-slate-500 dark:text-[#9CA3AF] mt-1">{photo.label}</p>
                </div>
              ))}
            </div>
            <Link
              to="/dietitian/ai-review"
              className="mt-4 w-full block text-center py-2 text-emerald-500 font-semibold hover:text-emerald-600"
            >
              AI Denetim Paneline Git →
            </Link>
          </div>
        </div>

        <section className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] p-4 sm:p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
            <h3 className="text-3xl font-bold">Bugünkü Görevler</h3>
            <Link
              to="/dietitian/tasks"
              className="text-sm font-semibold text-emerald-600 dark:text-emerald-400 hover:underline"
            >
              Tümünü gör →
            </Link>
          </div>
          {dashboardTasks.length === 0 ? (
            <p className="text-slate-500 dark:text-[#9CA3AF] py-4">Bugün için bekleyen görev yok veya liste henüz yüklenemedi.</p>
          ) : (
            <div className="space-y-3">
              {dashboardTasks.map((task) => (
                <div key={task.id} className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 dark:bg-[#0D1117] border border-slate-100 dark:border-slate-800 p-4">
                  <div className="flex items-start gap-3 min-w-0">
                    <input
                      type="checkbox"
                      disabled={taskBusyId === task.id}
                      checked={task.isCompleted}
                      onChange={() => void toggleTask(task)}
                      className="mt-1 h-5 w-5 shrink-0 rounded border-slate-300 dark:border-slate-600 disabled:opacity-50 accent-emerald-600"
                      aria-label="Tamamlandı"
                    />
                    <div className="min-w-0">
                      <p className="font-semibold text-lg truncate">{task.title}</p>
                      <p className="text-slate-500 dark:text-[#9CA3AF] text-sm line-clamp-2">{task.subtitle}</p>
                    </div>
                  </div>
                  <span className="text-sm font-semibold whitespace-nowrap text-amber-500 dark:text-amber-400">{task.dueLabel}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </SidebarLayout>
  );
}
