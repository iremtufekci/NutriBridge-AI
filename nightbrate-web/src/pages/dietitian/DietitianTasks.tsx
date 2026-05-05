import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CheckSquare, ChevronLeft, Loader2 } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type TaskItem = {
  id: string;
  taskKey: string;
  title: string;
  subtitle: string;
  category: string;
  clientId?: string | null;
  isCompleted: boolean;
  dueLabel: string;
};

type Bundle = {
  taskDate: string;
  pendingCount: number;
  completedCount: number;
  totalCount: number;
  tasks: TaskItem[];
};

function categoryStyle(cat: string) {
  switch (cat) {
    case "Critical":
      return "bg-rose-100 text-rose-600";
    case "MealLog":
      return "bg-amber-100 text-amber-700";
    case "ProgramReview":
      return "bg-emerald-100 text-emerald-700";
    default:
      return "bg-slate-100 text-slate-600";
  }
}

function categoryLabel(cat: string) {
  switch (cat) {
    case "Critical":
      return "Kritik";
    case "MealLog":
      return "Öğün";
    case "ProgramReview":
      return "Program";
    default:
      return cat;
  }
}

export function DietitianTasks() {
  const name = useAuthProfileDisplayName();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<Bundle>("/api/dietitian/daily-tasks/today");
      setBundle(data);
    } catch (e) {
      setError(getApiErrorMessage(e));
      setBundle(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const toggle = async (task: TaskItem) => {
    setBusyId(task.id);
    setError(null);
    try {
      await api.patch(`/api/dietitian/daily-tasks/${task.id}/complete`, {
        isCompleted: !task.isCompleted,
      });
      await load();
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <SidebarLayout userRole="dietitian" userName={name}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-slate-50 min-h-screen text-slate-900 pb-24 lg:pb-8">
        <div className="flex items-center gap-3">
          <Link
            to="/dietitian/dashboard"
            className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600"
            aria-label="Özet"
          >
            <ChevronLeft size={22} />
          </Link>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold flex items-center gap-2">
              <CheckSquare className="text-amber-500" size={28} />
              Günlük görevler
            </h1>
            {bundle?.taskDate && (
              <p className="text-sm text-slate-500 mt-1">
                Tarih: {bundle.taskDate} · Bekleyen {bundle.pendingCount} / Toplam {bundle.totalCount}
              </p>
            )}
          </div>
        </div>

        {loading && (
          <div className="flex justify-center py-16">
            <Loader2 className="h-10 w-10 animate-spin text-emerald-500" />
          </div>
        )}

        {error && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
            {error}
          </div>
        )}

        {!loading && bundle && (
          <div className="rounded-3xl border border-slate-200 bg-white p-4 sm:p-5 shadow-sm space-y-3">
            {bundle.tasks.length === 0 && (
              <p className="text-slate-500 py-8 text-center">Bugün için henüz görev üretilmedi.</p>
            )}
            {bundle.tasks.map((task) => (
              <div
                key={task.id}
                className="flex items-start gap-3 rounded-2xl bg-slate-50 border border-slate-100 p-4"
              >
                <button
                  type="button"
                  disabled={busyId === task.id}
                  onClick={() => void toggle(task)}
                  className="mt-0.5 h-6 w-6 shrink-0 rounded border-2 border-slate-300 flex items-center justify-center disabled:opacity-50"
                  aria-pressed={task.isCompleted}
                  aria-label={task.isCompleted ? "Tamamlanmadı işaretle" : "Tamamlandı işaretle"}
                >
                  {task.isCompleted && <span className="text-emerald-500 text-sm font-bold">✓</span>}
                </button>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2 gap-y-1">
                    <span className={`text-[10px] font-bold uppercase tracking-wide px-2 py-0.5 rounded-full ${categoryStyle(task.category)}`}>
                      {categoryLabel(task.category)}
                    </span>
                    {task.isCompleted && (
                      <span className="text-[10px] font-semibold text-emerald-600">Tamamlandı</span>
                    )}
                  </div>
                  <p className={`font-semibold text-lg mt-1 ${task.isCompleted ? "line-through opacity-60" : ""}`}>{task.title}</p>
                  <p className="text-slate-500 text-sm">{task.subtitle}</p>
                </div>
                <span className="text-xs font-semibold text-amber-600 whitespace-nowrap">{task.dueLabel}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
