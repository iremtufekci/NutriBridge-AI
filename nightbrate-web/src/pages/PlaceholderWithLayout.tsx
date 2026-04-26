import { Link } from "react-router-dom";
import { SidebarLayout } from "../components/SidebarLayout";
import { useAuthProfileDisplayName } from "../hooks/useAuthProfileDisplayName";

const dashboardPath: Record<"admin" | "dietitian" | "client", string> = {
  admin: "/admin/dashboard",
  dietitian: "/dietitian/dashboard",
  client: "/client/home",
};

function resolveRole(): "admin" | "dietitian" | "client" {
  const r = localStorage.getItem("userRole")?.toLowerCase();
  if (r === "admin" || r === "dietitian" || r === "client") return r;
  return "client";
}

export function PlaceholderWithLayout() {
  const userRole = resolveRole();
  const userName = useAuthProfileDisplayName();

  return (
    <SidebarLayout userRole={userRole} userName={userName}>
      <div className="mx-auto flex min-h-[min(60vh,28rem)] max-w-md flex-col items-center justify-center px-4 py-8 text-center md:min-h-[40vh]">
        <h1 className="mb-2 text-lg font-semibold text-slate-800 dark:text-slate-200">
          Sayfa hazırlanıyor
        </h1>
        <p className="mb-6 text-sm text-slate-500 dark:text-[#9CA3AF]">
          Bu bölüm yakında aktif olacak. Alttaki menüden diğer sayfalara gidebilir veya panele dönebilirsiniz.
        </p>
        <Link
          to={dashboardPath[userRole]}
          className="inline-flex min-h-[44px] min-w-[8rem] items-center justify-center rounded-xl bg-emerald-500 px-4 text-sm font-semibold text-white active:bg-emerald-600"
        >
          Panele dön
        </Link>
      </div>
    </SidebarLayout>
  );
}
