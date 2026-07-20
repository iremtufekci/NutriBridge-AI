import { ReactNode } from "react"; // Alt bileşen ve rozet tipleri

// Admin sayfa kabuğu özellikleri
type AdminPageShellProps = {
  title: string; // Sayfa başlığı
  subtitle?: string; // Alt açıklama
  badge?: ReactNode; // Sağ üst rozet (ör. bekleyen sayısı)
  children: ReactNode; // Sayfa içeriği
  maxWidth?: "default" | "narrow" | "wide"; // İçerik genişliği
};

// Genişlik sınıfı eşlemesi
const maxWidthClass = {
  default: "max-w-[1400px]",
  narrow: "max-w-2xl",
  wide: "max-w-7xl",
};

/** Yönetim sayfalarında tutarlı üst başlık ve içerik alanı. */
export function AdminPageShell({
  title,
  subtitle,
  badge,
  children,
  maxWidth = "default",
}: AdminPageShellProps) {
  return (
    <div className="min-h-full bg-gradient-to-b from-slate-50 to-slate-100/80 px-4 py-6 pb-24 text-slate-900 sm:px-6 lg:px-8 lg:py-8 lg:pb-8">
      <div className={`mx-auto space-y-6 ${maxWidthClass[maxWidth]}`}>
        {/* Üst başlık satırı */}
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-widest text-[#2ECC71]">
              Yönetim paneli
            </p>
            <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              {title}
            </h1>
            {subtitle ? <p className="mt-1.5 text-sm text-slate-500 sm:text-base">{subtitle}</p> : null}
          </div>
          {badge ? <div className="shrink-0">{badge}</div> : null}
        </header>
        {/* Sayfa özel içeriği */}
        {children}
      </div>
    </div>
  );
}

// Yönetim kartları için ortak Tailwind sınıfı
export const adminCardClass =
  "rounded-2xl border border-slate-200/90 bg-white shadow-sm shadow-slate-200/50 ring-1 ring-slate-100/80";
