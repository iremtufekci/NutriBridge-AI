import { ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { 
  LayoutDashboard, Users, Settings, Home, 
  MessageSquare, Book, ChefHat, LogOut 
} from "lucide-react";

interface SidebarProps {
  children: ReactNode;
  userRole: "admin" | "dietitian" | "client";
  userName: string;
}

export function SidebarLayout({ children, userRole, userName }: SidebarProps) {
  const location = useLocation();

  // Rol bazlı menü seçeneklerini tanımlıyoruz
  const menuConfig = {
    admin: [
      { label: "Dashboard", icon: LayoutDashboard, path: "/admin/dashboard" },
      { label: "Kullanıcılar", icon: Users, path: "/admin/users" },
      { label: "Sistem Ayarları", icon: Settings, path: "/admin/settings" }
    ],
    dietitian: [
      { label: "Genel Bakış", icon: Home, path: "/dietitian/dashboard" },
      { label: "Danışanlarım", icon: Users, path: "/dietitian/clients" },
      { label: "Mesajlar", icon: MessageSquare, path: "/dietitian/messages" }
    ],
    client: [
      { label: "Ana Sayfa", icon: Home, path: "/client/home" },
      { label: "Günlüğüm", icon: Book, path: "/client/journal" },
      { label: "AI Şef", icon: ChefHat, path: "/client/ai-chef" }
    ]
  };

  const currentMenu = menuConfig[userRole];

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* SOL MENÜ (SIDEBAR) */}
      <aside className="w-64 border-r border-border bg-card flex flex-col">
        <div className="p-6 border-b border-border">
          <h2 className="text-xl font-bold text-primary">NutriBridge AI</h2>
          <p className="text-xs text-muted-foreground mt-1">{userName}</p>
        </div>

        <nav className="flex-1 p-4 space-y-2">
          {currentMenu.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  isActive 
                    ? "bg-primary text-primary-foreground" 
                    : "hover:bg-muted text-muted-foreground"
                }`}
              >
                <Icon size={20} />
                <span className="font-medium">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-border">
          <button className="flex items-center gap-3 px-4 py-3 w-full text-destructive hover:bg-destructive/10 rounded-lg transition-colors">
            <LogOut size={20} />
            <span className="font-medium">Çıkış Yap</span>
          </button>
        </div>
      </aside>

      {/* SAĞ İÇERİK ALANI */}
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  );
}