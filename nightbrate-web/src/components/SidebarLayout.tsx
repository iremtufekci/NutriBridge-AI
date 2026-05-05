import { ReactNode, useMemo } from "react";
import { createPortal } from "react-dom";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  Settings,
  Home,
  ChefHat,
  LogOut,
  CalendarDays,
  ScanSearch,
  User,
  BookOpen,
  BarChart3,
  AlertTriangle,
  ClipboardCheck,
  History,
  Share2,
  ListTodo,
  FileText,
} from "lucide-react";

interface SidebarProps {
  children: ReactNode;
  userRole: "admin" | "dietitian" | "client";
  userName: string;
}

export function SidebarLayout({ children, userRole, userName }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userName");
    navigate("/login");
  };

  const menuConfig = useMemo(
    () => ({
      admin: [
        { label: "Özet", icon: LayoutDashboard, path: "/admin/dashboard" },
        { label: "Kullanıcı Yönetimi", icon: Users, path: "/admin/users" },
        { label: "Diyetisyen Onayları", icon: ClipboardCheck, path: "/admin/approvals" },
        { label: "Sistem Analitiği", icon: BarChart3, path: "/admin/analytics" },
        { label: "Ayarlar", icon: Settings, path: "/admin/settings" },
      ],
      dietitian: [
        { label: "Özet", icon: LayoutDashboard, path: "/dietitian/dashboard" },
        { label: "Görevlerim", icon: ListTodo, path: "/dietitian/tasks" },
        { label: "Danışanlarım", icon: Users, path: "/dietitian/clients" },
        { label: "Diyet Programları", icon: BookOpen, path: "/dietitian/programs" },
        { label: "Yapay zeka denetimi", icon: BarChart3, path: "/dietitian/ai-review" },
        { label: "Kritik Uyarılar", icon: AlertTriangle, path: "/dietitian/alerts" },
        { label: "Profil", icon: User, path: "/dietitian/profile" },
      ],
      client: [
        { label: "Ana Sayfa", icon: Home, path: "/client/home" },
        { label: "Diyet Programım", icon: CalendarDays, path: "/client/diet-program" },
        { label: "Geçmiş diyetlerim", icon: History, path: "/client/diet-program-history" },
        { label: "Yemek Analizi", icon: ScanSearch, path: "/client/food-scan" },
        { label: "PDF Analizi", icon: FileText, path: "/client/pdf-analysis" },
        { label: "Yapay zeka mutfak şefi", icon: ChefHat, path: "/client/ai-chef" },
        { label: "Paylaştığım tarifler", icon: Share2, path: "/client/ai-chef-shares" },
        { label: "Profilim", icon: User, path: "/client/profile" },
      ],
    }),
    []
  );

  const currentMenu = menuConfig[userRole];

  const pathIsActive = (path: string) => {
    if (location.pathname === path) return true;
    if (path === "/client/home" && location.pathname === "/client") return true;
    if (path === "/dietitian/clients" && location.pathname.startsWith("/dietitian/clients")) return true;
    if (path === "/dietitian/dashboard" && location.pathname === "/dietitian") return true;
    if (path === "/admin/dashboard" && location.pathname === "/admin") return true;
    return false;
  };

  const showMobileBar = userRole === "client" || userRole === "dietitian" || userRole === "admin";

  const mobileNav = showMobileBar && (
    <nav
      className="fixed bottom-0 left-0 right-0 z-[100] w-full max-w-full select-none border-t border-slate-200/95 bg-white/98 text-slate-500 shadow-[0_-6px_28px_rgba(15,23,42,0.08)] backdrop-blur-md lg:hidden"
      style={{ paddingBottom: "max(0.5rem, env(safe-area-inset-bottom, 0px))" }}
      aria-label="Ana menü"
    >
      <div className="mx-auto flex w-full max-w-full items-stretch justify-between">
        {currentMenu.map((item) => {
          const Icon = item.icon;
          const isActive = pathIsActive(item.path);
          const manyItems = currentMenu.length > 5;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={[
                "flex min-h-14 min-w-0 flex-1 flex-col items-center justify-center gap-0.5 px-0.5 py-1.5 text-center transition-colors",
                "active:bg-slate-100",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2ECC71]",
                isActive ? "text-[#2ECC71]" : "text-slate-500",
              ].join(" ")}
            >
              <Icon
                size={manyItems ? 20 : 24}
                strokeWidth={isActive ? 2.4 : 2}
                className="shrink-0"
                aria-hidden
              />
              <span
                className={[
                  "w-full max-w-full px-0.5 font-medium leading-tight",
                  manyItems ? "line-clamp-2 text-[9px]" : "line-clamp-2 text-[10px] sm:text-[11px]",
                  isActive ? "font-semibold text-[#2ECC71]" : "text-inherit",
                ].join(" ")}
              >
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );

  return (
    <div className="flex min-h-svh w-full min-w-0 flex-1 flex-col bg-slate-50 text-slate-900 antialiased lg:min-h-screen lg:flex-row">
      <aside className="hidden w-[17rem] shrink-0 flex-col border-r border-slate-200/90 bg-gradient-to-b from-slate-100 to-slate-50 lg:flex">
        <div className="border-b border-slate-200/90 px-5 py-5">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <h2 className="text-xl font-bold tracking-tight text-slate-900">
                <span className="text-[#22C55E]">NutriBridge</span> <span className="text-slate-800">AI</span>
              </h2>
              <p className="mt-0.5 text-[11px] font-medium uppercase tracking-wide text-slate-500">
                Akıllı beslenme platformu
              </p>
              <p className="mt-2 text-xs text-slate-600">
                {userRole === "client"
                  ? "Danışan paneli"
                  : userRole === "dietitian"
                    ? "Diyetisyen paneli"
                    : "Yönetim paneli"}
              </p>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-1 p-3">
          {currentMenu.map((item) => {
            const Icon = item.icon;
            const isActive = pathIsActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-[15px] transition-colors ${
                  isActive
                    ? "bg-[#22C55E] font-semibold text-white shadow-sm shadow-emerald-600/25"
                    : "text-slate-700 hover:bg-white/80"
                }`}
              >
                <Icon size={18} className={isActive ? "text-white" : "opacity-90"} strokeWidth={isActive ? 2.25 : 2} />
                <span className="font-medium">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-slate-200/90 p-4">
          <div className="mb-4 flex items-center gap-3 rounded-xl border border-slate-200/80 bg-white/70 px-3 py-2.5">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#22C55E] text-sm font-bold text-white">
              {(userName && userName.length > 0 ? userName.charAt(0) : "?").toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-800">{userName}</p>
              <p className="text-xs text-slate-500">
                {userRole === "client" ? "Danışan" : userRole === "dietitian" ? "Diyetisyen" : "Yönetici"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-slate-600 transition-colors hover:bg-white/90"
          >
            <LogOut size={18} />
            <span className="font-medium">Çıkış yap</span>
          </button>
        </div>
      </aside>

      <main className="min-h-0 min-w-0 flex-1 overflow-y-auto pb-[calc(5.25rem+env(safe-area-inset-bottom,0px))] lg:pb-0">
        <div
          className="sticky top-0 z-20 border-b border-slate-200/90 bg-white/95 px-4 py-3 shadow-sm shadow-slate-900/5 backdrop-blur-md lg:hidden"
          style={{ paddingTop: "max(0.75rem, env(safe-area-inset-top, 0px))" }}
        >
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-base font-bold text-emerald-500">NutriBridge</p>
              <p className="truncate text-xs text-slate-500" title={userName}>
                {userName}
              </p>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex min-h-10 min-w-10 items-center justify-center rounded-lg border border-slate-200 text-slate-500"
              aria-label="Çıkış yap"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
        {children}
      </main>

      {mobileNav ? createPortal(mobileNav, document.body) : null}
    </div>
  );
}
