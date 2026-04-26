import { Settings, Shield } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { AccountProfilePanel } from "../RoleAccountProfile";

export function AdminSettings() {
  const adminName = useAuthProfileDisplayName();

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="min-h-full bg-[#F4F6F8] dark:bg-[#0D1117] px-4 py-6 pb-24 transition-colors sm:px-6 sm:py-8 md:pb-8">
        <div className="mx-auto max-w-2xl">
          <div className="mb-2 inline-flex items-center gap-1.5 rounded-full border border-slate-200/80 bg-white/90 px-3 py-1 text-xs font-semibold text-slate-600 shadow-sm dark:border-[#374151] dark:bg-[#1F2937] dark:text-[#9CA3AF]">
            <Shield className="h-3.5 w-3.5 text-[#2ECC71]" strokeWidth={2.5} />
            Yönetim paneli
          </div>

          <div className="mt-4 flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#2ECC71] to-[#1ABC9C] text-white shadow-lg shadow-[#2ECC71]/20">
              <Settings className="h-7 w-7" strokeWidth={2.2} />
            </div>
            <div className="min-w-0 flex-1 pt-0.5">
              <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-3xl">
                Ayarlar
              </h1>
              <p className="mt-1 text-sm text-slate-500 dark:text-[#9CA3AF] sm:text-base">
                Hesap bilgileriniz ve yönetici tercihleri. Veriler sunucudaki kaydınızdan yüklenir.
              </p>
            </div>
          </div>

          <div className="mt-8">
            <h2 className="mb-1 text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-[#9CA3AF]">
              Hesap
            </h2>
            <AccountProfilePanel appRole="admin" />
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}
