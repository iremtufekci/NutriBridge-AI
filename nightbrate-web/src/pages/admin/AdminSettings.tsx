import { Settings, Shield } from "lucide-react"; // Ayarlar ve güvenlik ikonları
import { SidebarLayout } from "../../components/SidebarLayout"; // Yönetici kenar çubuğu
import { AdminPageShell } from "../../components/admin/AdminPageShell"; // Tutarlı admin sayfa düzeni
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName"; // Oturum adı
import { AccountProfilePanel } from "../RoleAccountProfile"; // Paylaşılan profil kartı

// Yönetici ayarlar ve hesap profili sayfası
export function AdminSettings() {
  const adminName = useAuthProfileDisplayName(); // Sol menüde gösterilecek ad

  // Yönetici kabuğu + dar genişlikte ayarlar düzeni
  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <AdminPageShell
        title="Ayarlar"
        subtitle="Hesap bilgileriniz ve yönetici tercihleri. Veriler sunucudaki kaydınızdan yüklenir."
        maxWidth="narrow"
      >
          {/* Üst bilgi şeridi */}
          <div className="mb-6 flex items-center gap-4 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-[#2ECC71] to-[#1ABC9C] text-white shadow-md shadow-[#2ECC71]/20">
              <Settings className="h-6 w-6" strokeWidth={2.2} />
            </div>
            <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600">
              <Shield className="h-3.5 w-3.5 text-[#2ECC71]" strokeWidth={2.5} />
              Güvenli yönetici hesabı
            </div>
          </div>

          {/* Profil bölümü */}
          <div>
            <h2 className="mb-1 text-sm font-semibold uppercase tracking-wider text-slate-500">
              Hesap
            </h2>
            <AccountProfilePanel appRole="admin" />
          </div>
      </AdminPageShell>
    </SidebarLayout>
  );
}
