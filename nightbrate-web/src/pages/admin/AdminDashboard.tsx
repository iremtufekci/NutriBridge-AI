import { useCallback, useEffect, useState } from "react";
import type { LucideIcon } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { Users, Stethoscope, Clock, Database } from "lucide-react";
import { AreaChart, Area, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type PendingDietitian = {
  id?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  clinicName?: string;
};

type RoleCount = { role: string; count: number };
type Monthly = { year: number; month: number; count: number };

type DashboardApi = {
  totalUsers?: number;
  activeUsers?: number;
  totalClients?: number;
  totalDietitians?: number;
  activeDietitians?: number;
  pendingDietitians?: number;
  roleDistribution?: RoleCount[];
  monthlyRegistrations?: Monthly[];
};

type ActivityItem = {
  id?: string;
  initial?: string;
  actorDisplayName?: string;
  description?: string;
  createdAt?: string;
};

function formatMonthTr(year: number, month: number) {
  return new Date(year, month - 1, 1).toLocaleDateString("tr-TR", { month: "short", year: "numeric" });
}

const ROLE_TR: Record<string, string> = {
  Admin: "Yönetici",
  Client: "Danışan",
  Dietitian: "Diyetisyen",
};

const PIE_COLORS = ["#2ECC71", "#1ABC9C", "#F39C12"];

function formatTimeAgoTr(iso: string | undefined) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  const diff = Date.now() - d.getTime();
  if (diff < 0) return "Az önce";
  const s = Math.floor(diff / 1000);
  if (s < 60) return "Az önce";
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} dk önce`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h} saat önce`;
  const days = Math.floor(h / 24);
  if (days < 7) return `${days} gün önce`;
  return d.toLocaleDateString("tr-TR");
}

function registrationMomPercent(monthly: { count: number }[]): string | null {
  if (monthly.length < 2) return null;
  const last = monthly[monthly.length - 1].count;
  const prev = monthly[monthly.length - 2].count;
  if (prev === 0) return last > 0 ? "+100%" : null;
  const p = ((last - prev) / prev) * 100;
  return `${p >= 0 ? "+" : ""}${p.toFixed(1)}%`;
}

type StatItem = {
  title: string;
  value: string;
  icon: LucideIcon;
  iconWrap: string;
  iconColor: string;
  /** Görseldeki yeşil trend; sadece 4. kartta (aylık MoM) */
  trend: string | null;
};

export function AdminDashboard() {
  const adminName = useAuthProfileDisplayName();
  const [pendingDietitians, setPendingDietitians] = useState<PendingDietitian[]>([]);
  const [statsData, setStatsData] = useState({
    activeUsers: 0,
    activeDietitians: 0,
    pendingDietitians: 0,
    totalUsers: 0,
  });
  const [rawMonthly, setRawMonthly] = useState<Monthly[]>([]);
  const [roleDistribution, setRoleDistribution] = useState<{ name: string; value: number; color: string }[]>([]);
  const [monthlyData, setMonthlyData] = useState<{ month: string; count: number }[]>([]);
  const [activities, setActivities] = useState<ActivityItem[]>([]);

  const loadStats = useCallback(async () => {
    const { data: stats } = await api.get<DashboardApi>("/api/admin/dashboard-stats");

    setStatsData({
      activeUsers: stats.activeUsers ?? 0,
      activeDietitians: stats.activeDietitians || 0,
      pendingDietitians: stats.pendingDietitians || 0,
      totalUsers: stats.totalUsers || 0,
    });

    const monthly = Array.isArray(stats.monthlyRegistrations) ? stats.monthlyRegistrations : [];
    setRawMonthly(monthly);
    setMonthlyData(
      monthly.map((m) => ({
        month: formatMonthTr(m.year, m.month),
        count: m.count,
      }))
    );

    const roles = Array.isArray(stats.roleDistribution) ? stats.roleDistribution : [];
    setRoleDistribution(
      roles.map((r, i) => ({
        name: ROLE_TR[r.role] || r.role,
        value: r.count,
        color: PIE_COLORS[i % PIE_COLORS.length],
      }))
    );
  }, []);

  const loadRecentActivities = useCallback(async () => {
    try {
      const { data } = await api.get<ActivityItem[]>("/api/admin/recent-activities?take=15");
      setActivities(Array.isArray(data) ? data : []);
    } catch {
      setActivities([]);
    }
  }, []);

  useEffect(() => {
    const load = async () => {
      try {
        await loadStats();
        const { data: pending } = await api.get("/api/admin/pending-dietitians");
        setPendingDietitians(Array.isArray(pending) ? pending : []);
        await loadRecentActivities();
      } catch (error) {
        console.error("Dashboard verisi alinamadi", error);
      }
    };
    void load();
  }, [loadStats, loadRecentActivities]);

  const approveDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${dietitianId}`);
      setPendingDietitians((prev) => prev.filter((x) => x.id !== dietitianId));
      await loadStats();
      await loadRecentActivities();
    } catch (error) {
      alert("Onaylama başarısız: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  const momTrend4 = registrationMomPercent(rawMonthly);

  const statItems: StatItem[] = [
    {
      title: "Toplam Aktif Kullanıcı",
      value: String(statsData.activeUsers),
      icon: Users,
      iconWrap: "bg-emerald-100 dark:bg-emerald-900/50",
      iconColor: "text-emerald-600 dark:text-[#2ECC71]",
      trend: null,
    },
    {
      title: "Aktif Diyetisyen Sayısı",
      value: String(statsData.activeDietitians),
      icon: Stethoscope,
      iconWrap: "bg-teal-100 dark:bg-cyan-900/40",
      iconColor: "text-teal-600 dark:text-cyan-400",
      trend: null,
    },
    {
      title: "Onay Bekleyen",
      value: String(statsData.pendingDietitians),
      icon: Clock,
      iconWrap: "bg-amber-100 dark:bg-amber-900/40",
      iconColor: "text-amber-600 dark:text-amber-400",
      trend: null,
    },
    {
      title: "Toplam Kayıt (Users)",
      value: String(statsData.totalUsers),
      icon: Database,
      iconWrap: "bg-violet-100 dark:bg-violet-900/40",
      iconColor: "text-violet-600 dark:text-violet-300",
      trend: momTrend4 ? `${momTrend4} · son aya göre yeni kayıt` : null,
    },
  ];

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 lg:pb-8">
        <div>
          <h1 className="text-3xl sm:text-5xl font-bold mb-1">Admin Dashboard</h1>
          <p className="text-slate-500 dark:text-[#9CA3AF]">Sistem genel görünümü ve istatistikler</p>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {statItems.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.title}
                className="rounded-3xl bg-white dark:bg-[#1F2937] border border-slate-200 dark:border-slate-700 p-4 sm:p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm text-slate-500 dark:text-[#9CA3AF] leading-tight pr-1">{item.title}</p>
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${item.iconWrap}`}
                  >
                    <Icon size={18} className={item.iconColor} />
                  </div>
                </div>
                <p className="text-3xl sm:text-4xl font-bold mt-2 tabular-nums">{item.value}</p>
                <p className="text-sm mt-1 min-h-[1.25rem] text-emerald-500">
                  {item.trend ? `↑ ${item.trend}` : "\u00a0"}
                </p>
              </div>
            );
          })}
        </div>

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
          <div className="xl:col-span-2 bg-white dark:bg-[#1F2937] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
            <h3 className="text-2xl font-bold mb-4">Aylık Kayıt Trendi</h3>
            {monthlyData.length === 0 ? (
              <p className="text-slate-500 text-sm">Henüz kayıt yok veya dönemde veri yok.</p>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={monthlyData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#22C55E" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#22C55E" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#9CA3AF" />
                  <XAxis dataKey="month" stroke="#64748B" fontSize={12} />
                  <YAxis allowDecimals={false} stroke="#64748B" fontSize={12} />
                  <Tooltip
                    contentStyle={{ backgroundColor: "#FFFFFF", border: "1px solid #E2E8F0", borderRadius: "8px", color: "#0D1117" }}
                  />
                  <Area
                    type="monotone"
                    dataKey="count"
                    name="Yeni kayıt"
                    stroke="#22C55E"
                    strokeWidth={2}
                    fill="url(#colorCount)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>

          <div className="bg-white dark:bg-[#1F2937] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
            <h3 className="text-2xl font-bold mb-4">Kullanıcı Rol Dağılımı</h3>
            {roleDistribution.length === 0 || roleDistribution.every((d) => d.value === 0) ? (
              <p className="text-slate-500 text-sm">Veri yok.</p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={roleDistribution}
                    innerRadius={64}
                    outerRadius={96}
                    paddingAngle={2}
                    dataKey="value"
                    stroke="none"
                  >
                    {roleDistribution.map((entry, index) => (
                      <Cell key={index} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: "#FFFFFF", border: "1px solid #E2E8F0", color: "#0D1117" }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="bg-white dark:bg-[#1F2937] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
          <h3 className="text-2xl font-bold mb-4">Onay bekleyen diyetisyenler</h3>
          <div className="space-y-3">
            {pendingDietitians.length === 0 && (
              <p className="text-sm text-slate-500 dark:text-[#9CA3AF]">Şu anda onay bekleyen diyetisyen yok.</p>
            )}

            {pendingDietitians.map((dietitian) => (
              <div
                key={dietitian.id}
                className="flex flex-col gap-3 p-3 sm:flex-row sm:items-center sm:justify-between sm:p-4 bg-slate-50 dark:bg-[#0D1117] rounded-2xl border border-slate-200 dark:border-slate-800"
              >
                <div className="min-w-0">
                  <p className="font-semibold">
                    {dietitian.firstName} {dietitian.lastName}
                  </p>
                  <p className="text-xs text-slate-500 dark:text-[#9CA3AF] break-words">
                    {dietitian.email} - {dietitian.clinicName}
                  </p>
                </div>
                <button
                  onClick={() => approveDietitian(dietitian.id)}
                  className="w-full shrink-0 px-4 py-2.5 rounded-xl bg-emerald-500 text-white hover:bg-emerald-600 text-sm font-semibold sm:w-auto min-h-[44px] sm:min-h-0"
                >
                  Onayla
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white dark:bg-[#1F2937] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
          <h3 className="text-2xl font-bold mb-4 text-slate-900 dark:text-white">Son Aktiviteler</h3>
          <div className="space-y-3">
            {activities.length === 0 && (
              <p className="text-sm text-slate-500 dark:text-[#9CA3AF]">Henüz kayıtlı aktivite yok.</p>
            )}
            {activities.map((a) => (
              <div
                key={a.id || `${a.actorDisplayName}-${a.createdAt}`}
                className="flex items-center justify-between gap-3 p-3 sm:p-4 bg-slate-50 dark:bg-[#0D1117] rounded-2xl border border-slate-200 dark:border-slate-800"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-10 h-10 rounded-full bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold text-sm flex-shrink-0">
                    {(a.initial || "?").charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-slate-900 dark:text-slate-100 truncate">
                      {a.actorDisplayName || "—"}
                    </p>
                    <p className="text-xs text-slate-500 dark:text-[#9CA3AF] truncate">{a.description}</p>
                  </div>
                </div>
                <span className="text-xs text-slate-500 dark:text-[#9CA3AF] flex-shrink-0 whitespace-nowrap">
                  {formatTimeAgoTr(a.createdAt)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}
