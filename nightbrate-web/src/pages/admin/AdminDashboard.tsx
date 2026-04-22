import { useEffect, useState } from "react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { Users, Stethoscope, Clock, Cpu } from "lucide-react";
import { AreaChart, Area, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { api } from "../../api/http";

type PendingDietitian = {
  id?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  clinicName?: string;
};

export function AdminDashboard() {
  const adminName = localStorage.getItem("userName") || "Admin User";
  const [pendingDietitians, setPendingDietitians] = useState<PendingDietitian[]>([]);
  const [statsData, setStatsData] = useState({
    totalUsers: 0,
    activeDietitians: 0,
    pendingDietitians: 0,
    totalClients: 0,
    totalDietitians: 0,
  });

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        const [{ data: stats }, { data: pending }] = await Promise.all([
          api.get("/api/admin/dashboard-stats"),
          api.get("/api/admin/pending-dietitians"),
        ]);

        setStatsData({
          totalUsers: stats.totalUsers || 0,
          activeDietitians: stats.activeDietitians || 0,
          pendingDietitians: stats.pendingDietitians || 0,
          totalClients: stats.totalClients || 0,
          totalDietitians: stats.totalDietitians || 0,
        });
        setPendingDietitians(Array.isArray(pending) ? pending : []);
      } catch (error) {
        console.error("Dashboard verisi alinamadi", error);
      }
    };
    loadDashboard();
  }, []);

  const approveDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${dietitianId}`);
      setPendingDietitians((prev) => prev.filter((x) => x.id !== dietitianId));
      setStatsData((prev) => ({
        ...prev,
        pendingDietitians: Math.max(prev.pendingDietitians - 1, 0),
        activeDietitians: prev.activeDietitians + 1,
      }));
    } catch (error) {
      alert("Onaylama başarısız: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  const monthlyData = [
    { month: "Ekim", users: 45 }, { month: "Kasım", users: 62 },
    { month: "Aralık", users: 78 }, { month: "Ocak", users: 95 },
    { month: "Şubat", users: 118 }, { month: "Mart", users: 142 },
  ];

  const roleDistribution = [
    { name: "Danışanlar", value: 856, color: "#2ECC71" },
    { name: "Diyetisyenler", value: 42, color: "#1ABC9C" },
    { name: "Adminler", value: 3, color: "#F39C12" },
  ];

  const recentActivities = [
    { id: 1, user: "Dr. Ayşe Kaya", action: "Yeni danışan ekledi", time: "5 dk önce" },
    { id: 2, user: "Mehmet Yılmaz", action: "Diyet programı güncelledi", time: "12 dk önce" },
    { id: 3, user: "Dr. Zeynep Demir", action: "Hesap oluşturdu", time: "1 saat önce" },
  ];

  const stats = [
    { title: "Toplam Aktif Kullanıcı", value: String(statsData.totalUsers), trend: "", icon: Users, iconColor: "text-emerald-500" },
    { title: "Aktif Diyetisyen Sayısı", value: String(statsData.activeDietitians), trend: "", icon: Stethoscope, iconColor: "text-emerald-500" },
    { title: "Onay Bekleyen", value: String(statsData.pendingDietitians), trend: "", icon: Clock, iconColor: "text-amber-500" },
    { title: "AI Kullanım Oranı Bu Ay", value: "89%", trend: "+5.2%", icon: Cpu, iconColor: "text-violet-500" },
  ];

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0F172A] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 md:pb-8">
        <div>
          <h1 className="text-3xl sm:text-5xl font-bold mb-1">Admin Dashboard</h1>
          <p className="text-slate-500 dark:text-slate-400">Sistem genel görünümü ve istatistikler</p>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {stats.map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.title} className="rounded-3xl bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-slate-700 p-4 sm:p-5 shadow-sm">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm text-slate-500 dark:text-slate-400">{item.title}</p>
                  <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                    <Icon size={18} className={item.iconColor} />
                  </div>
                </div>
                <p className="text-4xl font-bold mt-2">{item.value}</p>
                {item.trend ? <p className="text-sm text-emerald-500 mt-1">↑ {item.trend}</p> : <p className="text-sm mt-1">&nbsp;</p>}
              </div>
            );
          })}
        </div>

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
          <div className="xl:col-span-2 bg-white dark:bg-[#1E293B] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
            <h3 className="text-2xl font-bold mb-4">Aylık Kayıt Trendi</h3>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={monthlyData}>
                <defs>
                  <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22C55E" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#22C55E" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#94A3B8" vertical={false} />
                <XAxis dataKey="month" stroke="#64748B" fontSize={12} />
                <YAxis stroke="#64748B" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: "#FFFFFF", border: "1px solid #E2E8F0", borderRadius: "8px", color: "#0F172A" }}
                />
                <Area type="monotone" dataKey="users" stroke="#22C55E" strokeWidth={3} fill="url(#colorUsers)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-white dark:bg-[#1E293B] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
            <h3 className="text-2xl font-bold mb-4">Kullanıcı Rol Dağılımı</h3>
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={roleDistribution} innerRadius={60} outerRadius={90} paddingAngle={4} dataKey="value">
                  {roleDistribution.map((entry, index) => <Cell key={index} fill={entry.color} />)}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: "#FFFFFF", border: "1px solid #E2E8F0", color: "#0F172A" }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="mt-4 space-y-2">
              {roleDistribution.map((item, index) => (
                <div key={index} className="flex justify-between text-sm">
                  <span className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                    {item.name}
                  </span>
                  <span className="font-bold">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-[#1E293B] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
          <h3 className="text-2xl font-bold mb-4">Son Aktiviteler</h3>
          <div className="space-y-3">
            {recentActivities.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between p-3 sm:p-4 bg-slate-50 dark:bg-[#0F172A] rounded-2xl border border-slate-200 dark:border-slate-800">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-[#22C55E]/20 text-[#22C55E] flex items-center justify-center font-bold">
                    {activity.user.charAt(0)}
                  </div>
                  <div>
                    <p className="text-base font-semibold">{activity.user}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">{activity.action}</p>
                  </div>
                </div>
                <span className="text-xs text-slate-500">{activity.time}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white dark:bg-[#1E293B] rounded-3xl p-5 sm:p-6 border border-slate-200 dark:border-slate-700 shadow-sm">
          <h3 className="text-2xl font-bold mb-4">Onay Bekleyen Diyetisyenler</h3>
          <div className="space-y-3">
            {pendingDietitians.length === 0 && (
              <p className="text-sm text-slate-500 dark:text-slate-400">Şu anda onay bekleyen diyetisyen yok.</p>
            )}

            {pendingDietitians.map((dietitian) => (
              <div
                key={dietitian.id}
                className="flex items-center justify-between p-3 sm:p-4 bg-slate-50 dark:bg-[#0F172A] rounded-2xl border border-slate-200 dark:border-slate-800"
              >
                <div>
                  <p className="font-semibold">{dietitian.firstName} {dietitian.lastName}</p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">{dietitian.email} - {dietitian.clinicName}</p>
                </div>
                <button
                  onClick={() => approveDietitian(dietitian.id)}
                  className="px-4 py-2 rounded-xl bg-emerald-500 text-white hover:bg-emerald-600 text-sm font-semibold"
                >
                  Onayla
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}