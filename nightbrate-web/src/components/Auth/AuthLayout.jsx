import { Link } from "react-router-dom";
import { ChefHat, FileText, Sparkles, Users } from "lucide-react";

const features = [
  { icon: Sparkles, text: "AI destekli öğün ve belge analizi" },
  { icon: ChefHat, text: "Kişiselleştirilmiş diyet programları" },
  { icon: Users, text: "Diyetisyen–danışan eşleştirme" },
  { icon: FileText, text: "PDF rapor özeti ve paylaşım" },
];

export function AuthLayout({ children, footer, wide = false }) {
  return (
    <div className="flex min-h-svh w-full flex-col bg-white font-[family-name:var(--font-inter,Inter),system-ui,sans-serif]">
      {/* Üst web navigasyonu */}
      <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-[1400px] items-center px-5 sm:px-8 lg:px-10">
          <Link to="/login" className="flex items-center gap-2.5 no-underline">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#2ECC71] text-sm font-bold text-white">
              N
            </span>
            <span className="text-lg font-bold tracking-tight text-slate-900">NutriBridge</span>
          </Link>
        </div>
      </header>

      <div className="flex flex-1">
        {/* Sol marka paneli — tabletten itibaren görünür */}
        <aside className="relative hidden w-[48%] shrink-0 overflow-hidden bg-gradient-to-br from-[#2ECC71] via-[#27AE60] to-[#186a3b] md:flex md:flex-col md:justify-between md:px-10 md:py-12 lg:px-14 lg:py-16 xl:px-16">
          <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
          <div className="pointer-events-none absolute bottom-0 left-0 h-48 w-full bg-gradient-to-t from-black/10 to-transparent" />

          <div className="relative mt-4">
            <p className="text-sm font-medium uppercase tracking-widest text-white/75">Platform</p>
            <h2 className="mt-3 max-w-lg text-3xl font-semibold leading-tight tracking-tight text-white lg:text-[2.35rem] lg:leading-[1.15]">
              Beslenme takibini web panelinden yönetin
            </h2>
            <p className="mt-5 max-w-md text-base leading-relaxed text-white/90">
              Danışanlar, diyetisyenler ve yöneticiler aynı altyapıda; program, analiz ve raporlar tek
              yerde.
            </p>
          </div>

          <ul className="relative my-10 grid gap-3 lg:grid-cols-2 lg:gap-4">
            {features.map(({ icon: Icon, text }) => (
              <li
                key={text}
                className="flex items-start gap-3 rounded-xl border border-white/15 bg-white/10 px-4 py-3.5 backdrop-blur-sm"
              >
                <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-white/20">
                  <Icon className="h-4 w-4 text-white" strokeWidth={2.25} />
                </span>
                <span className="text-sm leading-snug text-white/95">{text}</span>
              </li>
            ))}
          </ul>

          <p className="relative text-xs text-white/65">
            © {new Date().getFullYear()} NutriBridge · Akıllı Beslenme Platformu
          </p>
        </aside>

        {/* Sağ form paneli — kart değil, düz web sayfası */}
        <main className="flex flex-1 flex-col bg-slate-50/80">
          <div
            className={`mx-auto flex w-full flex-1 flex-col justify-center px-5 py-10 sm:px-10 lg:px-16 xl:px-20 ${
              wide ? "max-w-3xl" : "max-w-xl"
            }`}
          >
            {children}
          </div>

          {footer ? (
            <div className="border-t border-slate-200/80 bg-white px-5 py-4 text-center sm:px-10">
              <p className="text-xs text-slate-500 sm:text-sm">{footer}</p>
            </div>
          ) : null}
        </main>
      </div>
    </div>
  );
}

export function AuthPageHeader({ title, subtitle }) {
  return (
    <header className="mb-8 border-b border-slate-200 pb-8 sm:mb-10">
      <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-[2rem]">{title}</h1>
      {subtitle && <p className="mt-2 text-base text-slate-500">{subtitle}</p>}
    </header>
  );
}

export function AuthField({ id, label, children, hint }) {
  return (
    <div className="space-y-2">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      {children}
      {hint && <p className="text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

export function AuthError({ message }) {
  if (!message) return null;
  return (
    <div
      role="alert"
      className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
    >
      {message}
    </div>
  );
}

export function AuthSuccess({ message }) {
  if (!message) return null;
  return (
    <div
      role="status"
      className="mb-6 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
    >
      {message}
    </div>
  );
}

export function AuthFooterLinks() {
  return (
    <div className="mt-10 flex flex-col gap-4 border-t border-slate-200 pt-8 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-slate-600">
        Hesabınız yok mu?{" "}
        <Link to="/register-client" className="font-semibold text-[#2ECC71] hover:underline">
          Danışan kaydı
        </Link>
      </p>
      <p className="text-sm text-slate-600">
        Diyetisyen misiniz?{" "}
        <Link to="/register-dietitian" className="font-semibold text-[#2ECC71] hover:underline">
          Kayıt olun
        </Link>
      </p>
    </div>
  );
}

export const authInputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-[15px] text-slate-900 shadow-sm placeholder:text-slate-400 outline-none transition-all focus:border-[#2ECC71] focus:ring-2 focus:ring-[#2ECC71]/20";

export const authSelectClass =
  "w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-[15px] text-slate-900 shadow-sm outline-none transition-all focus:border-[#2ECC71] focus:ring-2 focus:ring-[#2ECC71]/20";

export const authBtnPrimaryClass =
  "inline-flex w-full items-center justify-center rounded-lg bg-[#2ECC71] px-5 py-3 text-[15px] font-semibold text-white shadow-sm transition-all hover:bg-[#27AE60] focus:outline-none focus:ring-2 focus:ring-[#2ECC71]/40 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60";

export const authBtnSecondaryClass =
  "inline-flex w-full items-center justify-center rounded-lg border border-slate-300 bg-white px-5 py-3 text-[15px] font-semibold text-slate-700 shadow-sm transition-all hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-200";
