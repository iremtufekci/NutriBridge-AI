// Rol tabanlı kenar çubuğu ve mobil alt menü ile ana uygulama kabuğu
import { ReactNode, useCallback, useMemo, useState } from "react";
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
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

// Kenar çubuğu daraltma tercihi localStorage anahtarı
const SIDEBAR_STORAGE_KEY = "nutribridge.sidebar.collapsed";

// Kabuk bileşeni özellikleri: içerik, rol ve kullanıcı adı
interface SidebarProps {
  children: ReactNode;
  userRole: "admin" | "dietitian" | "client";
  userName: string;
}

// Tarayıcıdan kenar çubuğu daraltılmış mı bilgisini okur
function readCollapsedPreference(): boolean {
  try {
    return localStorage.getItem(SIDEBAR_STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

export function SidebarLayout({ children, userRole, userName }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(readCollapsedPreference);

  // Daralt/genişlet ve tercihi kalıcı olarak kaydet
  const toggleSidebar = useCallback(() => {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(SIDEBAR_STORAGE_KEY, next ? "1" : "0");
      } catch {
        /* ignore */
      }
      return next;
    });
  }, []);

  // Oturum bilgilerini temizleyip giriş sayfasına yönlendir
  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userName");
    navigate("/login");
  };

  // Her rol için menü öğeleri (etiket, ikon, rota)
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
        { label: "Anasayfa", icon: Home, path: "/dietitian/dashboard" },
        { label: "Görevlerim", icon: ListTodo, path: "/dietitian/tasks" },
        { label: "Danışanlarım", icon: Users, path: "/dietitian/clients" },
        { label: "Diyet Programları", icon: BookOpen, path: "/dietitian/programs" },
        { label: "Yemek analizi", icon: ScanSearch, path: "/dietitian/meal-analysis" },
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

  // Aktif sayfa vurgusu: tam eşleşme veya kısa yol alias'ları
  const pathIsActive = (path: string) => {
    if (location.pathname === path) return true;
    if (path === "/client/home" && location.pathname === "/client") return true;
    if (path === "/dietitian/clients" && location.pathname.startsWith("/dietitian/clients")) return true;
    if (path === "/dietitian/dashboard" && location.pathname === "/dietitian") return true;
    if (path === "/admin/dashboard" && location.pathname === "/admin") return true;
    return false;
  };

  const roleLabel =
    userRole === "client" ? "Danışan paneli" : userRole === "dietitian" ? "Diyetisyen paneli" : "Yönetim paneli";

  const activePageLabel =
    currentMenu.find((item) => pathIsActive(item.path))?.label ?? "Panel";

  const showMobileBar = userRole === "client" || userRole === "dietitian" || userRole === "admin";

  // Mobil: alt sabit menü (portal ile body'ye render)
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
      {/* Masaüstü: sol kenar çubuğu */}
      <aside
        className={[
          "relative hidden shrink-0 flex-col border-r border-slate-200/90 bg-gradient-to-b from-white via-slate-50 to-slate-100/90 transition-[width] duration-300 ease-in-out lg:flex",
          collapsed ? "w-[4.75rem]" : "w-[17rem]",
        ].join(" ")}
      >
        <button
          type="button"
          onClick={toggleSidebar}
          aria-label={collapsed ? "Kenar çubuğunu genişlet" : "Kenar çubuğunu daralt"}
          className="absolute -right-3 top-7 z-30 flex h-7 w-7 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 shadow-md transition-colors hover:border-[#2ECC71]/40 hover:text-[#2ECC71]"
        >
          {collapsed ? <ChevronRight size={14} strokeWidth={2.5} /> : <ChevronLeft size={14} strokeWidth={2.5} />}
        </button>

        <div className={`border-b border-slate-200/90 ${collapsed ? "px-2 py-4" : "px-5 py-5"}`}>
          {collapsed ? (
            <div className="flex justify-center">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#2ECC71] text-sm font-bold text-white shadow-sm shadow-emerald-600/20">
                N
              </div>
            </div>
          ) : (
            <div className="min-w-0 pr-2">
              <h2 className="text-xl font-bold tracking-tight text-slate-900">
                <span className="text-[#2ECC71]">NutriBridge</span>{" "}
                <span className="text-slate-800">AI</span>
              </h2>
              <p className="mt-0.5 text-[11px] font-medium uppercase tracking-wide text-slate-500">
                Akıllı beslenme platformu
              </p>
              <p className="mt-2 text-xs text-slate-600">{roleLabel}</p>
            </div>
          )}
        </div>

        <nav className={`flex-1 space-y-1 ${collapsed ? "p-2" : "p-3"}`}>
          {currentMenu.map((item) => {
            const Icon = item.icon;
            const isActive = pathIsActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                title={collapsed ? item.label : undefined}
                className={[
                  "flex items-center rounded-xl text-[15px] transition-colors",
                  collapsed ? "justify-center px-2 py-2.5" : "gap-3 px-3.5 py-2.5",
                  isActive
                    ? "bg-[#2ECC71] font-semibold text-white shadow-sm shadow-emerald-600/20"
                    : "text-slate-700 hover:bg-white/90 hover:shadow-sm",
                ].join(" ")}
              >
                <Icon
                  size={18}
                  className={`shrink-0 ${isActive ? "text-white" : "opacity-90"}`}
                  strokeWidth={isActive ? 2.25 : 2}
                />
                {!collapsed ? <span className="truncate font-medium">{item.label}</span> : null}
              </Link>
            );
          })}
        </nav>

        <div className={`border-t border-slate-200/90 ${collapsed ? "p-2" : "p-4"}`}>
          {collapsed ? (
            <div className="flex flex-col items-center gap-2">
              <div
                className="flex h-9 w-9 items-center justify-center rounded-full bg-[#2ECC71] text-sm font-bold text-white"
                title={userName}
              >
                {(userName && userName.length > 0 ? userName.charAt(0) : "?").toUpperCase()}
              </div>
              <button
                type="button"
                onClick={handleLogout}
                title="Çıkış yap"
                className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 transition-colors hover:bg-white/90"
              >
                <LogOut size={18} />
              </button>
            </div>
          ) : (
            <>
              <div className="mb-4 flex items-center gap-3 rounded-xl border border-slate-200/80 bg-white/80 px-3 py-2.5 shadow-sm">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#2ECC71] text-sm font-bold text-white">
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
            </>
          )}
        </div>
      </aside>

      {/* Ana içerik alanı */}
      <main className="min-h-0 min-w-0 flex-1 overflow-y-auto pb-[calc(5.25rem+env(safe-area-inset-bottom,0px))] lg:pb-0">
        {/* Mobil üst başlık çubuğu */}
        <div
          className="sticky top-0 z-20 border-b border-slate-200/90 bg-white/95 px-4 py-3 shadow-sm shadow-slate-900/5 backdrop-blur-md lg:hidden"
          style={{ paddingTop: "max(0.75rem, env(safe-area-inset-top, 0px))" }}
        >
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-base font-bold text-[#2ECC71]">NutriBridge</p>
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

        {/* Masaüstü: sayfa başlığı ve breadcrumb */}
        <div className="hidden lg:flex sticky top-0 z-10 h-12 items-center gap-3 border-b border-slate-200/80 bg-white/75 px-6 backdrop-blur-md">
          <button
            type="button"
            onClick={toggleSidebar}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200/90 text-slate-500 transition-colors hover:bg-slate-50 hover:text-[#2ECC71]"
            aria-label={collapsed ? "Kenar çubuğunu genişlet" : "Kenar çubuğunu daralt"}
          >
            {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
          </button>
          <span className="text-sm font-medium text-slate-700">{activePageLabel}</span>
          <span className="text-slate-300">/</span>
          <span className="text-sm text-slate-500">{roleLabel}</span>
        </div>

        {children}
      </main>

      {mobileNav ? createPortal(mobileNav, document.body) : null}
    </div>
  );
}
