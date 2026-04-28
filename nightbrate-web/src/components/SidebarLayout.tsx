import { ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { 
  LayoutDashboard, Users, Settings, Home,
  ChefHat, LogOut, CalendarDays,
  ScanSearch, User,   Moon, Sun, BookOpen,   BarChart3, AlertTriangle, ClipboardCheck, History, Share2, ListTodo
} from "lucide-react";
import { api } from "../api/http";

interface SidebarProps {
  children: ReactNode;
  userRole: "admin" | "dietitian" | "client";
  userName: string;
}

export function SidebarLayout({ children, userRole, userName }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [isDark, setIsDark] = useState(
    () =>
      (typeof localStorage !== "undefined" && localStorage.getItem("theme") === "dark") ||
      (typeof document !== "undefined" && document.documentElement.classList.contains("dark"))
  );
  const mountedRef = useRef(false);

  useEffect(() => {
    const onTheme = (e: Event) => {
      const d = (e as CustomEvent<{ isDark: boolean }>).detail?.isDark;
      if (typeof d === "boolean") setIsDark(d);
    };
    window.addEventListener("nightbrate-theme", onTheme);
    return () => window.removeEventListener("nightbrate-theme", onTheme);
  }, []);
  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userName");
    navigate("/login");
  };


  useEffect(() => {
    document.documentElement.classList.toggle("dark", isDark);
    localStorage.setItem("theme", isDark ? "dark" : "light");

    if (!mountedRef.current) {
      mountedRef.current = true;
      return;
    }

    api.post("/api/Auth/theme", { themePreference: isDark ? "dark" : "light" }).catch(() => {
      // Tema kaydi basarisiz olsa da UI akisini kesmiyoruz.
    });
  }, [isDark]);

  const menuConfig = useMemo(() => ({
    admin: [
      { label: "Dashboard", icon: LayoutDashboard, path: "/admin/dashboard" },
      { label: "Kullanıcı Yönetimi", icon: Users, path: "/admin/users" },
      { label: "Diyetisyen Onayları", icon: ClipboardCheck, path: "/admin/approvals" },
      { label: "Sistem Analitiği", icon: BarChart3, path: "/admin/analytics" },
      { label: "Ayarlar", icon: Settings, path: "/admin/settings" }
    ],
    dietitian: [
      { label: "Dashboard", icon: LayoutDashboard, path: "/dietitian/dashboard" },
      { label: "Görevlerim", icon: ListTodo, path: "/dietitian/tasks" },
      { label: "Danışanlarım", icon: Users, path: "/dietitian/clients" },
      { label: "Diyet Programları", icon: BookOpen, path: "/dietitian/programs" },
      { label: "AI Denetimi", icon: BarChart3, path: "/dietitian/ai-review" },
      { label: "Kritik Uyarılar", icon: AlertTriangle, path: "/dietitian/alerts" },
      { label: "Profil", icon: User, path: "/dietitian/profile" }
    ],
    client: [
      { label: "Ana Sayfa", icon: Home, path: "/client/home" },
      { label: "Diyet Programım", icon: CalendarDays, path: "/client/diet-program" },
      { label: "Geçmiş diyetlerim", icon: History, path: "/client/diet-program-history" },
      { label: "Yemek Analizi", icon: ScanSearch, path: "/client/food-scan" },
      { label: "AI Mutfak Şefi", icon: ChefHat, path: "/client/ai-chef" },
      { label: "Paylaştığım tarifler", icon: Share2, path: "/client/ai-chef-shares" },
      { label: "Profilim", icon: User, path: "/client/profile" }
    ]
  }), []);

  const currentMenu = menuConfig[userRole];

  const pathIsActive = (path: string) => {
    if (location.pathname === path) return true;
    if (path === "/client/home" && location.pathname === "/client") return true;
    if (path === "/dietitian/dashboard" && location.pathname === "/dietitian") return true;
    if (path === "/admin/dashboard" && location.pathname === "/admin") return true;
    return false;
  };

  const showMobileBar = userRole === "client" || userRole === "dietitian" || userRole === "admin";

  const mobileNav = showMobileBar && (
    <nav
      className="lg:hidden fixed bottom-0 left-0 right-0 z-[100] w-full max-w-full select-none border-t border-slate-200 bg-white text-slate-500 shadow-[0_-4px_24px_rgba(0,0,0,0.08)] dark:border-[#374151] dark:bg-[#1A202C] dark:text-[#9CA3AF] dark:shadow-[0_-4px_24px_rgba(0,0,0,0.45)]"
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
                "active:bg-slate-100 dark:active:bg-[#2D3748]/80",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2ECC71]",
                isActive
                  ? "text-[#2ECC71]"
                  : "text-slate-500 dark:text-[#9CA3AF]"
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
                  manyItems ? "text-[9px] line-clamp-2" : "text-[10px] sm:text-[11px] line-clamp-2",
                  isActive ? "text-[#2ECC71] font-semibold" : "text-inherit"
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
    <div className="flex w-full min-w-0 min-h-svh flex-1 flex-col bg-[#F8FAF7] text-slate-900 transition-colors lg:min-h-screen lg:flex-row dark:bg-slate-950 dark:text-slate-100">
      <aside className="hidden lg:flex w-64 shrink-0 border-r border-slate-200 bg-white dark:bg-[#1A202C] dark:border-[#374151] flex-col">
        <div className="p-6 border-b border-slate-200 dark:border-[#374151]">
          <h2 className="text-3xl font-bold text-emerald-500 dark:text-[#2ECC71]">NutriBridge</h2>
          <p className="text-xs text-slate-500 dark:text-[#9CA3AF] mt-1">
            {userRole === "client" ? "Danışan Paneli" : userRole === "dietitian" ? "Diyetisyen Paneli" : "Yönetim Paneli"}
          </p>
        </div>

        <nav className="flex-1 p-3 space-y-1.5">
          {currentMenu.map((item) => {
            const Icon = item.icon;
            const isActive = pathIsActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${
                  isActive 
                    ? "bg-[#2ECC71] text-white font-semibold"
                    : "hover:bg-slate-100 dark:hover:bg-[#2D3748] text-slate-700 dark:text-slate-300"
                }`}
              >
                <Icon size={18} />
                <span className="font-medium text-[15px]">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-200 dark:border-[#374151]">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-9 h-9 rounded-full bg-[#2ECC71] text-white flex items-center justify-center text-sm font-bold">
              {(userName && userName.length > 0 ? userName.charAt(0) : "?").toUpperCase()}
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">{userName}</p>
              <p className="text-xs text-slate-500 dark:text-[#9CA3AF]">
                {userRole === "client" ? "Danışan" : userRole === "dietitian" ? "Diyetisyen" : "Admin"}
              </p>
            </div>
          </div>
          <button
            onClick={() => setIsDark((prev) => !prev)}
            className="mb-2 flex items-center gap-3 px-3 py-2.5 w-full text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-[#2D3748] rounded-lg transition-colors"
          >
            {isDark ? <Sun size={18} /> : <Moon size={18} />}
            <span className="font-medium">{isDark ? "Gündüz Modu" : "Gece Modu"}</span>
          </button>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 w-full text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-[#2D3748] rounded-lg transition-colors"
          >
            <LogOut size={18} />
            <span className="font-medium">Çıkış Yap</span>
          </button>
        </div>
      </aside>

      <main className="min-h-0 min-w-0 flex-1 overflow-y-auto pb-[calc(5.25rem+env(safe-area-inset-bottom,0px))] lg:pb-0">
        <div
          className="lg:hidden sticky top-0 z-20 border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur dark:border-[#374151] dark:bg-[#1A202C]/95"
          style={{ paddingTop: "max(0.75rem, env(safe-area-inset-top, 0px))" }}
        >
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-base font-bold text-emerald-500 dark:text-[#2ECC71]">NutriBridge</p>
              <p className="truncate text-xs text-slate-500 dark:text-[#9CA3AF]" title={userName}>
                {userName}
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-1.5">
              <button
                type="button"
                onClick={() => setIsDark((prev) => !prev)}
                className="inline-flex min-h-10 min-w-10 items-center justify-center rounded-lg border border-slate-200 text-slate-500 dark:border-slate-700 dark:text-slate-300"
                aria-label={isDark ? "Gündüz modu" : "Gece modu"}
              >
                {isDark ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex min-h-10 min-w-10 items-center justify-center rounded-lg border border-slate-200 text-slate-500 dark:border-slate-700 dark:text-slate-300"
                aria-label="Çıkış yap"
              >
                <LogOut size={18} />
              </button>
            </div>
          </div>
        </div>
        {children}
      </main>

      {mobileNav ? createPortal(mobileNav, document.body) : null}
    </div>
  );
}