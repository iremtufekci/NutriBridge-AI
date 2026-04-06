import { SidebarLayout } from "../../components/SidebarLayout";
import { StatCard } from "../../components/UIComponents";
import { Users, BookOpen, CheckSquare, AlertTriangle, ChevronRight, MessageSquare } from "lucide-react";

export function DietitianDashboard() {
  // Giriş yapan diyetisyenin ismini alıyoruz
  const dietitianName = localStorage.getItem("userName") || "Diyetisyen";

  const criticalClients = [
    { id: 1, name: "Ayşe Yılmaz", reason: "3 Öğün Diyet Dışı", lastSeen: "2 saat önce", avatar: "AY" },
    { id: 2, name: "Mehmet Kaya", reason: "2 Gün Giriş Yok", lastSeen: "2 gün önce", avatar: "MK" },
  ];

  return (
    <SidebarLayout userRole="dietitian" userName={dietitianName}>
      <div className="p-8 space-y-8 bg-[#0F172A] min-h-screen text-white">
        
        {/* Karşılama Kartı (Gradientli) */}
        <div className="bg-gradient-to-r from-[#22C55E]/20 to-[#3B82F6]/10 rounded-3xl p-8 border border-white/5 shadow-xl">
          <h1 className="text-3xl font-bold mb-2 text-white">Merhaba, {dietitianName} 👋</h1>
          <p className="text-slate-400">Bugün takip etmeniz gereken <span className="text-[#22C55E] font-bold">{criticalClients.length} kritik durum</span> var. Danışanlarınızın güncel aktivitelerine göz atın.</p>
        </div>

        {/* İstatistikler */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard 
            title="Danışanlarım" 
            value="28" 
            icon={Users} 
            iconColor="text-[#22C55E]" 
          />
          <StatCard 
            title="Aktif Program" 
            value="24" 
            icon={BookOpen} 
            iconColor="text-[#3B82F6]" 
          />
          <StatCard 
            title="Günlük Görevler" 
            value="12" 
            icon={CheckSquare} 
            iconColor="text-[#F59E0B]" 
          />
          <StatCard 
            title="Kritik Uyarı" 
            value="3" 
            icon={AlertTriangle} 
            iconColor="text-[#EF4444]" 
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Kritik Danışanlar Listesi */}
          <div className="lg:col-span-2 bg-[#1E293B] rounded-2xl p-6 border border-slate-700 shadow-lg">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <AlertTriangle size={20} className="text-[#EF4444]" />
                Kritik Danışanlar
              </h3>
              <button className="text-xs text-[#22C55E] hover:underline">Tümünü Yönet</button>
            </div>
            
            <div className="space-y-4">
              {criticalClients.map(client => (
                <div 
                  key={client.id} 
                  className="p-4 bg-[#0F172A] rounded-xl border-l-4 border-[#EF4444] border-t border-b border-r border-slate-800 flex justify-between items-center hover:bg-slate-900 transition-all cursor-pointer group"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-full bg-[#EF4444]/20 text-[#EF4444] flex items-center justify-center font-bold text-sm">
                      {client.avatar}
                    </div>
                    <div>
                      <p className="font-bold text-slate-100">{client.name}</p>
                      <p className="text-xs text-[#EF4444] font-medium">{client.reason}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-[10px] text-slate-500 font-medium italic">{client.lastSeen}</span>
                    <div className="p-2 bg-slate-800 rounded-lg group-hover:text-[#22C55E] transition-colors">
                      <ChevronRight size={18} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Hızlı İşlemler Yan Panel */}
          <div className="bg-[#1E293B] rounded-2xl p-6 border border-slate-700 shadow-lg">
            <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
              <MessageSquare size={20} className="text-[#22C55E]" />
              Hızlı İşlemler
            </h3>
            <div className="space-y-3">
              <button className="flex items-center gap-3 w-full p-4 bg-[#0F172A] border border-slate-800 rounded-xl hover:border-[#22C55E] transition-all group">
                <div className="p-2 bg-[#22C55E]/10 rounded-lg text-[#22C55E]">
                  <MessageSquare size={20} />
                </div>
                <span className="text-sm font-semibold">Mesajları Yanıtla</span>
              </button>
              <button className="flex items-center gap-3 w-full p-4 bg-[#0F172A] border border-slate-800 rounded-xl hover:border-[#3B82F6] transition-all group">
                <div className="p-2 bg-[#3B82F6]/10 rounded-lg text-[#3B82F6]">
                  <CheckSquare size={20} />
                </div>
                <span className="text-sm font-semibold">Program Oluştur</span>
              </button>
            </div>
            
            <div className="mt-6 pt-6 border-t border-slate-700">
              <h4 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <CheckSquare size={16} className="text-[#F59E0B]" />
                Haftalık Rapor
              </h4>
              <p className="text-xs text-slate-400 leading-relaxed">Danışanlarınızın genel başarı oranı geçen haftaya göre <span className="text-[#22C55E] font-bold">%8 arttı.</span></p>
            </div>
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}