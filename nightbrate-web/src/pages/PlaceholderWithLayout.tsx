import { Link } from "react-router-dom"; // Panele dönüş linki
import { SidebarLayout } from "../components/SidebarLayout"; // Rol bazlı kenar çubuğu
import { useAuthProfileDisplayName } from "../hooks/useAuthProfileDisplayName"; // Kullanıcı adı kancası

// Her rol için varsayılan panel yolu
const dashboardPath: Record<"admin" | "dietitian" | "client", string> = {
  admin: "/admin/dashboard",
  dietitian: "/dietitian/dashboard",
  client: "/client/home",
};

// localStorage'dan geçerli kullanıcı rolünü oku
function resolveRole(): "admin" | "dietitian" | "client" {
  const r = localStorage.getItem("userRole")?.toLowerCase();
  if (r === "admin" || r === "dietitian" || r === "client") return r;
  return "client"; // Bilinmeyen rol için danışan varsay
}

// Henüz tamamlanmamış sayfalar için yer tutucu düzen
export function PlaceholderWithLayout() {
  const userRole = resolveRole(); // Aktif rol
  const userName = useAuthProfileDisplayName(); // Gösterim adı

  return (
    <SidebarLayout userRole={userRole} userName={userName}>
      <div className="mx-auto flex min-h-[min(60vh,28rem)] max-w-md flex-col items-center justify-center px-4 py-8 text-center md:min-h-[40vh]">
        {/* Bilgilendirme başlığı */}
        <h1 className="mb-2 text-lg font-semibold text-slate-800">
          Sayfa hazırlanıyor
        </h1>
        {/* Kullanıcıyı menüye yönlendiren açıklama */}
        <p className="mb-6 text-sm text-slate-500">
          Bu bölüm yakında aktif olacak. Alttaki menüden diğer sayfalara gidebilir veya panele dönebilirsiniz.
        </p>
        {/* Role göre ana panele dönüş */}
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
