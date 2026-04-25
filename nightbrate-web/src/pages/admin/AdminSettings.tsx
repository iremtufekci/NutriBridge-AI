import { SidebarLayout } from "../../components/SidebarLayout";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { AccountProfilePanel } from "../RoleAccountProfile";

export function AdminSettings() {
  const adminName = useAuthProfileDisplayName();

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="mx-auto max-w-lg px-4 py-8">
        <h1 className="mb-1 text-2xl font-bold text-slate-900 dark:text-slate-100 sm:text-3xl">Ayarlar</h1>
        <p className="mb-2 text-sm text-slate-500 dark:text-slate-400">Uygulama ve hesap tercihleri</p>
        <h2 className="mb-4 text-lg font-semibold text-slate-800 dark:text-slate-200">Kişisel bilgiler</h2>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Aşağıdaki alanlar veritabanındaki hesap kaydınızdan yüklenir.
        </p>
        <AccountProfilePanel appRole="admin" />
      </div>
    </SidebarLayout>
  );
}
