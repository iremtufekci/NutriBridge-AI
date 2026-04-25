import { ReactNode, useEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { 
  LayoutDashboard, Users, Settings, Home,
  ChefHat, LogOut, CalendarDays,
  ScanSearch, User, Moon, Sun, BookOpen, BarChart3, AlertTriangle, ClipboardCheck
} from "lucide-react";
import { api } from "../api/http";

interface SidebarProps {
  children: ReactNode;
  userRole: "admin" | "dietitian" | "client";
  userName: string;
}

/** 1024px altı: telefon + küçük tablet; yatay telefonda "md" eşiği (768) çubuğu yanlışlıkla gizlemesin diye piksel tabanlı. */
const MOBILE_BAR_MQ = "(max-width: 1023px)";

function subscribeMobileBar(mq: MediaQueryList, onChange: () => void) {
  mq.addEventListener("change", onChange);
  return () => mq.removeEventListener("change", onChange);
}

function useIsCompactViewport() {
  return useSyncExternalStore(
    (onStoreChange) => {
      if (typeof window === "undefined") {
        return () => {};
      }
      const mq = window.matchMedia(MOBILE_BAR_MQ);
      return subscribeMobileBar(mq, onStoreChange);
    },
    () =>
      typeof window !== "undefined" ? window.matchMedia(MOBILE_BAR_MQ).matches : false,
    () => false
  );
}

export function SidebarLayout({ children, userRole, userName }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [isDark, setIsDark] = useState(() => localStorage.getItem("theme") === "dark");
  const mountedRef = useRef(false);
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

    if (userRole === "client") {
      api.post("/api/client/theme", { themePreference: isDark ? "dark" : "light" }).catch(() => {
        // Tema kaydi basarisiz olsa da UI akisini kesmiyoruz.
      });
    }
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
      { label: "Danışanlarım", icon: Users, path: "/dietitian/clients" },
      { label: "Diyet Programları", icon: BookOpen, path: "/dietitian/programs" },
      { label: "AI Denetimi", icon: BarChart3, path: "/dietitian/ai-review" },
      { label: "Kritik Uyarılar", icon: AlertTriangle, path: "/dietitian/alerts" },
      { label: "Profil", icon: User, path: "/dietitian/profile" }
    ],
    client: [
      { label: "Ana Sayfa", icon: Home, path: "/client/home" },
      { label: "Diyet Programım", icon: CalendarDays, path: "/client/journal" },
      { label: "Yemek Analizi", icon: ScanSearch, path: "/client/food-scan" },
      { label: "AI Mutfak Şefi", icon: ChefHat, path: "/client/ai-chef" },
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
      className="md:hidden fixed bottom-0 left-0 right-0 z-[100] w-full max-w-full select-none border-t border-zinc-700/90 bg-zinc-950 text-zinc-400 shadow-[0_-4px_24px_rgba(0,0,0,0.45)]"
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
                "active:bg-zinc-800/80",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-500",
                isActive
                  ? "text-emerald-400"
                  : "text-zinc-400"
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
                  isActive ? "text-emerald-400 font-semibold" : "text-inherit"
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
    <div className="flex w-full min-w-0 min-h-svh flex-1 flex-col bg-[#F8FAF7] text-slate-900 transition-colors md:min-h-screen md:flex-row dark:bg-slate-950 dark:text-slate-100">
      <aside className="hidden md:flex w-64 border-r border-slate-200 bg-white dark:bg-slate-900 dark:border-slate-800 flex-col">
        <div className="p-6 border-b border-slate-200 dark:border-slate-800">
          <h2 className="text-3xl font-bold text-emerald-500">NutriBridge</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
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
                    ? "bg-emerald-500 text-white font-semibold"
                    : "hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300"
                }`}
              >
                <Icon size={18} />
                <span className="font-medium text-[15px]">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-9 h-9 rounded-full bg-emerald-500 text-white flex items-center justify-center text-sm font-bold">
              {(userName && userName.length > 0 ? userName.charAt(0) : "?").toUpperCase()}
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">{userName}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {userRole === "client" ? "Danışan" : userRole === "dietitian" ? "Diyetisyen" : "Admin"}
              </p>
            </div>
          </div>
          <button
            onClick={() => setIsDark((prev) => !prev)}
            className="mb-2 flex items-center gap-3 px-3 py-2.5 w-full text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
          >
            {isDark ? <Sun size={18} /> : <Moon size={18} />}
            <span className="font-medium">{isDark ? "Gündüz Modu" : "Gece Modu"}</span>
          </button>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 w-full text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
          >
            <LogOut size={18} />
            <span className="font-medium">Çıkış Yap</span>
          </button>
        </div>
      </aside>

      <main className="min-h-0 flex-1 overflow-y-auto pb-[calc(5.25rem+env(safe-area-inset-bottom,0px))] md:pb-0">
        <div className="md:hidden sticky top-0 z-20 bg-white/95 dark:bg-slate-900/95 backdrop-blur border-b border-slate-200 dark:border-slate-800 px-4 py-3">
          <div className="flex items-center justify-between">
            <p className="text-base font-bold text-emerald-500">NutriBridge</p>
            <button
              onClick={() => setIsDark((prev) => !prev)}
              className="p-2 rounded-lg text-slate-500 dark:text-slate-300 border border-slate-200 dark:border-slate-700"
            >
              {isDark ? <Sun size={16} /> : <Moon size={16} />}
            </button>
          </div>
        </div>
        {children}
      </main>

      {mobileNav ? createPortal(mobileNav, document.body) : null}
    </div>
  );
}