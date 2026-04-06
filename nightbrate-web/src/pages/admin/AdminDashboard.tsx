import { SidebarLayout } from "../../components/SidebarLayout";
import { StatCard } from "../../components/UIComponents";
import { Users, Stethoscope, Clock, Cpu } from "lucide-react";
import { AreaChart, Area, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

export function AdminDashboard() {
  // LocalStorage'dan gerçek admin ismini alalım
  const adminName = localStorage.getItem("userName") || "Admin Paneli";

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

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="p-8 space-y-8 bg-[#0F172A] min-h-screen text-white">
        {/* Üst Başlık */}
        <div>
          <h1 className="text-3xl font-bold mb-2">Sistem Özeti</h1>
          <p className="text-slate-400">Genel istatistikler ve kullanıcı dağılımı</p>
        </div>

        {/* İstatistik Kartları */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard title="Toplam Kullanıcı" value="901" icon={Users} iconColor="text-[#22C55E]" trend={{ value: "+12%", isPositive: true }} />
          <StatCard title="Diyetisyenler" value="42" icon={Stethoscope} iconColor="text-[#3B82F6]" />
          <StatCard title="Onay Bekleyen" value="7" icon={Clock} iconColor="text-[#F59E0B]" />
          <StatCard title="AI Kullanımı" value="89%" icon={Cpu} iconColor="text-[#8B5CF6]" />
        </div>

        {/* Grafikler Alanı */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Alan Grafiği (Trend) */}
          <div className="lg:col-span-2 bg-[#1E293B] rounded-xl p-6 border border-slate-700">
            <h3 className="text-lg font-semibold mb-4">Aylık Kayıt Trendi</h3>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={monthlyData}>
                <defs>
                  <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22C55E" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#22C55E" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
                <XAxis dataKey="month" stroke="#94A3B8" fontSize={12} />
                <YAxis stroke="#94A3B8" fontSize={12} />
                <Tooltip 
                  contentStyle={{ backgroundColor: "#0F172A", border: "1px solid #334155", borderRadius: "8px" }}
                  itemStyle={{ color: "#22C55E" }}
                />
                <Area type="monotone" dataKey="users" stroke="#22C55E" strokeWidth={3} fill="url(#colorUsers)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Pasta Grafiği (Rol Dağılımı) */}
          <div className="bg-[#1E293B] rounded-xl p-6 border border-slate-700">
            <h3 className="text-lg font-semibold mb-4">Rol Dağılımı</h3>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={roleDistribution} innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                  {roleDistribution.map((entry, index) => <Cell key={index} fill={entry.color} />)}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: "#0F172A", border: "1px solid #334155", color: "#fff" }} />
              </PieChart>
            </ResponsiveContainer>
            {/* Gösterge (Legend) */}
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

        {/* Son Aktiviteler */}
        <div className="bg-[#1E293B] rounded-xl p-6 border border-slate-700">
          <h3 className="text-lg font-semibold mb-4">Son Aktiviteler</h3>
          <div className="space-y-4">
            {recentActivities.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between p-3 bg-[#0F172A] rounded-lg border border-slate-800">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-[#22C55E]/20 text-[#22C55E] flex items-center justify-center font-bold">
                    {activity.user.charAt(0)}
                  </div>
                  <div>
                    <p className="text-sm font-medium">{activity.user}</p>
                    <p className="text-xs text-slate-400">{activity.action}</p>
                  </div>
                </div>
                <span className="text-xs text-slate-500">{activity.time}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}